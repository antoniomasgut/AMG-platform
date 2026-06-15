package com.amg.digitalitzacio.documents.delivery.api;

import com.amg.digitalitzacio.documents.delivery.application.TenantDocumentPreferencesService;
import com.amg.digitalitzacio.documents.delivery.domain.TenantDocumentPreferences;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents/preferences")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class TenantDocumentPreferencesController {

    private final TenantDocumentPreferencesService service;

    public record PreferencesRequest(
        String language,
        String standardDocChannel,
        String sensitiveDocChannel
    ) {}

    @GetMapping
    public ResponseEntity<TenantDocumentPreferences> get(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID tenantId) {
        return ResponseEntity.ok(service.getPreferences(resolveTenantId(user, tenantId)));
    }

    @PutMapping
    public ResponseEntity<TenantDocumentPreferences> update(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(required = false) UUID tenantId,
            @RequestBody PreferencesRequest req) {
        return ResponseEntity.ok(service.save(
            resolveTenantId(user, tenantId),
            req.language(), req.standardDocChannel(), req.sensitiveDocChannel()
        ));
    }

    private UUID resolveTenantId(UserPrincipal user, UUID tenantId) {
        if ("SUPER_ADMIN".equals(user.role()) && tenantId != null) return tenantId;
        return user.tenantId();
    }
}
