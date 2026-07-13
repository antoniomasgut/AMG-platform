package com.amg.digitalitzacio.content.api;

import com.amg.digitalitzacio.content.api.dto.*;
import com.amg.digitalitzacio.content.application.ContentPlanService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API del Content Planner (Spec 58 §6).
 * Endpoints amb {tenantId} a la ruta: aïllament per @PreAuthorize.
 * Endpoints per {planId}/{itemId}: només autenticats; l'ownership es comprova al servei.
 */
@RestController
@RequestMapping("/api/v1/content-plans")
@RequiredArgsConstructor
public class ContentPlanController {

    private final ContentPlanService service;

    // ── Endpoints per tenant (aïllament a @PreAuthorize) ──

    @PostMapping("/tenants/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public ResponseEntity<ContentPlanResponse> create(
            @PathVariable UUID tenantId,
            @RequestBody CreatePlanRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.createPlan(tenantId, req, principal));
    }

    @GetMapping("/tenants/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public ResponseEntity<List<ContentPlanResponse>> list(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.listPlans(tenantId, principal));
    }

    @GetMapping("/tenants/{tenantId}/pending")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public ResponseEntity<List<ContentPlanItemResponse>> pending(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getPending(tenantId, principal));
    }

    @GetMapping("/tenants/{tenantId}/default-language")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public ResponseEntity<Map<String, String>> getDefaultLanguage(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of("language", service.getDefaultLanguage(tenantId, principal)));
    }

    @PutMapping("/tenants/{tenantId}/default-language")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public ResponseEntity<Map<String, String>> setDefaultLanguage(
            @PathVariable UUID tenantId,
            @RequestBody LanguageRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of("language", service.setDefaultLanguage(tenantId, req.language(), principal)));
    }

    // ── Endpoints per pla/item (ownership al servei) ──

    @GetMapping("/{planId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContentPlanResponse> get(
            @PathVariable UUID planId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getPlan(planId, principal));
    }

    @PostMapping("/{planId}/generate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContentPlanResponse> generate(
            @PathVariable UUID planId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.generate(planId, principal));
    }

    @PostMapping("/{planId}/activate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContentPlanResponse> activate(
            @PathVariable UUID planId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.activate(planId, principal));
    }

    @DeleteMapping("/{planId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID planId,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.deletePlan(planId, principal);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContentPlanResponse> updateItem(
            @PathVariable UUID itemId,
            @RequestBody UpdateItemRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.updateItem(itemId, req, principal));
    }

    @PostMapping("/items/{itemId}/photo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContentPlanResponse> uploadPhoto(
            @PathVariable UUID itemId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.uploadPhoto(itemId, file, principal));
    }
}
