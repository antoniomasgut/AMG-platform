package com.amg.digitalitzacio.hosting.api;

import com.amg.digitalitzacio.hosting.api.dto.ReviewRequest;
import com.amg.digitalitzacio.hosting.api.dto.WebSiteResponse;
import com.amg.digitalitzacio.hosting.application.WebHostingService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hosting")
@RequiredArgsConstructor
public class WebHostingController {

    private final WebHostingService webHostingService;

    @GetMapping("/tenants/{tenantId}/sites")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WebSiteResponse>> listSites(@PathVariable UUID tenantId,
                                                            @AuthenticationPrincipal UserPrincipal principal) {
        if (!"SUPER_ADMIN".equals(principal.role()) && !tenantId.equals(principal.tenantId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(webHostingService.listSitesByTenant(tenantId));
    }

    @GetMapping("/tenants/{tenantId}/sites/{siteId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WebSiteResponse> getSite(@PathVariable UUID tenantId,
                                                    @PathVariable UUID siteId,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(webHostingService.getSite(siteId, principal));
    }

    @PostMapping("/tenants/{tenantId}/sites/external")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WebSiteResponse> requestExternalSite(
            @PathVariable UUID tenantId,
            @RequestParam("domain") String domain,
            @RequestParam(value = "contactEmail", required = false) String contactEmail,
            @RequestParam(value = "contactRedirectUrl", required = false) String contactRedirectUrl,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(webHostingService.requestExternalSite(tenantId, domain, contactEmail, contactRedirectUrl, principal));
    }

    @PostMapping(value = "/tenants/{tenantId}/sites/static", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WebSiteResponse> requestStaticSite(
            @PathVariable UUID tenantId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("domain") String domain,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(webHostingService.requestStaticSite(tenantId, file, domain, principal));
    }

    @PostMapping(value = "/tenants/{tenantId}/sites/container", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WebSiteResponse> requestContainerSite(
            @PathVariable UUID tenantId,
            @RequestParam("compose") MultipartFile compose,
            @RequestParam(value = "envExample", required = false) MultipartFile envExample,
            @RequestParam("domain") String domain,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "upstreamContainer", required = false) String upstreamContainer,
            @RequestParam(value = "upstreamPort", required = false) Integer upstreamPort,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(webHostingService.requestContainerSite(
                        tenantId, compose, envExample, domain, description,
                        upstreamContainer, upstreamPort, principal));
    }

    @GetMapping("/admin/sites/{siteId}/compose")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Resource> downloadCompose(@PathVariable UUID siteId,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        Resource resource = webHostingService.exportCompose(siteId, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"docker-compose.yml\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
    }

    @PutMapping(value = "/tenants/{tenantId}/sites/{siteId}/static", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WebSiteResponse> updateStaticSite(
            @PathVariable UUID tenantId,
            @PathVariable UUID siteId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(webHostingService.updateStaticSite(siteId, file, principal));
    }

    @GetMapping("/tenants/{tenantId}/sites/{siteId}/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> exportSite(@PathVariable UUID tenantId,
                                                @PathVariable UUID siteId,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        Resource resource = webHostingService.exportZip(siteId, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"site-" + siteId + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/tenants/{tenantId}/sites/{siteId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteSite(@PathVariable UUID tenantId,
                                            @PathVariable UUID siteId,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        webHostingService.deleteSite(siteId, principal);
        return ResponseEntity.noContent().build();
    }

    // --- Admin endpoints ---

    @GetMapping("/admin/sites")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<WebSiteResponse>> listAllSites() {
        return ResponseEntity.ok(webHostingService.listAllSites());
    }

    @GetMapping("/admin/sites/pending")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<WebSiteResponse>> listPendingSites() {
        return ResponseEntity.ok(webHostingService.listPendingSites());
    }

    @PostMapping("/admin/sites/{siteId}/send-snippets")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> sendSnippetsEmail(@PathVariable UUID siteId) {
        webHostingService.sendSnippetsEmail(siteId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/sites/{siteId}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<WebSiteResponse> approveSite(@PathVariable UUID siteId,
                                                        @RequestBody(required = false) ReviewRequest request,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        String notes = request != null ? request.notes() : null;
        String upstreamContainer = request != null ? request.upstreamContainer() : null;
        Integer upstreamPort = request != null ? request.upstreamPort() : null;
        return ResponseEntity.ok(webHostingService.approveSite(siteId, notes, upstreamContainer, upstreamPort, principal));
    }

    @PostMapping("/admin/sites/{siteId}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<WebSiteResponse> rejectSite(@PathVariable UUID siteId,
                                                       @RequestBody ReviewRequest request,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(webHostingService.rejectSite(siteId, request.notes(), principal));
    }
}
