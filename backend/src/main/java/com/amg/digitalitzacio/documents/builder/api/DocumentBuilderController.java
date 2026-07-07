package com.amg.digitalitzacio.documents.builder.api;

import com.amg.digitalitzacio.documents.builder.api.dto.*;
import com.amg.digitalitzacio.documents.builder.application.DocumentBuilderService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentBuilderController {

    private final DocumentBuilderService service;

    @GetMapping("/templates")
    public ResponseEntity<List<TemplateResponse>> listTemplates(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID tenantId) {
        return ResponseEntity.ok(service.listTemplates(resolveTenantId(user, tenantId)));
    }

    @PostMapping("/templates")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<TemplateResponse> createTemplate(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID tenantId,
            @Valid @RequestBody TemplateRequest req) {
        if (req.name() == null || req.name().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createTemplate(resolveTenantId(user, tenantId), req));
    }

    @GetMapping("/templates/{id}")
    public ResponseEntity<TemplateResponse> getTemplate(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.getTemplate(id, user));
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<TemplateResponse> updateTemplate(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id, @Valid @RequestBody TemplateRequest req) {
        return ResponseEntity.ok(service.updateTemplate(id, req, user));
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> deleteTemplate(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id) {
        service.deleteTemplate(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/templates/{id}/duplicate")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<TemplateResponse> duplicateTemplate(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.duplicateTemplate(id, user));
    }

    @GetMapping("/templates/{id}/versions")
    public ResponseEntity<List<VersionResponse>> listVersions(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.listVersions(id, user));
    }

    @GetMapping("/templates/{id}/versions/{version}")
    public ResponseEntity<VersionResponse> getVersion(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id, @PathVariable Integer version) {
        return ResponseEntity.ok(service.getVersion(id, version, user));
    }

    @PostMapping("/templates/{id}/restore/{version}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<TemplateResponse> restoreVersion(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id, @PathVariable Integer version) {
        return ResponseEntity.ok(service.restoreVersion(id, version, user));
    }

    @GetMapping("/templates/{id}/preview")
    public ResponseEntity<String> previewTemplate(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.previewTemplate(id, user));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<DocumentResponse> generateDocument(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID tenantId,
            @Valid @RequestBody GenerateRequest req) {
        if (req.templateId() == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(service.generateDocument(resolveTenantId(user, tenantId), req));
    }

    @PostMapping("/generate/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<DocumentResponse> generatePdf(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID tenantId,
            @Valid @RequestBody GenerateRequest req) {
        if (req.templateId() == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(service.generatePdf(resolveTenantId(user, tenantId), req));
    }

    @GetMapping("/list")
    public ResponseEntity<List<DocumentResponse>> listDocuments(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID tenantId) {
        return ResponseEntity.ok(service.listDocuments(resolveTenantId(user, tenantId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.getDocument(id, user));
    }

    @PostMapping("/ai/apply")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<AIApplyRequest.AIApplyResponse> applyAiOperations(
            @Valid @RequestBody AIApplyRequest req) {
        if (req.prompt() == null || req.prompt().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(service.applyAiOperations(req.templateId(), req.prompt()));
    }

    // ── Exporta plantilla a Google Docs ──────────────────────────

    @PostMapping("/templates/{id}/export-drive")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> exportTemplateToDrive(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id,
            @RequestParam(required = false) UUID tenantId) {
        try {
            var result = service.exportTemplateToDrive(id, resolveTenantId(user, tenantId));
            return ResponseEntity.ok(Map.of(
                    "fileId", result.fileId(),
                    "webViewLink", result.webViewLink() != null ? result.webViewLink() : "",
                    "title", result.title()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Exporta plantilla al Drive d'AMG (SA) ────────────────────

    @PostMapping("/templates/{id}/export-drive-amg")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> exportTemplateToDriveAMG(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id,
            @RequestParam(required = false) UUID tenantId) {
        try {
            var result = service.exportTemplateToDriveAMG(id, resolveTenantId(user, tenantId));
            return ResponseEntity.ok(Map.of(
                    "fileId", result.fileId(),
                    "webViewLink", result.webViewLink() != null ? result.webViewLink() : "",
                    "title", result.title()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ── Importa plantilla des d'un PDF extern ─────────────────────

    @PostMapping(value = "/templates/import-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> importFromPdf(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "quote") String documentType,
            @RequestParam(required = false) String templateName) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fitxer PDF requerit"));
        }
        if (!MediaType.APPLICATION_PDF_VALUE.equals(file.getContentType())
                && (file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".pdf"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Només s'accepten fitxers PDF"));
        }
        try {
            var result = service.importFromPdf(
                    resolveTenantId(user, tenantId),
                    file.getBytes(),
                    documentType,
                    templateName != null ? templateName : file.getOriginalFilename()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private UUID resolveTenantId(UserPrincipal user, UUID requestedTenantId) {
        if ("SUPER_ADMIN".equals(user.role()) && requestedTenantId != null) {
            return requestedTenantId;
        }
        return user.tenantId();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Void> handleForbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}
