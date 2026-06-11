package com.amg.digitalitzacio.documents.builder.api;

import com.amg.digitalitzacio.documents.builder.api.dto.*;
import com.amg.digitalitzacio.documents.builder.application.DocumentBuilderService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
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
    public ResponseEntity<TemplateResponse> getTemplate(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getTemplate(id));
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
    public ResponseEntity<List<VersionResponse>> listVersions(@PathVariable UUID id) {
        return ResponseEntity.ok(service.listVersions(id));
    }

    @GetMapping("/templates/{id}/versions/{version}")
    public ResponseEntity<VersionResponse> getVersion(@PathVariable UUID id, @PathVariable Integer version) {
        return ResponseEntity.ok(service.getVersion(id, version));
    }

    @PostMapping("/templates/{id}/restore/{version}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<TemplateResponse> restoreVersion(
            @AuthenticationPrincipal UserPrincipal user,
            @PathVariable UUID id, @PathVariable Integer version) {
        return ResponseEntity.ok(service.restoreVersion(id, version, user));
    }

    @GetMapping("/templates/{id}/preview")
    public ResponseEntity<String> previewTemplate(@PathVariable UUID id) {
        return ResponseEntity.ok(service.previewTemplate(id));
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
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getDocument(id));
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
