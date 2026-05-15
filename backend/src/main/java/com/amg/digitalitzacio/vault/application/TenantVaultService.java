package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.vault.api.dto.*;
import com.amg.digitalitzacio.vault.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantVaultService implements VaultService {

    private final ServiceProfileRepository serviceProfileRepository;
    private final PhaseRepository phaseRepository;
    private final CatalogServiceRepository catalogServiceRepository;
    private final CredentialFieldRepository credentialFieldRepository;
    private final TenantProfileRepository tenantProfileRepository;
    private final TenantPhaseRepository tenantPhaseRepository;
    private final TenantServiceRepository tenantServiceRepository;
    private final TenantCredentialRepository tenantCredentialRepository;
    private final TenantServiceAddonRepository tenantServiceAddonRepository;
    private final CredentialAuditLogRepository credentialAuditLogRepository;
    private final CommunicationRequestRepository communicationRequestRepository;
    private final VaultEncryption encryption;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;

    @Override
    @Transactional
    public AssignProfileResponse assignProfile(UUID tenantId, UUID profileId) {
        var profile = serviceProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found: " + profileId));

        tenantProfileRepository.findByTenantIdAndProfileId(tenantId, profileId).ifPresent(tp -> {
            if (tp.getIsActive()) throw new IllegalArgumentException("Profile already assigned to tenant");
        });

        var tp = TenantProfile.builder()
                .tenantId(tenantId).profileId(profileId).build();
        tenantProfileRepository.save(tp);

        var phases = phaseRepository.findByProfileIdOrderBySortOrder(profileId);
        var phaseSummaries = new ArrayList<AssignProfileResponse.PhaseSummary>();
        var totalPrice = BigDecimal.ZERO;

        for (var phase : phases) {
            tenantPhaseRepository.findByTenantIdAndPhaseId(tenantId, phase.getId()).ifPresent(tph -> {});

            var tph = TenantPhase.builder()
                    .tenantId(tenantId).profileId(profileId).phaseId(phase.getId()).build();
            tenantPhaseRepository.save(tph);

            var services = catalogServiceRepository.findByPhaseIdOrderBySortOrder(phase.getId());
            var phaseTotal = BigDecimal.ZERO;
            for (var svc : services) {
                tenantServiceRepository.findByTenantIdAndServiceId(tenantId, svc.getId()).ifPresent(ts -> {});
                var ts = TenantService.builder()
                        .tenantId(tenantId).serviceId(svc.getId()).phaseId(phase.getId()).build();
                tenantServiceRepository.save(ts);
                phaseTotal = phaseTotal.add(svc.getSalePrice());
            }

            phaseSummaries.add(new AssignProfileResponse.PhaseSummary(
                    phase.getId(), phase.getName(), phase.getSortOrder(), "PENDING_APPROVAL",
                    services.size(), phaseTotal));
            totalPrice = totalPrice.add(phaseTotal);
        }

        return new AssignProfileResponse(profileId, phaseSummaries, totalPrice);
    }

    @Override
    @Transactional
    public void removeProfile(UUID tenantId, UUID profileId) {
        var tp = tenantProfileRepository.findByTenantIdAndProfileId(tenantId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant profile not found"));
        tp.setIsActive(false);
        tenantProfileRepository.save(tp);
    }

    @Override
    @Transactional
    public ApprovePhaseResponse approvePhase(UUID tenantId, UUID phaseId) {
        var tph = tenantPhaseRepository.findByTenantIdAndPhaseId(tenantId, phaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant phase not found"));
        tph.setApprovalStatus(ApprovalStatus.APPROVED);
        tph.setApprovedAt(Instant.now());

        var services = catalogServiceRepository.findByPhaseIdOrderBySortOrder(phaseId);
        var amount = services.stream().map(CatalogService::getSalePrice).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create invoice via stub
        var invoiceId = invoiceService.createInvoice(tenantId, phaseId, amount);
        tph.setInvoiceId(invoiceId.toString());
        tph.setInvoiceAmount(amount);
        tph.setInvoiceStatus(com.amg.digitalitzacio.vault.domain.InvoiceStatus.SENT);

        // Charge via stub
        var paymentResult = paymentService.charge(tenantId, invoiceId, amount);
        if (paymentResult.success()) {
            tph.setPaymentStatus(PaymentStatus.PAID);
            tph.setPaidAt(Instant.now());
            tph.setImplementationStatus(ImplementationStatus.NOT_STARTED);
        } else {
            tph.setPaymentStatus(PaymentStatus.FAILED);
            log.warn("Payment failed for tenant={}, phase={}: {}", tenantId, phaseId, paymentResult.errorMessage());
        }

        tenantPhaseRepository.save(tph);

        return new ApprovePhaseResponse(phaseId, "APPROVED",
                tph.getPaymentStatus().name(), tph.getImplementationStatus().name(),
                invoiceId.toString(), amount);
    }

    @Override
    @Transactional
    public void rejectPhase(UUID tenantId, UUID phaseId) {
        var tph = tenantPhaseRepository.findByTenantIdAndPhaseId(tenantId, phaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant phase not found"));
        tph.setApprovalStatus(ApprovalStatus.REJECTED);
        tenantPhaseRepository.save(tph);
    }

    @Override
    @Transactional
    public void advancePhase(UUID tenantId, UUID phaseId, ImplementationStatus status) {
        var tph = tenantPhaseRepository.findByTenantIdAndPhaseId(tenantId, phaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant phase not found"));

        if (status == ImplementationStatus.COMPLETED) {
            var services = tenantServiceRepository.findByTenantIdAndPhaseId(tenantId, phaseId);
            boolean allVerified = services.stream().allMatch(ts -> ts.getStatus() == ServiceStatus.VERIFIED);
            if (!allVerified) {
                throw new IllegalArgumentException("All services must be VERIFIED before completing phase");
            }
            tph.setCompletedAt(Instant.now());
        }

        tph.setImplementationStatus(status);
        tenantPhaseRepository.save(tph);
    }

    @Override
    @Transactional
    public void changeServiceStatus(UUID tenantId, UUID serviceId, ServiceStatus newStatus) {
        var ts = tenantServiceRepository.findByTenantIdAndServiceId(tenantId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant service not found"));
        ts.setStatus(newStatus);
        ts.setStatusChangedAt(Instant.now());
        tenantServiceRepository.save(ts);
    }

    @Override
    @Transactional
    public void setCredential(UUID tenantId, UUID serviceId, UUID fieldId, String value, UUID userId) {
        tenantServiceRepository.findByTenantIdAndServiceId(tenantId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant service not found"));

        var existing = tenantCredentialRepository.findByTenantIdAndFieldId(tenantId, fieldId);
        var tc = existing.orElseGet(() -> TenantCredential.builder()
                .tenantId(tenantId).fieldId(fieldId).build());

        var encrypted = encryption.encrypt(value);
        tc.setEncryptedValue(encrypted);
        tc.setIsSet(true);
        tenantCredentialRepository.save(tc);

        // Audit log
        credentialAuditLogRepository.save(CredentialAuditLog.builder()
                .credentialId(tc.getId()).userId(userId)
                .action(AuditAction.CREATE)
                .maskedValue(CredentialResponse.mask(value))
                .createdAt(Instant.now()).build());

        // Check if all fields for this service are set
        var allFields = credentialFieldRepository.findByServiceIdOrderBySortOrder(serviceId);
        var setFields = tenantCredentialRepository.findByTenantId(tenantId);
        boolean allSet = allFields.stream().allMatch(f ->
                setFields.stream().anyMatch(tc2 -> tc2.getFieldId().equals(f.getId()) && tc2.getIsSet()));

        if (allSet) {
            var ts = tenantServiceRepository.findByTenantIdAndServiceId(tenantId, serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Tenant service not found"));
            ts.setStatus(ServiceStatus.CONFIGURED);
            ts.setStatusChangedAt(Instant.now());
            tenantServiceRepository.save(ts);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyService(UUID tenantId, UUID serviceId) {
        // Stub — always returns true
        return true;
    }

    @Override
    @Transactional
    public AddonResponse addAddon(UUID tenantId, UUID serviceId, UUID addedBy) {
        var svc = catalogServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + serviceId));
        if (!svc.getIsAddon()) {
            throw new IllegalArgumentException("Service is not an add-on");
        }

        tenantServiceRepository.findByTenantIdAndServiceId(tenantId, serviceId).ifPresent(ts -> {
            throw new IllegalArgumentException("Add-on already assigned to tenant");
        });

        var ts = TenantService.builder()
                .tenantId(tenantId).serviceId(serviceId).phaseId(null).build();
        tenantServiceRepository.save(ts);

        boolean requiresApproval = svc.getSalePrice() != null && svc.getSalePrice().compareTo(BigDecimal.ZERO) > 0;

        var tsa = TenantServiceAddon.builder()
                .tenantId(tenantId).serviceId(serviceId).addedBy(addedBy)
                .approvalRequired(requiresApproval)
                .createdAt(Instant.now()).build();
        tenantServiceAddonRepository.save(tsa);

        var status = requiresApproval ? "PENDING_CLIENT_APPROVAL" : "ACTIVE";
        return new AddonResponse(requiresApproval, svc.getSalePrice(), status);
    }

    @Override
    @Transactional
    public void approveAddon(UUID tenantId, UUID serviceId) {
        var tsa = tenantServiceAddonRepository.findByTenantIdAndServiceId(tenantId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant add-on not found"));
        tsa.setApprovalStatus(ApprovalStatus.APPROVED);
        tenantServiceAddonRepository.save(tsa);
    }

    @Override
    @Transactional
    public void removeAddon(UUID tenantId, UUID serviceId) {
        var ts = tenantServiceRepository.findByTenantIdAndServiceId(tenantId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant service not found"));
        tenantServiceRepository.delete(ts);

        tenantServiceAddonRepository.findByTenantIdAndServiceId(tenantId, serviceId)
                .ifPresent(tenantServiceAddonRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public SetupResponse getSetup(UUID tenantId, boolean includeClearValue) {
        var tenantProfiles = tenantProfileRepository.findByTenantId(tenantId);
        var profileSetups = new ArrayList<SetupResponse.ProfileSetup>();

        for (var tp : tenantProfiles) {
            var profile = serviceProfileRepository.findById(tp.getProfileId()).orElse(null);
            if (profile == null) continue;

            var phases = phaseRepository.findByProfileIdOrderBySortOrder(tp.getProfileId());
            var phaseSetups = new ArrayList<SetupResponse.ProfileSetup.PhaseSetup>();

            for (var phase : phases) {
                var tphOpt = tenantPhaseRepository.findByTenantIdAndPhaseId(tenantId, phase.getId());
                if (tphOpt.isEmpty()) continue;
                var tph = tphOpt.get();

                var services = catalogServiceRepository.findByPhaseIdOrderBySortOrder(phase.getId());
                var serviceSetups = new ArrayList<SetupResponse.ProfileSetup.PhaseSetup.ServiceSetup>();

                for (var svc : services) {
                    var tsOpt = tenantServiceRepository.findByTenantIdAndServiceId(tenantId, svc.getId());
                    if (tsOpt.isEmpty()) continue;
                    var ts = tsOpt.get();

                    var fields = credentialFieldRepository.findByServiceIdOrderBySortOrder(svc.getId());
                    var fieldSetups = fields.stream().map(f -> {
                        var tcOpt = tenantCredentialRepository.findByTenantIdAndFieldId(tenantId, f.getId());
                        return new SetupResponse.ProfileSetup.PhaseSetup.ServiceSetup.FieldSetup(
                                f.getId(), f.getKey(), f.getLabel(), tcOpt.map(TenantCredential::getIsSet).orElse(false));
                    }).toList();

                    serviceSetups.add(new SetupResponse.ProfileSetup.PhaseSetup.ServiceSetup(
                            new SetupResponse.ProfileSetup.PhaseSetup.ServiceSetup.ServiceRef(
                                    svc.getId(), svc.getName(), svc.getSlug(), svc.getType().name()),
                            ts.getStatus().name(), fieldSetups));
                }

                phaseSetups.add(new SetupResponse.ProfileSetup.PhaseSetup(
                        new SetupResponse.ProfileSetup.PhaseSetup.PhaseRef(
                                phase.getId(), phase.getName(), phase.getSortOrder()),
                        tph.getApprovalStatus().name(),
                        tph.getInvoiceStatus() != null ? tph.getInvoiceStatus().name() : null,
                        tph.getPaymentStatus() != null ? tph.getPaymentStatus().name() : null,
                        tph.getImplementationStatus().name(),
                        serviceSetups));
            }

            profileSetups.add(new SetupResponse.ProfileSetup(
                    new SetupResponse.ProfileSetup.ProfileRef(profile.getId(), profile.getName(), profile.getSlug()),
                    phaseSetups));
        }

        var addons = tenantServiceAddonRepository.findByTenantId(tenantId);
        var addonSetups = addons.stream().map(a -> {
            var svc = catalogServiceRepository.findById(a.getServiceId()).orElse(null);
            return new SetupResponse.AddonSetup(
                    new SetupResponse.AddonSetup.ServiceRef(
                            a.getServiceId(), svc != null ? svc.getName() : "Unknown"),
                    a.getApprovalRequired(),
                    a.getApprovalStatus() != null ? a.getApprovalStatus().name() : null);
        }).toList();

        return new SetupResponse(profileSetups, addonSetups);
    }

    // --- Guided Configuration ---

    @Override
    @Transactional
    public RequestInfoResponse requestClientInfo(UUID tenantId, UUID serviceId, RequestInfoRequest request) {
        var ts = tenantServiceRepository.findByTenantIdAndServiceId(tenantId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant service not found: " + serviceId));

        if (ts.getStatus() != ServiceStatus.PENDING && ts.getStatus() != ServiceStatus.AWAITING_CLIENT) {
            throw new IllegalArgumentException("Service is not in a configurable state: " + ts.getStatus());
        }

        // Look up the service definition for message generation
        var svc = catalogServiceRepository.findById(ts.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service definition not found"));

        // Determine recipient and channel from tenant context
        CommunicationChannel channel = CommunicationChannel.EMAIL; // default
        String recipient = "client@unknown.com";

        // Build message based on request type
        String body;
        String subject;
        UUID fieldId = request.fieldId();

        var reqType = RequestType.valueOf(request.requestType());
        if (reqType == RequestType.REQUEST_CREDENTIAL && fieldId != null) {
            var field = credentialFieldRepository.findById(fieldId).orElse(null);
            var fieldLabel = field != null ? field.getLabel() : "desconegut";
            body = request.customMessage() != null ? request.customMessage()
                    : "Per configurar " + svc.getName() + " necessitam el camp " + fieldLabel + ".";
            subject = "Configuració de " + svc.getName();
        } else if (reqType == RequestType.REQUEST_PERMISSION) {
            body = request.customMessage() != null ? request.customMessage()
                    : "Dona'ns permís per continuar amb la configuració de " + svc.getName() + ".";
            subject = "Permís requerit per " + svc.getName();
        } else {
            body = request.customMessage() != null ? request.customMessage()
                    : "Necessitam més informació per configurar " + svc.getName() + ".";
            subject = "Informació requerida per " + svc.getName();
        }

        var commRequest = CommunicationRequest.builder()
                .tenantId(tenantId).tenantServiceId(ts.getId())
                .channel(channel).recipient(recipient)
                .subject(subject).body(body)
                .status(CommunicationStatus.SENT)
                .requestType(reqType).fieldId(fieldId)
                .sentAt(Instant.now())
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        commRequest = communicationRequestRepository.save(commRequest);

        // Update service status to AWAITING_CLIENT
        ts.setStatus(ServiceStatus.AWAITING_CLIENT);
        ts.setStatusChangedAt(Instant.now());
        tenantServiceRepository.save(ts);

        log.info("Communication request {} sent for service {} (tenant {})", commRequest.getId(), serviceId, tenantId);

        return new RequestInfoResponse(commRequest.getId(), channel.name(),
                CommunicationStatus.SENT.name(),
                "Missatge enviat a " + recipient, commRequest.getExpiresAt());
    }

    @Override
    @Transactional
    public CommunicationRespondResponse handleClientResponse(UUID requestId, CommunicationRespondRequest request) {
        var commReq = communicationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Communication request not found: " + requestId));

        commReq.setStatus(CommunicationStatus.RESPONDED);
        commReq.setRespondedAt(Instant.now());
        commReq.setResponseData(request.text());
        communicationRequestRepository.save(commReq);

        var ts = tenantServiceRepository.findById(commReq.getTenantServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant service not found"));

        String action;
        String serviceStatus;

        if (commReq.getRequestType() == RequestType.REQUEST_CREDENTIAL && commReq.getFieldId() != null) {
            // Store encrypted credential
            var existingTc = tenantCredentialRepository.findByTenantIdAndFieldId(commReq.getTenantId(), commReq.getFieldId());
            var tc = existingTc.orElseGet(() -> TenantCredential.builder()
                    .tenantId(commReq.getTenantId()).fieldId(commReq.getFieldId()).build());
            tc.setEncryptedValue(encryption.encrypt(request.text()));
            tc.setIsSet(true);
            tenantCredentialRepository.save(tc);

            action = "credential_stored";
            ts.setStatus(ServiceStatus.CONFIGURED);
            ts.setStatusChangedAt(Instant.now());
            tenantServiceRepository.save(ts);

            // Check if all services in phase are configured → update phase status
            if (ts.getPhaseId() != null) {
                checkPhaseCompletion(commReq.getTenantId(), ts.getPhaseId());
            }
        } else if (commReq.getRequestType() == RequestType.REQUEST_PERMISSION) {
            action = "permission_granted";
            ts.setStatus(ServiceStatus.CONFIGURED);
            ts.setStatusChangedAt(Instant.now());
            tenantServiceRepository.save(ts);

            if (ts.getPhaseId() != null) {
                checkPhaseCompletion(commReq.getTenantId(), ts.getPhaseId());
            }
        } else {
            action = "info_received_pending_admin";
            ts.setStatus(ServiceStatus.AWAITING_CLIENT);
            tenantServiceRepository.save(ts);
        }

        serviceStatus = ts.getStatus().name();
        return new CommunicationRespondResponse(true, action, serviceStatus);
    }

    private void checkPhaseCompletion(UUID tenantId, UUID phaseId) {
        var services = tenantServiceRepository.findByTenantIdAndPhaseId(tenantId, phaseId);
        boolean allConfigured = services.stream()
                .allMatch(ts -> ts.getStatus() == ServiceStatus.CONFIGURED || ts.getStatus() == ServiceStatus.VERIFIED);

        if (allConfigured) {
            // Update TenantProfile phaseStatus to AWAITING_CONFIRMATION
            var phase = phaseRepository.findById(phaseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Phase not found: " + phaseId));
            var profileId = phase.getProfileId();

            var tpOpt = tenantProfileRepository.findByTenantIdAndProfileId(tenantId, profileId);
            tpOpt.ifPresent(tp -> {
                tp.setPhaseStatus(PhaseStatus.AWAITING_CONFIRMATION);
                tenantProfileRepository.save(tp);
            });

            log.info("Phase {} (tenant {}) completed configuration, awaiting client confirmation", phaseId, tenantId);
        }
    }

    @Override
    @Transactional
    public ConfirmPhaseResponse confirmPhase(UUID tenantId, UUID profileId, ConfirmPhaseRequest request) {
        var tp = tenantProfileRepository.findByTenantIdAndProfileId(tenantId, profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant profile not found"));

        tp.setPhaseStatus(PhaseStatus.COMPLETED);
        tenantProfileRepository.save(tp);

        var phases = phaseRepository.findByProfileIdOrderBySortOrder(profileId);
        ConfirmPhaseResponse.NextPhase nextPhase = null;
        boolean profileCompleted = true;

        // Find current phase index and check if there's a next phase
        UUID currentPhaseId = null;
        for (var phase : phases) {
            var tphOpt = tenantPhaseRepository.findByTenantIdAndPhaseId(tenantId, phase.getId());
            if (tphOpt.isPresent() && tphOpt.get().getImplementationStatus() != ImplementationStatus.COMPLETED) {
                // Check if this is the current phase (first not-completed one)
                if (currentPhaseId == null) {
                    currentPhaseId = phase.getId();
                    // This is the one being confirmed
                    var tph = tphOpt.get();
                    tph.setImplementationStatus(ImplementationStatus.COMPLETED);
                    tph.setCompletedAt(Instant.now());
                    tenantPhaseRepository.save(tph);
                }
            }
        }

        if (currentPhaseId == null) {
            // Profile is completed
            tp.setCompletedAt(Instant.now());
            tenantProfileRepository.save(tp);
            profileCompleted = true;
        } else {
            // Check if there's a next phase after the confirmed one
            boolean foundCurrent = false;
            for (var phase : phases) {
                if (foundCurrent) {
                    nextPhase = new ConfirmPhaseResponse.NextPhase(phase.getId(), phase.getName(), phase.getSortOrder());
                    tp.setPhaseStatus(PhaseStatus.CONFIGURING);
                    tenantProfileRepository.save(tp);

                    // Verify the TenantPhase exists
                    var nextTph = tenantPhaseRepository.findByTenantIdAndPhaseId(tenantId, phase.getId())
                            .orElseGet(() -> {
                                var newTph = TenantPhase.builder()
                                        .tenantId(tenantId).profileId(profileId).phaseId(phase.getId()).build();
                                return tenantPhaseRepository.save(newTph);
                            });
                    if (nextTph.getImplementationStatus() == null) {
                        nextTph.setImplementationStatus(ImplementationStatus.NOT_STARTED);
                        tenantPhaseRepository.save(nextTph);
                    }
                    profileCompleted = false;
                    break;
                }
                if (phase.getId().equals(request.phaseId())) {
                    foundCurrent = true;
                }
            }
            if (nextPhase == null) {
                // No next phase found, profile completed
                tp.setCompletedAt(Instant.now());
                tp.setPhaseStatus(PhaseStatus.COMPLETED);
                tenantProfileRepository.save(tp);
                profileCompleted = true;
            }
        }

        return new ConfirmPhaseResponse(tp.getPhaseStatus().name(), nextPhase, profileCompleted);
    }

    @Override
    public MonitoringResponse.InvoiceMonitoring getInvoiceMonitoring(UUID tenantId) {
        var tphases = tenantPhaseRepository.findByTenantId(tenantId);
        var phases = tphases.stream()
                .filter(tph -> tph.getInvoiceStatus() != null)
                .map(tph -> {
                    var phase = phaseRepository.findById(tph.getPhaseId()).orElse(null);
                    return new MonitoringResponse.InvoiceMonitoring.PhaseInvoice(
                            phase != null ? phase.getName() : "Unknown",
                            phase != null ? phase.getSortOrder() : 0,
                            tph.getInvoiceId(), tph.getInvoiceAmount(),
                            tph.getInvoiceStatus().name(), tph.getPaidAt());
                }).toList();

        var pending = (int) tphases.stream().filter(t -> t.getInvoiceStatus() == InvoiceStatus.SENT).count();
        var overdue = (int) tphases.stream().filter(t -> t.getInvoiceStatus() == InvoiceStatus.OVERDUE).count();
        var totalPaid = tphases.stream()
                .filter(t -> t.getPaymentStatus() == PaymentStatus.PAID)
                .map(t -> t.getInvoiceAmount() != null ? t.getInvoiceAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new MonitoringResponse.InvoiceMonitoring(phases, pending, overdue, totalPaid);
    }

    @Override
    public MonitoringResponse.PaymentMonitoring getPaymentMonitoring(UUID tenantId) {
        var tphases = tenantPhaseRepository.findByTenantId(tenantId);
        var phases = tphases.stream()
                .filter(tph -> tph.getPaymentStatus() != null)
                .map(tph -> {
                    var phase = phaseRepository.findById(tph.getPhaseId()).orElse(null);
                    return new MonitoringResponse.PaymentMonitoring.PhasePayment(
                            phase != null ? phase.getName() : "Unknown",
                            tph.getInvoiceAmount(),
                            tph.getPaymentStatus().name(), tph.getPaidAt(), "card");
                }).toList();

        var pending = (int) tphases.stream().filter(t -> t.getPaymentStatus() == PaymentStatus.PENDING).count();
        var failed = (int) tphases.stream().filter(t -> t.getPaymentStatus() == PaymentStatus.FAILED).count();

        return new MonitoringResponse.PaymentMonitoring(phases, pending, failed);
    }

    @Override
    public MonitoringResponse.PhaseMonitoring getPhaseMonitoring(UUID tenantId) {
        var tphases = tenantPhaseRepository.findByTenantId(tenantId);
        var total = tphases.size();
        var pendingApproval = (int) tphases.stream().filter(t -> t.getApprovalStatus() == ApprovalStatus.PENDING_APPROVAL).count();
        var inProgress = (int) tphases.stream().filter(t -> t.getImplementationStatus() == ImplementationStatus.IN_PROGRESS).count();
        var completed = (int) tphases.stream().filter(t -> t.getImplementationStatus() == ImplementationStatus.COMPLETED).count();
        var rejected = (int) tphases.stream().filter(t -> t.getApprovalStatus() == ApprovalStatus.REJECTED).count();

        var phases = tphases.stream().map(tph -> {
            var phase = phaseRepository.findById(tph.getPhaseId()).orElse(null);
            var progress = switch (tph.getImplementationStatus()) {
                case NOT_STARTED -> "0%";
                case IN_PROGRESS -> "50%";
                case COMPLETED -> "100%";
            };
            return new MonitoringResponse.PhaseMonitoring.PhaseProgress(
                    phase != null ? phase.getName() : "Unknown",
                    tph.getApprovalStatus().name(),
                    tph.getImplementationStatus().name(), progress);
        }).toList();

        return new MonitoringResponse.PhaseMonitoring(total, pendingApproval, inProgress, completed, rejected, phases);
    }
}
