package com.amg.digitalitzacio.vault.api;

import com.amg.digitalitzacio.shared.security.UserPrincipal;
import com.amg.digitalitzacio.vault.api.dto.*;
import com.amg.digitalitzacio.vault.api.dto.UpdateServicePriceRequest;
import com.amg.digitalitzacio.vault.application.*;
import com.amg.digitalitzacio.vault.domain.ImplementationStatus;
import com.amg.digitalitzacio.vault.domain.ServiceStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @PreAuthorize("isAuthenticated()")
    public List<ProfileResponse> listProfiles() {
        return profileService.listProfiles();
    }

    @GetMapping("/profiles/{id}")
    @PreAuthorize("isAuthenticated()")
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

    @PostMapping("/profiles/{profileId}/services")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse addServiceToProfile(@PathVariable UUID profileId, @RequestBody CreateServiceRequest request) {
        return profileService.addServiceToProfile(profileId, request);
    }

    @PutMapping("/services/{serviceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ProfileResponse updateService(@PathVariable UUID serviceId, @RequestBody CreateServiceRequest request) {
        return profileService.updateService(serviceId, request);
    }

    @DeleteMapping("/services/{serviceId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteService(@PathVariable UUID serviceId) {
        profileService.deleteService(serviceId);
    }

    @PostMapping("/services")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileResponse createAddonService(@RequestBody CreateAddonServiceRequest request) {
        return profileService.createAddonService(request);
    }

    @GetMapping("/services")
    @PreAuthorize("isAuthenticated()")
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

    @PostMapping("/tenants/{tenantId}/phases/{phaseId}/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignPhase(@PathVariable UUID tenantId, @PathVariable UUID phaseId) {
        vaultService.assignPhase(tenantId, phaseId);
    }

    @PostMapping("/tenants/{tenantId}/catalog-services/{serviceId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addStandaloneService(@PathVariable UUID tenantId, @PathVariable UUID serviceId) {
        vaultService.addStandaloneService(tenantId, serviceId);
    }

    @DeleteMapping("/tenants/{tenantId}/tenant-services/{tenantServiceId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTenantService(@PathVariable UUID tenantId, @PathVariable UUID tenantServiceId) {
        vaultService.removeTenantService(tenantId, tenantServiceId);
    }

    // --- Budget ---

    @GetMapping("/tenants/{tenantId}/budget")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public BudgetResponse calculateBudget(@PathVariable UUID tenantId,
                                           @RequestParam UUID profileId,
                                           @RequestParam(required = false) List<UUID> addonIds,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        boolean includeCost = !"CLIENT".equals(principal.role());
        return profileService.calculateBudget(profileId, addonIds, includeCost);
    }

    // --- Phase lifecycle ---

    @PostMapping("/tenants/{tenantId}/phases/{phaseId}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApprovePhaseResponse approvePhase(@PathVariable UUID tenantId, @PathVariable UUID phaseId) {
        return vaultService.approvePhase(tenantId, phaseId);
    }

    @PostMapping("/tenants/{tenantId}/phases/{phaseId}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
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

    @PatchMapping("/tenants/{tenantId}/services/{serviceId}/toggle")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public ResponseEntity<java.util.Map<String, Boolean>> toggleService(
            @PathVariable UUID tenantId, @PathVariable UUID serviceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean enabled = vaultService.toggleService(tenantId, serviceId);
        return ResponseEntity.ok(java.util.Map.of("isEnabled", enabled));
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public VerifyResponse verifyService(@PathVariable UUID tenantId, @PathVariable UUID serviceId) {
        boolean verified = vaultService.verifyService(tenantId, serviceId);
        return new VerifyResponse(verified, verified ? "Connexió correcta" : "Verificació fallida");
    }

    @PatchMapping("/services/{id}/price")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ProfileResponse.ServiceResponse updateServicePrice(@PathVariable UUID id,
            @RequestBody UpdateServicePriceRequest request) {
        return profileService.updateServicePrice(id, request);
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
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

    // --- Guided Configuration ---

    @PostMapping("/tenants/{tenantId}/services/{serviceId}/request")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public RequestInfoResponse requestClientInfo(@PathVariable UUID tenantId, @PathVariable UUID serviceId,
                                                  @RequestBody RequestInfoRequest request) {
        return vaultService.requestClientInfo(tenantId, serviceId, request);
    }

    @PostMapping("/communication/{requestId}/respond")
    public CommunicationRespondResponse handleClientResponse(@PathVariable UUID requestId,
                                                              @Valid @RequestBody CommunicationRespondRequest request) {
        return vaultService.handleClientResponse(requestId, request);
    }

    @PostMapping("/tenants/{tenantId}/profiles/{profileId}/confirm-phase")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public ConfirmPhaseResponse confirmPhase(@PathVariable UUID tenantId, @PathVariable UUID profileId,
                                              @RequestBody ConfirmPhaseRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return vaultService.confirmPhase(tenantId, profileId, request);
    }

    // --- Setup & Monitoring ---

    @GetMapping("/tenants/{tenantId}/setup")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public SetupResponse getSetup(@PathVariable UUID tenantId,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        boolean includeClearValue = !"CLIENT".equals(principal.role());
        return vaultService.getSetup(tenantId, includeClearValue);
    }

    @GetMapping("/tenants/{tenantId}/monitoring/invoices")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public MonitoringResponse.InvoiceMonitoring getInvoiceMonitoring(@PathVariable UUID tenantId) {
        return vaultService.getInvoiceMonitoring(tenantId);
    }

    @GetMapping("/tenants/{tenantId}/monitoring/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public MonitoringResponse.PaymentMonitoring getPaymentMonitoring(@PathVariable UUID tenantId) {
        return vaultService.getPaymentMonitoring(tenantId);
    }

    @GetMapping("/tenants/{tenantId}/monitoring/phases")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public MonitoringResponse.PhaseMonitoring getPhaseMonitoring(@PathVariable UUID tenantId) {
        return vaultService.getPhaseMonitoring(tenantId);
    }

    // --- Versionat de catàleg ---

    @PostMapping("/catalog-services/{id}/bump")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bumpCatalogVersion(@PathVariable UUID id) {
        vaultService.bumpCatalogVersion(id);
    }

    @PostMapping("/tenants/{tenantId}/services/{serviceId}/acknowledge")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acknowledgeOutdated(@PathVariable UUID tenantId, @PathVariable UUID serviceId) {
        vaultService.acknowledgeOutdated(tenantId, serviceId);
    }

    @GetMapping("/outdated")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public List<OutdatedServiceResponse> listOutdatedServices() {
        return vaultService.listOutdatedServices();
    }
}
