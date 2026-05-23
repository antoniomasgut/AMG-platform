package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.vault.api.dto.*;
import com.amg.digitalitzacio.vault.domain.ImplementationStatus;
import com.amg.digitalitzacio.vault.domain.ServiceStatus;

import java.util.List;
import java.util.UUID;

public interface VaultService {
    AssignProfileResponse assignProfile(UUID tenantId, UUID profileId);
    void removeProfile(UUID tenantId, UUID profileId);
    ApprovePhaseResponse approvePhase(UUID tenantId, UUID phaseId);
    void rejectPhase(UUID tenantId, UUID phaseId);
    void advancePhase(UUID tenantId, UUID phaseId, ImplementationStatus status);
    void changeServiceStatus(UUID tenantId, UUID serviceId, ServiceStatus newStatus);
    boolean toggleService(UUID tenantId, UUID serviceId);
    void setCredential(UUID tenantId, UUID serviceId, UUID fieldId, String value, UUID userId);
    boolean verifyService(UUID tenantId, UUID serviceId);
    AddonResponse addAddon(UUID tenantId, UUID serviceId, UUID addedBy);
    void approveAddon(UUID tenantId, UUID serviceId);
    void removeAddon(UUID tenantId, UUID serviceId);
    SetupResponse getSetup(UUID tenantId, boolean includeClearValue);

    // Guided configuration
    RequestInfoResponse requestClientInfo(UUID tenantId, UUID serviceId, RequestInfoRequest request);
    CommunicationRespondResponse handleClientResponse(UUID requestId, CommunicationRespondRequest request);
    ConfirmPhaseResponse confirmPhase(UUID tenantId, UUID profileId, ConfirmPhaseRequest request);

    MonitoringResponse.InvoiceMonitoring getInvoiceMonitoring(UUID tenantId);
    MonitoringResponse.PaymentMonitoring getPaymentMonitoring(UUID tenantId);
    MonitoringResponse.PhaseMonitoring getPhaseMonitoring(UUID tenantId);

    void bumpCatalogVersion(UUID serviceId);
    void acknowledgeOutdated(UUID tenantId, UUID serviceId);
    List<OutdatedServiceResponse> listOutdatedServices();
}
