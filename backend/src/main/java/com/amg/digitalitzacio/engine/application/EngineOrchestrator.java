package com.amg.digitalitzacio.engine.application;

import com.amg.digitalitzacio.engine.api.dto.*;
import com.amg.digitalitzacio.engine.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.vault.domain.CatalogServiceRepository;
import com.amg.digitalitzacio.vault.domain.ServiceType;
import com.amg.digitalitzacio.vault.domain.TenantServiceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EngineOrchestrator implements EngineService {

    private final LandingRepository landingRepository;
    private final LandingVersionRepository landingVersionRepository;
    private final ContactLeadRepository contactLeadRepository;
    private final CatalogServiceRepository catalogServiceRepository;
    private final TenantServiceRepository tenantServiceRepository;
    private final LandingTemplateRepository landingTemplateRepository;
    private final TemplateSectionRepository templateSectionRepository;
    private final ObjectMapper objectMapper;

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
                .metaDescription(request.metaDescription())
                .templateId(request.templateId())
                .status(LandingStatus.DRAFT)
                .build();
        landing = landingRepository.save(landing);

        // Create initial DRAFT version — populate from template defaults if templateId provided
        var initialContent = buildTemplateDefaultContent(request.templateId());
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

        var versionCount = landingVersionRepository.countByLandingId(landingId);
        var version = LandingVersion.builder()
                .landingId(landingId)
                .versionNumber((int) versionCount + 1)
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
    public PublishResponse publish(UUID landingId) {
        var landing = landingRepository.findById(landingId)
                .orElseThrow(() -> new ResourceNotFoundException("Landing not found: " + landingId));

        var draft = landingVersionRepository
                .findTopByLandingIdAndStatusOrderByVersionNumberDesc(landingId, VersionStatus.DRAFT)
                .orElseThrow(() -> new IllegalArgumentException("No DRAFT version to publish"));

        draft.setStatus(VersionStatus.PUBLISHED);
        draft.setPublishedAt(Instant.now());
        landingVersionRepository.save(draft);

        landing.setStatus(LandingStatus.PUBLISHED);
        landing.setPublishedVersionId(draft.getId());
        landingRepository.save(landing);

        var publicUrl = buildPublicUrl(landing);
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
    public String renderLanding(String slug, String host) {
        var landing = landingRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Landing not found: " + slug));

        if (landing.getStatus() != LandingStatus.PUBLISHED || landing.getPublishedVersionId() == null) {
            throw new ResourceNotFoundException("Landing not published: " + slug);
        }

        var version = landingVersionRepository.findById(landing.getPublishedVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Published version not found"));

        // Simple HTML rendering — will be replaced by proper SSR in future
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
               "  <url><loc>https://" + (landing.getCustomDomain() != null ? landing.getCustomDomain() : slug + ".amg.cat") +
               "</loc></url>\n" +
               "</urlset>";
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

        var lead = ContactLead.builder()
                .landingId(landing.getId())
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .message(request.message())
                .metadata(toJson(metadata))
                .build();
        contactLeadRepository.save(lead);

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

        // Build content from template sections + filled content
        var content = buildFilledContent(request.templateId(), request.filledSections());
        var version = LandingVersion.builder()
                .landingId(landing.getId())
                .versionNumber(1)
                .status(VersionStatus.DRAFT)
                .content(content)
                .build();
        landingVersionRepository.save(version);

        return toLandingResponse(landing);
    }

    /**
     * Build initial version content from template defaults.
     * Returns {"blocks":[]} if no template or no sections.
     */
    private String buildTemplateDefaultContent(UUID templateId) {
        if (templateId == null) return "{\"blocks\":[]}";
        var sections = templateSectionRepository.findByTemplateIdOrderBySortOrder(templateId);
        if (sections.isEmpty()) return "{\"blocks\":[]}";

        var blocks = sections.stream()
                .map(section -> {
                    Map<String, Object> block = new LinkedHashMap<>();
                    block.put("id", "blk_" + UUID.randomUUID().toString().substring(0, 8));
                    block.put("type", section.getBlockType().name().toLowerCase());
                    Map<String, Object> props = new LinkedHashMap<>();
                    if (section.getDefaultProps() != null && !section.getDefaultProps().isBlank()) {
                        var parsed = fromJson(section.getDefaultProps());
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
     * filledSections is a Map<sectionId, props>.
     */
    private String buildFilledContent(UUID templateId, Map<String, Map<String, Object>> filledSections) {
        if (templateId == null) return "{\"blocks\":[]}";
        var sections = templateSectionRepository.findByTemplateIdOrderBySortOrder(templateId);
        if (sections.isEmpty()) return "{\"blocks\":[]}";

        var blocks = sections.stream()
                .map(section -> {
                    Map<String, Object> block = new LinkedHashMap<>();
                    block.put("id", "blk_" + UUID.randomUUID().toString().substring(0, 8));
                    block.put("type", section.getBlockType().name().toLowerCase());
                    // Merge: start with defaultProps, overlay with filled content
                    Map<String, Object> props = new LinkedHashMap<>();
                    if (section.getDefaultProps() != null && !section.getDefaultProps().isBlank()) {
                        var parsed = fromJson(section.getDefaultProps());
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
        return "https://" + landing.getSlug() + ".amg.cat";
    }

    private DomainConfigResponse toDomainConfigResponse(Landing landing) {
        return new DomainConfigResponse(
                landing.getCustomDomain(),
                landing.getManagedDomain(),
                landing.getDomainStatus().name(),
                "Afegeix un registre CNAME de " + landing.getCustomDomain() + " a landings.amg.cat",
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

        var fontFamily = styles != null ? styles.getOrDefault("fontFamily", "'Inter', sans-serif") : "'Inter', sans-serif";
        var primaryColor = styles != null ? styles.getOrDefault("primaryColor", "#1a365d") : "#1a365d";
        var bgColor = styles != null ? styles.getOrDefault("bgColor", "#ffffff") : "#ffffff";
        var textColor = styles != null ? styles.getOrDefault("textColor", "#1a202c") : "#1a202c";

        var blocksHtml = new StringBuilder();
        var blocks = content != null ? content.get("blocks") : null;
        if (blocks instanceof List<?> blockList) {
            for (var block : blockList) {
                if (block instanceof Map) {
                    @SuppressWarnings("unchecked")
                    var blockMap = (Map<String, Object>) block;
                    blocksHtml.append(renderBlock(blockMap));
                }
            }
        }

        var publicUrl = buildPublicUrl(landing);

        return "<!DOCTYPE html><html lang=\"ca\"><head>" +
               "<meta charset=\"UTF-8\">" +
               "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
               (landing.getMetaDescription() != null ? "<meta name=\"description\" content=\"" + escapeHtml(landing.getMetaDescription()) + "\">" : "") +
               // OG tags per SEO i xarxes socials
               "<meta property=\"og:title\" content=\"" + escapeHtml(landing.getTitle()) + "\">" +
               (landing.getMetaDescription() != null ? "<meta property=\"og:description\" content=\"" + escapeHtml(landing.getMetaDescription()) + "\">" : "") +
               (landing.getOgImageUrl() != null ? "<meta property=\"og:image\" content=\"" + escapeHtml(landing.getOgImageUrl()) + "\">" : "") +
               "<meta property=\"og:url\" content=\"" + escapeHtml(publicUrl) + "\">" +
               "<meta property=\"og:type\" content=\"website\">" +
               "<link rel=\"canonical\" href=\"" + escapeHtml(publicUrl) + "\">" +
               "<title>" + escapeHtml(landing.getTitle()) + "</title>" +
               "<style>" +
               "* { margin: 0; padding: 0; box-sizing: border-box; }" +
               "body { font-family: " + fontFamily + "; background: " + bgColor + "; color: " + textColor + "; }" +
               ".container { max-width: 1200px; margin: 0 auto; padding: 0 20px; }" +
               ".btn { display: inline-block; padding: 12px 24px; background: " + primaryColor + "; color: #fff; " +
               "text-decoration: none; border-radius: 8px; font-weight: 600; }" +
               // Footer legal automàtic
               ".legal-footer { background: #f7fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 14px; color: #718096; }" +
               ".legal-footer a { color: " + primaryColor + "; text-decoration: underline; }" +
               "</style>" +
               "</head><body>" +
               blocksHtml.toString() +
               // RGPD/LSSI: footer automàtic amb enllaços legals
               "<footer class=\"legal-footer\">" +
               "<div class=\"container\">" +
               "<p>&copy; " + java.time.Year.now() + " " + escapeHtml(landing.getTitle()) + ". Tots els drets reservats.</p>" +
               "<p><a href=\"/legal/avis-legal\">Avís legal</a> &middot; " +
               "<a href=\"/legal/politica-de-privacitat\">Política de privacitat</a> &middot; " +
               "<a href=\"/legal/politica-de-cookies\">Política de cookies</a></p>" +
               "</div></footer>" +
               "</body></html>";
    }

    private String renderBlock(Map<String, Object> block) {
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
            case "hero" -> renderHero(props);
            case "text" -> renderText(props);
            case "services" -> renderServices(props);
            case "contact-form" -> renderContactForm(props);
            case "faq" -> renderFaq(props);
            case "cta" -> renderCta(props);
            default -> "<div class=\"container\"><p>Unknown block: " + type + "</p></div>";
        };
    }

    private String renderHero(Map<String, Object> props) {
        var title = props.getOrDefault("title", "");
        var subtitle = props.getOrDefault("subtitle", "");
        var ctaText = props.getOrDefault("ctaText", "");
        var ctaUrl = props.getOrDefault("ctaUrl", "#");
        var bgImage = props.getOrDefault("bgImageUrl", "");

        var bgStyle = !bgImage.toString().isBlank()
                ? "style=\"background: url('" + escapeHtml(bgImage.toString()) + "') center/cover;\""
                : "style=\"background: #1a365d; color: #fff;\"";

        return "<section " + bgStyle + ">" +
               "<div class=\"container\" style=\"padding: 100px 20px; text-align: center;\">" +
               "<h1 style=\"font-size: 48px; margin-bottom: 16px;\">" + escapeHtml(title.toString()) + "</h1>" +
               "<p style=\"font-size: 20px; margin-bottom: 32px;\">" + escapeHtml(subtitle.toString()) + "</p>" +
               (!ctaText.toString().isBlank() ? "<a href=\"" + escapeHtml(ctaUrl.toString()) + "\" class=\"btn\">" + escapeHtml(ctaText.toString()) + "</a>" : "") +
               "</div></section>";
    }

    private String renderText(Map<String, Object> props) {
        var title = props.getOrDefault("title", "");
        var body = props.getOrDefault("body", "");
        return "<section class=\"container\" style=\"padding: 60px 20px;\">" +
               "<h2>" + escapeHtml(title.toString()) + "</h2>" +
               "<p>" + escapeHtml(body.toString()) + "</p>" +
               "</section>";
    }

    @SuppressWarnings("unchecked")
    private String renderServices(Map<String, Object> props) {
        var title = String.valueOf(props.getOrDefault("title", ""));
        var items = props.getOrDefault("items", List.of());
        var html = new StringBuilder();
        html.append("<section class=\"container\" style=\"padding: 60px 20px;\">");
        html.append("<h2 style=\"text-align: center; margin-bottom: 40px;\">").append(escapeHtml(title)).append("</h2>");
        html.append("<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 24px;\">");
        if (items instanceof List<?> itemList) {
            for (var item : itemList) {
                if (item instanceof Map) {
                    var itemMap = (Map<String, Object>) item;
                    html.append("<div style=\"padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px;\">");
                    html.append("<h3>").append(escapeHtml(String.valueOf(itemMap.getOrDefault("title", "")))).append("</h3>");
                    html.append("<p>").append(escapeHtml(String.valueOf(itemMap.getOrDefault("description", "")))).append("</p>");
                    html.append("</div>");
                }
            }
        }
        html.append("</div></section>");
        return html.toString();
    }

    private String renderContactForm(Map<String, Object> props) {
        var title = props.getOrDefault("title", "");
        return "<section class=\"container\" style=\"padding: 60px 20px;\">" +
               "<h2 style=\"text-align: center; margin-bottom: 24px;\">" + escapeHtml(title.toString()) + "</h2>" +
               "<div style=\"max-width: 500px; margin: 0 auto;\">" +
               "<p style=\"text-align: center; color: #718096;\">Formulari de contacte disponible al frontend</p>" +
               "</div></section>";
    }

    @SuppressWarnings("unchecked")
    private String renderFaq(Map<String, Object> props) {
        var title = String.valueOf(props.getOrDefault("title", ""));
        var items = props.getOrDefault("items", List.of());
        var html = new StringBuilder();
        html.append("<section class=\"container\" style=\"padding: 60px 20px;\">");
        html.append("<h2 style=\"text-align: center; margin-bottom: 40px;\">").append(escapeHtml(title)).append("</h2>");
        if (items instanceof List<?> itemList) {
            for (var item : itemList) {
                if (item instanceof Map) {
                    var itemMap = (Map<String, Object>) item;
                    html.append("<div style=\"margin-bottom: 16px; padding: 16px; border: 1px solid #e2e8f0; border-radius: 8px;\">");
                    html.append("<h3>").append(escapeHtml(String.valueOf(itemMap.getOrDefault("question", "")))).append("</h3>");
                    html.append("<p>").append(escapeHtml(String.valueOf(itemMap.getOrDefault("answer", "")))).append("</p>");
                    html.append("</div>");
                }
            }
        }
        html.append("</div></section>");
        return html.toString();
    }

    private String renderCta(Map<String, Object> props) {
        var text = props.getOrDefault("text", "");
        var buttonText = props.getOrDefault("buttonText", "");
        var buttonUrl = props.getOrDefault("buttonUrl", "#");
        return "<section style=\"background: #1a365d; color: #fff; text-align: center; padding: 60px 20px;\">" +
               "<div class=\"container\">" +
               "<h2>" + escapeHtml(text.toString()) + "</h2>" +
               (!buttonText.toString().isBlank() ? "<a href=\"" + escapeHtml(buttonUrl.toString()) + "\" class=\"btn\" style=\"margin-top: 24px;\">" + escapeHtml(buttonText.toString()) + "</a>" : "") +
               "</div></section>";
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
}
