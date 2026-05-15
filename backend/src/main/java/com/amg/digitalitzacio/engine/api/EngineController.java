package com.amg.digitalitzacio.engine.api;

import com.amg.digitalitzacio.engine.api.dto.*;
import com.amg.digitalitzacio.engine.application.EngineService;
import com.amg.digitalitzacio.engine.application.TemplateService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/engine")
@RequiredArgsConstructor
public class EngineController {

    private final EngineService engineService;
    private final TemplateService templateService;

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
    public PublishResponse publish(@PathVariable UUID landingId) {
        return engineService.publish(landingId);
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

    @GetMapping(value = "/render/{slug}", produces = MediaType.TEXT_HTML_VALUE)
    public String renderLanding(@PathVariable String slug, @RequestHeader(value = "Host", required = false) String host) {
        return engineService.renderLanding(slug, host);
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
