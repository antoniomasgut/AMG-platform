package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.prospecting.api.dto.*;
import com.amg.digitalitzacio.prospecting.domain.*;
import com.amg.digitalitzacio.shared.exception.ConflictException;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProspectingOrchestrator implements ProspectingService {

    private final ProspectCampaignRepository campaignRepository;
    private final ProspectRepository prospectRepository;
    private final ProspectScraper scraper;
    private final LeadService leadService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public CampaignResponse createCampaign(CreateCampaignRequest request, UUID createdBy) {
        var campaign = ProspectCampaign.builder()
                .name(request.name())
                .sector(request.sector())
                .location(request.location())
                .source(request.source())
                .status(CampaignStatus.DRAFT)
                .searchParams(request.searchParams())
                .notes(request.notes())
                .createdBy(createdBy)
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
    public List<CampaignResponse> listCampaigns(String sector, String location) {
        List<ProspectCampaign> campaigns;
        if (sector != null && location != null) {
            campaigns = campaignRepository.findBySectorAndLocation(sector, location);
        } else if (sector != null) {
            campaigns = campaignRepository.findAll().stream()
                    .filter(c -> sector.equals(c.getSector()))
                    .toList();
        } else {
            campaigns = campaignRepository.findAll();
        }
        return campaigns.stream().map(this::toCampaignResponse).collect(Collectors.toList());
    }

    @Override
    public CampaignResponse updateCampaign(UUID campaignId, UpdateCampaignRequest request) {
        var campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
        if (request.name() != null) campaign.setName(request.name());
        if (request.sector() != null) campaign.setSector(request.sector());
        if (request.location() != null) campaign.setLocation(request.location());
        if (request.source() != null) campaign.setSource(request.source());
        if (request.searchParams() != null) campaign.setSearchParams(request.searchParams());
        if (request.notes() != null) campaign.setNotes(request.notes());
        campaign = campaignRepository.save(campaign);
        return toCampaignResponse(campaign);
    }

    @Override
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

        int count = 0;
        int skipped = 0;
        for (var prospect : prospects) {
            // Deduplicació per googlePlaceId (global)
            if (prospect.getGooglePlaceId() != null
                    && prospectRepository.existsByGooglePlaceId(prospect.getGooglePlaceId())) {
                skipped++;
                continue;
            }
            // Deduplicació per telèfon (evita el mateix negoci amb Place ID diferent)
            if (prospect.getPhone() != null && !prospect.getPhone().isBlank()
                    && prospectRepository.existsByPhone(prospect.getPhone())) {
                skipped++;
                continue;
            }
            prospectRepository.save(prospect);
            count++;
        }
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
    public ProspectResponse updateProspect(UUID prospectId, UpdateProspectRequest request) {
        var prospect = prospectRepository.findById(prospectId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospect not found: " + prospectId));
        if (request.status() != null) prospect.setStatus(request.status());
        if (request.notes() != null) prospect.setNotes(request.notes());
        prospect = prospectRepository.save(prospect);
        return toProspectResponse(prospect);
    }

    @Override
    public ProspectResponse enrichProspect(UUID prospectId) {
        var prospect = prospectRepository.findById(prospectId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospect not found: " + prospectId));
        var enrichment = scraper.enrich(prospect.getName(), prospect.getWebsite(), prospect.getEmail());
        if (enrichment.email() != null && !enrichment.email().isBlank()) prospect.setEmail(enrichment.email());
        if (enrichment.website() != null && !enrichment.website().isBlank()) {
            prospect.setWebsite(enrichment.website());
            prospect.setHasWebsite(true);
        }
        if (enrichment.instagram() != null) prospect.setInstagram(enrichment.instagram());
        if (enrichment.hasWhatsapp() != null) prospect.setHasWhatsapp(enrichment.hasWhatsapp());
        prospect = prospectRepository.save(prospect);
        return toProspectResponse(prospect);
    }

    @Override
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
                          || (p.getEmail() == null || p.getEmail().isBlank()))
                .toList();
        int enriched = 0;
        for (var prospect : prospects) {
            try {
                var enrichment = scraper.enrich(prospect.getName(), prospect.getWebsite(), prospect.getEmail());
                boolean changed = false;
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
        var leadId = leadService.createLead(
                prospect.getName(), prospect.getEmail(), prospect.getPhone(),
                prospect.getWebsite(), prospect.getDescription(),
                prospect.getSource().name(), tenantId);

        prospect.setLeadId(leadId);
        prospect.setStatus(ProspectStatus.EXPORTED);
        prospectRepository.save(prospect);

        campaign.setTotalExported(campaign.getTotalExported() + 1);
        campaignRepository.save(campaign);

        return new LeadExportResponse(prospectId, leadId, "/leads/" + leadId, ProspectStatus.EXPORTED.name());
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
    public int exportContactableProspects(UUID campaignId) {
        var prospects = prospectRepository.findByCampaignId(campaignId).stream()
                .filter(p -> p.getStatus() != ProspectStatus.EXPORTED)
                .filter(p -> (p.getPhone() != null && !p.getPhone().isBlank())
                          || (p.getEmail() != null && !p.getEmail().isBlank()))
                .toList();
        int count = 0;
        for (var prospect : prospects) {
            try {
                exportProspect(prospect.getId());
                count++;
            } catch (Exception ignored) {
            }
        }
        log.info("Exported {} contactable prospects from campaign {}", count, campaignId);
        return count;
    }

    private CampaignResponse toCampaignResponse(ProspectCampaign c) {
        return new CampaignResponse(c.getId(), c.getName(), c.getSector(), c.getLocation(),
                c.getSource().name(), c.getStatus().name(), c.getTotalFound(), c.getTotalExported(),
                c.getSearchParams(), c.getNotes(), c.getCreatedBy(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private ProspectResponse toProspectResponse(Prospect p) {
        return new ProspectResponse(p.getId(), p.getCampaignId(), p.getName(), p.getDescription(),
                p.getSector(), p.getAddress(), p.getCity(), p.getPostalCode(), p.getPhone(), p.getEmail(),
                p.getWebsite(), p.getInstagram(), p.getGoogleRating(), p.getGoogleReviews(),
                p.getGooglePlaceId(), p.getHasWebsite(), p.getHasInstagram(), p.getHasWhatsapp(),
                p.getStatus().name(), p.getSource().name(), p.getExternalId(), p.getLeadId(),
                p.getNotes(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
