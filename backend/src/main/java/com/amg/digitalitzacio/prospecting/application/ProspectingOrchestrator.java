package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.prospecting.api.dto.*;
import com.amg.digitalitzacio.prospecting.domain.*;
import org.springframework.web.multipart.MultipartFile;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import com.amg.digitalitzacio.shared.exception.ConflictException;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProspectingOrchestrator implements ProspectingService {

    private final ProspectCampaignRepository campaignRepository;
    private final ProspectRepository prospectRepository;
    private final ProspectScraper scraper;
    private final LeadService leadService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final AIProviderRouter aiProviderRouter;
    private final WebAnalyzerService webAnalyzerService;
    private final ProspectAnalysisService prospectAnalysisService;
    private final WidgetCodeGeneratorService widgetCodeGeneratorService;
    private final ProspectChannelDetectorService channelDetectorService;
    private final ProspectDemoGeneratorService prospectDemoGeneratorService;
    private final ProspectMessageGeneratorService prospectMessageGeneratorService;
    private final ProspectCsvImportService prospectCsvImportService;

    private static final int TIER_PRIORITY = 81;
    private static final int TIER_DEMO = 61;
    private static final int TIER_REVIEW = 31;

    private static final Set<String> PROFITABLE_SECTORS = Set.of(
        "FISIOTERAPEUTA", "PSICOLEG", "NUTRICIONISTA", "PERRUQUERIA", "ESTETICA",
        "PERRUQUERIA_CANINA", "VETERINARI", "RESTAURANTE", "DENTISTA",
        "MARE_DE_DIA", "CLINICA", "ACADEMIA"
    );

    // Indicadors de retail/venda de productes — no són clients d'AMG (serveis)
    private static final Set<String> RETAIL_KEYWORDS = Set.of(
        "ferreteria", "ferreter", "bricolaje", "bricol", "manualidades",
        "materials de construcció", "materials construccio", "pintures i vernissos",
        "venda de pintures", "venda de materials", "home depot", "leroy merlin",
        "leroymerlin", "bauhaus", "aki ", " aki", "brico depot", "bricodepot",
        "cadena de", "grandes almacenes", "gran superficie", "supermercat",
        "distribuidor de", "distribuïdor de", "mayorista", "magatzem de",
        "almacén de", "shop", "store", "tienda de", "tenda de"
    );

    private static boolean isRetailBusiness(Prospect p) {
        String text = ((p.getName() != null ? p.getName() : "") + " " +
                       (p.getDescription() != null ? p.getDescription() : "")).toLowerCase();
        return RETAIL_KEYWORDS.stream().anyMatch(text::contains);
    }

    @Override
    @Transactional
    public CampaignResponse createCampaign(CreateCampaignRequest request, UUID createdBy) {
        var status = Boolean.TRUE.equals(request.scheduled()) ? CampaignStatus.SCHEDULED : CampaignStatus.DRAFT;
        var campaign = ProspectCampaign.builder()
                .name(request.name())
                .sector(request.sector())
                .location(request.location())
                .source(request.source())
                .status(status)
                .searchParams(request.searchParams())
                .notes(request.notes())
                .createdBy(createdBy)
                .scheduledNextRun(request.scheduledNextRun())
                .repeatIntervalDays(request.repeatIntervalDays())
                .build();
        campaign = campaignRepository.save(campaign);
        return toCampaignResponse(campaign);
    }

    @Override
    public CampaignResponse getCampaign(UUID campaignId) {
        var campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
        return toCampaignResponse(campaign);
    }

    @Override
    public Page<CampaignResponse> listCampaigns(String sector, String location, String status, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        CampaignStatus statusEnum = null;
        if (status != null) {
            try { statusEnum = CampaignStatus.valueOf(status.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        return campaignRepository.findFiltered(sector, location, statusEnum, pageable)
                .map(this::toCampaignResponse);
    }

    @Override
    @Transactional
    public CampaignResponse updateCampaign(UUID campaignId, UpdateCampaignRequest request) {
        var campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
        if (request.name() != null) campaign.setName(request.name());
        if (request.sector() != null) campaign.setSector(request.sector());
        if (request.location() != null) campaign.setLocation(request.location());
        if (request.source() != null) campaign.setSource(request.source());
        if (request.searchParams() != null) campaign.setSearchParams(request.searchParams());
        if (request.notes() != null) campaign.setNotes(request.notes());
        if (request.scheduled() != null) campaign.setStatus(request.scheduled() ? CampaignStatus.SCHEDULED : CampaignStatus.DRAFT);
        if (request.scheduledNextRun() != null) campaign.setScheduledNextRun(request.scheduledNextRun());
        if (request.repeatIntervalDays() != null) campaign.setRepeatIntervalDays(request.repeatIntervalDays());
        campaign = campaignRepository.save(campaign);
        return toCampaignResponse(campaign);
    }

    @Override
    @Transactional
    public void deleteCampaign(UUID campaignId) {
        campaignRepository.deleteById(campaignId);
    }

    @Override
    public CampaignRunResponse runCampaign(UUID campaignId) {
        var campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));

        campaign.setStatus(CampaignStatus.IN_PROGRESS);
        campaignRepository.save(campaign);

        // Parsejar searchParams (JSON) per passar maxResults al scraper
        var params = new HashMap<String, Object>();
        if (campaign.getSearchParams() != null && !campaign.getSearchParams().isBlank()) {
            try {
                params.putAll(objectMapper.readValue(campaign.getSearchParams(),
                    new TypeReference<Map<String, Object>>() {}));
            } catch (Exception e) {
                log.warn("Could not parse searchParams for campaign {}: {}", campaignId, e.getMessage());
            }
        }

        var prospects = scraper.search(campaign.getSector(), campaign.getLocation(), params, campaignId);

        // Deduplicació bulk: 2 queries IN en lloc de 2*N queries individuals
        var incomingPlaceIds = prospects.stream()
                .map(p -> p.getGooglePlaceId()).filter(id -> id != null).collect(Collectors.toSet());
        var incomingPhones = prospects.stream()
                .map(p -> p.getPhone()).filter(ph -> ph != null && !ph.isBlank()).collect(Collectors.toSet());

        var existingPlaceIds = incomingPlaceIds.isEmpty()
                ? java.util.Collections.<String>emptySet()
                : prospectRepository.findExistingPlaceIds(campaignId, incomingPlaceIds);
        var existingPhones = incomingPhones.isEmpty()
                ? java.util.Collections.<String>emptySet()
                : prospectRepository.findExistingPhones(campaignId, incomingPhones);

        var toSave = new java.util.ArrayList<Prospect>();
        int skipped = 0;
        for (var prospect : prospects) {
            if (prospect.getGooglePlaceId() != null && existingPlaceIds.contains(prospect.getGooglePlaceId())) {
                skipped++;
                continue;
            }
            if (prospect.getPhone() != null && !prospect.getPhone().isBlank()
                    && existingPhones.contains(prospect.getPhone())) {
                skipped++;
                continue;
            }
            toSave.add(prospect);
        }
        prospectRepository.saveAll(toSave);
        int count = toSave.size();
        log.info("Campaign {}: {} prospects saved, {} duplicates skipped", campaignId, count, skipped);

        campaign.setStatus(CampaignStatus.COMPLETED);
        campaign.setTotalFound(count);
        campaignRepository.save(campaign);

        return new CampaignRunResponse(campaignId, CampaignStatus.COMPLETED.name(), "0s");
    }

    @Override
    public List<ProspectResponse> listProspects(UUID campaignId, String status) {
        List<Prospect> prospects;
        if (status != null && !status.isBlank()) {
            prospects = prospectRepository.findByCampaignIdAndStatus(campaignId, ProspectStatus.valueOf(status.toUpperCase()));
        } else {
            prospects = prospectRepository.findByCampaignId(campaignId);
        }
        return prospects.stream().map(this::toProspectResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProspectResponse updateProspect(UUID prospectId, UpdateProspectRequest request) {
        var prospect = prospectRepository.findById(prospectId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospect not found: " + prospectId));
        if (request.status()  != null) prospect.setStatus(request.status());
        if (request.notes()   != null) prospect.setNotes(request.notes());
        if (request.name()    != null && !request.name().isBlank())    prospect.setName(request.name());
        if (request.phone()   != null) prospect.setPhone(request.phone().isBlank() ? null : request.phone());
        if (request.email()   != null) prospect.setEmail(request.email().isBlank() ? null : request.email());
        if (request.website() != null) {
            prospect.setWebsite(request.website().isBlank() ? null : request.website());
            prospect.setHasWebsite(!request.website().isBlank());
        }
        if (request.address() != null) prospect.setAddress(request.address().isBlank() ? null : request.address());
        if (request.city()    != null) prospect.setCity(request.city().isBlank() ? null : request.city());
        prospect.setScore(calculateScore(prospect));
        prospect = prospectRepository.save(prospect);
        return toProspectResponse(prospect);
    }

    @Override
    public ProspectResponse enrichProspect(UUID prospectId) {
        var prospect = prospectRepository.findById(prospectId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospect not found: " + prospectId));

        // Fase 2: obtenir telèfon i web via Place Details si no tenim telèfon
        if ((prospect.getPhone() == null || prospect.getPhone().isBlank()) && prospect.getGooglePlaceId() != null) {
            applyDetails(prospect, scraper.fetchDetails(prospect.getGooglePlaceId()));
        }

        // Netejar emails placeholder abans d'enriquir
        if (isPlaceholderEmail(prospect.getEmail())) prospect.setEmail(null);

        // Enriquiment web: extreure email, Instagram
        var enrichment = scraper.enrich(prospect.getName(), prospect.getWebsite(), prospect.getEmail());
        if (enrichment.email() != null && !enrichment.email().isBlank()) prospect.setEmail(enrichment.email());
        if (enrichment.website() != null && !enrichment.website().isBlank()) {
            prospect.setWebsite(enrichment.website());
            prospect.setHasWebsite(true);
        }
        if (enrichment.instagram() != null) prospect.setInstagram(enrichment.instagram());
        if (enrichment.hasWhatsapp() != null) prospect.setHasWhatsapp(enrichment.hasWhatsapp());

        prospect.setScore(calculateScore(prospect));
        prospect = prospectRepository.save(prospect);
        return toProspectResponse(prospect);
    }

    @Override
    public List<ProspectResponse> scoreProspects(UUID campaignId) {
        var prospects = prospectRepository.findByCampaignId(campaignId);
        for (var p : prospects) {
            p.setScore(calculateScore(p));
        }
        prospectRepository.saveAll(prospects);
        log.info("Scored {} prospects in campaign {}", prospects.size(), campaignId);
        return prospects.stream()
                .sorted(java.util.Comparator.comparingInt(p -> -(p.getScore() != null ? p.getScore() : 0)))
                .map(this::toProspectResponse)
                .collect(Collectors.toList());
    }

    @Override
    public int qualifyTop(UUID campaignId, int topN) {
        var candidates = prospectRepository.findByCampaignId(campaignId).stream()
                .filter(p -> p.getGooglePlaceId() != null)
                .filter(p -> p.getPhone() == null || p.getPhone().isBlank())
                .sorted(java.util.Comparator.comparingInt(p -> -(p.getScore() != null ? p.getScore() : 0)))
                .limit(topN)
                .toList();
        int qualified = fetchDetailsAndScoreAll(candidates);
        log.info("QualifyTop {}/{} prospects in campaign {}", qualified, candidates.size(), campaignId);
        return qualified;
    }

    @Override
    public int qualifyByMinScore(UUID campaignId, int minScore) {
        var candidates = prospectRepository.findByCampaignId(campaignId).stream()
                .filter(p -> p.getScore() != null && p.getScore() >= minScore)
                .filter(p -> p.getGooglePlaceId() != null)
                .filter(p -> p.getPhone() == null || p.getPhone().isBlank())
                .toList();
        int qualified = fetchDetailsAndScoreAll(candidates);
        log.info("QualifyByMinScore(>={}) {}/{} prospects in campaign {}", minScore, qualified, candidates.size(), campaignId);
        return qualified;
    }

    private int fetchDetailsAndScoreAll(List<Prospect> candidates) {
        int qualified = 0;
        for (var prospect : candidates) {
            try {
                applyDetails(prospect, scraper.fetchDetails(prospect.getGooglePlaceId()));
                prospect.setScore(calculateScore(prospect));
                prospectRepository.save(prospect);
                qualified++;
            } catch (Exception e) {
                log.debug("fetchDetailsAndScore failed for prospect {}: {}", prospect.getId(), e.getMessage());
            }
        }
        return qualified;
    }

    private void applyDetails(Prospect prospect, ProspectScraper.PlaceDetails details) {
        if (details.phone() != null) prospect.setPhone(details.phone());
        if (details.website() != null) { prospect.setWebsite(details.website()); prospect.setHasWebsite(true); }
        if (details.city() != null && (prospect.getCity() == null || prospect.getCity().isBlank())) prospect.setCity(details.city());
        if (details.postalCode() != null) prospect.setPostalCode(details.postalCode());
        if (details.description() != null && (prospect.getDescription() == null || prospect.getDescription().isBlank())) prospect.setDescription(details.description());
        if (!details.reviews().isEmpty()) {
            try { prospect.setReviewsJson(objectMapper.writeValueAsString(details.reviews())); }
            catch (Exception ignored) {}
        }
    }

    private static boolean isPlaceholderEmail(String email) {
        if (email == null || email.isBlank()) return false;
        String e = email.toLowerCase();
        return e.contains("example") || e.contains("noreply") || e.contains("no-reply")
            || e.contains("@test.") || e.contains("@domain.") || e.contains("@email.");
    }

    private int calculateScore(Prospect p) {
        int score = detectSignals(p).stream().mapToInt(ProspectSignal::points).sum();
        return Math.max(0, Math.min(100, score));
    }

    private void recalculateAndSave(Prospect p) {
        int score = calculateScore(p);
        p.setScore(score);
        p.setProspectTier(computeTier(score));
        prospectRepository.save(p);
    }

    @Override
    @Transactional
    public CampaignResponse cloneCampaign(UUID campaignId) {
        var original = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
        var clone = ProspectCampaign.builder()
                .name(original.getName() + " (còpia)")
                .sector(original.getSector())
                .location(original.getLocation())
                .source(original.getSource())
                .status(CampaignStatus.DRAFT)
                .searchParams(original.getSearchParams())
                .notes(original.getNotes())
                .createdBy(original.getCreatedBy())
                .build();
        clone = campaignRepository.save(clone);
        log.info("Cloned campaign {} → {}", campaignId, clone.getId());
        return toCampaignResponse(clone);
    }

    @Override
    public int enrichAllProspects(UUID campaignId) {
        var prospects = prospectRepository.findByCampaignId(campaignId).stream()
                .filter(p -> p.getStatus() != ProspectStatus.EXPORTED)
                .filter(p -> (p.getWebsite() == null || p.getWebsite().isBlank())
                          || (p.getEmail() == null || p.getEmail().isBlank())
                          || isPlaceholderEmail(p.getEmail()))
                .toList();
        int enriched = 0;
        for (var prospect : prospects) {
            try {
                // Netejar emails placeholder abans d'enriquir
                boolean changed = false;
                if (isPlaceholderEmail(prospect.getEmail())) {
                    prospect.setEmail(null);
                    changed = true;
                }

                var enrichment = scraper.enrich(prospect.getName(), prospect.getWebsite(), prospect.getEmail());
                if (enrichment.email() != null && !enrichment.email().isBlank()
                        && (prospect.getEmail() == null || prospect.getEmail().isBlank())) {
                    prospect.setEmail(enrichment.email());
                    changed = true;
                }
                if (enrichment.website() != null && !enrichment.website().isBlank()
                        && (prospect.getWebsite() == null || prospect.getWebsite().isBlank())) {
                    prospect.setWebsite(enrichment.website());
                    prospect.setHasWebsite(true);
                    changed = true;
                }
                if (changed) {
                    prospect.setScore(calculateScore(prospect));
                    prospectRepository.save(prospect);
                    enriched++;
                }
            } catch (Exception e) {
                log.debug("Enrich failed for prospect {}: {}", prospect.getId(), e.getMessage());
            }
        }
        log.info("Enriched {}/{} prospects in campaign {}", enriched, prospects.size(), campaignId);
        return enriched;
    }

    @Override
    @Transactional
    public LeadExportResponse exportProspect(UUID prospectId) {
        var prospect = prospectRepository.findById(prospectId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospect not found: " + prospectId));
        if (prospect.getStatus() == ProspectStatus.EXPORTED) {
            throw new ConflictException("Prospect already exported");
        }
        var campaign = campaignRepository.findById(prospect.getCampaignId())
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));
        UUID tenantId = null;
        if (campaign.getCreatedBy() != null) {
            tenantId = userRepository.findById(campaign.getCreatedBy())
                    .map(u -> u.getTenantId()).orElse(null);
        }
        var result = leadService.createLead(
                prospect.getName(), prospect.getEmail(), prospect.getPhone(),
                prospect.getWebsite(), prospect.getDescription(),
                prospect.getSource().name(), tenantId);

        prospect.setLeadId(result.leadId());
        prospect.setStatus(ProspectStatus.EXPORTED);
        prospectRepository.save(prospect);

        if (result.isNew()) {
            campaign.setTotalExported(campaign.getTotalExported() + 1);
            campaignRepository.save(campaign);
        }

        String status = result.isNew() ? ProspectStatus.EXPORTED.name() : "LINKED_EXISTING";
        return new LeadExportResponse(prospectId, result.leadId(), "/leads/" + result.leadId(), status);
    }

    @Override
    public int exportAllProspects(UUID campaignId) {
        var prospects = prospectRepository.findByCampaignIdAndStatus(campaignId, ProspectStatus.NEW);
        int count = 0;
        for (var prospect : prospects) {
            try {
                exportProspect(prospect.getId());
                count++;
            } catch (Exception ignored) {
            }
        }
        return count;
    }

    @Override
    public Map<String, Integer> exportQualifiedProspects(UUID campaignId) {
        var prospects = prospectRepository.findByCampaignIdAndStatus(campaignId, ProspectStatus.QUALIFIED);
        int exported = 0, skipped = 0;
        for (var prospect : prospects) {
            try {
                var r = exportProspect(prospect.getId());
                if ("LINKED_EXISTING".equals(r.status())) skipped++; else exported++;
            } catch (Exception ignored) {}
        }
        log.info("Exported {} / skipped {} qualified prospects from campaign {}", exported, skipped, campaignId);
        return Map.of("exported", exported, "skipped", skipped);
    }

    @Override
    public Map<String, Integer> exportContactableProspects(UUID campaignId) {
        var prospects = prospectRepository.findByCampaignId(campaignId).stream()
                .filter(p -> p.getStatus() != ProspectStatus.EXPORTED)
                .filter(p -> (p.getPhone() != null && !p.getPhone().isBlank())
                          || (p.getEmail() != null && !p.getEmail().isBlank()))
                .toList();
        int exported = 0, skipped = 0;
        for (var prospect : prospects) {
            try {
                var r = exportProspect(prospect.getId());
                if ("LINKED_EXISTING".equals(r.status())) skipped++; else exported++;
            } catch (Exception ignored) {
            }
        }
        log.info("Exported {} / skipped {} contactable prospects from campaign {}", exported, skipped, campaignId);
        return Map.of("exported", exported, "skipped", skipped);
    }

    @Override
    @Transactional
    public CampaignResponse scheduleCampaign(UUID campaignId, Instant nextRun, int repeatDays) {
        var campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
        campaign.setStatus(CampaignStatus.SCHEDULED);
        campaign.setScheduledNextRun(nextRun);
        campaign.setRepeatIntervalDays(repeatDays);
        campaign = campaignRepository.save(campaign);
        return toCampaignResponse(campaign);
    }

    @Override
    @Transactional
    public CampaignResponse unscheduleCampaign(UUID campaignId) {
        var campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
        campaign.setStatus(CampaignStatus.DRAFT);
        campaign.setScheduledNextRun(null);
        campaign.setRepeatIntervalDays(null);
        campaign = campaignRepository.save(campaign);
        return toCampaignResponse(campaign);
    }

    private CampaignResponse toCampaignResponse(ProspectCampaign c) {
        return new CampaignResponse(c.getId(), c.getName(), c.getSector(), c.getLocation(),
                c.getSource().name(), c.getStatus().name(), c.getTotalFound(), c.getTotalExported(),
                c.getSearchParams(), c.getNotes(), c.getCreatedBy(), c.getCreatedAt(), c.getUpdatedAt(),
                c.getScheduledNextRun(), c.getRepeatIntervalDays());
    }

    @Override
    public String generateOutreach(UUID prospectId, String channel) {
        var p = prospectRepository.findById(prospectId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospect not found: " + prospectId));
        var signals = detectSignals(p);

        var signalsText = signals.isEmpty() ? "Cap senyal específic detectat." :
            signals.stream().map(s -> "- " + s.label() + " → Proposta: " + s.pitch()).reduce("", (a, b) -> a + "\n" + b);

        boolean isWhatsapp = "whatsapp".equalsIgnoreCase(channel);

        String systemPrompt = isWhatsapp ? """
            Ets un agent comercial de l'agència AMG Digitalització (Mallorca).
            Escriu un missatge de WhatsApp breu i directe (màx 3 paràgrafs curts) en CATALÀ per a un negoci local.
            Tono proper i professional. Comença sense "Hola" genèric — usa el nom del negoci.
            Menciona 1-2 problemes concrets que tens detectats. NO llistes de punts. Acaba amb una pregunta oberta senzilla.
            Retorna NOMÉS el text del missatge, sense explicacions ni metadades.
            """ : """
            Ets un agent comercial de l'agència AMG Digitalització (Mallorca).
            Escriu un email de prospecció en CATALÀ per a un negoci local. Assumpte inclòs al principi (línia "Assumpte: ...").
            Tono professional però proper. Màx 4 paràgrafs. Menciona els problemes concrets detectats i les solucions que ofereixes.
            Signatura: AMG Digitalització | Toni Mas | amgdl.com
            Retorna NOMÉS el text de l'email (assumpte + cos), sense explicacions ni metadades.
            """;

        String userMessage = String.format("""
            Negoci: %s
            Sector: %s
            Municipi: %s
            Puntuació Google: %s (%s ressenyes)
            Té web: %s | Té WhatsApp: %s | Té Instagram: %s

            Senyals d'oportunitat detectats:
            %s
            """,
            p.getName(),
            p.getSector() != null ? p.getSector() : "no especificat",
            p.getCity() != null ? p.getCity() : "Mallorca",
            p.getGoogleRating() != null ? p.getGoogleRating() + "★" : "desconegut",
            p.getGoogleReviews() != null ? p.getGoogleReviews() : "0",
            Boolean.TRUE.equals(p.getHasWebsite()) ? "Sí" : "No",
            Boolean.TRUE.equals(p.getHasWhatsapp()) ? "Sí" : "No",
            Boolean.TRUE.equals(p.getHasInstagram()) ? "Sí" : "No",
            signalsText);

        var aiProvider = aiProviderRouter.forModel(aiProviderRouter.defaultModel());
        return aiProvider.chat(systemPrompt, List.of(), userMessage);
    }

    private ProspectResponse toProspectResponse(Prospect p) {
        List<String> reviews = List.of();
        if (p.getReviewsJson() != null) {
            try { reviews = objectMapper.readValue(p.getReviewsJson(), new TypeReference<List<String>>() {}); }
            catch (Exception ignored) {}
        }
        return new ProspectResponse(
                p.getId(), p.getCampaignId(), p.getName(), p.getDescription(),
                p.getSector(), p.getAddress(), p.getCity(), p.getPostalCode(), p.getPhone(), p.getEmail(),
                p.getWebsite(), p.getInstagram(), p.getGoogleRating(), p.getGoogleReviews(),
                p.getGooglePlaceId(), p.getHasWebsite(), p.getHasInstagram(), p.getHasWhatsapp(),
                // web analysis
                p.getHasSsl(), p.getIsResponsive(), p.getHasAnalytics(), p.getHasPixel(),
                p.getHasGtm(), p.getHasChatWidget(), p.getHasBookingSystem(),
                p.getHasContactForm(), p.getHasFaq(), p.getHasClearCta(),
                p.getTechStack(), p.getWebLoadMs(), p.getCmsDetected(),
                // social
                p.getFacebookUrl(), p.getLinkedinUrl(), p.getTiktokUrl(), p.getYoutubeUrl(),
                p.getHasFacebook(), p.getHasLinkedin(), p.getHasTiktok(),
                // scoring
                p.getScore(), p.getProspectTier(), p.getScoreBreakdown(),
                // ai
                p.getAiReport(), p.getAiPitch(), p.getDemoUrl(),
                // meta
                p.getStatus().name(), p.getSource().name(), p.getExternalId(), p.getLeadId(),
                p.getNotes(), p.getCreatedAt(), p.getUpdatedAt(),
                p.getWebAnalyzedAt(), p.getAiAnalyzedAt(),
                reviews, detectSignals(p),
                p.getPitchSentAt(), p.getPitchOpenedAt(), p.getDemoOpenedAt(),
                p.getFollowupCount(), p.getLastFollowupAt());
    }

    private static com.amg.digitalitzacio.prospecting.api.dto.ProspectSignal signal(
            String code, String label, String pitch, String tone, int points) {
        return new com.amg.digitalitzacio.prospecting.api.dto.ProspectSignal(code, label, pitch, tone, points);
    }

    private List<ProspectSignal> detectSignals(Prospect p) {
        var signals = new ArrayList<ProspectSignal>();

        // ── Potencial comercial ────────────────────────────────────────────────
        if (p.getSector() != null && PROFITABLE_SECTORS.contains(p.getSector().toUpperCase())) {
            signals.add(signal("PROFITABLE_SECTOR", "Sector d'alta conversió", "F1+F2 → agent + agenda → ROI ràpid", "opportunity", 20));
        }
        if (p.getGoogleReviews() != null) {
            int rev = p.getGoogleReviews();
            if (rev > 100) {
                signals.add(signal("HIGH_REVIEWS", "Molt actiu a Google (" + rev + " reviews)", "Base sòlida de clients → automatitzar fidelització (F4)", "opportunity", 10));
            } else if (rev < 10) {
                signals.add(signal("LOW_REVIEWS", "Molt pocs reviews (" + rev + ")", "Automatització de recollida de ressenyes (F4)", "warning", 0));
            } else if (rev < 30) {
                signals.add(signal("FEW_REVIEWS", "Poca activitat Google (" + rev + " reviews)", "Agent de fidelització + campanya de reviews", "info", 0));
            }
        }
        if (p.getGoogleRating() != null) {
            double r = p.getGoogleRating().doubleValue();
            if (r >= 4.4) {
                signals.add(signal("GOOD_RATING", "Excel·lent rating (" + p.getGoogleRating() + "★)", "Aprofitar reputació per automatitzar captació", "opportunity", 10));
            } else if (r < 3.5) {
                signals.add(signal("LOW_RATING", "Rating baix (" + p.getGoogleRating() + "★)", "Agent de resposta a ressenyes + gestió reputació", "danger", 0));
            } else if (r < 4.3) {
                signals.add(signal("MIXED_RATING", "Rating millorable (" + p.getGoogleRating() + "★)", "Agent de fidelització + automatització reviews", "info", 0));
            }
        }
        if (p.getPhone() != null && p.getPhone().replaceAll("\\D","").startsWith("6")) {
            signals.add(signal("HAS_MOBILE", "Telèfon mòbil disponible", "Canal directe per WhatsApp Business", "info", 5));
        }

        // ── Necessitat de digitalització ──────────────────────────────────────
        if (Boolean.TRUE.equals(p.getHasWebsite()) && !Boolean.TRUE.equals(p.getHasChatWidget())) {
            signals.add(signal("NO_CHAT", "Sense chat a la web", "Widget IA de xat (Spec 30) — augment conversió", "warning", 10));
        }
        if (!Boolean.TRUE.equals(p.getHasWhatsapp())) {
            signals.add(signal("NO_WHATSAPP", "Sense WhatsApp Business", "Agent IA per WhatsApp + WABA API → porta d'entrada F1", "warning", 10));
        }
        if (Boolean.TRUE.equals(p.getHasWebsite()) && !Boolean.TRUE.equals(p.getHasBookingSystem())) {
            signals.add(signal("NO_BOOKING", "Sense sistema de reserves online", "Agenda automàtica (F2) → estalvi 2-3h/dia de telèfon", "warning", 10));
        }
        if (!Boolean.TRUE.equals(p.getHasAnalytics()) && !Boolean.TRUE.equals(p.getHasGtm()) && !Boolean.TRUE.equals(p.getHasPixel())) {
            signals.add(signal("NO_AUTOMATIONS", "Sense tracking ni automatitzacions", "GTM + Meta Pixel + Analytics → base per a F3 i F4", "warning", 15));
        }
        if (Boolean.TRUE.equals(p.getHasWebsite()) &&
            (Boolean.FALSE.equals(p.getIsResponsive()) || (p.getWebLoadMs() != null && p.getWebLoadMs() > 3000))) {
            signals.add(signal("OLD_WEB", "Web lenta o no adaptada a mòbil", "Landing AMG Pro → millora UX i conversió", "danger", 15));
        }
        if (Boolean.TRUE.equals(p.getHasWebsite()) && !Boolean.TRUE.equals(p.getHasClearCta())) {
            signals.add(signal("NO_CTA", "Sense CTA clara a la web", "Optimització conversió + botó WhatsApp/chat", "warning", 10));
        }
        if (Boolean.TRUE.equals(p.getHasWebsite()) && !Boolean.TRUE.equals(p.getHasFaq())) {
            signals.add(signal("NO_FAQ", "Sense secció FAQ", "FAQ automàtica des de la KB de l'agent IA", "info", 5));
        }
        if (Boolean.TRUE.equals(p.getHasWebsite()) && !Boolean.TRUE.equals(p.getHasContactForm())) {
            signals.add(signal("NO_FORM", "Sense formulari de contacte", "Formulari AMG → captura leads a la BD", "warning", 10));
        }
        if (!Boolean.TRUE.equals(p.getHasWebsite())) {
            signals.add(signal("NO_WEBSITE", "Sense web gestionada", "Landing AMG = base per a chat, formularis i Meta Pixel", "warning", 3));
        }
        if (!Boolean.TRUE.equals(p.getHasInstagram())) {
            signals.add(signal("NO_INSTAGRAM", "Sense Instagram detectat", "Presència digital + Social Publisher (Spec 52)", "neutral", 1));
        }

        // ── Penalitzacions ────────────────────────────────────────────────────
        if (isRetailBusiness(p)) {
            signals.add(signal("RETAIL_NOT_SERVICE", "Sembla una tenda o distribuïdor (no servei)", "Verificar manualment — pot no ser el client objectiu d'AMG", "danger", -40));
        }
        if (Boolean.TRUE.equals(p.getHasWebsite()) && p.getWebLoadMs() != null && p.getWebLoadMs() < 0) {
            signals.add(signal("DOMAIN_DOWN", "Domini no accessible", "Cal verificar manualment", "danger", -50));
        }

        return signals;
    }

    private String computeTier(int score) {
        if (score >= TIER_PRIORITY) return "PRIORITY";
        if (score >= TIER_DEMO) return "DEMO";
        if (score >= TIER_REVIEW) return "REVIEW";
        return "DISCARD";
    }

    @Override
    @Transactional
    public ProspectResponse analyzeWeb(UUID prospectId) {
        var p = prospectRepository.findById(prospectId)
            .orElseThrow(() -> new ResourceNotFoundException("Prospect not found: " + prospectId));
        webAnalyzerService.analyze(p);
        recalculateAndSave(p);
        log.info("Web analyzed for prospect {}: cms={}, ssl={}, score={}", p.getId(), p.getCmsDetected(), p.getHasSsl(), p.getScore());
        return toProspectResponse(p);
    }

    @Override
    @Transactional
    public int analyzeAllWeb(UUID campaignId) {
        var prospects = prospectRepository.findByCampaignId(campaignId).stream()
            .filter(p -> p.getWebAnalyzedAt() == null)
            .filter(p -> p.getWebsite() != null && !p.getWebsite().isBlank())
            .toList();
        int analyzed = 0;
        for (var p : prospects) {
            try {
                webAnalyzerService.analyze(p);
                recalculateAndSave(p);
                analyzed++;
                Thread.sleep(1500);
            } catch (Exception e) {
                log.debug("Web analysis failed for {}: {}", p.getId(), e.getMessage());
            }
        }
        log.info("Web analysis done: {}/{} prospects in campaign {}", analyzed, prospects.size(), campaignId);
        return analyzed;
    }

    @Override
    @Transactional
    public ProspectResponse generateAiReport(UUID prospectId) {
        var p = prospectRepository.findById(prospectId)
            .orElseThrow(() -> new ResourceNotFoundException("Prospect not found: " + prospectId));
        prospectAnalysisService.generateReport(p);
        prospectAnalysisService.generatePitch(p);
        if (Boolean.TRUE.equals(p.getHasWebsite())) {
            p.setWidgetCode(widgetCodeGeneratorService.generate(p));
        }
        prospectRepository.save(p);
        // Genera demo automàticament si és PRIORITY
        prospectDemoGeneratorService.generateIfEligible(p);
        return toProspectResponse(p);
    }

    @Override
    public String getWidgetCode(UUID prospectId) {
        var p = prospectRepository.findById(prospectId)
            .orElseThrow(() -> new ResourceNotFoundException("Prospect not found: " + prospectId));
        if (p.getWidgetCode() != null) return p.getWidgetCode();
        return widgetCodeGeneratorService.generate(p);
    }

    @Override
    public Map<String, String> detectChannels(UUID prospectId) {
        var p = prospectRepository.findById(prospectId)
            .orElseThrow(() -> new ResourceNotFoundException("Prospect not found: " + prospectId));
        return channelDetectorService.detectAll(p);
    }

    @Override
    public Map<String, Object> getCampaignDashboard(UUID campaignId) {
        var prospects = prospectRepository.findByCampaignId(campaignId);
        return buildDashboardStats(prospects);
    }

    @Override
    public Map<String, Object> getGlobalDashboard() {
        var prospects = prospectRepository.findAll();
        return buildDashboardStats(prospects);
    }

    @Override
    @Transactional
    public String generatePitch(UUID prospectId) {
        return prospectMessageGeneratorService.generatePitch(prospectId);
    }

    @Override
    @Transactional
    public Map<String, String> generateDemo(UUID prospectId) {
        var p = prospectRepository.findById(prospectId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospect not found: " + prospectId));
        // Forçar generació síncrona independentment del tier (acció manual)
        String savedTier = p.getProspectTier();
        p.setProspectTier("PRIORITY");
        // generateIfEligible és @Async però l'objecte ja té demoUrl pendent: cridem el servei directament
        prospectDemoGeneratorService.generateSync(p);
        p.setProspectTier(savedTier);
        prospectRepository.save(p);
        return Map.of("demoUrl", p.getDemoUrl() != null ? p.getDemoUrl() : "");
    }

    @Override
    public Map<String, Integer> importCsv(UUID campaignId, MultipartFile file) {
        return prospectCsvImportService.importCsv(campaignId, file);
    }

    @Override
    public List<DuplicateGroupResponse> findDuplicates() {
        // Agrupa per googlePlaceId
        var byPlaceId = prospectRepository.findDuplicatesByGooglePlaceId().stream()
                .collect(Collectors.groupingBy(Prospect::getGooglePlaceId));

        // Agrupa per telèfon
        var byPhone = prospectRepository.findDuplicatesByPhone().stream()
                .collect(Collectors.groupingBy(Prospect::getPhone));

        var result = new ArrayList<DuplicateGroupResponse>();

        byPlaceId.forEach((placeId, prospects) -> {
            var summaries = prospects.stream()
                    .map(p -> new DuplicateGroupResponse.ProspectSummary(
                            p.getId(), p.getCampaignId(), p.getName(), p.getPhone(),
                            p.getEmail(), p.getGooglePlaceId(),
                            p.getStatus() != null ? p.getStatus().name() : null))
                    .toList();
            result.add(new DuplicateGroupResponse("PLACE_ID", placeId, summaries));
        });

        byPhone.forEach((phone, prospects) -> {
            var summaries = prospects.stream()
                    .map(p -> new DuplicateGroupResponse.ProspectSummary(
                            p.getId(), p.getCampaignId(), p.getName(), p.getPhone(),
                            p.getEmail(), p.getGooglePlaceId(),
                            p.getStatus() != null ? p.getStatus().name() : null))
                    .toList();
            result.add(new DuplicateGroupResponse("PHONE", phone, summaries));
        });

        return result;
    }

    @Override
    @Transactional
    public void trackPitchOpen(UUID prospectId) {
        prospectRepository.findById(prospectId).ifPresent(p -> {
            if (p.getPitchOpenedAt() == null) {
                p.setPitchOpenedAt(Instant.now());
                prospectRepository.save(p);
                log.debug("Pitch obert pel prospect {}", prospectId);
            }
        });
    }

    private Map<String, Object> buildDashboardStats(List<Prospect> prospects) {
        long total = prospects.size();
        long discard = prospects.stream().filter(p -> "DISCARD".equals(p.getProspectTier())).count();
        long review  = prospects.stream().filter(p -> "REVIEW".equals(p.getProspectTier())).count();
        long demo    = prospects.stream().filter(p -> "DEMO".equals(p.getProspectTier())).count();
        long priority= prospects.stream().filter(p -> "PRIORITY".equals(p.getProspectTier())).count();
        long exported= prospects.stream().filter(p -> p.getStatus() == ProspectStatus.EXPORTED).count();
        double avgScore = prospects.stream()
            .filter(p -> p.getScore() != null).mapToInt(Prospect::getScore).average().orElse(0);

        var bySector = prospects.stream()
            .filter(p -> p.getSector() != null && p.getScore() != null)
            .collect(Collectors.groupingBy(Prospect::getSector,
                Collectors.averagingInt(Prospect::getScore)));

        return Map.of(
            "total", total,
            "discarded", discard,
            "review", review,
            "demo", demo,
            "priority", priority,
            "exported", exported,
            "avgScore", Math.round(avgScore * 10.0) / 10.0,
            "conversionRate", total > 0 ? Math.round((exported * 1000.0 / total)) / 10.0 : 0,
            "bySector", bySector
        );
    }
}
