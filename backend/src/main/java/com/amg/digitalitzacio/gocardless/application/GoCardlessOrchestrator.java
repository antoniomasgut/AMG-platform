package com.amg.digitalitzacio.gocardless.application;

import com.amg.digitalitzacio.finops.domain.InvoiceStatus;
import com.amg.digitalitzacio.finops.domain.MonthlyInvoiceRepository;
import com.amg.digitalitzacio.gocardless.api.dto.*;
import com.amg.digitalitzacio.gocardless.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoCardlessOrchestrator implements GoCardlessService {

    private final GoCardlessClient goCardlessClient;
    private final GoCardlessConfigRepository configRepository;
    private final GoCardlessMandateRepository mandateRepository;
    private final GoCardlessPaymentRepository paymentRepository;
    private final MonthlyInvoiceRepository monthlyInvoiceRepository;
    private final com.amg.digitalitzacio.billing.application.PostAcceptanceService postAcceptanceService;

    @Override
    @Transactional
    public GoCardlessConfigResponse configure(GoCardlessConfigRequest request) {
        var existing = configRepository.findByTenantId(request.tenantId());
        var config = existing.orElseGet(() -> GoCardlessConfig.builder()
                .tenantId(request.tenantId())
                .build());
        if (request.apiKeyRef() != null) config.setApiKeyRef(request.apiKeyRef());
        if (request.creditorId() != null) config.setCreditorId(request.creditorId());
        if (request.webhookSecret() != null) config.setWebhookSecret(request.webhookSecret());
        if (request.environment() != null) {
            config.setEnvironment(GoCardlessEnvironment.valueOf(request.environment().toUpperCase()));
        }
        config.setIsActive(true);
        config = configRepository.save(config);
        return toConfigResponse(config);
    }

    @Override
    @Transactional(readOnly = true)
    public GoCardlessConfigResponse getConfig(UUID tenantId) {
        var config = configRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("GoCardless not configured for tenant: " + tenantId));
        return toConfigResponse(config);
    }

    @Override
    @Transactional
    public InitiateMandateResponse initiateMandate(UUID tenantId, String successReturnUrl) {
        var mandate = mandateRepository.findByTenantId(tenantId)
                .orElseGet(() -> GoCardlessMandate.builder().tenantId(tenantId).build());

        var redirectUrl = successReturnUrl != null ? successReturnUrl
                : "https://portal.amg.cat/api/v1/gocardless/tenants/" + tenantId + "/mandate/complete";

        var result = goCardlessClient.createRedirectFlow(
                tenantId.toString(), redirectUrl, "Domiciliació SEPA AMG Digitalització");

        mandate.setGcRedirectFlowId(result.flowId());
        mandate.setStatus(GoCardlessMandateStatus.PENDING_SUBMISSION);
        mandateRepository.save(mandate);

        return new InitiateMandateResponse(result.flowId(), result.redirectUrl());
    }

    @Override
    @Transactional
    public GoCardlessMandateResponse completeMandate(UUID tenantId, String redirectFlowId) {
        var mandate = mandateRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No pending mandate for tenant: " + tenantId));

        var result = goCardlessClient.completeRedirectFlow(tenantId.toString(), redirectFlowId);

        mandate.setGcMandateId(result.mandateId());
        mandate.setGcRedirectFlowId(redirectFlowId);
        mandate.setAccountHolderName(result.accountHolderName());
        mandate.setBankName(result.bankName());
        mandate.setLastFourDigits(result.lastFourDigits());
        mandate.setStatus(GoCardlessMandateStatus.ACTIVE);
        mandate = mandateRepository.save(mandate);

        return toMandateResponse(mandate);
    }

    @Override
    @Transactional(readOnly = true)
    public GoCardlessMandateResponse getMandate(UUID tenantId) {
        var mandate = mandateRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No mandate for tenant: " + tenantId));
        return toMandateResponse(mandate);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GoCardlessMandateResponse> findMandate(UUID tenantId) {
        return mandateRepository.findByTenantId(tenantId).map(this::toMandateResponse);
    }

    @Override
    @Transactional
    public void cancelMandate(UUID tenantId) {
        var mandate = mandateRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No mandate for tenant: " + tenantId));
        if (mandate.getGcMandateId() != null) {
            goCardlessClient.cancelMandate(tenantId.toString(), mandate.getGcMandateId());
        }
        mandate.setStatus(GoCardlessMandateStatus.CANCELLED);
        mandateRepository.save(mandate);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GoCardlessPaymentResponse> listPayments(UUID tenantId, int page, int size) {
        return paymentRepository.findByTenantId(tenantId, PageRequest.of(page, size))
                .map(this::toPaymentResponse);
    }

    @Override
    @Transactional
    public GoCardlessWebhookResponse processWebhook(GoCardlessWebhookRequest request) {
        if (request.events() == null) return new GoCardlessWebhookResponse(true);

        for (var event : request.events()) {
            try {
                handleEvent(event);
            } catch (Exception e) {
                log.warn("GoCardless webhook: error handling event {} {}: {}",
                        event.resourceType(), event.action(), e.getMessage());
            }
        }
        return new GoCardlessWebhookResponse(true);
    }

    private void handleEvent(GoCardlessWebhookRequest.GoCardlessEvent event) {
        if (event.resourceType() == null || event.action() == null) return;

        switch (event.resourceType().toLowerCase()) {
            case "payments" -> handlePaymentEvent(event);
            case "mandates" -> handleMandateEvent(event);
            default -> log.debug("GoCardless webhook: unhandled resource_type {}", event.resourceType());
        }
    }

    private void handlePaymentEvent(GoCardlessWebhookRequest.GoCardlessEvent event) {
        var gcPaymentId = event.links() != null ? event.links().get("payment") : null;
        if (gcPaymentId == null) return;

        paymentRepository.findByGcPaymentId(gcPaymentId).ifPresent(payment -> {
            switch (event.action().toLowerCase()) {
                case "paid_out" -> {
                    payment.setStatus(GoCardlessPaymentStatus.PAID_OUT);
                    payment.setPaidOutAt(Instant.now());
                    paymentRepository.save(payment);
                    monthlyInvoiceRepository.findById(payment.getMonthlyInvoiceId()).ifPresent(inv -> {
                        inv.setStatus(InvoiceStatus.PAID);
                        inv.setSepaCollected(true);
                        monthlyInvoiceRepository.save(inv);
                    });
                    final var paidPayment = payment;
                    org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                            new org.springframework.transaction.support.TransactionSynchronizationAdapter() {
                                @Override public void afterCommit() {
                                    postAcceptanceService.onPaymentReceived(
                                            paidPayment.getTenantId(),
                                            paidPayment.getAmount(),
                                            paidPayment.getGcPaymentId(),
                                            "GOCARDLESS");
                                }
                            });
                    log.info("GoCardless: payment {} paid out", gcPaymentId);
                }
                case "failed" -> {
                    payment.setStatus(GoCardlessPaymentStatus.FAILED);
                    payment.setFailureReason("GoCardless payment failed");
                    paymentRepository.save(payment);
                    log.warn("GoCardless: payment {} failed for tenant {}", gcPaymentId, payment.getTenantId());
                }
                case "cancelled" -> {
                    payment.setStatus(GoCardlessPaymentStatus.CANCELLED);
                    paymentRepository.save(payment);
                }
                default -> {}
            }
        });
    }

    private void handleMandateEvent(GoCardlessWebhookRequest.GoCardlessEvent event) {
        var gcMandateId = event.links() != null ? event.links().get("mandate") : null;
        if (gcMandateId == null) return;

        mandateRepository.findByGcRedirectFlowId(gcMandateId).or(() ->
                mandateRepository.findAll().stream()
                        .filter(m -> gcMandateId.equals(m.getGcMandateId()))
                        .findFirst()
        ).ifPresent(mandate -> {
            switch (event.action().toLowerCase()) {
                case "cancelled" -> {
                    mandate.setStatus(GoCardlessMandateStatus.CANCELLED);
                    mandateRepository.save(mandate);
                    log.warn("GoCardless: mandate {} cancelled for tenant {}", gcMandateId, mandate.getTenantId());
                }
                case "expired" -> {
                    mandate.setStatus(GoCardlessMandateStatus.EXPIRED);
                    mandateRepository.save(mandate);
                }
                default -> {}
            }
        });
    }

    @Override
    @Transactional
    public void chargeMonthlyInvoices(String period) {
        var invoices = monthlyInvoiceRepository.findByPeriod(period);

        for (var invoice : invoices) {
            if (invoice.getStatus() == InvoiceStatus.PAID) continue;
            if (paymentRepository.findByMonthlyInvoiceId(invoice.getId()).isPresent()) continue;

            var mandateOpt = mandateRepository.findByTenantIdAndStatus(
                    invoice.getTenantId(), GoCardlessMandateStatus.ACTIVE);

            mandateOpt.ifPresent(mandate -> {
                try {
                    var chargeDate = LocalDate.now().withDayOfMonth(5);
                    var gcPaymentId = goCardlessClient.createPayment(
                            mandate.getTenantId().toString(),
                            mandate.getGcMandateId(),
                            invoice.getAmount(),
                            chargeDate,
                            "Quota mensual AMG – " + period);

                    var payment = GoCardlessPayment.builder()
                            .tenantId(invoice.getTenantId())
                            .monthlyInvoiceId(invoice.getId())
                            .gcPaymentId(gcPaymentId)
                            .amount(invoice.getAmount())
                            .chargeDate(chargeDate)
                            .status(GoCardlessPaymentStatus.PENDING_SUBMISSION)
                            .build();
                    paymentRepository.save(payment);
                    log.info("GoCardless: queued payment {} for tenant {} period {}",
                            gcPaymentId, invoice.getTenantId(), period);
                } catch (Exception e) {
                    log.error("GoCardless: failed to charge tenant {} for period {}: {}",
                            invoice.getTenantId(), period, e.getMessage());
                }
            });
        }
    }

    private GoCardlessConfigResponse toConfigResponse(GoCardlessConfig config) {
        return new GoCardlessConfigResponse(
                config.getId(), config.getTenantId(),
                config.getEnvironment().name(),
                config.getCreditorId(),
                Boolean.TRUE.equals(config.getIsActive()),
                config.getCreatedAt());
    }

    private GoCardlessMandateResponse toMandateResponse(GoCardlessMandate m) {
        return new GoCardlessMandateResponse(
                m.getId(), m.getTenantId(), m.getGcMandateId(),
                m.getStatus().name(), m.getAccountHolderName(),
                m.getBankName(), m.getLastFourDigits(), m.getCreatedAt());
    }

    private GoCardlessPaymentResponse toPaymentResponse(GoCardlessPayment p) {
        return new GoCardlessPaymentResponse(
                p.getId(), p.getTenantId(), p.getMonthlyInvoiceId(),
                p.getGcPaymentId(), p.getAmount(), p.getStatus().name(),
                p.getChargeDate(), p.getPaidOutAt(), p.getFailureReason(), p.getCreatedAt());
    }
}
