package com.amg.digitalitzacio.engine.application;

import com.amg.digitalitzacio.engine.api.dto.*;
import com.amg.digitalitzacio.engine.domain.*;
import com.amg.digitalitzacio.google.application.GoogleBusinessReviewSyncService;
import com.amg.digitalitzacio.google.domain.GoogleBusinessReview;
import com.amg.digitalitzacio.leads.domain.Lead;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.leads.domain.LeadSource;
import com.amg.digitalitzacio.leads.domain.PipelineStage;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.shared.notification.NotificationEvent;
import com.amg.digitalitzacio.shared.notification.TenantNotificationService;
import com.amg.digitalitzacio.vault.domain.CatalogServiceRepository;
import com.amg.digitalitzacio.vault.domain.ServiceType;
import com.amg.digitalitzacio.vault.domain.TenantServiceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EngineOrchestrator implements EngineService {

    @Value("${app.landing.base-domain:webs.amgdl.com}")
    private String landingBaseDomain;

    @Value("${app.api.url:https://api.amgdl.com}")
    private String apiBaseUrl;

    private final LandingRepository landingRepository;
    private final LandingVersionRepository landingVersionRepository;
    private final ContactLeadRepository contactLeadRepository;
    private final LeadRepository leadRepository;
    private final CatalogServiceRepository catalogServiceRepository;
    private final TenantServiceRepository tenantServiceRepository;
    private final LandingTemplateRepository landingTemplateRepository;
    private final TemplateSectionRepository templateSectionRepository;
    private final TenantNotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final com.amg.digitalitzacio.engine.infrastructure.TraefikConfigWriter traefikConfigWriter;
    private final com.amg.digitalitzacio.billing.application.PostAcceptanceService postAcceptanceService;
    private final GoogleBusinessReviewSyncService googleBusinessReviewSyncService;
    private final TenantVariableResolver tenantVariableResolver;

    // --- Landings ---

    @Override
    @Transactional
    public LandingResponse createLanding(UUID tenantId, CreateLandingRequest request) {
        if (landingRepository.existsByTenantIdAndSlug(tenantId, request.slug())) {
            throw new IllegalArgumentException("Slug duplicat dins el tenant");
        }

        // Verify tenant has an active LANDING service
        verifyTenantHasLandingService(tenantId);

        var landing = Landing.builder()
                .tenantId(tenantId)
                .serviceId(request.serviceId() != null ? request.serviceId() : findLandingServiceId(tenantId))
                .title(request.title())
                .slug(request.slug())
                .landingType(parseLandingType(request.landingType()))
                .metaDescription(request.metaDescription())
                .templateId(request.templateId())
                .status(LandingStatus.DRAFT)
                .build();
        landing = landingRepository.save(landing);

        // Create initial DRAFT version — populate from template defaults if templateId provided
        var initialContent = buildTemplateDefaultContent(request.templateId(), tenantId);
        var version = LandingVersion.builder()
                .landingId(landing.getId())
                .versionNumber(1)
                .status(VersionStatus.DRAFT)
                .content(initialContent)
                .build();
        landingVersionRepository.save(version);

        return toLandingResponse(landing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LandingSummary> listLandings(UUID tenantId, int page, int size, String search) {
        var pageable = PageRequest.of(page, size);
        var landingPage = (search != null && !search.isBlank())
                ? landingRepository.searchByTenantId(tenantId, search, pageable)
                : landingRepository.findByTenantId(tenantId, pageable);
        return landingPage.stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LandingResponse getLanding(UUID tenantId, UUID id) {
        var landing = findLanding(tenantId, id);
        return toLandingResponse(landing);
    }

    @Override
    @Transactional
    public LandingResponse updateLanding(UUID tenantId, UUID id, CreateLandingRequest request) {
        var landing = findLanding(tenantId, id);

        if (request.title() != null) landing.setTitle(request.title());
        if (request.slug() != null) {
            if (!request.slug().equals(landing.getSlug()) && landingRepository.existsByTenantIdAndSlug(tenantId, request.slug())) {
                throw new IllegalArgumentException("Slug duplicat dins el tenant");
            }
            landing.setSlug(request.slug());
        }
        if (request.metaDescription() != null) landing.setMetaDescription(request.metaDescription());
        if (request.landingType() != null) landing.setLandingType(parseLandingType(request.landingType()));

        landing = landingRepository.save(landing);
        return toLandingResponse(landing);
    }

    @Override
    @Transactional
    public void deleteLanding(UUID tenantId, UUID id) {
        var landing = findLanding(tenantId, id);

        // RGPD: en eliminar la landing, eliminar tots els ContactLead associats
        var leads = contactLeadRepository.findByLandingId(landing.getId());
        contactLeadRepository.deleteAll(leads);

        landingRepository.delete(landing);
    }

    // --- Versions ---

    @Override
    @Transactional
    public VersionResponse createVersion(UUID landingId, CreateVersionRequest request, UUID principalTenantId) {
        var landing = landingRepository.findById(landingId)
                .orElseThrow(() -> new ResourceNotFoundException("Landing not found: " + landingId));
        if (principalTenantId != null && !landing.getTenantId().equals(principalTenantId)) {
            throw new ResourceNotFoundException("Landing not found: " + landingId);
        }

        var loc = request.locale() != null && !request.locale().isBlank() ? request.locale() : "ca";
        var versionCount = landingVersionRepository.countByLandingIdAndLocale(landingId, loc);
        var version = LandingVersion.builder()
                .landingId(landingId)
                .versionNumber((int) versionCount + 1)
                .locale(loc)
                .status(VersionStatus.DRAFT)
                .content(toJson(request.content() != null ? request.content() : Map.of("blocks", List.of())))
                .styles(toJson(request.styles()))
                .build();
        version = landingVersionRepository.save(version);
        return toVersionResponse(version);
    }

    @Override
    @Transactional
    public VersionResponse updateVersion(UUID landingId, UUID versionId, CreateVersionRequest request, UUID principalTenantId) {
        var landing = landingRepository.findById(landingId)
                .orElseThrow(() -> new ResourceNotFoundException("Landing not found: " + landingId));
        if (principalTenantId != null && !landing.getTenantId().equals(principalTenantId)) {
            throw new ResourceNotFoundException("Landing not found: " + landingId);
        }

        var version = landingVersionRepository.findByLandingIdAndId(landingId, versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Version not found: " + versionId));

        if (version.getStatus() != VersionStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT versions can be updated");
        }

        if (request.content() != null) version.setContent(toJson(request.content()));
        if (request.styles() != null) version.setStyles(toJson(request.styles()));
        version = landingVersionRepository.save(version);
        return toVersionResponse(version);
    }

    @Override
    @Transactional
    public PublishResponse publish(UUID landingId, String locale) {
        var landing = landingRepository.findById(landingId)
                .orElseThrow(() -> new ResourceNotFoundException("Landing not found: " + landingId));
        var loc = locale != null && !locale.isBlank() ? locale : "ca";

        var draft = landingVersionRepository
                .findTopByLandingIdAndLocaleAndStatusOrderByVersionNumberDesc(landingId, loc, VersionStatus.DRAFT)
                .orElseThrow(() -> new IllegalArgumentException("No DRAFT version to publish for locale: " + loc));

        draft.setStatus(VersionStatus.PUBLISHED);
        draft.setPublishedAt(Instant.now());
        landingVersionRepository.save(draft);

        landing.setStatus(LandingStatus.PUBLISHED);
        // published_version_id always tracks the 'ca' version for backward compat
        if ("ca".equals(loc)) landing.setPublishedVersionId(draft.getId());
        landingRepository.save(landing);

        var publicUrl = buildPublicUrl(landing);
        traefikConfigWriter.regenerate(landingRepository.findAll());
        return new PublishResponse(draft.getVersionNumber(), "PUBLISHED", publicUrl, draft.getPublishedAt());
    }

    @Override
    @Transactional
    public void unpublish(UUID landingId) {
        var landing = landingRepository.findById(landingId)
                .orElseThrow(() -> new ResourceNotFoundException("Landing not found: " + landingId));

        landing.setPublishedVersionId(null);
        landing.setStatus(LandingStatus.DRAFT);
        landingRepository.save(landing);
        traefikConfigWriter.regenerate(landingRepository.findAll());
    }

    // --- Domain ---

    @Override
    @Transactional
    public DomainConfigResponse configureDomain(UUID landingId, DomainConfigRequest request, UUID principalId, String principalRole) {
        var landing = landingRepository.findById(landingId)
                .orElseThrow(() -> new ResourceNotFoundException("Landing not found: " + landingId));

        var managed = request.managed() != null && request.managed();

        // CLIENT només pot crear dominis autogestionats
        if (managed && "CLIENT".equals(principalRole)) {
            throw new IllegalArgumentException("CLIENT no pot configurar un domini gestionat");
        }

        landing.setCustomDomain(request.domain());
        landing.setManagedDomain(managed);

        if (managed) {
            // Domini gestionat: nosaltres comprem i gestionem
            landing.setDomainStatus(request.renewalDate() != null ? DomainStatus.PURCHASED : DomainStatus.PENDING_PURCHASE);
            landing.setDomainRegistrar(request.registrar());
            landing.setDomainRenewalDate(request.renewalDate());
            landing.setDomainRenewalPrice(request.renewalPrice());
            landing.setDomainOwnerName(request.ownerName());
            landing.setDomainOwnerEmail(request.ownerEmail());
            landing.setDomainOwnerPhone(request.ownerPhone());
        } else {
            // Autogestionat: el client configura el DNS
            landing.setDomainStatus(DomainStatus.DNS_PENDING);
        }
        landing.setDomainVerified(false);
        landingRepository.save(landing);

        return toDomainConfigResponse(landing);
    }

    @Override
    @Transactional
    public DomainConfigResponse verifyDomain(UUID landingId) {
        var landing = landingRepository.findById(landingId)
                .orElseThrow(() -> new ResourceNotFoundException("Landing not found: " + landingId));

        // Simple DNS check simulation — in production this would query DNS CNAME records
        landing.setDomainVerified(true);
        landing.setDomainStatus(DomainStatus.VERIFIED);
        landingRepository.save(landing);

        return toDomainConfigResponse(landing);
    }

    @Override
    @Transactional
    public DomainConfigResponse updateDomainStatus(UUID landingId, UpdateDomainStatusRequest request) {
        var landing = landingRepository.findById(landingId)
                .orElseThrow(() -> new ResourceNotFoundException("Landing not found: " + landingId));

        if (request.domainStatus() != null) {
            landing.setDomainStatus(DomainStatus.valueOf(request.domainStatus()));
        }
        if (request.registrar() != null) landing.setDomainRegistrar(request.registrar());
        if (request.renewalDate() != null) landing.setDomainRenewalDate(request.renewalDate());
        if (request.renewalPrice() != null) landing.setDomainRenewalPrice(request.renewalPrice());
        landingRepository.save(landing);

        return toDomainConfigResponse(landing);
    }

    @Override
    @Transactional
    public void removeDomain(UUID landingId) {
        var landing = landingRepository.findById(landingId)
                .orElseThrow(() -> new ResourceNotFoundException("Landing not found: " + landingId));

        landing.setCustomDomain(null);
        landing.setDomainVerified(false);
        landing.setManagedDomain(false);
        landing.setDomainStatus(DomainStatus.NOT_CONFIGURED);
        landing.setDomainRegistrar(null);
        landing.setDomainRenewalDate(null);
        landing.setDomainRenewalPrice(null);
        landing.setDomainOwnerName(null);
        landing.setDomainOwnerEmail(null);
        landing.setDomainOwnerPhone(null);
        landingRepository.save(landing);
    }

    // --- Render (public) ---

    @Override
    @Transactional(readOnly = true)
    public String renderLanding(String slug, String host, String locale) {
        var landing = landingRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Landing not found: " + slug));

        if (landing.getStatus() != LandingStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Landing not published: " + slug);
        }

        var loc = locale != null && !locale.isBlank() ? locale : "ca";
        // Fallback: requested locale → 'ca'
        var version = landingVersionRepository
                .findByLandingIdAndLocaleAndStatus(landing.getId(), loc, VersionStatus.PUBLISHED)
                .or(() -> landingVersionRepository.findByLandingIdAndLocaleAndStatus(landing.getId(), "ca", VersionStatus.PUBLISHED))
                // legacy fallback: use published_version_id directly
                .or(() -> landing.getPublishedVersionId() != null
                        ? landingVersionRepository.findById(landing.getPublishedVersionId())
                        : Optional.empty())
                .orElseThrow(() -> new ResourceNotFoundException("Published version not found for locale: " + loc));

        // Increment view count
        landing.setViewCount(landing.getViewCount() + 1);
        landingRepository.save(landing);

        return buildHtmlPage(landing, version);
    }

    @Override
    public String renderSitemap(String slug) {
        var landing = landingRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Landing not found: " + slug));

        if (landing.getStatus() != LandingStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Landing not published: " + slug);
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n" +
               "  <url><loc>https://" + (landing.getCustomDomain() != null ? landing.getCustomDomain() : slug + "." + landingBaseDomain) +
               "</loc></url>\n" +
               "</urlset>";
    }

    // --- Stats ---

    @Override
    public LandingStatsResponse getLandingStats(UUID tenantId, UUID landingId) {
        var landing = landingRepository.findById(landingId)
                .filter(l -> l.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Landing not found: " + landingId));
        long contacts = contactLeadRepository.countByLandingId(landingId);
        return new LandingStatsResponse(landing.getViewCount(), contacts);
    }

    // --- Contact ---

    @Override
    @Transactional
    public ContactResponse submitContact(String slug, ContactRequest request) {
        var landing = landingRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Landing not found: " + slug));

        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        // RGPD/LSSI: consentiment explícit obligatori
        if (request.consent() == null || !request.consent()) {
            throw new IllegalArgumentException("Cal acceptar la política de privacitat");
        }

        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("consent", true);
        metadata.put("consentAt", Instant.now().toString());

        var contactLead = ContactLead.builder()
                .landingId(landing.getId())
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .message(request.message())
                .metadata(toJson(metadata))
                .build();
        contactLeadRepository.save(contactLead);

        // Auto-crear lead al CRM si no existeix; si existeix, actualitzar lastContactAt
        if (request.email() != null && !request.email().isBlank()) {
            var existing = leadRepository.findFirstByTenantIdAndEmail(landing.getTenantId(), request.email());
            if (existing.isEmpty()) {
                Lead crmLead = new Lead();
                crmLead.setTenantId(landing.getTenantId());
                crmLead.setName(request.name() != null ? request.name() : request.email());
                crmLead.setEmail(request.email());
                crmLead.setPhone(request.phone());
                crmLead.setSource(request.utmSource() != null ? inferSource(request.utmSource()) : LeadSource.LANDING_FORM);
                crmLead.setStage(PipelineStage.NEW);
                crmLead.setLastContactAt(Instant.now());
                crmLead.setUtmSource(request.utmSource());
                crmLead.setUtmMedium(request.utmMedium());
                crmLead.setUtmCampaign(request.utmCampaign());
                crmLead.setNotes("Formulari de contacte: " + landing.getTitle()
                        + (request.message() != null ? "\n\n" + request.message() : ""));
                leadRepository.save(crmLead);
            } else {
                existing.get().setLastContactAt(Instant.now());
                leadRepository.save(existing.get());
            }
        }

        notificationService.notify(landing.getTenantId(), NotificationEvent.CONTACT_FORM, Map.of(
                "landing_title", landing.getTitle(),
                "nom",     request.name()    != null ? request.name()    : "—",
                "email",   request.email()   != null ? request.email()   : "—",
                "phone",   request.phone()   != null ? request.phone()   : "—",
                "message", request.message() != null ? request.message() : "—"));

        postAcceptanceService.onContactForm(
                landing.getTenantId(), landing.getTitle(),
                request.name(), request.phone(), request.message());

        return new ContactResponse("Missatge rebut correctament");
    }

    // --- Template-based landing creation ---

    @Override
    @Transactional
    public LandingResponse createLandingFromTemplate(UUID tenantId, CreateLandingFromTemplateRequest request) {
        if (landingRepository.existsByTenantIdAndSlug(tenantId, request.slug())) {
            throw new IllegalArgumentException("Slug duplicat dins el tenant");
        }
        verifyTenantHasLandingService(tenantId);

        var landing = Landing.builder()
                .tenantId(tenantId)
                .serviceId(request.serviceId())
                .title(request.title())
                .slug(request.slug())
                .metaDescription(request.metaDescription())
                .templateId(request.templateId())
                .status(LandingStatus.DRAFT)
                .build();
        landing = landingRepository.save(landing);

        // Build content from template sections + filled content, with tenant variable injection
        var content = buildFilledContent(request.templateId(), request.filledSections(), tenantId);
        // Use template's defaultStyles as fallback when caller does not provide styles
        String stylesJson = null;
        if (request.styles() != null) {
            stylesJson = toJson(request.styles());
        } else if (request.templateId() != null) {
            var tpl = landingTemplateRepository.findById(request.templateId());
            if (tpl.isPresent() && tpl.get().getDefaultStyles() != null) {
                stylesJson = tpl.get().getDefaultStyles();
            }
        }
        var version = LandingVersion.builder()
                .landingId(landing.getId())
                .versionNumber(1)
                .status(VersionStatus.DRAFT)
                .content(content)
                .styles(stylesJson)
                .build();
        landingVersionRepository.save(version);

        return toLandingResponse(landing);
    }

    /**
     * Build initial version content from template defaults, with tenant variable injection.
     * Returns {"blocks":[]} if no template or no sections.
     */
    private String buildTemplateDefaultContent(UUID templateId, UUID tenantId) {
        if (templateId == null) return "{\"blocks\":[]}";
        var sections = templateSectionRepository.findByTemplateIdOrderBySortOrder(templateId);
        if (sections.isEmpty()) return "{\"blocks\":[]}";

        var vars = tenantVariableResolver.buildVariables(tenantId);
        var blocks = sections.stream()
                .map(section -> {
                    Map<String, Object> block = new LinkedHashMap<>();
                    block.put("id", "blk_" + UUID.randomUUID().toString().substring(0, 8));
                    block.put("type", section.getBlockType().typeName());
                    Map<String, Object> props = new LinkedHashMap<>();
                    var rawProps = tenantVariableResolver.resolveProps(section.getDefaultProps(), vars);
                    if (rawProps != null && !rawProps.isBlank()) {
                        var parsed = fromJson(rawProps);
                        if (parsed != null) props.putAll(parsed);
                    }
                    block.put("props", props);
                    return block;
                })
                .toList();

        var root = new LinkedHashMap<String, Object>();
        root.put("blocks", blocks);
        return toJson(root);
    }

    /**
     * Build version content from template sections + ADMIN-filled content per section.
     * Applies tenant variable injection on default props before merging filled content.
     */
    private String buildFilledContent(UUID templateId, Map<String, Map<String, Object>> filledSections, UUID tenantId) {
        if (templateId == null) return "{\"blocks\":[]}";
        var sections = templateSectionRepository.findByTemplateIdOrderBySortOrder(templateId);
        if (sections.isEmpty()) return "{\"blocks\":[]}";

        var vars = tenantVariableResolver.buildVariables(tenantId);
        var blocks = sections.stream()
                .map(section -> {
                    Map<String, Object> block = new LinkedHashMap<>();
                    block.put("id", "blk_" + UUID.randomUUID().toString().substring(0, 8));
                    block.put("type", section.getBlockType().typeName());
                    // Merge: start with defaultProps (with variable injection), overlay with filled content
                    Map<String, Object> props = new LinkedHashMap<>();
                    var rawProps = tenantVariableResolver.resolveProps(section.getDefaultProps(), vars);
                    if (rawProps != null && !rawProps.isBlank()) {
                        var parsed = fromJson(rawProps);
                        if (parsed != null) props.putAll(parsed);
                    }
                    var filled = filledSections != null ? filledSections.get(section.getId().toString()) : null;
                    if (filled != null) props.putAll(filled);
                    block.put("props", props);
                    return block;
                })
                .toList();

        var root = new LinkedHashMap<String, Object>();
        root.put("blocks", blocks);
        return toJson(root);
    }

    // --- Helpers ---

    private UUID findLandingServiceId(UUID tenantId) {
        var landingServices = catalogServiceRepository.findByType(ServiceType.LANDING);
        if (landingServices.isEmpty()) return null;
        var landingService = landingServices.get(0);
        var tenantService = tenantServiceRepository.findByTenantIdAndServiceId(tenantId, landingService.getId());
        return tenantService.isPresent() ? landingService.getId() : null;
    }

    private void verifyTenantHasLandingService(UUID tenantId) {
        var landingServices = catalogServiceRepository.findByType(ServiceType.LANDING);
        if (landingServices.isEmpty()) {
            throw new IllegalArgumentException("No LANDING service configured in catalog");
        }
        var landingService = landingServices.get(0);
        var tenantService = tenantServiceRepository.findByTenantIdAndServiceId(tenantId, landingService.getId());
        if (tenantService.isEmpty()) {
            throw new IllegalArgumentException("El tenant no té el servei LANDING actiu");
        }
    }

    private Landing findLanding(UUID tenantId, UUID id) {
        return landingRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Landing not found: " + id));
    }

    private String buildPublicUrl(Landing landing) {
        if (landing.getCustomDomain() != null && landing.getDomainStatus() == DomainStatus.VERIFIED) {
            return "https://" + landing.getCustomDomain();
        }
        return "https://" + landing.getSlug() + "." + landingBaseDomain;
    }

    private DomainConfigResponse toDomainConfigResponse(Landing landing) {
        return new DomainConfigResponse(
                landing.getCustomDomain(),
                landing.getManagedDomain(),
                landing.getDomainStatus().name(),
                "Afegeix un registre CNAME de " + landing.getCustomDomain() + " a " + landingBaseDomain,
                landing.getDomainRegistrar(),
                landing.getDomainRenewalDate(),
                landing.getDomainRenewalPrice()
        );
    }

    private String toJson(Map<String, Object> map) {
        if (map == null) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) return new LinkedHashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
    }

    private LandingResponse toLandingResponse(Landing landing) {
        var versions = landingVersionRepository.findByLandingIdOrderByVersionNumberDesc(landing.getId())
                .stream().map(this::toVersionResponse).toList();

        return new LandingResponse(
                landing.getId(),
                landing.getTitle(),
                landing.getSlug(),
                landing.getStatus().name(),
                landing.getLandingType() != null ? landing.getLandingType().name() : "MICRO",
                buildPublicUrl(landing),
                landing.getCustomDomain(),
                landing.getDomainVerified(),
                landing.getManagedDomain(),
                landing.getDomainStatus().name(),
                landing.getDomainRegistrar(),
                landing.getDomainOwnerName(),
                landing.getDomainOwnerEmail(),
                landing.getDomainOwnerPhone(),
                versions,
                landing.getCreatedAt(),
                landing.getUpdatedAt()
        );
    }

    private LandingSummary toSummary(Landing landing) {
        return new LandingSummary(
                landing.getId(),
                landing.getTitle(),
                landing.getSlug(),
                landing.getStatus().name(),
                buildPublicUrl(landing),
                landing.getDomainVerified(),
                landing.getManagedDomain(),
                landing.getDomainStatus().name(),
                landing.getCreatedAt()
        );
    }

    private VersionResponse toVersionResponse(LandingVersion version) {
        return new VersionResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getLocale() != null ? version.getLocale() : "ca",
                version.getStatus().name(),
                fromJson(version.getContent()),
                fromJson(version.getStyles()),
                version.getPublishedAt(),
                version.getCreatedAt()
        );
    }

    private String buildHtmlPage(Landing landing, LandingVersion version) {
        var content = fromJson(version.getContent());
        var styles = fromJson(version.getStyles());

        // Suport font dual (fontHeading + fontBody) amb fallback a fontFamily
        var fontHeading = styles != null
                ? styles.getOrDefault("fontHeading", styles.getOrDefault("fontFamily", "Montserrat, sans-serif"))
                : "Montserrat, sans-serif";
        var fontBody = styles != null
                ? styles.getOrDefault("fontBody", styles.getOrDefault("fontFamily", "'Open Sans', sans-serif"))
                : "'Open Sans', sans-serif";
        var primaryColor = styles != null ? styles.getOrDefault("primaryColor", "#1a365d") : "#1a365d";
        var accentColor = styles != null
                ? styles.getOrDefault("accentColor", styles.getOrDefault("secondaryColor", "#f6ad55"))
                : "#f6ad55";
        var bgColor = styles != null ? styles.getOrDefault("bgColor", "#ffffff") : "#ffffff";
        var textColor = styles != null ? styles.getOrDefault("textColor", "#1a202c") : "#1a202c";

        // Google Fonts — construir link per les dues fonts
        var headingName = fontHeading.toString().split(",")[0].trim().replace("'", "");
        var bodyName = fontBody.toString().split(",")[0].trim().replace("'", "");
        var fontsParam = headingName.equals(bodyName)
                ? headingName.replace(" ", "+") + ":wght@400;600;700"
                : headingName.replace(" ", "+") + ":wght@400;600;700|" + bodyName.replace(" ", "+") + ":wght@400;600";
        var googleFontsLink = "<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">" +
                "<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>" +
                "<link href=\"https://fonts.googleapis.com/css2?family=" + fontsParam + "&display=swap\" rel=\"stylesheet\">";

        var publicUrl = buildPublicUrl(landing);
        var sv = new StyleVars(
                primaryColor.toString(), accentColor.toString(),
                bgColor.toString(), textColor.toString(),
                fontHeading.toString(), fontBody.toString()
        );

        // Dades de contacte ràpid i analytics (opcionals)
        var waRaw        = styles != null ? styles.getOrDefault("whatsappNumber", "") : "";
        var phone        = styles != null ? styles.getOrDefault("phone", "")           : "";
        var address      = styles != null ? styles.getOrDefault("address", "")         : "";
        var bizType      = styles != null ? styles.getOrDefault("businessType", "LocalBusiness") : "LocalBusiness";
        var gaId         = styles != null ? styles.getOrDefault("gaId", "")            : "";
        var customCss    = styles != null ? styles.getOrDefault("customCss", "")       : "";
        var logoUrl      = styles != null ? styles.getOrDefault("logoUrl", "").toString() : "";
        var showNav      = styles == null || !Boolean.FALSE.equals(styles.get("showNav"));
        // Idioma de la pàgina (ca per defecte)
        var lang         = styles != null ? styles.getOrDefault("language", "ca").toString() : "ca";
        // URL base de les pàgines legals (per defecte apunta al portal AMG)
        var legalBase    = styles != null ? styles.getOrDefault("legalBaseUrl", "https://amgdl.com/ca/legal").toString() : "https://amgdl.com/ca/legal";
        var waNumber     = waRaw.toString().replaceAll("[^0-9+]", "");
        var chatEnabled  = styles != null && Boolean.TRUE.equals(styles.get("chatEnabled"));
        var chatBizName  = styles != null ? styles.getOrDefault("chatBusinessName", landing.getTitle()).toString() : landing.getTitle();

        // Detectar imatge hero per a preload (millora LCP)
        String heroImageUrl = "";
        var blocks = content != null ? content.get("blocks") : null;
        var blocksHtml = new StringBuilder();
        boolean hasChatCta = false;
        if (blocks instanceof List<?> blockList) {
            for (var block : blockList) {
                if (block instanceof Map) {
                    @SuppressWarnings("unchecked")
                    var blockMap = (Map<String, Object>) block;
                    if ("chat-cta".equals(blockMap.get("type"))) hasChatCta = true;
                    // Capturar imatge del primer hero per a preload
                    if (heroImageUrl.isBlank() && "hero".equals(blockMap.get("type"))) {
                        var bp = blockMap.getOrDefault("props", Map.of());
                        if (bp instanceof Map<?,?> bpm) {
                            Object bgImage = bpm.get("bgImage");
                            Object bgImageUrl = bpm.get("bgImageUrl");
                            Object bi = bgImage != null ? bgImage : (bgImageUrl != null ? bgImageUrl : "");
                            if (bi != null && !bi.toString().isBlank()) heroImageUrl = bi.toString();
                        }
                    }
                    blocksHtml.append(renderBlock(blockMap, sv, landing.getSlug(), landing.getTenantId()));
                }
            }
        }

        // Schema.org sempre present (nom i URL com a mínim)
        var schemaJson   = buildSchemaOrg(landing.getTitle(), publicUrl, phone.toString(), address.toString(), bizType.toString());
        boolean hasChatWidget = chatEnabled || hasChatCta;
        var waButton     = buildWhatsAppButton(waNumber, hasChatWidget);
        var stickyNav    = showNav ? buildStickyNav(landing.getTitle(), logoUrl, sv) : "";
        // GA4: s'injecta però s'activa NOMÉS quan l'usuari accepta cookies
        var gaScript     = buildGa4ScriptDeferred(gaId.toString());
        var chatWidget   = hasChatWidget ? buildChatWidget(landing.getSlug(), sv.primary(), chatBizName) : "";
        var cookieBanner = buildCookieBanner(sv.primary(), legalBase);
        final String heroPreload = heroImageUrl.isBlank() ? "" :
                "<link rel=\"preload\" as=\"image\" href=\"" + escapeHtml(heroImageUrl) + "\">";

        return "<!DOCTYPE html><html lang=\"" + escapeHtml(lang) + "\"><head>" +
               "<meta charset=\"UTF-8\">" +
               "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
               "<meta name=\"robots\" content=\"index, follow\">" +
               (landing.getMetaDescription() != null ? "<meta name=\"description\" content=\"" + escapeHtml(landing.getMetaDescription()) + "\">" : "") +
               // Open Graph
               "<meta property=\"og:title\" content=\"" + escapeHtml(landing.getTitle()) + "\">" +
               (landing.getMetaDescription() != null ? "<meta property=\"og:description\" content=\"" + escapeHtml(landing.getMetaDescription()) + "\">" : "") +
               (landing.getOgImageUrl() != null ? "<meta property=\"og:image\" content=\"" + escapeHtml(landing.getOgImageUrl()) + "\">" : "") +
               "<meta property=\"og:url\" content=\"" + escapeHtml(publicUrl) + "\">" +
               "<meta property=\"og:type\" content=\"website\">" +
               "<meta property=\"og:locale\" content=\"" + escapeHtml(lang) + "_ES\">" +
               // Twitter Card
               "<meta name=\"twitter:card\" content=\"summary_large_image\">" +
               "<meta name=\"twitter:title\" content=\"" + escapeHtml(landing.getTitle()) + "\">" +
               (landing.getMetaDescription() != null ? "<meta name=\"twitter:description\" content=\"" + escapeHtml(landing.getMetaDescription()) + "\">" : "") +
               (landing.getOgImageUrl() != null ? "<meta name=\"twitter:image\" content=\"" + escapeHtml(landing.getOgImageUrl()) + "\">" : "") +
               "<link rel=\"canonical\" href=\"" + escapeHtml(publicUrl) + "\">" +
               // Preload imatge hero (millora LCP)
               heroPreload +
               "<title>" + escapeHtml(landing.getTitle()) + "</title>" +
               googleFontsLink +
               gaScript +
               schemaJson +
               "<style>" +
               buildGlobalCss(sv) +
               buildWhatsAppCss() +
               buildCookieBannerCss() +
               (customCss.toString().isBlank() ? "" : customCss.toString()) +
               "</style>" +
               "</head><body>" +
               stickyNav +
               blocksHtml +
               // Lightbox global (galeries d'imatges)
               "<div id=\"lb-ov\" onclick=\"if(event.target===this)closeLb()\">" +
               "<button id=\"lb-cls\" onclick=\"closeLb()\">&times;</button>" +
               "<button id=\"lb-p\" onclick=\"lbMove(-1)\">&#8592;</button>" +
               "<img id=\"lb-img\" src=\"\" alt=\"\">" +
               "<button id=\"lb-n\" onclick=\"lbMove(1)\">&#8594;</button>" +
               "</div>" +
               "<footer class=\"legal-footer\"><div class=\"w\">" +
               "<p>&copy; " + java.time.Year.now() + " " + escapeHtml(landing.getTitle()) + ". Tots els drets reservats.</p>" +
               "<p><a href=\"" + escapeHtml(legalBase) + "/avis-legal\" target=\"_blank\" rel=\"noopener\">Av&iacute;s legal</a> &middot; " +
               "<a href=\"" + escapeHtml(legalBase) + "/politica-privacitat\" target=\"_blank\" rel=\"noopener\">Pol&iacute;tica de privacitat</a> &middot; " +
               "<a href=\"" + escapeHtml(legalBase) + "/cookies\" target=\"_blank\" rel=\"noopener\">Pol&iacute;tica de cookies</a></p>" +
               "</div></footer>" +
               waButton +
               chatWidget +
               cookieBanner +
               buildScrollAnimScript() +
               "</body></html>";
    }

    private record StyleVars(String primary, String accent, String bg, String text, String fontH, String fontB) {}

    private String buildSchemaOrg(String name, String url, String phone, String address, String type) {
        if (phone.isBlank() && address.isBlank()) return "";
        var sb = new StringBuilder();
        sb.append("<script type=\"application/ld+json\">{")
          .append("\"@context\":\"https://schema.org\",")
          .append("\"@type\":\"").append(escapeHtml(type)).append("\",")
          .append("\"name\":\"").append(escapeHtml(name)).append("\",")
          .append("\"url\":\"").append(escapeHtml(url)).append("\"");
        if (!phone.isBlank())   sb.append(",\"telephone\":\"").append(escapeHtml(phone)).append("\"");
        if (!address.isBlank()) sb.append(",\"address\":{\"@type\":\"PostalAddress\",\"streetAddress\":\"").append(escapeHtml(address)).append("\",\"addressCountry\":\"ES\"}");
        sb.append("}</script>");
        return sb.toString();
    }

    private String buildWhatsAppButton(String waNumber, boolean chatPresent) {
        if (waNumber.isBlank()) return "";
        // Quan hi ha widget de xat, pugem el botó de WhatsApp per evitar solapament
        String waExtraStyle = chatPresent ? " style=\"bottom:96px\"" : "";
        return "<a href=\"https://wa.me/" + escapeHtml(waNumber) + "\" target=\"_blank\" rel=\"noopener\" class=\"wa-btn\"" + waExtraStyle + " aria-label=\"Contacta per WhatsApp\">" +
               "<svg width=\"28\" height=\"28\" viewBox=\"0 0 24 24\" fill=\"#fff\">" +
               "<path d=\"M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347z\"/>" +
               "<path d=\"M12 0C5.373 0 0 5.373 0 12c0 2.123.554 4.117 1.528 5.845L0 24l6.336-1.508A11.934 11.934 0 0012 24c6.627 0 12-5.373 12-12S18.627 0 12 0zm0 21.818a9.804 9.804 0 01-5.003-1.368l-.358-.213-3.722.885.916-3.618-.234-.372A9.807 9.807 0 012.182 12C2.182 6.562 6.562 2.182 12 2.182S21.818 6.562 21.818 12 17.438 21.818 12 21.818z\"/>" +
               "</svg></a>";
    }

    /**
     * GA4: el codi es defineix però s'activa NOMES quan el visitant accepta cookies (consent mode).
     * Compleix RGPD/ePrivacy: no s'envia cap dada a Google fins a consentiment explícit.
     */
    private String buildGa4ScriptDeferred(String gaId) {
        if (gaId == null || gaId.isBlank() || !gaId.startsWith("G-")) return "";
        String escapedId = escapeHtml(gaId);
        return "<script>" +
               // Inicialitzar dataLayer i gtag en mode 'denied' per defecte
               "window.dataLayer=window.dataLayer||[];" +
               "function gtag(){dataLayer.push(arguments)}" +
               "gtag('consent','default',{analytics_storage:'denied',ad_storage:'denied'});" +
               // Funció que s'activa quan s'accepten les cookies
               "window.__amgActivateGa=function(){" +
               "var s=document.createElement('script');" +
               "s.async=true;" +
               "s.src='https://www.googletagmanager.com/gtag/js?id=" + escapedId + "';" +
               "document.head.appendChild(s);" +
               "gtag('js',new Date());" +
               "gtag('config','" + escapedId + "');" +
               "gtag('consent','update',{analytics_storage:'granted'});" +
               "};" +
               // Si l'usuari ja havia acceptat prèviament, activar ara directament
               "if(localStorage.getItem('amg_cookie_consent')==='all'){window.__amgActivateGa();}" +
               "</script>";
    }

    private String buildWhatsAppCss() {
        return ".wa-btn{position:fixed;bottom:24px;right:24px;z-index:10000;background:#25d366;width:56px;height:56px;" +
               "border-radius:50%;display:flex;align-items:center;justify-content:center;box-shadow:0 4px 20px rgba(37,211,102,.4);" +
               "text-decoration:none;transition:transform .2s,box-shadow .2s}" +
               ".wa-btn:hover{transform:scale(1.1);box-shadow:0 6px 24px rgba(37,211,102,.6)}" +
               "@media(max-width:640px){.wa-btn{bottom:16px;right:16px;width:52px;height:52px}}";
    }

    private String buildGlobalCss(StyleVars s) {
        return ":root{--p:" + s.primary() + ";--a:" + s.accent() + ";--bg:" + s.bg() + ";--tx:" + s.text() + "}" +
               "*{margin:0;padding:0;box-sizing:border-box}" +
               "body{font-family:" + s.fontB() + ";background:var(--bg);color:var(--tx);line-height:1.6}" +
               "h1,h2,h3,h4{font-family:" + s.fontH() + ";line-height:1.2}" +
               "img{max-width:100%;display:block}" +
               ".w{max-width:1100px;margin:0 auto;padding:0 20px}" +
               ".sec{padding:80px 0}" +
               ".sec-title{font-size:clamp(1.5rem,3vw,2rem);font-weight:700;text-align:center;margin-bottom:48px;color:var(--tx)}" +
               // Botons
               ".btn{display:inline-block;padding:14px 36px;background:var(--p);color:#fff;text-decoration:none;border-radius:8px;font-weight:700;font-size:1rem;transition:opacity .2s,transform .15s}" +
               ".btn:hover{opacity:.88;transform:translateY(-1px)}" +
               ".btn-inv{background:#fff;color:var(--p)}" +
               ".btn-outline{background:transparent;border:2px solid #fff;color:#fff}" +
               ".btn-outline:hover{background:rgba(255,255,255,.12)}" +
               // Cards i grids
               ".card{background:#fff;border-radius:12px;padding:32px 28px;box-shadow:0 2px 16px rgba(0,0,0,.07);transition:transform .2s,box-shadow .2s}" +
               ".card:hover{transform:translateY(-3px);box-shadow:0 8px 28px rgba(0,0,0,.12)}" +
               ".grid-3{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:24px}" +
               ".grid-2{display:grid;grid-template-columns:repeat(auto-fit,minmax(300px,1fr));gap:20px}" +
               // Icona de servei
               ".svc-icon{width:52px;height:52px;border-radius:12px;display:flex;align-items:center;justify-content:center;margin-bottom:20px;font-size:1.6rem;background:var(--p)12}" +
               ".svc-icon svg{display:block}" +
               // Nav sticky
               "#amg-nav{position:sticky;top:0;z-index:8999;background:rgba(255,255,255,.96);backdrop-filter:blur(10px);-webkit-backdrop-filter:blur(10px);border-bottom:1px solid rgba(0,0,0,.07);transition:box-shadow .25s}" +
               "#amg-nav.scrolled{box-shadow:0 2px 20px rgba(0,0,0,.1)}" +
               ".nav-inner{display:flex;align-items:center;justify-content:space-between;height:64px}" +
               ".nav-brand{text-decoration:none;display:flex;align-items:center;gap:10px;font-family:" + s.fontH() + ";font-weight:700;font-size:1.05rem;color:var(--p)}" +
               ".nav-logo{height:36px;object-fit:contain}" +
               ".nav-cta{padding:10px 22px;font-size:.88rem}" +
               // Trust bar
               ".trust-bar{padding:44px 0;background:var(--p)}" +
               ".trust-bar-inner{display:flex;justify-content:center;flex-wrap:wrap;gap:40px 60px;text-align:center}" +
               ".trust-stat{min-width:90px}" +
               ".trust-value{font-family:" + s.fontH() + ";font-size:2.4rem;font-weight:800;line-height:1;color:#fff}" +
               ".trust-label{font-size:.8rem;opacity:.82;margin-top:6px;text-transform:uppercase;letter-spacing:.06em;color:#fff}" +
               // Footer legal
               ".legal-footer{background:#f8fafc;border-top:1px solid #e2e8f0;padding:24px;text-align:center;font-size:14px;color:#718096}" +
               ".legal-footer a{color:var(--p);text-decoration:underline}" +
               // Inputs
               "input,textarea{width:100%;padding:12px 16px;border:1px solid #d1d5db;border-radius:8px;font-family:" + s.fontB() + ";font-size:.95rem;margin-bottom:12px;outline:none}" +
               "input:focus,textarea:focus{border-color:var(--p)}" +
               // Responsive
               "@media(max-width:640px){.sec{padding:56px 0}.sec-title{margin-bottom:32px}.nav-cta{padding:9px 16px;font-size:.82rem}.trust-bar-inner{gap:28px 40px}.trust-value{font-size:1.8rem}}" +
               // Animació scroll
               "[data-anim]{opacity:0;transform:translateY(28px);transition:opacity .55s ease,transform .55s ease}" +
               "[data-anim].visible{opacity:1;transform:none}" +
               // Galeria d'imatges
               ".gallery-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(220px,1fr));gap:10px;margin-top:32px}" +
               ".gl-item{aspect-ratio:4/3;overflow:hidden;border-radius:10px;cursor:zoom-in;position:relative}" +
               ".gl-item img{width:100%;height:100%;object-fit:cover;transition:transform .4s ease,box-shadow .3s}" +
               ".gl-item:hover img{transform:scale(1.08)}" +
               ".gl-item::after{content:'';position:absolute;inset:0;background:rgba(0,0,0,0);transition:background .3s;border-radius:10px}" +
               ".gl-item:hover::after{background:rgba(0,0,0,.18)}" +
               // Lightbox
               "#lb-ov{display:none;position:fixed;inset:0;background:rgba(0,0,0,.93);z-index:20000;align-items:center;justify-content:center;cursor:zoom-out}" +
               "#lb-ov.open{display:flex}" +
               "#lb-img{max-width:90vw;max-height:85vh;object-fit:contain;border-radius:4px;cursor:default;box-shadow:0 0 60px rgba(0,0,0,.8)}" +
               "#lb-cls{position:absolute;top:16px;right:20px;color:#fff;font-size:2rem;cursor:pointer;background:none;border:none;line-height:1;opacity:.8;padding:8px}" +
               "#lb-cls:hover{opacity:1}" +
               "#lb-p,#lb-n{position:absolute;top:50%;transform:translateY(-50%);color:#fff;font-size:1.8rem;cursor:pointer;background:rgba(255,255,255,.15);border:none;padding:14px 20px;border-radius:8px;line-height:1;user-select:none;transition:background .2s}" +
               "#lb-p:hover,#lb-n:hover{background:rgba(255,255,255,.3)}" +
               "#lb-p{left:12px}#lb-n{right:12px}" +
               "@media(max-width:640px){#lb-p,#lb-n{padding:10px 14px;font-size:1.4rem}}";
    }

    @SuppressWarnings("unchecked")
    private String renderTrustBar(Map<String, Object> props, StyleVars s) {
        var items = props.getOrDefault("items", List.of());
        var html = new StringBuilder();
        // Banda diagonal amb skewY — el contingut interior fa counter-skew per quedar recte
        html.append("<div style=\"transform:skewY(-2.5deg);background:var(--p);padding:5vw 0;margin:-3vw 0;position:relative;z-index:2;overflow:hidden\">");
        html.append("<div style=\"transform:skewY(2.5deg);display:flex;justify-content:center;flex-wrap:wrap;gap:40px 60px;text-align:center;padding:16px 20px;max-width:1100px;margin:0 auto\">");
        if (items instanceof List<?> list) {
            for (var item : list) {
                if (item instanceof Map m) {
                    var im = (Map<String, Object>) m;
                    var value = str(im, "value", "");
                    var label = str(im, "label", "");
                    var icon  = str(im, "icon", "");
                    // Extreure part numèrica i sufix per a l'animació de comptador
                    var numStr = value.matches("^[0-9.].*") ? value.replaceAll("^([0-9.]+).*", "$1") : "";
                    var sufStr = numStr.isEmpty() ? "" : value.substring(numStr.length());
                    String countAttr = numStr.isEmpty() ? ""
                            : " data-countup=\"" + numStr + "\" data-suffix=\"" + escapeHtml(sufStr) + "\"";
                    String iconPart = icon.isBlank() ? ""
                            : "<div style=\"font-size:1.6rem;margin-bottom:6px\">" + escapeHtml(icon) + "</div>";
                    html.append("<div class=\"trust-stat\" data-anim>")
                        .append(iconPart)
                        .append("<div class=\"trust-value\"").append(countAttr).append(">").append(escapeHtml(value)).append("</div>")
                        .append("<div class=\"trust-label\">").append(escapeHtml(label)).append("</div>")
                        .append("</div>");
                }
            }
        }
        html.append("</div></div>");
        return html.toString();
    }

    @SuppressWarnings("unchecked")
    private String renderGallery(Map<String, Object> props, StyleVars s) {
        var title = str(props, "title", "");
        var rawItems = props.getOrDefault("items", List.of());
        var imageList = new java.util.ArrayList<Map<String, Object>>();
        if (rawItems instanceof List<?> list) {
            for (var item : list) {
                if (item instanceof Map m) imageList.add((Map<String, Object>) m);
            }
        }
        if (imageList.isEmpty()) return "";

        // Variable JS global per a les dades del lightbox d'aquesta galeria
        var galleryVar = "glr" + Integer.toHexString(System.identityHashCode(props));
        var jsItems = new StringBuilder("[");
        for (int i = 0; i < imageList.size(); i++) {
            var img = imageList.get(i);
            if (i > 0) jsItems.append(",");
            jsItems.append("{u:\"").append(escapeJs(str(img, "url", ""))).append("\"")
                   .append(",a:\"").append(escapeJs(str(img, "alt", ""))).append("\"}");
        }
        jsItems.append("]");

        var html = new StringBuilder();
        html.append("<section class=\"sec\" style=\"background:#f8fafc\"><div class=\"w\" data-anim>");
        if (!title.isBlank()) {
            html.append("<h2 class=\"sec-title\">").append(escapeHtml(title)).append("</h2>");
        }
        html.append("<script>window['").append(galleryVar).append("']=").append(jsItems).append(";</script>");
        html.append("<div class=\"gallery-grid\">");
        for (int i = 0; i < imageList.size(); i++) {
            var img = imageList.get(i);
            var url = str(img, "url", "");
            var alt = str(img, "alt", "");
            if (url.isBlank()) continue;
            html.append("<div class=\"gl-item\" onclick=\"openLb(window['").append(galleryVar).append("'],").append(i).append(")\">");
            html.append("<img src=\"").append(escapeHtml(url)).append("\" alt=\"").append(escapeHtml(alt)).append("\" loading=\"lazy\">");
            html.append("</div>");
        }
        html.append("</div></div></section>");
        return html.toString();
    }

    private String buildStickyNav(String businessName, String logoUrl, StyleVars s) {
        String brand = (logoUrl != null && !logoUrl.isBlank())
                ? "<img src=\"" + escapeHtml(logoUrl) + "\" alt=\"" + escapeHtml(businessName) + "\" class=\"nav-logo\">"
                : escapeHtml(businessName);
        return "<nav id=\"amg-nav\" role=\"navigation\">" +
               "<div class=\"w nav-inner\">" +
               "<a href=\"#\" class=\"nav-brand\">" + brand + "</a>" +
               "<a href=\"#contact\" class=\"btn nav-cta\">Contacta'ns</a>" +
               "</div></nav>";
    }

    private String buildScrollAnimScript() {
        return "<script>(function(){" +
               // Animació scroll
               "var io=new IntersectionObserver(function(e){" +
               "e.forEach(function(x){if(x.isIntersecting){x.target.classList.add('visible');" +
               "io.unobserve(x.target)}})},{threshold:0.1});" +
               "document.querySelectorAll('[data-anim]').forEach(function(el){io.observe(el)});" +
               // Nav shadow en scroll
               "var nav=document.getElementById('amg-nav');" +
               "if(nav){window.addEventListener('scroll',function(){nav.classList.toggle('scrolled',window.scrollY>30);},{passive:true});}" +
               // Comptadors animats (trust-bar i qualsevol [data-countup])
               "document.querySelectorAll('[data-countup]').forEach(function(el){" +
               "var cio=new IntersectionObserver(function(en){en.forEach(function(x){" +
               "if(!x.isIntersecting)return;cio.unobserve(x.target);" +
               "var tgt=parseFloat(x.target.dataset.countup);" +
               "var suf=x.target.dataset.suffix||'';" +
               "var dec=tgt%1!==0,t0=null;" +
               "function anim(ts){if(!t0)t0=ts;" +
               "var p=Math.min((ts-t0)/1200,1),ease=1-Math.pow(1-p,3),cur=tgt*ease;" +
               "x.target.textContent=(dec?cur.toFixed(1):Math.floor(cur))+suf;" +
               "if(p<1)requestAnimationFrame(anim);}" +
               "requestAnimationFrame(anim);" +
               "});},{threshold:0.5});cio.observe(el);});" +
               // Lightbox per a galeries d'imatges
               "var lbData=[],lbIdx=0;" +
               "window.openLb=function(items,idx){lbData=items;lbIdx=idx;" +
               "document.getElementById('lb-img').src=lbData[lbIdx].u;" +
               "document.getElementById('lb-img').alt=lbData[lbIdx].a||'';" +
               "document.getElementById('lb-ov').classList.add('open');" +
               "document.body.style.overflow='hidden';};" +
               "window.closeLb=function(){" +
               "document.getElementById('lb-ov').classList.remove('open');" +
               "document.body.style.overflow='';};" +
               "window.lbMove=function(d){lbIdx=(lbIdx+d+lbData.length)%lbData.length;" +
               "document.getElementById('lb-img').src=lbData[lbIdx].u;};" +
               "document.addEventListener('keydown',function(e){" +
               "var ov=document.getElementById('lb-ov');" +
               "if(!ov||!ov.classList.contains('open'))return;" +
               "if(e.key==='Escape')closeLb();" +
               "if(e.key==='ArrowLeft')lbMove(-1);" +
               "if(e.key==='ArrowRight')lbMove(1);});" +
               "})();</script>";
    }

    private String renderBlock(Map<String, Object> block, StyleVars s, String slug, UUID tenantId) {
        var type = String.valueOf(block.getOrDefault("type", ""));
        var rawProps = block.getOrDefault("props", Map.of());
        Map<String, Object> props;
        if (rawProps instanceof Map) {
            @SuppressWarnings("unchecked")
            var p = (Map<String, Object>) rawProps;
            props = p;
        } else {
            props = Map.of();
        }
        return switch (type) {
            case "hero"          -> renderHero(props, s);
            case "text"          -> renderText(props, s);
            case "services"      -> renderServices(props, s);
            case "contact-form"  -> renderContactForm(props, s, slug);
            case "faq"           -> renderFaq(props, s);
            case "cta"           -> renderCta(props, s);
            case "chat-cta"      -> renderChatCta(props, s);
            case "testimonials"  -> renderTestimonials(props, s);
            case "footer"        -> renderFooterBlock(props, s);
            case "opening-hours" -> renderOpeningHours(props, s);
            case "pricing"       -> renderPricing(props, s);
            case "team"          -> renderTeam(props, s);
            case "video"         -> renderVideo(props, s);
            case "reviews"       -> renderReviews(props, s, tenantId);
            case "trust-bar"     -> renderTrustBar(props, s);
            case "gallery"       -> renderGallery(props, s);
            default              -> "";
        };
    }

    private String renderHero(Map<String, Object> props, StyleVars s) {
        var title      = str(props, "title", "");
        var subtitle   = str(props, "subtitle", "");
        var ctaText    = str(props, "ctaText", "");
        var ctaUrl     = str(props, "ctaLink", str(props, "ctaUrl", "#contact"));
        var ctaAction  = str(props, "ctaAction", "link");
        var ctaSecText = str(props, "ctaSecondaryText", "");
        var ctaSecUrl  = str(props, "ctaSecondaryLink", str(props, "ctaSecondaryUrl", "#contact"));
        var bgImage    = str(props, "bgImage", str(props, "bgImageUrl", ""));

        var bgCss = bgImage.isBlank()
                ? "background:linear-gradient(135deg," + s.accent() + " 0%," + s.primary() + " 100%)"
                : "background:linear-gradient(135deg," + s.primary() + "e0 0%," + s.primary() + "88 100%)," +
                  "url('" + escapeHtml(bgImage) + "') center/cover no-repeat";

        String ctaPrimary = "";
        if (!ctaText.isBlank()) {
            ctaPrimary = "chat".equals(ctaAction)
                ? "<button class=\"btn btn-inv\" onclick=\"openChatWidget()\">" + escapeHtml(ctaText) + "</button>"
                : "<a href=\"" + escapeHtml(ctaUrl) + "\" class=\"btn btn-inv\">" + escapeHtml(ctaText) + "</a>";
        }
        String ctaSecondary = ctaSecText.isBlank() ? ""
                : "<a href=\"" + escapeHtml(ctaSecUrl) + "\" class=\"btn btn-outline\">" + escapeHtml(ctaSecText) + "</a>";

        String ctasHtml = (ctaPrimary + ctaSecondary).isBlank() ? ""
                : "<div style=\"display:flex;gap:16px;justify-content:center;flex-wrap:wrap;margin-top:36px\">" + ctaPrimary + ctaSecondary + "</div>";

        return "<section style=\"" + bgCss + ";color:#fff;position:relative;clip-path:polygon(0 0,100% 0,100% 93%,0 100%);padding-bottom:40px\">" +
               "<div class=\"w\" style=\"padding:110px 20px 80px;text-align:center\">" +
               "<h1 style=\"font-size:clamp(2rem,5vw,3.6rem);font-weight:800;margin-bottom:20px;text-shadow:0 2px 20px rgba(0,0,0,.25)\">" + escapeHtml(title) + "</h1>" +
               "<p style=\"font-size:clamp(1rem,2.5vw,1.25rem);opacity:.9;max-width:640px;margin:0 auto;line-height:1.75;text-shadow:0 1px 8px rgba(0,0,0,.2)\">" + escapeHtml(subtitle) + "</p>" +
               ctasHtml +
               "</div></section>";
    }

    private String renderChatCta(Map<String, Object> props, StyleVars s) {
        var title       = str(props, "title", "Reserva la teva cita");
        var subtitle    = str(props, "subtitle", "Respon en menys d'1 minut");
        var buttonText  = str(props, "buttonText", "Xateja amb nosaltres");
        var accentColor = str(props, "accentColor", s.primary());
        return "<section class=\"sec\" style=\"background:" + s.bg() + "\">" +
               "<div class=\"w\" style=\"text-align:center\" data-anim>" +
               "<h2 class=\"sec-title\">" + escapeHtml(title) + "</h2>" +
               "<p style=\"opacity:.7;margin-bottom:32px\">" + escapeHtml(subtitle) + "</p>" +
               "<button onclick=\"openChatWidget()\" style=\"" +
               "background:" + escapeHtml(accentColor) + ";color:#fff;border:none;cursor:pointer;" +
               "padding:16px 40px;border-radius:8px;font-size:1.1rem;font-weight:700;" +
               "box-shadow:0 4px 20px rgba(0,0,0,.15);transition:opacity .2s\" " +
               "onmouseover=\"this.style.opacity='.85'\" onmouseout=\"this.style.opacity='1'\">" +
               escapeHtml(buttonText) + "</button>" +
               "</div></section>";
    }

    private String buildChatWidget(String landingSlug, String primaryColor, String businessName) {
        String pc = escapeHtml(primaryColor);
        return "<div id=\"amg-chat-widget\">" +
               "<button id=\"amg-chat-btn\" onclick=\"toggleChatPanel()\" " +
               "style=\"position:fixed;bottom:24px;right:24px;z-index:10001;width:60px;height:60px;" +
               "border-radius:50%;background:" + pc + ";border:none;cursor:pointer;" +
               "box-shadow:0 4px 20px rgba(0,0,0,.25);display:flex;align-items:center;justify-content:center;" +
               "transition:transform .2s\" " +
               "onmouseover=\"this.style.transform='scale(1.1)'\" onmouseout=\"this.style.transform='scale(1)'\">" +
               "<svg width=\"28\" height=\"28\" viewBox=\"0 0 24 24\" fill=\"#fff\">" +
               "<path d=\"M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2z\"/></svg>" +
               "</button>" +
               "<div id=\"amg-chat-panel\" style=\"display:none;position:fixed;bottom:96px;right:24px;" +
               "width:340px;height:75vh;max-height:560px;z-index:10001;border-radius:16px;" +
               "box-shadow:0 8px 40px rgba(0,0,0,.2);background:#fff;" +
               "flex-direction:column;overflow:hidden\">" +
               // Capçalera
               "<div style=\"background:" + pc + ";padding:16px 20px;color:#fff;display:flex;align-items:center;gap:12px;flex-shrink:0\">" +
               "<div style=\"width:40px;height:40px;border-radius:50%;background:rgba(255,255,255,.25);display:flex;align-items:center;justify-content:center\">" +
               "<svg width=\"22\" height=\"22\" viewBox=\"0 0 24 24\" fill=\"#fff\"><path d=\"M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 14H9V8h2v8zm4 0h-2V8h2v8z\"/></svg></div>" +
               "<div><div style=\"font-weight:700;font-size:.95rem\">" + escapeHtml(businessName) + "</div>" +
               "<div style=\"font-size:.75rem;opacity:.85\">&#x25cf; En l&iacute;nia</div></div>" +
               "<button onclick=\"closeChatPanel()\" style=\"margin-left:auto;background:none;border:none;color:#fff;font-size:1.4rem;cursor:pointer;line-height:1\">&times;</button>" +
               "</div>" +
               // Formulari pre-xat
               "<div id=\"amg-prechat\" style=\"flex:1;display:flex;flex-direction:column;justify-content:center;padding:24px 20px;gap:12px\">" +
               "<p style=\"margin:0 0 4px;font-size:.9rem;font-weight:600;color:#1a1a1a\">Abans de començar...</p>" +
               "<p style=\"margin:0 0 12px;font-size:.82rem;color:#6b7280\">Deixa'ns el teu nom i telèfon perquè puguem contactar-te si cal.</p>" +
               "<input id=\"amg-pc-name\" type=\"text\" placeholder=\"El teu nom *\" maxlength=\"100\" " +
               "style=\"border:1px solid #e0e0e0;border-radius:8px;padding:10px 14px;font-size:.88rem;outline:none;width:100%;box-sizing:border-box\" />" +
               "<input id=\"amg-pc-phone\" type=\"tel\" placeholder=\"Tel&egrave;fon *\" maxlength=\"20\" " +
               "style=\"border:1px solid #e0e0e0;border-radius:8px;padding:10px 14px;font-size:.88rem;outline:none;width:100%;box-sizing:border-box\" " +
               "onkeydown=\"if(event.key==='Enter')submitPreChat()\" />" +
               // Consentiment RGPD obligatori al xat
               "<div style=\"display:flex;align-items:flex-start;gap:8px;margin-top:4px\">" +
               "<input id=\"amg-pc-consent\" type=\"checkbox\" style=\"width:auto;margin-top:3px;flex-shrink:0;cursor:pointer\" />" +
               "<label for=\"amg-pc-consent\" style=\"font-size:.78rem;color:#6b7280;cursor:pointer;line-height:1.4\">" +
               "Accepto que les meves dades siguin tractades per atendre la meva consulta.</label>" +
               "</div>" +
               "<div id=\"amg-pc-err\" style=\"color:#e53e3e;font-size:.8rem;display:none\">Omple els camps obligatoris i accepta el tractament de dades.</div>" +
               "<button onclick=\"submitPreChat()\" " +
               "style=\"background:" + pc + ";color:#fff;border:none;border-radius:8px;padding:12px;font-size:.9rem;" +
               "font-weight:600;cursor:pointer;width:100%\">Iniciar xat &rarr;</button>" +
               "</div>" +
               // Àrea de missatges (oculta fins que comença la sessió)
               "<div id=\"amg-chat-messages\" style=\"flex:1;overflow-y:auto;padding:16px;display:none;flex-direction:column;gap:10px\"></div>" +
               "<div id=\"amg-chat-footer\" style=\"display:none;padding:12px 16px;border-top:1px solid #f0f0f0;gap:8px\">" +
               "<input id=\"amg-chat-input\" type=\"text\" placeholder=\"Escriu el teu missatge...\" maxlength=\"500\" " +
               "style=\"flex:1;border:1px solid #e0e0e0;border-radius:20px;padding:10px 16px;font-size:.9rem;outline:none\" " +
               "onkeydown=\"if(event.key==='Enter')sendChatMessage()\" />" +
               "<button onclick=\"sendChatMessage()\" " +
               "style=\"background:" + pc + ";border:none;border-radius:50%;width:40px;height:40px;" +
               "cursor:pointer;display:flex;align-items:center;justify-content:center;flex-shrink:0\">" +
               "<svg width=\"18\" height=\"18\" viewBox=\"0 0 24 24\" fill=\"#fff\"><path d=\"M2.01 21L23 12 2.01 3 2 10l15 2-15 2z\"/></svg>" +
               "</button></div></div></div>" +
               buildChatScript(landingSlug, apiBaseUrl);
    }

    private String buildChatScript(String landingSlug, String apiBase) {
        return "<script>(function(){" +
               "var SLUG='" + escapeJs(landingSlug) + "';" +
               "var BASE='" + escapeJs(apiBase.replaceAll("/+$", "")) + "/api/v1/chat';" +
               "var sessionId=localStorage.getItem('amg_chat_sid_'+SLUG);" +
               "var panel=null,msgs=null,input=null,footer=null,prechat=null,opened=false;" +
               "function init(){" +
               "panel=document.getElementById('amg-chat-panel');" +
               "msgs=document.getElementById('amg-chat-messages');" +
               "input=document.getElementById('amg-chat-input');" +
               "footer=document.getElementById('amg-chat-footer');" +
               "prechat=document.getElementById('amg-prechat');}" +
               "function showChat(){" +
               "prechat.style.display='none';" +
               "msgs.style.display='flex';" +
               "footer.style.display='flex';}" +
               "window.toggleChatPanel=function(){init();" +
               "if(!opened){panel.style.display='flex';opened=true;" +
               "if(sessionId)showChat();}else{panel.style.display='none';opened=false;}};" +
               "window.openChatWidget=function(){init();" +
               "panel.style.display='flex';opened=true;" +
               "if(sessionId)showChat();};" +
               "window.closeChatPanel=function(){init();" +
               "panel.style.display='none';opened=false;};" +
               "window.submitPreChat=function(){init();" +
               "var name=document.getElementById('amg-pc-name').value.trim();" +
               "var phone=document.getElementById('amg-pc-phone').value.trim();" +
               "var err=document.getElementById('amg-pc-err');" +
               "var consent=document.getElementById('amg-pc-consent').checked;" +
               "if(!name||!phone||!consent){err.style.display='block';return;}" +
               "err.style.display='none';" +
               "showChat();" +
               "startSession(name,phone);};" +
               "function startSession(name,phone){" +
               "addMsg('assistant','...',true);" +
               "fetch(BASE+'/sessions',{method:'POST',headers:{'Content-Type':'application/json'}," +
               "body:JSON.stringify({landingSlug:SLUG,contactName:name||null,contactPhone:phone||null})})" +
               ".then(function(r){return r.json();})" +
               ".then(function(d){" +
               "removeTyping();" +
               "if(d.sessionId){sessionId=d.sessionId;" +
               "localStorage.setItem('amg_chat_sid_'+SLUG,sessionId);" +
               "addMsg('assistant',d.greeting||'Hola! En què puc ajudar-te?');}})" +
               ".catch(function(){removeTyping();addMsg('assistant','Ho sent, en aquest moment no puc respondre.');});}" +
               "window.sendChatMessage=function(){init();" +
               "var txt=input.value.trim();if(!txt||!sessionId)return;" +
               "input.value='';addMsg('user',txt);" +
               "addMsg('assistant','...',true);" +
               "fetch(BASE+'/sessions/'+sessionId+'/messages',{method:'POST',headers:{'Content-Type':'application/json'}," +
               "body:JSON.stringify({message:txt})})" +
               ".then(function(r){return r.json();})" +
               ".then(function(d){removeTyping();" +
               "if(d.terminated){addMsg('assistant',d.reply);input.disabled=true;" +
               "setTimeout(function(){closeChatPanel();localStorage.removeItem('amg_chat_sid_'+SLUG);sessionId=null;},3000);" +
               "}else{addMsg('assistant',d.reply);}})" +
               ".catch(function(){removeTyping();addMsg('assistant','Error en enviar el missatge.');});};" +
               "function addMsg(role,text,isTyping){init();" +
               "var d=document.createElement('div');" +
               "d.style.cssText='max-width:80%;padding:10px 14px;border-radius:16px;font-size:.88rem;line-height:1.5;word-break:break-word;';" +
               "if(role==='user'){d.style.cssText+=';background:" + escapeJs(buildChatUserBubbleColor()) + ";" +
               "color:#fff;align-self:flex-end;border-bottom-right-radius:4px';" +
               "}else{d.style.cssText+=';background:#f3f4f6;color:#1a1a1a;align-self:flex-start;border-bottom-left-radius:4px';}" +
               "if(isTyping)d.setAttribute('id','amg-typing');" +
               "d.textContent=text;" +
               "msgs.appendChild(d);msgs.scrollTop=msgs.scrollHeight;}" +
               "function removeTyping(){var t=document.getElementById('amg-typing');if(t)t.remove();}" +
               "})();</script>";
    }

    private String buildChatUserBubbleColor() {
        // Retorna un color lleugerament més fosc per als missatges de l'usuari
        return "#374151";
    }

    /** Banner de cookies RGPD per a microlandings — CSS injectat al <style> */
    private String buildCookieBannerCss() {
        return "#amg-cookie-banner{position:fixed;bottom:0;left:0;right:0;z-index:9999;background:#1e293b;color:#e2e8f0;" +
               "padding:16px 24px;display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:12px;" +
               "box-shadow:0 -4px 24px rgba(0,0,0,.35);font-size:.85rem;line-height:1.5}" +
               "#amg-cookie-banner a{color:#93c5fd;text-decoration:underline}" +
               "#amg-cookie-banner.hidden{display:none}" +
               ".amg-cookie-btn{padding:10px 20px;border:none;border-radius:6px;font-weight:600;" +
               "font-size:.82rem;cursor:pointer;white-space:nowrap}" +
               ".amg-cookie-accept{background:var(--p,#2563eb);color:#fff}" +
               ".amg-cookie-reject{background:transparent;color:#94a3b8;border:1px solid #475569}" +
               "@media(max-width:640px){#amg-cookie-banner{flex-direction:column;align-items:flex-start}}";
    }

    /** Widget HTML del banner de cookies + JavaScript de gestió del consentiment */
    private String buildCookieBanner(String primaryColor, String legalBase) {
        return "<div id=\"amg-cookie-banner\">" +
               "<p style=\"margin:0;max-width:640px\">" +
               "Fem servir <strong>cookies t&egrave;cniques</strong> i, si ens ho permets, cookies <strong>anal&iacute;tiques</strong> " +
               "per millorar l'experi&egrave;ncia. " +
               "<a href=\"" + escapeHtml(legalBase) + "/cookies\" target=\"_blank\" rel=\"noopener\">M&eacute;s informaci&oacute;</a>." +
               "</p>" +
               "<div style=\"display:flex;gap:10px;flex-shrink:0\">" +
               "<button class=\"amg-cookie-btn amg-cookie-reject\" onclick=\"amgCookieReject()\">Nomes essencials</button>" +
               "<button class=\"amg-cookie-btn amg-cookie-accept\" onclick=\"amgCookieAccept()\">Acceptar-ho tot</button>" +
               "</div></div>" +
               "<script>" +
               "(function(){" +
               "var banner=document.getElementById('amg-cookie-banner');" +
               // Si ja hi ha consentiment guardat, amagar el banner
               "if(localStorage.getItem('amg_cookie_consent')){banner.classList.add('hidden');return;}" +
               "window.amgCookieAccept=function(){" +
               "localStorage.setItem('amg_cookie_consent','all');" +
               "banner.classList.add('hidden');" +
               // Activar GA4 si estava configurat
               "if(typeof window.__amgActivateGa==='function')window.__amgActivateGa();" +
               "};" +
               "window.amgCookieReject=function(){" +
               "localStorage.setItem('amg_cookie_consent','necessary');" +
               "banner.classList.add('hidden');" +
               "};" +
               "})();</script>";
    }

    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
    }

    private String renderText(Map<String, Object> props, StyleVars s) {
        var title = str(props, "title", "");
        var body  = str(props, "body", "");
        return "<section class=\"sec\" style=\"background:" + s.bg() + "\">" +
               "<div class=\"w\" style=\"max-width:760px\" data-anim>" +
               "<h2 style=\"font-size:2rem;font-weight:700;margin-bottom:20px;color:" + s.text() + "\">" + escapeHtml(title) + "</h2>" +
               "<div style=\"opacity:.8;line-height:1.8;font-size:1.05rem\">" + sanitizeBody(body) + "</div>" +
               "</div></section>";
    }

    @SuppressWarnings("unchecked")
    private String renderServices(Map<String, Object> props, StyleVars s) {
        var title = str(props, "title", "Serveis");
        var items = props.getOrDefault("items", List.of());
        var html  = new StringBuilder();
        html.append("<section class=\"sec\" style=\"background:").append(s.bg()).append("\">")
            .append("<div class=\"w\"><h2 class=\"sec-title\">").append(escapeHtml(title)).append("</h2>")
            .append("<div class=\"grid-3\">");
        if (items instanceof List<?> list) {
            for (var item : list) {
                if (item instanceof Map m) {
                    var im = (Map<String, Object>) m;
                    var name = str(im, "title", str(im, "name", ""));
                    var desc = str(im, "description", str(im, "desc", ""));
                    var icon = str(im, "icon", "");
                    String iconHtml = buildServiceIconHtml(icon, s.primary());
                    html.append("<div class=\"card\" data-anim>")
                        .append(iconHtml)
                        .append("<h3 style=\"font-weight:700;margin-bottom:10px;color:").append(s.text()).append(";font-size:1.05rem\">").append(escapeHtml(name)).append("</h3>")
                        .append("<p style=\"opacity:.65;font-size:.93rem;line-height:1.65\">").append(escapeHtml(desc)).append("</p>")
                        .append("</div>");
                }
            }
        }
        html.append("</div></div></section>");
        return html.toString();
    }

    private String buildServiceIconHtml(String icon, String primaryColor) {
        if (icon == null || icon.isBlank()) {
            return "<div class=\"svc-icon\" style=\"background:" + primaryColor + "18\">" + svgIcon("star", primaryColor) + "</div>";
        }
        if (icon.startsWith("http://") || icon.startsWith("https://")) {
            return "<div class=\"svc-icon\" style=\"background:transparent\"><img src=\"" + escapeHtml(icon) + "\" alt=\"\" style=\"width:48px;height:48px;object-fit:contain\"></div>";
        }
        // Emoji o text curt (≤4 caràcters)
        if (icon.length() <= 4) {
            return "<div class=\"svc-icon\" style=\"background:" + primaryColor + "18;font-size:1.8rem\">" + escapeHtml(icon) + "</div>";
        }
        // Nom d'icona coneguda
        return "<div class=\"svc-icon\" style=\"background:" + primaryColor + "18\">" + svgIcon(icon.toLowerCase(), primaryColor) + "</div>";
    }

    private String svgIcon(String name, String color) {
        String c = escapeHtml(color);
        String path = switch (name) {
            case "calendar"    -> "M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z";
            case "clock"       -> "M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z";
            case "phone"       -> "M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z";
            case "heart"       -> "M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z";
            case "user"        -> "M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z";
            case "scissors"    -> "M6 9l6 6m0 0l6-6m-6 6V3m0 12v6";
            case "check"       -> "M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z";
            case "star"        -> "M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z";
            case "home"        -> "M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6";
            case "wrench","tool"-> "M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M15 12a3 3 0 11-6 0 3 3 0 016 0z";
            case "leaf","plant" -> "M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z";
            case "shield"      -> "M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z";
            case "award"       -> "M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z";
            case "zap","bolt"  -> "M13 10V3L4 14h7v7l9-11h-7z";
            case "map-pin","location" -> "M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z M15 11a3 3 0 11-6 0 3 3 0 016 0z";
            case "smile"       -> "M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z";
            case "graduation-cap","cap" -> "M12 14l9-5-9-5-9 5 9 5z M12 14l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z";
            default            -> "M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z";
        };
        return "<svg width=\"26\" height=\"26\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"" + c +
               "\" stroke-width=\"1.8\" stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"" + path + "\"/></svg>";
    }

    private String renderContactForm(Map<String, Object> props, StyleVars s, String slug) {
        var title = str(props, "title", "Contacte");
        return "<section class=\"sec\" id=\"contact\" style=\"background:" + s.accent() + "10\">" +
               "<div class=\"w\" style=\"max-width:560px\">" +
               "<h2 class=\"sec-title\" data-anim>" + escapeHtml(title) + "</h2>" +
               "<form action=\"#\" method=\"POST\" onsubmit=\"return false\" style=\"background:#fff;padding:32px;border-radius:8px;box-shadow:0 2px 12px rgba(0,0,0,.08)\">" +
               "<input name=\"nom\" placeholder=\"Nom i cognoms *\" type=\"text\" required>" +
               "<input name=\"email\" placeholder=\"Correu electrònic *\" type=\"email\" required>" +
               "<input name=\"telefon\" placeholder=\"Telèfon\" type=\"tel\">" +
               "<textarea name=\"missatge\" placeholder=\"El vostre missatge\" rows=\"4\" style=\"resize:vertical\"></textarea>" +
               "<div style=\"margin-bottom:16px;display:flex;align-items:flex-start;gap:8px\">" +
               "<input id=\"amg-privacy-consent\" type=\"checkbox\" required style=\"width:auto;margin-top:6px;cursor:pointer;margin-bottom:0\">" +
               "<label for=\"amg-privacy-consent\" style=\"font-size:0.85rem;color:#4b5563;cursor:pointer\">" +
               "Accepto la <a href=\"/legal/politica-de-privacitat\" target=\"_blank\" style=\"color:" + s.primary() + ";text-decoration:underline\">pol&iacute;tica de privacitat</a> i l'ús de les meves dades per a aquesta consulta." +
               "</label></div>" +
               "<button type=\"submit\" style=\"width:100%;padding:14px;background:" + s.primary() + ";color:#fff;border:none;border-radius:8px;font-weight:700;font-size:1rem;cursor:pointer\">Enviar missatge</button>" +
               "</form></div>" +
               "<script>" +
               "(function() {" +
               "  var form = document.querySelector('#contact form');" +
               "  if (!form) return;" +
               "  var statusDiv = document.createElement('div');" +
               "  statusDiv.style.marginTop = '16px';" +
               "  statusDiv.style.fontSize = '0.9rem';" +
               "  statusDiv.style.fontWeight = '600';" +
               "  statusDiv.style.display = 'none';" +
               "  form.appendChild(statusDiv);" +
               "  var btn = form.querySelector('button[type=\"submit\"]');" +
               "  var getUtm = function(name) {" +
               "    var match = RegExp('[?&]' + name + '=([^&]*)').exec(window.location.search);" +
               "    var val = match && decodeURIComponent(match[1].replace(/\\+/g, ' '));" +
               "    if (val) {" +
               "      sessionStorage.setItem('amg_' + name, val);" +
               "      return val;" +
               "    }" +
               "    return sessionStorage.getItem('amg_' + name);" +
               "  };" +
               "  btn.onclick = function(e) {" +
               "    e.preventDefault();" +
               "    var name = form.querySelector('input[name=\"nom\"]').value.trim();" +
               "    var email = form.querySelector('input[name=\"email\"]').value.trim();" +
               "    var phone = form.querySelector('input[name=\"telefon\"]').value.trim();" +
               "    var msg = form.querySelector('textarea[name=\"missatge\"]').value.trim();" +
               "    var consent = form.querySelector('#amg-privacy-consent').checked;" +
               "    if (!name || !email || !consent) {" +
               "      statusDiv.style.color = '#e53e3e';" +
               "      statusDiv.textContent = 'Si us plau, omple els camps obligatoris i accepta la política de privacitat.';" +
               "      statusDiv.style.display = 'block';" +
               "      return;" +
               "    }" +
               "    btn.disabled = true;" +
               "    btn.textContent = 'Enviant...';" +
               "    statusDiv.style.display = 'none';" +
               "    var payload = {" +
               "      name: name," +
               "      email: email," +
               "      phone: phone," +
               "      message: msg," +
               "      consent: consent," +
               "      utmSource: getUtm('utm_source')," +
               "      utmMedium: getUtm('utm_medium')," +
               "      utmCampaign: getUtm('utm_campaign')" +
               "    };" +
               "    fetch('/api/v1/engine/render/" + escapeJs(slug) + "/contact', {" +
               "      method: 'POST'," +
               "      headers: { 'Content-Type': 'application/json' }," +
               "      body: JSON.stringify(payload)" +
               "    })" +
               "    .then(function(res) {" +
               "      if (!res.ok) throw new Error();" +
               "      return res.json();" +
               "    })" +
               "    .then(function(d) {" +
               "      statusDiv.style.color = '#10b981';" +
               "      statusDiv.textContent = d.message || 'Missatge rebut correctament.';" +
               "      statusDiv.style.display = 'block';" +
               "      form.reset();" +
               "      btn.textContent = 'Missatge enviat!';" +
               "    })" +
               "    .catch(function() {" +
               "      btn.disabled = false;" +
               "      btn.textContent = 'Enviar missatge';" +
               "      statusDiv.style.color = '#e53e3e';" +
               "      statusDiv.textContent = 'S\\'ha produït un error en enviar el missatge. Intenta-ho de nou.';" +
               "      statusDiv.style.display = 'block';" +
               "    });" +
               "  };" +
               "})();" +
               "</script></section>";
    }

    @SuppressWarnings("unchecked")
    private String renderFaq(Map<String, Object> props, StyleVars s) {
        var title = str(props, "title", "Preguntes freqüents");
        var items = props.getOrDefault("items", List.of());
        var html  = new StringBuilder();
        html.append("<section class=\"sec\" style=\"background:").append(s.bg()).append("\">")
            .append("<div class=\"w\" style=\"max-width:760px\"><h2 class=\"sec-title\">").append(escapeHtml(title)).append("</h2>")
            .append("<div style=\"display:flex;flex-direction:column;gap:12px\">");
        if (items instanceof List<?> list) {
            for (var item : list) {
                if (item instanceof Map m) {
                    var im = (Map<String, Object>) m;
                    var q  = str(im, "question", str(im, "q", ""));
                    var a  = str(im, "answer",   str(im, "a", ""));
                    html.append("<div class=\"card\" data-anim style=\"border-left:4px solid ").append(s.primary()).append("\">")
                        .append("<h3 style=\"font-weight:700;margin-bottom:8px;font-size:1rem\">").append(escapeHtml(q)).append("</h3>")
                        .append("<p style=\"opacity:.65;font-size:.95rem;line-height:1.6\">").append(escapeHtml(a)).append("</p>")
                        .append("</div>");
                }
            }
        }
        html.append("</div></div></section>");
        return html.toString();
    }

    private String renderCta(Map<String, Object> props, StyleVars s) {
        var title   = str(props, "title", str(props, "text", ""));
        var btnText = str(props, "ctaText", str(props, "buttonText", "Contacta"));
        var btnUrl  = str(props, "ctaLink", str(props, "buttonUrl", "#contact"));
        return "<section style=\"background:linear-gradient(135deg," + s.primary() + " 0%," + s.accent() + " 100%);text-align:center;padding:80px 20px\">" +
               "<div class=\"w\" data-anim>" +
               "<h2 style=\"font-size:clamp(1.5rem,4vw,2.5rem);font-weight:800;color:#fff;margin-bottom:28px\">" + escapeHtml(title) + "</h2>" +
               "<a href=\"" + escapeHtml(btnUrl) + "\" class=\"btn btn-inv\">" + escapeHtml(btnText) + "</a>" +
               "</div></section>";
    }

    @SuppressWarnings("unchecked")
    private String renderTestimonials(Map<String, Object> props, StyleVars s) {
        var title = str(props, "title", "Testimonis");
        var items = props.getOrDefault("items", List.of());
        var html  = new StringBuilder();
        html.append("<section class=\"sec\" style=\"background:").append(s.accent()).append("10\">")
            .append("<div class=\"w\"><h2 class=\"sec-title\">").append(escapeHtml(title)).append("</h2>")
            .append("<div class=\"grid-3\">");
        if (items instanceof List<?> list) {
            for (var item : list) {
                if (item instanceof Map m) {
                    var im     = (Map<String, Object>) m;
                    var name   = str(im, "name", "");
                    var text   = str(im, "text", "");
                    var rating = im.getOrDefault("rating", 5);
                    var stars  = "★".repeat(rating instanceof Number n ? n.intValue() : 5);
                    html.append("<div class=\"card\" data-anim>")
                        .append("<div style=\"color:#f59e0b;margin-bottom:12px\">").append(stars).append("</div>")
                        .append("<p style=\"opacity:.75;font-size:.95rem;line-height:1.7;font-style:italic;margin-bottom:16px\">&ldquo;").append(escapeHtml(text)).append("&rdquo;</p>")
                        .append("<p style=\"font-weight:700;font-size:.9rem;color:").append(s.primary()).append("\">").append(escapeHtml(name)).append("</p>")
                        .append("</div>");
                }
            }
        }
        html.append("</div></div></section>");
        return html.toString();
    }

    private String renderFooterBlock(Map<String, Object> props, StyleVars s) {
        var copyright = str(props, "copyright", "© " + java.time.Year.now());
        return "<footer style=\"background:" + s.accent() + ";color:#fff;padding:40px 20px;text-align:center\">" +
               "<p style=\"opacity:.7;font-size:.9rem\">" + escapeHtml(copyright) + "</p>" +
               "</footer>";
    }

    @SuppressWarnings("unchecked")
    private String renderOpeningHours(Map<String, Object> props, StyleVars s) {
        var title = str(props, "title", "Horaris d'atenció");
        var items = props.getOrDefault("hours", List.of());
        var todayDow = java.time.LocalDate.now().getDayOfWeek().getValue(); // 1=Mon..7=Sun
        // Map frontend day-index (0=Dilluns) to Java DayOfWeek (Mon=1..Sun=7)
        var html = new StringBuilder();
        html.append("<section class=\"sec\" style=\"background:").append(s.bg()).append("\">")
            .append("<div class=\"w\" style=\"max-width:560px\">")
            .append("<h2 class=\"sec-title\">").append(escapeHtml(title)).append("</h2>")
            .append("<div style=\"border-radius:8px;overflow:hidden;border:1px solid ").append(s.accent()).append("20\">");
        if (items instanceof List<?> list) {
            int idx = 0;
            for (var item : list) {
                if (item instanceof Map m) {
                    var im = (Map<String, Object>) m;
                    var day = str(im, "day", "");
                    var open = str(im, "open", "");
                    var close = str(im, "close", "");
                    var closed = Boolean.TRUE.equals(im.get("closed"));
                    // Determine if today (idx 0=Mon..6=Sun maps to DayOfWeek 1..7)
                    var isToday = (idx + 1) == todayDow || (idx == 6 && todayDow == 7);
                    var bg = isToday ? s.primary() + "12" : (idx % 2 == 0 ? "#fff" : s.accent() + "05");
                    var border = isToday ? "border-left:4px solid " + s.primary() + ";" : "border-left:4px solid transparent;";
                    html.append("<div style=\"display:flex;justify-content:space-between;align-items:center;padding:12px 20px;background:").append(bg).append(";").append(border).append("\">")
                        .append("<span style=\"font-weight:").append(isToday ? "700" : "400").append(";color:").append(isToday ? s.primary() : s.text()).append(";font-size:.95rem\">").append(escapeHtml(day)).append("</span>")
                        .append("<span style=\"color:").append(closed ? "#94a3b8" : s.text()).append(";font-size:.9rem;font-weight:").append(isToday ? "600" : "400").append("\">")
                        .append(closed ? "Tancat" : escapeHtml(open) + " – " + escapeHtml(close))
                        .append("</span></div>");
                    idx++;
                }
            }
        }
        html.append("</div></div></section>");
        return html.toString();
    }

    private String str(Map<String, Object> m, String key, String def) {
        var v = m.get(key);
        return v != null ? v.toString() : def;
    }

    @SuppressWarnings("unchecked")
    private String renderPricing(Map<String, Object> props, StyleVars s) {
        var title = str(props, "title", "Tarifes");
        var items = props.getOrDefault("items", List.of());
        var html  = new StringBuilder();
        html.append("<section class=\"sec\" style=\"background:").append(s.bg()).append("\">")
            .append("<div class=\"w\"><h2 class=\"sec-title\" data-anim>").append(escapeHtml(title)).append("</h2>")
            .append("<div class=\"grid-3\">");
        if (items instanceof List<?> list) {
            for (var item : list) {
                if (item instanceof Map m) {
                    var im          = (Map<String, Object>) m;
                    var name        = str(im, "name", "");
                    var desc        = str(im, "description", str(im, "desc", ""));
                    var price       = str(im, "price", "0");
                    var period      = str(im, "period", "mes");
                    var highlighted = Boolean.TRUE.equals(im.get("highlighted"));
                    var features    = im.getOrDefault("features", List.of());
                    var bg          = highlighted ? s.primary() : "#fff";
                    var fg          = highlighted ? "#fff" : s.text();
                    html.append("<div class=\"card\" data-anim style=\"background:").append(bg)
                        .append(";color:").append(fg)
                        .append(highlighted ? ";box-shadow:0 8px 32px " + s.primary() + "55" : "")
                        .append("\">");
                    if (highlighted) html.append("<p style=\"text-align:center;background:").append(s.accent()).append(";color:#fff;font-size:.75rem;font-weight:700;padding:4px 14px;border-radius:99px;margin-bottom:16px\">Recomanat</p>");
                    html.append("<h3 style=\"font-weight:700;font-size:1.2rem;margin-bottom:8px\">").append(escapeHtml(name)).append("</h3>")
                        .append("<p style=\"opacity:.65;font-size:.85rem;margin-bottom:16px\">").append(escapeHtml(desc)).append("</p>")
                        .append("<p style=\"font-size:2.2rem;font-weight:800;margin-bottom:20px\">").append(escapeHtml(price))
                        .append("<span style=\"font-size:.9rem;font-weight:400;opacity:.6\">€/").append(escapeHtml(period)).append("</span></p>")
                        .append("<ul style=\"list-style:none;padding:0;display:flex;flex-direction:column;gap:8px\">");
                    if (features instanceof List<?> flist) {
                        for (var f : flist) {
                            html.append("<li style=\"font-size:.9rem\">✓ ").append(escapeHtml(f.toString())).append("</li>");
                        }
                    }
                    html.append("</ul></div>");
                }
            }
        }
        html.append("</div></div></section>");
        return html.toString();
    }

    @SuppressWarnings("unchecked")
    private String renderTeam(Map<String, Object> props, StyleVars s) {
        var title = str(props, "title", "L'equip");
        var items = props.getOrDefault("items", List.of());
        var html  = new StringBuilder();
        html.append("<section class=\"sec\" style=\"background:").append(s.bg()).append("\">")
            .append("<div class=\"w\"><h2 class=\"sec-title\" data-anim>").append(escapeHtml(title)).append("</h2>")
            .append("<div class=\"grid-3\">");
        if (items instanceof List<?> list) {
            for (var item : list) {
                if (item instanceof Map m) {
                    var im    = (Map<String, Object>) m;
                    var name  = str(im, "name", "");
                    var role  = str(im, "role", "");
                    var bio   = str(im, "bio", "");
                    var photo = str(im, "photo", "");
                    html.append("<div class=\"card\" data-anim style=\"text-align:center\">")
                        .append(photo.isBlank()
                            ? "<div style=\"width:72px;height:72px;border-radius:50%;background:" + s.primary() + "22;display:flex;align-items:center;justify-content:center;margin:0 auto 16px;font-size:1.8rem\">👤</div>"
                            : "<img src=\"" + escapeHtml(photo) + "\" alt=\"\" style=\"width:72px;height:72px;border-radius:50%;object-fit:cover;margin:0 auto 16px\">")
                        .append("<h3 style=\"font-weight:700;margin-bottom:4px\">").append(escapeHtml(name)).append("</h3>")
                        .append("<p style=\"color:").append(s.primary()).append(";font-size:.85rem;font-weight:600;margin-bottom:10px\">").append(escapeHtml(role)).append("</p>")
                        .append("<p style=\"opacity:.6;font-size:.85rem;line-height:1.6\">").append(escapeHtml(bio)).append("</p>")
                        .append("</div>");
                }
            }
        }
        html.append("</div></div></section>");
        return html.toString();
    }

    private String renderVideo(Map<String, Object> props, StyleVars s) {
        var title    = str(props, "title", "");
        var videoUrl = str(props, "videoUrl", "");
        var caption  = str(props, "caption", "");
        if (videoUrl.isBlank()) return "";
        var embedUrl = videoUrl.replace("watch?v=", "embed/").replace("youtu.be/", "www.youtube.com/embed/");
        if (embedUrl.contains("&")) embedUrl = embedUrl.substring(0, embedUrl.indexOf("&"));
        var html = new StringBuilder();
        html.append("<section class=\"sec\" style=\"background:").append(s.bg()).append("\">")
            .append("<div class=\"w\" style=\"max-width:900px\">")
            .append(title.isBlank() ? "" : "<h2 class=\"sec-title\" data-anim>" + escapeHtml(title) + "</h2>")
            .append("<div data-anim style=\"border-radius:8px;overflow:hidden;aspect-ratio:16/9\">")
            .append("<iframe src=\"").append(escapeHtml(embedUrl))
            .append("\" style=\"width:100%;height:100%;border:0\" allowfullscreen loading=\"lazy\"></iframe>")
            .append("</div>")
            .append(caption.isBlank() ? "" : "<p style=\"text-align:center;opacity:.6;margin-top:12px;font-size:.9rem\">" + escapeHtml(caption) + "</p>")
            .append("</div></section>");
        return html.toString();
    }

    @SuppressWarnings("unchecked")
    private String renderReviews(Map<String, Object> props, StyleVars s, UUID tenantId) {
        var title     = str(props, "title", "Ressenyes");
        var gmUrl     = str(props, "googleMapsUrl", "");
        var source    = str(props, "source", "manual");
        var minRating = props.getOrDefault("minRating", 4);
        var maxItems  = props.getOrDefault("maxItems", 6);

        record ReviewItem(String name, String text, String date, int rating) {}
        var reviewItems = new java.util.ArrayList<ReviewItem>();

        if ("google_business".equals(source) && tenantId != null) {
            int minR = minRating instanceof Number n ? n.intValue() : 4;
            int maxI = maxItems instanceof Number n ? n.intValue() : 6;
            try {
                var gbpReviews = googleBusinessReviewSyncService.getReviews(tenantId, minR, maxI);
                for (var r : gbpReviews) {
                    var dateStr = r.getReviewTime() != null
                        ? r.getReviewTime().toString().substring(0, 10) : "";
                    reviewItems.add(new ReviewItem(
                        r.getAuthorName() != null ? r.getAuthorName() : "Client",
                        r.getComment()    != null ? r.getComment()    : "",
                        dateStr, r.getRating()
                    ));
                }
            } catch (Exception e) {
                log.warn("Error obtenint ressenyes GBP per tenant {}: {}", tenantId, e.getMessage());
            }
        } else {
            var items = props.getOrDefault("items", List.of());
            if (items instanceof List<?> list) {
                for (var item : list) {
                    if (item instanceof Map m) {
                        var im = (Map<String, Object>) m;
                        var rating = im.getOrDefault("rating", 5);
                        reviewItems.add(new ReviewItem(
                            str(im, "name", ""),
                            str(im, "text", ""),
                            str(im, "date", ""),
                            rating instanceof Number n ? Math.min(n.intValue(), 5) : 5
                        ));
                    }
                }
            }
        }

        var html = new StringBuilder();

        // JSON-LD Schema.org/Review per a rich snippets
        if (!reviewItems.isEmpty()) {
            html.append("<script type=\"application/ld+json\">[");
            for (int i = 0; i < reviewItems.size(); i++) {
                var r = reviewItems.get(i);
                if (i > 0) html.append(",");
                html.append("{\"@context\":\"https://schema.org\",\"@type\":\"Review\",")
                    .append("\"reviewRating\":{\"@type\":\"Rating\",\"ratingValue\":").append(r.rating()).append(",\"bestRating\":5},")
                    .append("\"author\":{\"@type\":\"Person\",\"name\":\"").append(escapeJson(r.name())).append("\"},")
                    .append("\"reviewBody\":\"").append(escapeJson(r.text())).append("\",")
                    .append("\"datePublished\":\"").append(escapeJson(r.date())).append("\"}");
            }
            html.append("]</script>");
        }

        html.append("<section class=\"sec\" style=\"background:").append(s.accent()).append("10\">")
            .append("<div class=\"w\"><h2 class=\"sec-title\" data-anim>").append(escapeHtml(title)).append("</h2>")
            .append("<div class=\"grid-3\">");

        for (var r : reviewItems) {
            var stars = "★".repeat(Math.min(r.rating(), 5));
            html.append("<div class=\"card\" data-anim>")
                .append("<div style=\"color:#f59e0b;margin-bottom:10px\">").append(stars).append("</div>")
                .append("<p style=\"opacity:.75;font-size:.9rem;line-height:1.7;font-style:italic;margin-bottom:16px\">&ldquo;").append(escapeHtml(r.text())).append("&rdquo;</p>")
                .append("<div style=\"display:flex;justify-content:space-between;align-items:center\">")
                .append("<span style=\"font-weight:700;font-size:.85rem;color:").append(s.primary()).append("\">").append(escapeHtml(r.name())).append("</span>")
                .append("<span style=\"opacity:.4;font-size:.75rem\">").append(escapeHtml(r.date())).append("</span>")
                .append("</div></div>");
        }

        html.append("</div>");
        if (!gmUrl.isBlank()) {
            html.append("<div style=\"text-align:center;margin-top:28px\">")
                .append("<a href=\"").append(escapeHtml(gmUrl)).append("\" target=\"_blank\" rel=\"noopener noreferrer\"")
                .append(" style=\"display:inline-flex;align-items:center;gap:8px;background:#fff;border:1px solid ").append(s.primary())
                .append(";color:").append(s.primary()).append(";border-radius:99px;padding:10px 24px;font-weight:600;font-size:.9rem;text-decoration:none\">")
                .append("G Veure totes les ressenyes a Google</a>")
                .append("</div>");
        }
        html.append("</div></section>");
        return html.toString();
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String sanitizeBody(String html) {
        if (html == null || html.isBlank()) return "";
        // Permet tags de format bàsic; elimina scripts
        return html.replaceAll("(?i)<script[^>]*>.*?</script>", "")
                   .replaceAll("(?i)<iframe[^>]*>.*?</iframe>", "")
                   .replaceAll("(?i) on\\w+=\"[^\"]*\"", "");
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @Override
    public Map<String, Long> getGlobalLandingsSummary() {
        long total     = landingRepository.count();
        long published = landingRepository.countByStatus(LandingStatus.PUBLISHED);
        return Map.of("total", total, "published", published);
    }

    private LandingType parseLandingType(String value) {
        if (value == null) return LandingType.MICRO;
        try { return LandingType.valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException e) { return LandingType.MICRO; }
    }

    private LeadSource inferSource(String utmSource) {
        if (utmSource == null) return LeadSource.LANDING_FORM;
        return switch (utmSource.toLowerCase()) {
            case "facebook", "fb"  -> LeadSource.FACEBOOK;
            case "instagram", "ig" -> LeadSource.INSTAGRAM;
            case "meta"            -> LeadSource.META_ADS;
            default                -> LeadSource.LANDING_FORM;
        };
    }
}
