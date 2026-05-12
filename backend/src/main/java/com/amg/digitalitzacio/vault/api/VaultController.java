package com.amg.digitalitzacio.vault.api;

import com.amg.digitalitzacio.shared.security.UserPrincipal;
import com.amg.digitalitzacio.vault.api.dto.*;
import com.amg.digitalitzacio.vault.application.ProfileService;
import com.amg.digitalitzacio.vault.application.VaultService;
import com.amg.digitalitzacio.vault.domain.ImplementationStatus;
import com.amg.digitalitzacio.vault.domain.ServiceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vault")
@RequiredArgsConstructor
public class VaultController {

    private final ProfileService profileService;
    private final VaultService vaultService;

    // ── 4.1 Perfils ──

    @PostMapping("/profiles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ProfileResponse> createProfile(@RequestBody CreateProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.createProfile(request));
    }

    @GetMapping("/profiles")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProfileResponse>> listProfiles() {
        return ResponseEntity.ok(profileService.listProfiles());
    }

    @GetMapping("/profiles/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(profileService.getProfile(id));
    }

    @PutMapping("/profiles/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ProfileResponse> updateProfile(@PathVariable UUID id, @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(id, request));
    }

    @DeleteMapping("/profiles/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deactivateProfile(@PathVariable UUID id) {
        profileService.deactivateProfile(id);
        return ResponseEntity.noContent().build();
    }

    // ── 4.2 Fases ──

    @PostMapping("/profiles/{profileId}/phases")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ProfileResponse> addPhase(@PathVariable UUID profileId, @RequestBody CreatePhaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.addPhase(profileId, request));
    }

    @PutMapping("/profiles/{profileId}/phases/{phaseId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ProfileResponse> updatePhase(@PathVariable UUID profileId, @PathVariable UUID phaseId,
                                                       @RequestBody UpdatePhaseRequest request) {
        return ResponseEntity.ok(profileService.updatePhase(profileId, phaseId, request));
    }

    @DeleteMapping("/profiles/{profileId}/phases/{phaseId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deletePhase(@PathVariable UUID profileId, @PathVariable UUID phaseId) {
        profileService.deletePhase(profileId, phaseId);
        return ResponseEntity.noContent().build();
    }

    // ── 4.3 Serveis ──

    @PostMapping("/phases/{phaseId}/services")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ProfileResponse> addServiceToPhase(@PathVariable UUID phaseId, @RequestBody CreateServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.addServiceToPhase(phaseId, request));
    }

    @PostMapping("/services")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ServiceResponse> createAddonService(@RequestBody CreateAddonServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.createAddonService(request));
    }

    @GetMapping("/services")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ServiceResponse>> listServices() {
        return ResponseEntity.ok(profileService.listServices());
    }

    // ── 4.4 Assignació i cicle de vida ──

    @PostMapping("/tenants/{tenantId}/profiles/{profileId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<AssignProfileResponse> assignProfile(@PathVariable UUID tenantId, @PathVariable UUID profileId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vaultService.assignProfile(tenantId, profileId));
    }

    @DeleteMapping("/tenants/{tenantId}/profiles/{profileId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> removeProfile(@PathVariable UUID tenantId, @PathVariable UUID profileId) {
        vaultService.removeProfile(tenantId, profileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tenants/{tenantId}/budget")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #tenantId == authentication.principal.tenantId)")
    public ResponseEntity<BudgetResponse> getBudget(@PathVariable UUID tenantId,
                                                    @RequestParam UUID profileId,
                                                    @RequestParam(required = false) List<UUID> addonIds,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        boolean includeCost = hasRole("SUPER_ADMIN") || hasRole("ADMIN");
        return ResponseEntity.ok(profileService.calculateBudget(profileId, addonIds, includeCost));
    }

    @PostMapping("/tenants/{tenantId}/phases/{phaseId}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #tenantId == authentication.principal.tenantId)")
    public ResponseEntity<ApprovePhaseResponse> approvePhase(@PathVariable UUID tenantId, @PathVariable UUID phaseId) {
        return ResponseEntity.ok(vaultService.approvePhase(tenantId, phaseId));
    }

    @PostMapping("/tenants/{tenantId}/phases/{phaseId}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #tenantId == authentication.principal.tenantId)")
    public ResponseEntity<Void> rejectPhase(@PathVariable UUID tenantId, @PathVariable UUID phaseId) {
        vaultService.rejectPhase(tenantId, phaseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tenants/{tenantId}/phases/{phaseId}/advance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> advancePhase(@PathVariable UUID tenantId, @PathVariable UUID phaseId,
                                             @RequestBody AdvancePhaseRequest request) {
        vaultService.advancePhase(tenantId, phaseId, request.getStatus());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/tenants/{tenantId}/services/{serviceId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> changeServiceStatus(@PathVariable UUID tenantId, @PathVariable UUID serviceId,
                                                    @RequestBody ChangeServiceStatusRequest request) {
        vaultService.changeServiceStatus(tenantId, serviceId, request.getStatus());
        return ResponseEntity.ok().build();
    }

    // ── 4.5 Credencials ──

    @PutMapping("/tenants/{tenantId}/services/{serviceId}/fields/{fieldId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> setCredential(@PathVariable UUID tenantId,
                                                              @PathVariable UUID serviceId,
                                                              @PathVariable UUID fieldId,
                                                              @RequestBody SetCredentialRequest request,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        vaultService.setCredential(tenantId, serviceId, fieldId, request.getValue(), principal.id());
        return ResponseEntity.ok(Map.of("isSet", true, "maskedValue", "***set"));
    }

    @PostMapping("/tenants/{tenantId}/services/{serviceId}/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> verifyService(@PathVariable UUID tenantId,
                                                              @PathVariable UUID serviceId) {
        vaultService.verifyService(tenantId, serviceId);
        return ResponseEntity.ok(Map.of("verified", true, "message", "Connexió verificada"));
    }

    // ── 4.6 Add-ons ──

    @PostMapping("/tenants/{tenantId}/addons/{serviceId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<AddonResponse> addAddon(@PathVariable UUID tenantId, @PathVariable UUID serviceId,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vaultService.addAddon(tenantId, serviceId, principal.id()));
    }

    @PostMapping("/tenants/{tenantId}/addons/{serviceId}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #tenantId == authentication.principal.tenantId)")
    public ResponseEntity<Void> approveAddon(@PathVariable UUID tenantId, @PathVariable UUID serviceId) {
        vaultService.approveAddon(tenantId, serviceId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/tenants/{tenantId}/addons/{serviceId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> removeAddon(@PathVariable UUID tenantId, @PathVariable UUID serviceId) {
        vaultService.removeAddon(tenantId, serviceId);
        return ResponseEntity.noContent().build();
    }

    // ── 4.7 Setup / Dashboard ──

    @GetMapping("/tenants/{tenantId}/setup")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #tenantId == authentication.principal.tenantId)")
    public ResponseEntity<SetupResponse> getSetup(@PathVariable UUID tenantId,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        boolean includeClearValue = hasRole("SUPER_ADMIN") || hasRole("ADMIN");
        return ResponseEntity.ok(vaultService.getSetup(tenantId, includeClearValue));
    }

    // Monitorització

    @GetMapping("/tenants/{tenantId}/monitoring/invoices")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #tenantId == authentication.principal.tenantId)")
    public ResponseEntity<MonitoringResponse.InvoiceMonitoring> getInvoiceMonitoring(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(vaultService.getInvoiceMonitoring(tenantId));
    }

    @GetMapping("/tenants/{tenantId}/monitoring/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #tenantId == authentication.principal.tenantId)")
    public ResponseEntity<MonitoringResponse.PaymentMonitoring> getPaymentMonitoring(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(vaultService.getPaymentMonitoring(tenantId));
    }

    @GetMapping("/tenants/{tenantId}/monitoring/phases")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<MonitoringResponse.PhaseMonitoring> getPhaseMonitoring(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(vaultService.getPhaseMonitoring(tenantId));
    }

    // ── Helpers ──

    private boolean hasRole(String role) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
