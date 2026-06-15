package com.amg.digitalitzacio.documents.delivery.api;

import com.amg.digitalitzacio.documents.delivery.api.dto.AuditEventResponse;
import com.amg.digitalitzacio.documents.delivery.api.dto.IssueTokenRequest;
import com.amg.digitalitzacio.documents.delivery.api.dto.TokenResponse;
import com.amg.digitalitzacio.documents.delivery.application.SecureDocumentService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents/tokens")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class SecureDocumentTokenController {

    private final SecureDocumentService service;
    private final SystemConfigService sysConfig;

    @PostMapping
    public ResponseEntity<TokenResponse> issue(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID tenantId,
            @Valid @RequestBody IssueTokenRequest req) {
        UUID tid = resolveTenantId(user, tenantId);
        var token = service.issue(tid, user.id(),
            req.storageFileId(), req.fileName(), req.mimeType(),
            req.recipientEmail(), req.recipientPhone(), req.recipientName(),
            req.description(), req.sensitivity(),
            req.documentType(), req.sourceEntityId(), req.sourceEntityType());
        return ResponseEntity.status(HttpStatus.CREATED).body(TokenResponse.from(token, baseUrl()));
    }

    @GetMapping
    public ResponseEntity<Page<TokenResponse>> list(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tid = resolveTenantId(user, tenantId);
        String base = baseUrl();
        return ResponseEntity.ok(
            service.list(tid, PageRequest.of(page, size))
                   .map(t -> TokenResponse.from(t, base)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TokenResponse> get(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID tenantId,
            @PathVariable UUID id) {
        UUID tid = resolveTenantId(user, tenantId);
        return ResponseEntity.ok(TokenResponse.from(service.getToken(tid, id), baseUrl()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID tenantId,
            @PathVariable UUID id) {
        UUID tid = resolveTenantId(user, tenantId);
        service.revoke(tid, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<List<AuditEventResponse>> audit(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID tenantId,
            @PathVariable UUID id) {
        UUID tid = resolveTenantId(user, tenantId);
        return ResponseEntity.ok(service.getAudit(tid, id).stream()
            .map(AuditEventResponse::from).toList());
    }

    private UUID resolveTenantId(UserPrincipal user, UUID tenantId) {
        if ("SUPER_ADMIN".equals(user.role()) && tenantId != null) return tenantId;
        return user.tenantId();
    }

    private String baseUrl() {
        String base = sysConfig.get("APP_BASE_URL");
        return (base != null && !base.isBlank()) ? base : "https://app.amgdl.com";
    }
}
