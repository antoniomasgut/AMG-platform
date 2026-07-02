package com.amg.digitalitzacio.engine.api;

import com.amg.digitalitzacio.engine.api.dto.*;
import com.amg.digitalitzacio.engine.application.EngineService;
import com.amg.digitalitzacio.engine.application.LandingGeneratorService;
import com.amg.digitalitzacio.engine.application.TemplateService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/engine")
@RequiredArgsConstructor
public class EngineController {

    private final EngineService engineService;
    private final TemplateService templateService;
    private final LandingGeneratorService landingGeneratorService;

    // --- Admin global summary ---

    @GetMapping("/admin/landings/summary")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public java.util.Map<String, Long> getLandingsSummary() {
        return engineService.getGlobalLandingsSummary();
    }

    // --- Landings ---

    @PostMapping("/tenants/{tenantId}/landings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public LandingResponse createLanding(@PathVariable UUID tenantId, @RequestBody CreateLandingRequest request) {
        return engineService.createLanding(tenantId, request);
    }

    @PostMapping("/tenants/{tenantId}/landings/from-template")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public LandingResponse createLandingFromTemplate(@PathVariable UUID tenantId,
                                                      @RequestBody CreateLandingFromTemplateRequest request) {
        return engineService.createLandingFromTemplate(tenantId, request);
    }

    @GetMapping("/tenants/{tenantId}/landings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public List<LandingSummary> listLandings(@PathVariable UUID tenantId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(required = false) String search,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return engineService.listLandings(tenantId, page, size, search);
    }

    @GetMapping("/tenants/{tenantId}/landings/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public LandingResponse getLanding(@PathVariable UUID tenantId, @PathVariable UUID id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return engineService.getLanding(tenantId, id);
    }

    @PutMapping("/tenants/{tenantId}/landings/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public LandingResponse updateLanding(@PathVariable UUID tenantId, @PathVariable UUID id,
                                          @RequestBody CreateLandingRequest request) {
        return engineService.updateLanding(tenantId, id, request);
    }

    @DeleteMapping("/tenants/{tenantId}/landings/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLanding(@PathVariable UUID tenantId, @PathVariable UUID id) {
        engineService.deleteLanding(tenantId, id);
    }

    // --- Versions ---

    @PostMapping("/landings/{landingId}/versions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasRole('CLIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public VersionResponse createVersion(@PathVariable UUID landingId, @RequestBody CreateVersionRequest request,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return engineService.createVersion(landingId, request, principal.tenantId());
    }

    @PutMapping("/landings/{landingId}/versions/{versionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or hasRole('CLIENT')")
    public VersionResponse updateVersion(@PathVariable UUID landingId, @PathVariable UUID versionId,
                                          @RequestBody CreateVersionRequest request,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return engineService.updateVersion(landingId, versionId, request, principal.tenantId());
    }

    @PostMapping("/landings/{landingId}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public PublishResponse publish(@PathVariable UUID landingId,
                                   @RequestParam(defaultValue = "ca") String locale) {
        return engineService.publish(landingId, locale);
    }

    @PostMapping("/landings/{landingId}/unpublish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unpublish(@PathVariable UUID landingId) {
        engineService.unpublish(landingId);
    }

    // --- Domain ---

    @PutMapping("/landings/{landingId}/domain")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public DomainConfigResponse configureDomain(@PathVariable UUID landingId,
                                                 @RequestBody DomainConfigRequest request,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return engineService.configureDomain(landingId, request, principal.id(), principal.role());
    }

    @GetMapping("/landings/{landingId}/domain/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public DomainConfigResponse verifyDomain(@PathVariable UUID landingId) {
        return engineService.verifyDomain(landingId);
    }

    @PostMapping("/landings/{landingId}/domain/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public DomainConfigResponse updateDomainStatus(@PathVariable UUID landingId,
                                                    @RequestBody UpdateDomainStatusRequest request) {
        return engineService.updateDomainStatus(landingId, request);
    }

    @DeleteMapping("/landings/{landingId}/domain")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeDomain(@PathVariable UUID landingId) {
        engineService.removeDomain(landingId);
    }

    // --- Render (public) ---

    private static final List<String> SUPPORTED_LOCALES = List.of("ca", "es", "en", "de");

    private String resolveLocale(String lang, String acceptLanguage) {
        if (lang != null && SUPPORTED_LOCALES.contains(lang)) return lang;
        if (acceptLanguage != null && !acceptLanguage.isBlank()) {
            return Arrays.stream(acceptLanguage.split(","))
                .map(part -> part.split(";")[0].trim().toLowerCase())
                .map(tag -> tag.length() >= 2 ? tag.substring(0, 2) : tag)
                .filter(SUPPORTED_LOCALES::contains)
                .findFirst()
                .orElse("ca");
        }
        return "ca";
    }

    @GetMapping(value = "/render/{slug}", produces = MediaType.TEXT_HTML_VALUE)
    public String renderLanding(@PathVariable String slug,
                                @RequestHeader(value = "Host", required = false) String host,
                                @RequestParam(required = false) String lang,
                                @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        return engineService.renderLanding(slug, host, resolveLocale(lang, acceptLanguage));
    }

    // --- Auto-translate ---

    @PostMapping("/tenants/{tenantId}/landings/{landingId}/auto-translate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLIENT')")
    public List<VersionResponse> autoTranslate(@PathVariable UUID tenantId,
                                               @PathVariable UUID landingId,
                                               @RequestBody AutoTranslateRequest request) {
        return engineService.autoTranslate(tenantId, landingId, request.sourceLocale(), request.targetLocales());
    }

    @GetMapping("/tenants/{tenantId}/landing-defaults")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLIENT')")
    public LandingDefaultsResponse getLandingDefaults(@PathVariable UUID tenantId) {
        return engineService.getLandingDefaults(tenantId);
    }

    @GetMapping(value = "/render/{slug}/{locale}", produces = MediaType.TEXT_HTML_VALUE)
    public String renderLandingLocale(@PathVariable String slug, @PathVariable String locale,
                                      @RequestHeader(value = "Host", required = false) String host) {
        return engineService.renderLanding(slug, host, locale);
    }

    @GetMapping(value = "/render/{slug}/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String renderSitemap(@PathVariable String slug) {
        return engineService.renderSitemap(slug);
    }

    @PostMapping("/render/{slug}/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactResponse submitContact(@PathVariable String slug, @RequestBody ContactRequest request) {
        return engineService.submitContact(slug, request);
    }

    @PostMapping("/generate-block")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CLIENT')")
    public GenerateBlockResponse generateBlock(@RequestBody GenerateBlockRequest request) {
        return landingGeneratorService.generate(request);
    }

    @GetMapping("/tenants/{tenantId}/landings/{landingId}/stats")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CLIENT')")
    public LandingStatsResponse getLandingStats(
            @PathVariable UUID tenantId,
            @PathVariable UUID landingId) {
        return engineService.getLandingStats(tenantId, landingId);
    }

    // --- Templates ---

    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public List<LandingTemplateSummary> listTemplates() {
        return templateService.listTemplates();
    }

    @GetMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public LandingTemplateResponse getTemplate(@PathVariable UUID id) {
        return templateService.getTemplate(id);
    }

    @PostMapping("/templates")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public LandingTemplateResponse createTemplate(@RequestBody CreateTemplateRequest request) {
        return templateService.createTemplate(request);
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public LandingTemplateResponse updateTemplate(@PathVariable UUID id, @RequestBody CreateTemplateRequest request) {
        return templateService.updateTemplate(id, request);
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@PathVariable UUID id) {
        templateService.deleteTemplate(id);
    }

    @PostMapping("/templates/{templateId}/sections")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public LandingTemplateResponse addSection(@PathVariable UUID templateId, @RequestBody TemplateSectionRequest request) {
        return templateService.addSection(templateId, request);
    }

    @PutMapping("/templates/{templateId}/sections/{sectionId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public LandingTemplateResponse updateSection(@PathVariable UUID templateId, @PathVariable UUID sectionId,
                                                  @RequestBody TemplateSectionRequest request) {
        return templateService.updateSection(templateId, sectionId, request);
    }

    @DeleteMapping("/templates/{templateId}/sections/{sectionId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSection(@PathVariable UUID templateId, @PathVariable UUID sectionId) {
        templateService.removeSection(templateId, sectionId);
    }
}
