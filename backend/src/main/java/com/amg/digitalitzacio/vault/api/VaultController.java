package com.amg.digitalitzacio.vault.api;

import com.amg.digitalitzacio.shared.security.UserPrincipal;
import com.amg.digitalitzacio.vault.api.dto.*;
import com.amg.digitalitzacio.vault.application.*;
import com.amg.digitalitzacio.vault.domain.ImplementationStatus;
import com.amg.digitalitzacio.vault.domain.ServiceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vault")
@RequiredArgsConstructor
public class VaultController {

    private final ProfileService profileService;
    private final VaultService vaultService;

    // --- Profiles ---

    @PostMapping("/profiles")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse createProfile(@RequestBody CreateProfileRequest request) {
        return profileService.createProfile(request);
    }

    @GetMapping("/profiles")
    public List<ProfileResponse> listProfiles() {
        return profileService.listProfiles();
    }

    @GetMapping("/profiles/{id}")
    public ProfileResponse getProfile(@PathVariable UUID id) {
        return profileService.getProfile(id);
    }

    @PutMapping("/profiles/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ProfileResponse updateProfile(@PathVariable UUID id, @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(id, request);
    }

    @DeleteMapping("/profiles/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProfile(@PathVariable UUID id) {
        profileService.deactivateProfile(id);
    }

    // --- Phases ---

    @PostMapping("/profiles/{profileId}/phases")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse addPhase(@PathVariable UUID profileId, @RequestBody CreatePhaseRequest request) {
        return profileService.addPhase(profileId, request);
    }

    @PutMapping("/profiles/{profileId}/phases/{phaseId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ProfileResponse updatePhase(@PathVariable UUID profileId, @PathVariable UUID phaseId,
                                       @RequestBody UpdatePhaseRequest request) {
        return profileService.updatePhase(profileId, phaseId, request);
    }

    @DeleteMapping("/profiles/{profileId}/phases/{phaseId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ProfileResponse deletePhase(@PathVariable UUID profileId, @PathVariable UUID phaseId) {
        return profileService.deletePhase(profileId, phaseId);
    }

    // --- Services ---

    @PostMapping("/phases/{phaseId}/services")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse addServiceToPhase(@PathVariable UUID phaseId, @RequestBody CreateServiceRequest request) {
        return profileService.addServiceToPhase(phaseId, request);
    }

    @PostMapping("/services")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse createAddonService(@RequestBody CreateAddonServiceRequest request) {
        return profileService.createAddonService(request);
    }

    @GetMapping("/services")
    public List<ProfileResponse.ServiceResponse> listServices() {
        return profileService.listServices();
    }

    // --- Tenant Profile Assignment ---

    @PostMapping("/tenants/{tenantId}/profiles/{profileId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignProfileResponse assignProfile(@PathVariable UUID tenantId, @PathVariable UUID profileId) {
        return vaultService.assignProfile(tenantId, profileId);
    }

    @DeleteMapping("/tenants/{tenantId}/profiles/{profileId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeProfile(@PathVariable UUID tenantId, @PathVariable UUID profileId) {
        vaultService.removeProfile(tenantId, profileId);
    }

    // --- Budget ---

    @GetMapping("/tenants/{tenantId}/budget")
    public BudgetResponse calculateBudget(@PathVariable UUID tenantId,
                                           @RequestParam UUID profileId,
                                           @RequestParam(required = false) List<UUID> addonIds,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        boolean includeCost = !"CLIENT".equals(principal.role());
        return profileService.calculateBudget(profileId, addonIds, includeCost);
    }

    // --- Phase lifecycle ---

    @PostMapping("/tenants/{tenantId}/phases/{phaseId}/approve")
    public ApprovePhaseResponse approvePhase(@PathVariable UUID tenantId, @PathVariable UUID phaseId) {
        return vaultService.approvePhase(tenantId, phaseId);
    }

    @PostMapping("/tenants/{tenantId}/phases/{phaseId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectPhase(@PathVariable UUID tenantId, @PathVariable UUID phaseId) {
        vaultService.rejectPhase(tenantId, phaseId);
    }

    @PostMapping("/tenants/{tenantId}/phases/{phaseId}/advance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void advancePhase(@PathVariable UUID tenantId, @PathVariable UUID phaseId,
                              @RequestBody AdvancePhaseRequest request) {
        vaultService.advancePhase(tenantId, phaseId, ImplementationStatus.valueOf(request.status()));
    }

    // --- Service status ---

    @PutMapping("/tenants/{tenantId}/services/{serviceId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeServiceStatus(@PathVariable UUID tenantId, @PathVariable UUID serviceId,
                                     @RequestBody ChangeServiceStatusRequest request) {
        vaultService.changeServiceStatus(tenantId, serviceId, ServiceStatus.valueOf(request.status()));
    }

    // --- Credentials ---

    @PutMapping("/tenants/{tenantId}/services/{serviceId}/fields/{fieldId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public CredentialResponse setCredential(@PathVariable UUID tenantId, @PathVariable UUID serviceId,
                                             @PathVariable UUID fieldId, @RequestBody SetCredentialRequest request,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        vaultService.setCredential(tenantId, serviceId, fieldId, request.value(), principal.id());
        var tc = vaultService.getSetup(tenantId, false);
        return new CredentialResponse(fieldId, true, CredentialResponse.mask(request.value()));
    }

    @PostMapping("/tenants/{tenantId}/services/{serviceId}/verify")
    public VerifyResponse verifyService(@PathVariable UUID tenantId, @PathVariable UUID serviceId) {
        boolean verified = vaultService.verifyService(tenantId, serviceId);
        return new VerifyResponse(verified, verified ? "Connexió correcta" : "Verificació fallida");
    }

    // --- Add-ons ---

    @PostMapping("/tenants/{tenantId}/addons/{serviceId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AddonResponse addAddon(@PathVariable UUID tenantId, @PathVariable UUID serviceId,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return vaultService.addAddon(tenantId, serviceId, principal.id());
    }

    @PostMapping("/tenants/{tenantId}/addons/{serviceId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approveAddon(@PathVariable UUID tenantId, @PathVariable UUID serviceId) {
        vaultService.approveAddon(tenantId, serviceId);
    }

    @DeleteMapping("/tenants/{tenantId}/addons/{serviceId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAddon(@PathVariable UUID tenantId, @PathVariable UUID serviceId) {
        vaultService.removeAddon(tenantId, serviceId);
    }

    // --- Setup & Monitoring ---

    @GetMapping("/tenants/{tenantId}/setup")
    public SetupResponse getSetup(@PathVariable UUID tenantId,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        boolean includeClearValue = !"CLIENT".equals(principal.role());
        return vaultService.getSetup(tenantId, includeClearValue);
    }

    @GetMapping("/tenants/{tenantId}/monitoring/invoices")
    public MonitoringResponse.InvoiceMonitoring getInvoiceMonitoring(@PathVariable UUID tenantId) {
        return vaultService.getInvoiceMonitoring(tenantId);
    }

    @GetMapping("/tenants/{tenantId}/monitoring/payments")
    public MonitoringResponse.PaymentMonitoring getPaymentMonitoring(@PathVariable UUID tenantId) {
        return vaultService.getPaymentMonitoring(tenantId);
    }

    @GetMapping("/tenants/{tenantId}/monitoring/phases")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public MonitoringResponse.PhaseMonitoring getPhaseMonitoring(@PathVariable UUID tenantId) {
        return vaultService.getPhaseMonitoring(tenantId);
    }
}
