package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.vault.api.dto.*;
import com.amg.digitalitzacio.vault.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantVaultService implements VaultService {

    private final ServiceProfileRepository profileRepository;
    private final PhaseRepository phaseRepository;
    private final ServiceRepository serviceRepository;
    private final CredentialFieldRepository fieldRepository;
    private final TenantProfileRepository tenantProfileRepository;
    private final TenantPhaseRepository tenantPhaseRepository;
    private final TenantServiceRepository tenantServiceRepository;
    private final TenantCredentialRepository tenantCredentialRepository;
    private final TenantServiceAddonRepository addonRepository;
    private final CredentialAuditLogRepository auditLogRepository;
    private final VaultEncryption encryption;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;

    // ── Assignar / Desassignar perfil ──

    @Transactional
    public AssignProfileResponse assignProfile(UUID tenantId, UUID profileId) {
        var profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil no trobat"));

        if (tenantProfileRepository.findByTenantIdAndProfileId(tenantId, profileId).isPresent()) {
            throw new IllegalArgumentException("Aquest perfil ja està assignat al tenant");
        }

        // Crear TenantProfile
        var tenantProfile = TenantProfile.builder()
                .tenantId(tenantId)
                .profileId(profileId)
                .isActive(true)
                .startedAt(Instant.now())
                .build();
        tenantProfileRepository.save(tenantProfile);

        // Crear TenantPhase per cada fase + TenantService per cada servei
        var phases = phaseRepository.findByProfileIdOrderBySortOrder(profileId);
        var totalPrice = BigDecimal.ZERO;

        for (var phase : phases) {
            tenantPhaseRepository.save(TenantPhase.builder()
                    .tenantId(tenantId)
                    .profileId(profileId)
                    .phaseId(phase.getId())
                    .approvalStatus(ApprovalStatus.PENDING_APPROVAL)
                    .implementationStatus(ImplementationStatus.NOT_STARTED)
                    .build());

            var services = serviceRepository.findByPhaseIdOrderBySortOrder(phase.getId());
            for (var service : services) {
                tenantServiceRepository.save(TenantService.builder()
                        .tenantId(tenantId)
                        .serviceId(service.getId())
                        .phaseId(phase.getId())
                        .status(ServiceStatus.PENDING)
                        .build());
            }
            var phaseTotal = services.stream().map(CatalogService::getSalePrice).reduce(BigDecimal.ZERO, BigDecimal::add);
            totalPrice = totalPrice.add(phaseTotal);
        }

        var phaseSummaries = phases.stream().map(phase -> AssignProfileResponse.PhaseSummary.builder()
                .phaseId(phase.getId())
                .name(phase.getName())
                .sortOrder(phase.getSortOrder())
                .approvalStatus(ApprovalStatus.PENDING_APPROVAL.name())
                .totalServices(serviceRepository.findByPhaseIdOrderBySortOrder(phase.getId()).size())
                .totalPrice(serviceRepository.findByPhaseIdOrderBySortOrder(phase.getId()).stream()
                        .map(CatalogService::getSalePrice).reduce(BigDecimal.ZERO, BigDecimal::add))
                .build()).toList();

        return AssignProfileResponse.builder()
                .profileId(profileId)
                .phases(phaseSummaries)
                .totalPrice(totalPrice)
                .build();
    }

    @Transactional
    public void removeProfile(UUID tenantId, UUID profileId) {
        var tp = tenantProfileRepository.findByTenantIdAndProfileId(tenantId, profileId)
                .orElseThrow(() -> new IllegalArgumentException("El tenant no té aquest perfil assignat"));
        tp.setIsActive(false);
        tenantProfileRepository.save(tp);
    }

    // ── Cicle de vida de fase ──

    @Transactional
    public ApprovePhaseResponse approvePhase(UUID tenantId, UUID phaseId) {
        var tp = tenantPhaseRepository.findByTenantIdAndPhaseId(tenantId, phaseId)
                .orElseThrow(() -> new IllegalArgumentException("Fase no assignada al tenant"));

        if (tp.getApprovalStatus() == ApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("La fase ja està aprovada");
        }

        // Calcular import
        var services = tenantServiceRepository.findByTenantIdAndPhaseId(tenantId, phaseId);
        var phase = phaseRepository.findById(phaseId).orElseThrow();
        var profile = profileRepository.findById(tp.getProfileId()).orElseThrow();
        var amount = services.stream()
                .map(ts -> serviceRepository.findById(ts.getServiceId()).orElseThrow())
                .map(CatalogService::getSalePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Crear factura
        var invoiceId = invoiceService.createInvoice(tenantId, phaseId, amount);

        // Cobrar
        var paymentResult = paymentService.charge(tenantId, invoiceId, amount);

        if (paymentResult.success()) {
            tp.setApprovalStatus(ApprovalStatus.APPROVED);
            tp.setApprovedAt(Instant.now());
            tp.setInvoiceId(invoiceId);
            tp.setInvoiceAmount(amount);
            tp.setInvoiceStatus(InvoiceStatus.PAID);
            tp.setPaymentStatus(PaymentStatus.PAID);
            tp.setPaidAt(Instant.now());
            tp.setImplementationStatus(ImplementationStatus.NOT_STARTED);
        } else {
            tp.setApprovalStatus(ApprovalStatus.APPROVED);
            tp.setApprovedAt(Instant.now());
            tp.setInvoiceId(invoiceId);
            tp.setInvoiceAmount(amount);
            tp.setInvoiceStatus(InvoiceStatus.PENDING);
            tp.setPaymentStatus(PaymentStatus.FAILED);
            tp.setImplementationStatus(ImplementationStatus.NOT_STARTED);
        }

        tenantPhaseRepository.save(tp);

        return ApprovePhaseResponse.builder()
                .phaseId(phaseId)
                .approvalStatus(tp.getApprovalStatus().name())
                .paymentStatus(tp.getPaymentStatus() != null ? tp.getPaymentStatus().name() : null)
                .implementationStatus(tp.getImplementationStatus().name())
                .invoiceId(invoiceId)
                .amount(amount)
                .build();
    }

    @Transactional
    public void rejectPhase(UUID tenantId, UUID phaseId) {
        var tp = tenantPhaseRepository.findByTenantIdAndPhaseId(tenantId, phaseId)
                .orElseThrow(() -> new IllegalArgumentException("Fase no assignada al tenant"));
        tp.setApprovalStatus(ApprovalStatus.REJECTED);
        tenantPhaseRepository.save(tp);
    }

    @Transactional
    public void advancePhase(UUID tenantId, UUID phaseId, ImplementationStatus status) {
        var tp = tenantPhaseRepository.findByTenantIdAndPhaseId(tenantId, phaseId)
                .orElseThrow(() -> new IllegalArgumentException("Fase no assignada al tenant"));

        if (tp.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new IllegalArgumentException("No es pot avançar: la fase no està aprovada");
        }

        // Si es marca COMPLETED, verificar que tots els serveis estan VERIFIED
        if (status == ImplementationStatus.COMPLETED) {
            var services = tenantServiceRepository.findByTenantIdAndPhaseId(tenantId, phaseId);
            var allVerified = services.stream().allMatch(s -> s.getStatus() == ServiceStatus.VERIFIED);
            if (!allVerified) {
                throw new IllegalArgumentException("Hi ha serveis pendents de verificació");
            }
            tp.setCompletedAt(Instant.now());
        }

        tp.setImplementationStatus(status);
        tenantPhaseRepository.save(tp);
    }

    // ── Estat serveis ──

    @Transactional
    public void changeServiceStatus(UUID tenantId, UUID serviceId, ServiceStatus newStatus) {
        var ts = tenantServiceRepository.findByTenantIdAndServiceId(tenantId, serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Servei no assignat al tenant"));
        ts.setStatus(newStatus);
        ts.setStatusChangedAt(Instant.now());
        tenantServiceRepository.save(ts);
    }

    // ── Credencials ──

    @Transactional
    public void setCredential(UUID tenantId, UUID serviceId, UUID fieldId, String value, UUID userId) {
        var ts = tenantServiceRepository.findByTenantIdAndServiceId(tenantId, serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Servei no assignat al tenant"));

        var field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new IllegalArgumentException("Camp no trobat"));
        if (!field.getServiceId().equals(ts.getServiceId())) {
            throw new IllegalArgumentException("El camp no pertany a aquest servei");
        }

        var encrypted = encryption.encrypt(value);
        var masked = encryption.mask(value);

        var existing = tenantCredentialRepository.findByTenantIdAndFieldId(tenantId, fieldId);
        var credential = existing.orElse(TenantCredential.builder()
                .tenantId(tenantId)
                .fieldId(fieldId)
                .build());
        credential.setEncryptedValue(encrypted);
        credential.setIsSet(true);
        tenantCredentialRepository.save(credential);

        // Audit log
        auditLogRepository.save(CredentialAuditLog.builder()
                .credentialId(credential.getId())
                .userId(userId)
                .action(existing.isPresent() ? AuditAction.UPDATE : AuditAction.CREATE)
                .maskedValue(masked)
                .build());

        // Si tots els camps requerits del servei estan configurats, marcar CONFIGURED
        var allFields = fieldRepository.findByServiceIdOrderBySortOrder(ts.getServiceId());
        var requiredFields = allFields.stream().filter(CredentialField::getIsRequired).toList();
        var allSet = requiredFields.stream().allMatch(f ->
                tenantCredentialRepository.findByTenantIdAndFieldId(tenantId, f.getId())
                        .map(TenantCredential::getIsSet).orElse(false));
        if (allSet && ts.getStatus() == ServiceStatus.AWAITING_CLIENT) {
            ts.setStatus(ServiceStatus.CONFIGURED);
            ts.setStatusChangedAt(Instant.now());
            tenantServiceRepository.save(ts);
        }
    }

    @Transactional
    public void verifyService(UUID tenantId, UUID serviceId) {
        var ts = tenantServiceRepository.findByTenantIdAndServiceId(tenantId, serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Servei no assignat al tenant"));

        var fields = fieldRepository.findByServiceIdOrderBySortOrder(ts.getServiceId());
        for (var field : fields) {
            var cred = tenantCredentialRepository.findByTenantIdAndFieldId(tenantId, field.getId());
            cred.ifPresent(c -> {
                c.setLastVerifiedAt(Instant.now());
                tenantCredentialRepository.save(c);
            });
        }
    }

    // ── Add-ons ──

    @Transactional
    public AddonResponse addAddon(UUID tenantId, UUID serviceId, UUID addedBy) {
        var service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Servei no trobat"));
        if (!service.getIsAddon()) {
            throw new IllegalArgumentException("Aquest servei no és un add-on");
        }

        if (tenantServiceRepository.findByTenantIdAndServiceId(tenantId, serviceId).isPresent()) {
            throw new IllegalArgumentException("Aquest add-on ja està assignat al tenant");
        }

        var approvalRequired = service.getSalePrice().compareTo(BigDecimal.ZERO) > 0;

        // Crear TenantService
        tenantServiceRepository.save(TenantService.builder()
                .tenantId(tenantId)
                .serviceId(serviceId)
                .status(ServiceStatus.PENDING)
                .build());

        // Crear TenantServiceAddon
        var addon = TenantServiceAddon.builder()
                .tenantId(tenantId)
                .serviceId(serviceId)
                .addedBy(addedBy)
                .approvalRequired(approvalRequired)
                .approvalStatus(approvalRequired ? AddonApprovalStatus.PENDING : AddonApprovalStatus.APPROVED)
                .build();
        addonRepository.save(addon);

        return AddonResponse.builder()
                .approvalRequired(approvalRequired)
                .salePrice(service.getSalePrice())
                .status(approvalRequired ? "PENDING_CLIENT_APPROVAL" : "APPROVED")
                .build();
    }

    @Transactional
    public void approveAddon(UUID tenantId, UUID serviceId) {
        var addon = addonRepository.findByTenantIdAndServiceId(tenantId, serviceId)
                .orElseThrow(() -> new IllegalArgumentException("Add-on no trobat"));
        addon.setApprovalStatus(AddonApprovalStatus.APPROVED);
        addonRepository.save(addon);
    }

    @Transactional
    public void removeAddon(UUID tenantId, UUID serviceId) {
        // Eliminar TenantServiceAddon
        addonRepository.findByTenantIdAndServiceId(tenantId, serviceId)
                .ifPresent(addonRepository::delete);
        // Eliminar TenantService
        tenantServiceRepository.findByTenantIdAndServiceId(tenantId, serviceId)
                .ifPresent(tenantServiceRepository::delete);
    }

    // ── Setup (Estat de configuració) ──

    public SetupResponse getSetup(UUID tenantId, boolean includeClearValue) {
        var tenantProfiles = tenantProfileRepository.findByTenantId(tenantId).stream()
                .filter(TenantProfile::getIsActive)
                .toList();

        var profileSetups = tenantProfiles.stream().map(tp -> {
            var profile = profileRepository.findById(tp.getProfileId()).orElseThrow();
            var phases = phaseRepository.findByProfileIdOrderBySortOrder(tp.getProfileId());

            var phaseSetups = phases.stream().map(phase -> {
                var tPhase = tenantPhaseRepository.findByTenantIdAndPhaseId(tenantId, phase.getId());
                var services = serviceRepository.findByPhaseIdOrderBySortOrder(phase.getId());
                var serviceSetups = services.stream().map(svc -> {
                    var ts = tenantServiceRepository.findByTenantIdAndServiceId(tenantId, svc.getId());
                    var fields = fieldRepository.findByServiceIdOrderBySortOrder(svc.getId());
                    var fieldSetups = fields.stream().map(f -> {
                        var cred = tenantCredentialRepository.findByTenantIdAndFieldId(tenantId, f.getId());
                        return SetupResponse.FieldSetup.builder()
                                .id(f.getId())
                                .key(f.getFieldKey())
                                .label(f.getLabel())
                                .isSet(cred.map(TenantCredential::getIsSet).orElse(false))
                                .maskedValue(cred.map(c -> encryption.mask(encryption.decrypt(c.getEncryptedValue()))).orElse(null))
                                .clearValue(includeClearValue ? cred.map(c -> encryption.decrypt(c.getEncryptedValue())).orElse(null) : null)
                                .build();
                    }).toList();
                    return SetupResponse.ServiceSetup.builder()
                            .service(SetupResponse.ServiceRef.builder()
                                    .id(svc.getId()).name(svc.getName()).type(svc.getType().name()).build())
                            .status(ts.map(TenantService::getStatus).map(Enum::name).orElse("PENDING"))
                            .fields(fieldSetups)
                            .build();
                }).toList();

                return SetupResponse.PhaseSetup.builder()
                        .phase(SetupResponse.PhaseSetup.PhaseRef.builder()
                                .id(phase.getId()).name(phase.getName()).sortOrder(phase.getSortOrder()).build())
                        .approvalStatus(tPhase.map(TenantPhase::getApprovalStatus).map(Enum::name).orElse("PENDING_APPROVAL"))
                        .paymentStatus(tPhase.map(TenantPhase::getPaymentStatus).map(Enum::name).orElse(null))
                        .implementationStatus(tPhase.map(TenantPhase::getImplementationStatus).map(Enum::name).orElse("NOT_STARTED"))
                        .services(serviceSetups)
                        .build();
            }).toList();

            return SetupResponse.ProfileSetup.builder()
                    .profile(SetupResponse.ProfileSetup.ProfileRef.builder()
                            .id(profile.getId()).name(profile.getName()).slug(profile.getSlug()).build())
                    .phases(phaseSetups)
                    .build();
        }).toList();

        var addons = serviceRepository.findByIsAddonTrue().stream()
                .filter(svc -> tenantServiceRepository.findByTenantIdAndServiceId(tenantId, svc.getId()).isPresent())
                .map(svc -> {
                    var addon = addonRepository.findByTenantIdAndServiceId(tenantId, svc.getId());
                    return SetupResponse.AddonSetup.builder()
                            .service(SetupResponse.ServiceRef.builder()
                                    .id(svc.getId()).name(svc.getName()).type(svc.getType().name()).build())
                            .approvalRequired(addon.map(TenantServiceAddon::getApprovalRequired).orElse(false))
                            .approvalStatus(addon.map(TenantServiceAddon::getApprovalStatus).map(Enum::name).orElse(null))
                            .build();
                }).toList();

        return SetupResponse.builder().profiles(profileSetups).addons(addons).build();
    }

    // ── Monitorització ──

    public MonitoringResponse.InvoiceMonitoring getInvoiceMonitoring(UUID tenantId) {
        var phases = tenantPhaseRepository.findByTenantIdOrderByCreatedAt(tenantId);
        var phaseInvoices = phases.stream().map(tp -> {
            var phase = phaseRepository.findById(tp.getPhaseId()).orElse(null);
            return MonitoringResponse.InvoiceMonitoring.PhaseInvoice.builder()
                    .phase(MonitoringResponse.InvoiceMonitoring.PhaseInvoice.PhaseRef.builder()
                            .name(phase != null ? phase.getName() : "Desconegut")
                            .sortOrder(phase != null ? phase.getSortOrder() : 0)
                            .build())
                    .invoiceId(tp.getInvoiceId())
                    .amount(tp.getInvoiceAmount())
                    .invoiceStatus(tp.getInvoiceStatus() != null ? tp.getInvoiceStatus().name() : null)
                    .paidAt(tp.getPaidAt())
                    .build();
        }).toList();

        var pending = (int) phaseInvoices.stream()
                .filter(i -> "PENDING".equals(i.getInvoiceStatus())).count();
        var overdue = (int) phaseInvoices.stream()
                .filter(i -> "OVERDUE".equals(i.getInvoiceStatus())).count();
        var totalPaid = phaseInvoices.stream()
                .filter(i -> "PAID".equals(i.getInvoiceStatus()))
                .map(MonitoringResponse.InvoiceMonitoring.PhaseInvoice::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return MonitoringResponse.InvoiceMonitoring.builder()
                .phases(phaseInvoices)
                .pendingInvoices(pending)
                .overdueInvoices(overdue)
                .totalPaid(totalPaid)
                .build();
    }

    public MonitoringResponse.PaymentMonitoring getPaymentMonitoring(UUID tenantId) {
        var phases = tenantPhaseRepository.findByTenantIdOrderByCreatedAt(tenantId);
        var phasePayments = phases.stream().filter(tp -> tp.getPaymentStatus() != null).map(tp -> {
            var phase = phaseRepository.findById(tp.getPhaseId()).orElse(null);
            return MonitoringResponse.PaymentMonitoring.PhasePayment.builder()
                    .phase(MonitoringResponse.PaymentMonitoring.PhasePayment.PhaseRef.builder()
                            .name(phase != null ? phase.getName() : "Desconegut").build())
                    .amount(tp.getInvoiceAmount())
                    .paymentStatus(tp.getPaymentStatus().name())
                    .paidAt(tp.getPaidAt())
                    .paymentMethod(tp.getPaymentStatus() == PaymentStatus.PAID ? "card" : null)
                    .build();
        }).toList();

        var pending = (int) phasePayments.stream()
                .filter(p -> "PENDING".equals(p.getPaymentStatus())).count();
        var failed = (int) phasePayments.stream()
                .filter(p -> "FAILED".equals(p.getPaymentStatus())).count();

        return MonitoringResponse.PaymentMonitoring.builder()
                .phases(phasePayments)
                .pendingPayments(pending)
                .failedPayments(failed)
                .build();
    }

    public MonitoringResponse.PhaseMonitoring getPhaseMonitoring(UUID tenantId) {
        var phases = tenantPhaseRepository.findByTenantIdOrderByCreatedAt(tenantId);
        var pendingApproval = phases.stream().filter(tp -> tp.getApprovalStatus() == ApprovalStatus.PENDING_APPROVAL).count();
        var inProgress = phases.stream().filter(tp -> tp.getImplementationStatus() == ImplementationStatus.IN_PROGRESS).count();
        var completed = phases.stream().filter(tp -> tp.getImplementationStatus() == ImplementationStatus.COMPLETED).count();
        var rejected = phases.stream().filter(tp -> tp.getApprovalStatus() == ApprovalStatus.REJECTED).count();

        var phaseStatuses = phases.stream().map(tp -> {
            var phase = phaseRepository.findById(tp.getPhaseId()).orElse(null);
            var services = tenantServiceRepository.findByTenantIdAndPhaseId(tenantId, tp.getPhaseId());
            var total = services.size();
            var done = (int) services.stream().filter(s -> s.getStatus() == ServiceStatus.VERIFIED).count();
            var progress = total > 0 ? (done * 100 / total) + "%" : "0%";
            return MonitoringResponse.PhaseMonitoring.PhaseStatus.builder()
                    .name(phase != null ? phase.getName() : "Desconegut")
                    .approvalStatus(tp.getApprovalStatus().name())
                    .implementationStatus(tp.getImplementationStatus().name())
                    .progress(progress)
                    .build();
        }).toList();

        return MonitoringResponse.PhaseMonitoring.builder()
                .totalPhases(phases.size())
                .pendingApproval((int) pendingApproval)
                .inProgress((int) inProgress)
                .completed((int) completed)
                .rejected((int) rejected)
                .phases(phaseStatuses)
                .build();
    }
}
