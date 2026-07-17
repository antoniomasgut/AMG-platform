package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.agents.domain.NexeServiceConfigRepository;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.engine.domain.LandingRepository;
import com.amg.digitalitzacio.google.domain.GoogleConnectionRepository;
import com.amg.digitalitzacio.shared.exception.ConflictException;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.vault.api.dto.*;
import com.amg.digitalitzacio.vault.domain.*;
import com.amg.digitalitzacio.whatsapp.domain.WhatsAppWabaConfigRepository;
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
    private final VaultCommunicationService vaultCommunicationService;
    private final VaultEncryption encryption;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final TenantRepository tenantRepository;
    private final NexeServiceConfigRepository nexeServiceConfigRepository;
    private final WhatsAppWabaConfigRepository whatsAppWabaConfigRepository;
    private final GoogleConnectionRepository googleConnectionRepository;
    private final LandingRepository landingRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AssignProfileResponse assignProfile(UUID tenantId, UUID profileId) {
        var profile = serviceProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found: " + profileId));

        tenantProfileRepository.findByTenantIdAndProfileId(tenantId, profileId).ifPresent(tp -> {
            if (tp.getIsActive()) throw new ConflictException("Profile already assigned to tenant");
        });

        var tp = TenantProfile.builder()
                .tenantId(tenantId).profileId(profileId).build();
        tenantProfileRepository.save(tp);

        var phases = phaseRepository.findByProfileIdOrderBySortOrder(profileId);
        var phaseSummaries = new ArrayList<AssignProfileResponse.PhaseSummary>();
        var totalPrice = BigDecimal.ZERO;

        for (var phase : phases) {
            if (tenantPhaseRepository.findByTenantIdAndPhaseId(tenantId, phase.getId()).isEmpty()) {
                var tph = TenantPhase.builder()
                        .tenantId(tenantId).profileId(profileId).phaseId(phase.getId()).build();
                tenantPhaseRepository.save(tph);
            }

            var services = catalogServiceRepository.findByPhaseIdOrderBySortOrder(phase.getId());
            var phaseTotal = BigDecimal.ZERO;
            for (var svc : services) {
                var existingTs = tenantServiceRepository.findByTenantIdAndServiceId(tenantId, svc.getId());
                if (existingTs.isEmpty()) {
                    var ts = TenantService.builder()
                            .tenantId(tenantId).serviceId(svc.getId()).phaseId(phase.getId())
                            .setupPriceLocked(svc.getSalePrice() != null ? svc.getSalePrice() : BigDecimal.ZERO)
                            .monthlyPriceLocked(svc.getMonthlyPrice() != null ? svc.getMonthlyPrice() : BigDecimal.ZERO)
                            .catalogVersionLocked(svc.getVersion() != null ? svc.getVersion() : 1)
                            .build();
                    tenantServiceRepository.save(ts);
                }
                phaseTotal = phaseTotal.add(svc.getSalePrice() != null ? svc.getSalePrice() : BigDecimal.ZERO);
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
    public void assignPhase(UUID tenantId, UUID phaseId) {
        var phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Phase not found: " + phaseId));
        if (tenantPhaseRepository.findByTenantIdAndPhaseId(tenantId, phaseId).isPresent()) {
            throw new ConflictException("Phase already assigned to tenant");
        }
        var tph = TenantPhase.builder()
                .tenantId(tenantId).profileId(phase.getProfileId()).phaseId(phaseId).build();
        tenantPhaseRepository.save(tph);
        var services = catalogServiceRepository.findByPhaseIdOrderBySortOrder(phaseId);
        for (var svc : services) {
            if (tenantServiceRepository.findByTenantIdAndServiceId(tenantId, svc.getId()).isEmpty()) {
                var ts = TenantService.builder()
                        .tenantId(tenantId).serviceId(svc.getId()).phaseId(phaseId)
                        .setupPriceLocked(svc.getSalePrice() != null ? svc.getSalePrice() : java.math.BigDecimal.ZERO)
                        .monthlyPriceLocked(svc.getMonthlyPrice() != null ? svc.getMonthlyPrice() : java.math.BigDecimal.ZERO)
                        .catalogVersionLocked(svc.getVersion() != null ? svc.getVersion() : 1)
                        .build();
                tenantServiceRepository.save(ts);
            }
        }
    }

    @Override
    @Transactional
    public void addStandaloneService(UUID tenantId, UUID catalogServiceId) {
        var svc = catalogServiceRepository.findById(catalogServiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + catalogServiceId));
        if (tenantServiceRepository.findByTenantIdAndServiceId(tenantId, catalogServiceId).isPresent()) {
            throw new ConflictException("Service already assigned to tenant");
        }
        var ts = TenantService.builder()
                .tenantId(tenantId).serviceId(catalogServiceId).phaseId(null)
                .setupPriceLocked(svc.getSalePrice() != null ? svc.getSalePrice() : java.math.BigDecimal.ZERO)
                .monthlyPriceLocked(svc.getMonthlyPrice() != null ? svc.getMonthlyPrice() : java.math.BigDecimal.ZERO)
                .catalogVersionLocked(svc.getVersion() != null ? svc.getVersion() : 1)
                .build();
        tenantServiceRepository.save(ts);
    }

    @Override
    @Transactional
    public void removeTenantService(UUID tenantId, UUID tenantServiceId) {
        var ts = tenantServiceRepository.findById(tenantServiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant service not found: " + tenantServiceId));
        if (!ts.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Tenant service not found: " + tenantServiceId);
        }
        tenantServiceRepository.delete(ts);
    }

    @Override
    @Transactional
    public ApprovePhaseResponse approvePhase(UUID tenantId, UUID phaseId) {
        var tph = tenantPhaseRepository.findByTenantIdAndPhaseId(tenantId, phaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant phase not found"));

        var services = catalogServiceRepository.findByPhaseIdOrderBySortOrder(phaseId);
        var amount = services.stream()
                .map(s -> s.getSalePrice() != null ? s.getSalePrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Create invoice via stub
        var invoiceId = invoiceService.createInvoice(tenantId, phaseId, amount);
        tph.setInvoiceId(invoiceId.toString());
        tph.setInvoiceAmount(amount);
        tph.setInvoiceStatus(com.amg.digitalitzacio.vault.domain.InvoiceStatus.SENT);

        // Charge via stub — approval only confirmed if payment succeeds
        var paymentResult = paymentService.charge(tenantId, invoiceId, amount);
        if (paymentResult.success()) {
            tph.setApprovalStatus(ApprovalStatus.APPROVED);
            tph.setApprovedAt(Instant.now());
            tph.setPaymentStatus(PaymentStatus.PAID);
            tph.setPaidAt(Instant.now());
            tph.setImplementationStatus(ImplementationStatus.NOT_STARTED);
        } else {
            tph.setPaymentStatus(PaymentStatus.FAILED);
            log.warn("Payment failed for tenant={}, phase={}: {}", tenantId, phaseId, paymentResult.errorMessage());
        }

        tenantPhaseRepository.save(tph);

        return new ApprovePhaseResponse(phaseId,
                tph.getApprovalStatus().name(),
                tph.getPaymentStatus() != null ? tph.getPaymentStatus().name() : PaymentStatus.PENDING.name(),
                tph.getImplementationStatus() != null ? tph.getImplementationStatus().name() : ImplementationStatus.NOT_STARTED.name(),
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

    @Transactional
    public boolean toggleService(UUID tenantId, UUID serviceId) {
        var ts = tenantServiceRepository.findByTenantIdAndServiceId(tenantId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant service not found"));
        boolean newState = !Boolean.TRUE.equals(ts.getIsEnabled());
        ts.setIsEnabled(newState);
        tenantServiceRepository.save(ts);
        return newState;
    }

    @Override
    @Transactional
    public void setCredential(UUID tenantId, UUID serviceId, UUID fieldId, String value, UUID userId) {
        tenantServiceRepository.findByTenantIdAndServiceId(tenantId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant service not found"));

        var existing = tenantCredentialRepository.findByTenantIdAndFieldId(tenantId, fieldId);
        boolean isNew = existing.isEmpty();
        var tc = existing.orElseGet(() -> TenantCredential.builder()
                .tenantId(tenantId).fieldId(fieldId).build());

        var encrypted = encryption.encrypt(value);
        tc.setEncryptedValue(encrypted);
        tc.setIsSet(true);
        tenantCredentialRepository.save(tc);

        // Audit log
        credentialAuditLogRepository.save(CredentialAuditLog.builder()
                .credentialId(tc.getId()).userId(userId)
                .action(isNew ? AuditAction.CREATE : AuditAction.UPDATE)
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
    @Transactional
    public boolean verifyService(UUID tenantId, UUID serviceId) {
        var svc = catalogServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + serviceId));

        var verified = switch (svc.getSlug()) {
            case "smtp-corporatiu" -> verifySmtp(tenantId);
            default -> true;
        };

        if (verified) {
            tenantServiceRepository.findByTenantIdAndServiceId(tenantId, serviceId).ifPresent(ts -> {
                if (ts.getStatus() != ServiceStatus.VERIFIED) {
                    ts.setStatus(ServiceStatus.VERIFIED);
                    ts.setStatusChangedAt(Instant.now());
                    tenantServiceRepository.save(ts);
                    eventPublisher.publishEvent(
                            new com.amg.digitalitzacio.shared.events.ServiceVerifiedEvent(
                                    tenantId, serviceId, svc.getSlug()));
                    log.info("Servei {} verificat per al tenant {}", svc.getSlug(), tenantId);
                }
            });
        }

        return verified;
    }

    private boolean verifySmtp(UUID tenantId) {
        var fields = credentialFieldRepository.findByServiceIdOrderBySortOrder(
                findServiceBySlug("smtp-corporatiu").getId());

        // Map field keys to their values from tenant vault
        var creds = new java.util.HashMap<String, String>();
        for (var field : fields) {
            var tcOpt = tenantCredentialRepository.findByTenantIdAndFieldId(tenantId, field.getId());
            if (tcOpt.isEmpty() || !tcOpt.get().getIsSet()) {
                log.warn("SMTP verify failed for tenant {}: field {} not set", tenantId, field.getKey());
                return false;
            }
            var decrypted = encryption.decrypt(tcOpt.get().getEncryptedValue());
            creds.put(field.getKey(), decrypted);
        }

        var host = creds.get("smtp_host");
        var portStr = creds.get("smtp_port");
        var user = creds.get("smtp_user");
        var pass = creds.get("smtp_pass");
        var security = creds.get("smtp_security");

        if (host == null || portStr == null || user == null || pass == null || security == null) {
            log.warn("SMTP verify failed for tenant {}: missing required credentials", tenantId);
            return false;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            log.warn("SMTP verify failed for tenant {}: invalid port {}", tenantId, portStr);
            return false;
        }

        try {
            var props = new java.util.Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");

            if ("TLS".equalsIgnoreCase(security)) {
                props.put("mail.smtp.starttls.enable", "true");
            } else if ("SSL".equalsIgnoreCase(security)) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.socketFactory.port", port);
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            }

            var session = jakarta.mail.Session.getInstance(props, new jakarta.mail.Authenticator() {
                @Override
                protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new jakarta.mail.PasswordAuthentication(user, pass);
                }
            });

            try (var transport = session.getTransport("smtp")) {
                transport.connect(host, port, user, pass);
                log.info("SMTP verification succeeded for tenant {}", tenantId);
                return true;
            }
        } catch (Exception e) {
            log.warn("SMTP verification failed for tenant {}: {}", tenantId, e.getMessage());
            return false;
        }
    }

    private CatalogService findServiceBySlug(String slug) {
        return catalogServiceRepository.findBySlugAndPhaseIdIsNull(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Base service not found: " + slug));
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
                .tenantId(tenantId).serviceId(serviceId).phaseId(null)
                .setupPriceLocked(svc.getSalePrice() != null ? svc.getSalePrice() : BigDecimal.ZERO)
                .monthlyPriceLocked(svc.getMonthlyPrice() != null ? svc.getMonthlyPrice() : BigDecimal.ZERO)
                .catalogVersionLocked(svc.getVersion() != null ? svc.getVersion() : 1)
                .build();
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
                            ts.getId(),
                            new SetupResponse.ProfileSetup.PhaseSetup.ServiceSetup.ServiceRef(
                                    svc.getId(), svc.getName(), svc.getSlug(), svc.getType().name()),
                            ts.getStatus().name(),
                            Boolean.TRUE.equals(ts.getIsEnabled()),
                            fieldSetups));
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
        var addonServiceIds = addons.stream().map(a -> a.getServiceId()).collect(java.util.stream.Collectors.toSet());
        var addonSetups = addons.stream().map(a -> {
            var svc = catalogServiceRepository.findById(a.getServiceId()).orElse(null);
            return new SetupResponse.AddonSetup(
                    new SetupResponse.AddonSetup.ServiceRef(
                            a.getServiceId(), svc != null ? svc.getName() : "Unknown"),
                    a.getApprovalRequired(),
                    a.getApprovalStatus() != null ? a.getApprovalStatus().name() : null);
        }).toList();

        var allTenantServices = tenantServiceRepository.findByTenantId(tenantId);
        var standaloneSetups = allTenantServices.stream()
                .filter(ts -> ts.getPhaseId() == null && !addonServiceIds.contains(ts.getServiceId()))
                .map(ts -> {
                    var svc = catalogServiceRepository.findById(ts.getServiceId()).orElse(null);
                    return new SetupResponse.StandaloneSetup(
                            ts.getId(),
                            new SetupResponse.StandaloneSetup.ServiceRef(
                                    ts.getServiceId(),
                                    svc != null ? svc.getName() : "Unknown",
                                    svc != null ? svc.getSlug() : "",
                                    svc != null ? svc.getType().name() : ""),
                            ts.getStatus().name(),
                            Boolean.TRUE.equals(ts.getIsEnabled()));
                }).toList();

        return new SetupResponse(profileSetups, addonSetups, standaloneSetups);
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
        var tenant = tenantRepository.findById(tenantId).orElse(null);
        CommunicationChannel channel = CommunicationChannel.EMAIL; // default
        String recipient = tenant != null && tenant.getEmail() != null ? tenant.getEmail() : null;
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalStateException("Tenant " + tenantId + " no té email configurat — no es pot enviar la sol·licitud");
        }

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

        var commRequest = vaultCommunicationService.send(tenantId, ts.getId(), channel, recipient,
                subject, body, reqType, fieldId,
                Instant.now().plus(7, ChronoUnit.DAYS));

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

        if (commReq.getStatus() != CommunicationStatus.SENT) {
            throw new IllegalStateException("Communication request is not in SENT status: " + commReq.getStatus());
        }
        if (commReq.getExpiresAt() != null && Instant.now().isAfter(commReq.getExpiresAt())) {
            throw new IllegalStateException("Communication request has expired at: " + commReq.getExpiresAt());
        }

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

        // Mark the specific requested phase as COMPLETED
        if (request.phaseId() != null) {
            tenantPhaseRepository.findByTenantIdAndPhaseId(tenantId, request.phaseId()).ifPresent(tph -> {
                tph.setImplementationStatus(ImplementationStatus.COMPLETED);
                tph.setCompletedAt(Instant.now());
                tenantPhaseRepository.save(tph);
            });
        }

        // Find the next non-completed phase after request.phaseId()
        boolean foundCurrent = request.phaseId() == null; // if no phaseId, treat first as next
        for (var phase : phases) {
            if (foundCurrent) {
                var tphOpt = tenantPhaseRepository.findByTenantIdAndPhaseId(tenantId, phase.getId());
                boolean alreadyDone = tphOpt.map(t -> t.getImplementationStatus() == ImplementationStatus.COMPLETED).orElse(false);
                if (!alreadyDone) {
                    nextPhase = new ConfirmPhaseResponse.NextPhase(phase.getId(), phase.getName(), phase.getSortOrder());
                    tp.setPhaseStatus(PhaseStatus.CONFIGURING);
                    tenantProfileRepository.save(tp);

                    var nextTph = tphOpt.orElseGet(() -> {
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
            }
            if (phase.getId().equals(request.phaseId())) {
                foundCurrent = true;
            }
        }

        if (profileCompleted) {
            tp.setCompletedAt(Instant.now());
            tp.setPhaseStatus(PhaseStatus.COMPLETED);
            tenantProfileRepository.save(tp);
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

    @Override
    @Transactional
    public void bumpCatalogVersion(UUID serviceId) {
        var svc = catalogServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog service not found: " + serviceId));
        svc.setVersion(svc.getVersion() + 1);
        catalogServiceRepository.save(svc);

        var newVersion = svc.getVersion();
        tenantServiceRepository.findByServiceId(serviceId).forEach(ts -> {
            if (ts.getCatalogVersionLocked() < newVersion) {
                ts.setOutdated(true);
                ts.setOutdatedAt(Instant.now());
                tenantServiceRepository.save(ts);
                log.info("TenantService {} marcat com a desfassat (v{} → v{})", ts.getId(), ts.getCatalogVersionLocked(), newVersion);
            }
        });
    }

    @Override
    @Transactional
    public void acknowledgeOutdated(UUID tenantId, UUID serviceId) {
        var ts = tenantServiceRepository.findByTenantIdAndServiceId(tenantId, serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant service not found"));
        ts.setOutdated(false);
        ts.setOutdatedAt(null);
        tenantServiceRepository.save(ts);
        log.info("TenantService {} revisat i desfàs netejat per tenant {}", serviceId, tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.amg.digitalitzacio.vault.api.dto.OutdatedServiceResponse> listOutdatedServices() {
        return tenantServiceRepository.findByOutdatedTrue().stream()
                .map(ts -> {
                    var svc = catalogServiceRepository.findById(ts.getServiceId()).orElse(null);
                    return new com.amg.digitalitzacio.vault.api.dto.OutdatedServiceResponse(
                            ts.getId(),
                            ts.getTenantId(),
                            ts.getServiceId(),
                            svc != null ? svc.getName() : "Desconegut",
                            svc != null ? svc.getSlug() : "",
                            svc != null ? svc.getVersion() : -1,
                            ts.getCatalogVersionLocked(),
                            ts.getOutdatedAt()
                    );
                }).toList();
    }

    // ── NexeLocal phase toggle ────────────────────────────────────────────────

    @Override
    @Transactional
    public boolean toggleContractedPhase(UUID tenantId, String phase) {
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));

        var contracted = parseCommaSeparated(tenant.getContractedPhases());
        if (!contracted.contains(phase)) {
            throw new IllegalArgumentException("Phase " + phase + " not contracted for tenant " + tenantId);
        }

        // activePhases null = totes les contractades actives
        var active = new TreeSet<>(tenant.getActivePhases() != null
                ? parseCommaSeparated(tenant.getActivePhases())
                : contracted);

        boolean willBeActive;
        if (active.contains(phase)) {
            active.remove(phase);
            willBeActive = false;
        } else {
            active.add(phase);
            willBeActive = true;
        }

        // Si active == contracted, tornem a null (estat per defecte)
        if (active.equals(new TreeSet<>(contracted))) {
            tenant.setActivePhases(null);
        } else {
            tenant.setActivePhases(String.join(",", active));
        }
        tenantRepository.save(tenant);
        log.info("Tenant {} phase {} toggled to {}", tenantId, phase, willBeActive);
        return willBeActive;
    }

    @Override
    @Transactional(readOnly = true)
    public NexeSetupResponse getNexeSetup(UUID tenantId) {
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));

        var contracted = parseCommaSeparated(tenant.getContractedPhases());
        var active = new HashSet<>(tenant.getActivePhases() != null
                ? parseCommaSeparated(tenant.getActivePhases())
                : contracted);

        // Configuració per fase (AGENDA, PRESSUPOSTOS, FIDELITZACIO, EQUIP)
        var configs = nexeServiceConfigRepository.findByTenantId(tenantId);
        var configuredKeys = new HashSet<String>();
        for (var cfg : configs) {
            var val = cfg.getConfigJson();
            if (val != null && !val.isBlank() && !val.equals("{}") && !val.equals("null")) {
                configuredKeys.add(cfg.getServiceKey());
            }
        }

        // F1 configurada si hi ha almenys una landing publicada o URL web externa
        boolean f1Configured = landingRepository.countByTenantId(tenantId) > 0
                || (tenant.getAgentSystemPrompt() != null && !tenant.getAgentSystemPrompt().isBlank());

        var phases = contracted.stream().sorted().map(key -> {
            boolean isConfigured = switch (key) {
                case "F1" -> f1Configured;
                case "F2" -> configuredKeys.contains("AGENDA");
                case "F3" -> configuredKeys.contains("PRESSUPOSTOS");
                case "F4" -> configuredKeys.contains("FIDELITZACIO");
                case "F5" -> configuredKeys.contains("EQUIP");
                default   -> false;
            };
            return new NexeSetupResponse.PhaseStatus(key, active.contains(key), isConfigured);
        }).toList();

        // Prerequisits
        var waConfig = whatsAppWabaConfigRepository.findByTenantId(tenantId);
        var googleConnected = googleConnectionRepository.existsByTenantIdAndActiveTrue(tenantId);
        var equipConfig = configs.stream()
                .filter(c -> "EQUIP".equals(c.getServiceKey()))
                .findFirst();
        boolean telegramConfigured = equipConfig.map(c -> {
            var val = c.getConfigJson();
            return val != null && val.contains("telegram_group_id");
        }).orElse(false);

        var prerequisites = new NexeSetupResponse.Prerequisites(
                waConfig.isPresent(),
                waConfig.map(w -> w.getPhoneNumberId()).orElse(null),
                tenant.getAgentSystemPrompt() != null && !tenant.getAgentSystemPrompt().isBlank(),
                landingRepository.countByTenantId(tenantId),
                googleConnected,
                telegramConfigured
        );

        return new NexeSetupResponse(new ArrayList<>(contracted), phases, prerequisites);
    }

    private List<String> parseCommaSeparated(String value) {
        if (value == null || value.isBlank()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(value.split(",")));
    }
}
