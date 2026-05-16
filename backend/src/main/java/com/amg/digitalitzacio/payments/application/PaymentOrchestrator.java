package com.amg.digitalitzacio.payments.application;

import com.amg.digitalitzacio.billing.domain.BudgetRepository;
import com.amg.digitalitzacio.billing.domain.BudgetStatus;
import com.amg.digitalitzacio.finops.application.FinOpsService;
import com.amg.digitalitzacio.gocardless.api.dto.ProviderSummaryResponse;
import com.amg.digitalitzacio.gocardless.application.GoCardlessService;
import com.amg.digitalitzacio.payments.api.dto.*;
import com.amg.digitalitzacio.payments.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentOrchestrator implements PaymentService {

    private final StripeClient stripeClient;
    private final StripeConfigRepository stripeConfigRepository;
    private final PaymentRepository paymentRepository;
    private final BudgetRepository budgetRepository;
    private final FinOpsService finOpsService;
    private final GoCardlessService goCardlessService;

    @Value("${app.payments.success-url:https://portal.amg.cat/payments/success}")
    private String successUrl;

    @Value("${app.payments.cancel-url:https://portal.amg.cat/payments/cancel}")
    private String cancelUrl;

    @Override
    @Transactional
    public StripeConfigResponse configure(StripeConfigRequest request) {
        var existing = stripeConfigRepository.findByTenantId(request.tenantId());
        var config = existing.orElseGet(() -> StripeConfig.builder()
                .tenantId(request.tenantId())
                .build());
        config.setApiKeyRef(request.apiKeyRef());
        config.setWebhookSecret(request.webhookSecret());
        config.setIsActive(true);
        config = stripeConfigRepository.save(config);
        return toConfigResponse(config);
    }

    @Override
    @Transactional(readOnly = true)
    public StripeConfigResponse getConfig(UUID tenantId) {
        var config = findConfig(tenantId);
        return toConfigResponse(config);
    }

    @Override
    @Transactional
    public PaymentResponse createCheckoutSession(UUID budgetId) {
        var budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + budgetId));

        if (budget.getStatus() != BudgetStatus.ACCEPTED) {
            throw new IllegalArgumentException("Budget must be ACCEPTED to create a checkout. Current status: " + budget.getStatus());
        }

        var payment = Payment.builder()
                .tenantId(budget.getTenantId())
                .budgetId(budgetId)
                .amount(budget.getTotal())
                .currency("EUR")
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        try {
            var checkoutUrl = stripeClient.createCheckoutSession(
                    budgetId, payment.getAmount(), payment.getCurrency(),
                    successUrl, cancelUrl);
            payment.setCheckoutUrl(checkoutUrl);
            payment.setStripeSessionId("sess_" + UUID.randomUUID());
            payment = paymentRepository.save(payment);
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorMessage(e.getMessage());
            payment = paymentRepository.save(payment);
        }

        return toPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId, UUID currentTenantId) {
        var payment = currentTenantId != null
                ? paymentRepository.findByIdAndTenantId(paymentId, currentTenantId)
                : paymentRepository.findById(paymentId);
        return toPaymentResponse(payment.orElseThrow(
                () -> new ResourceNotFoundException("Payment not found: " + paymentId)));
    }

    @Override
    @Transactional(readOnly = true)
    public String getReceiptUrl(UUID paymentId, UUID currentTenantId) {
        var pay = currentTenantId != null
                ? paymentRepository.findByIdAndTenantId(paymentId, currentTenantId)
                : paymentRepository.findById(paymentId);
        var payment = pay.orElseThrow(
                () -> new ResourceNotFoundException("Payment not found: " + paymentId));
        if (payment.getStripePaymentIntentId() != null) {
            return stripeClient.getReceiptUrl(payment.getStripePaymentIntentId());
        }
        return null;
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(UUID paymentId) {
        var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new IllegalArgumentException("Only COMPLETED payments can be refunded");
        }

        if (payment.getStripePaymentIntentId() != null) {
            stripeClient.refundPayment(payment.getStripePaymentIntentId());
        }
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(Instant.now());
        payment = paymentRepository.save(payment);
        return toPaymentResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> listPayments(UUID tenantId, String status, int page, int size) {
        var pageable = PageRequest.of(page, size);
        Page<Payment> result;
        if (status != null && !status.isBlank()) {
            result = paymentRepository.findByTenantIdAndStatus(
                    tenantId, PaymentStatus.valueOf(status.toUpperCase()), pageable);
        } else {
            result = paymentRepository.findByTenantId(tenantId, pageable);
        }
        return result.map(this::toPaymentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDashboardResponse getDashboard(UUID tenantId) {
        var totalPayments = paymentRepository.countByTenantIdAndStatus(tenantId, PaymentStatus.COMPLETED);
        var pendingCount = paymentRepository.countByTenantIdAndStatus(tenantId, PaymentStatus.PENDING);
        var failedCount = paymentRepository.countByTenantIdAndStatus(tenantId, PaymentStatus.FAILED);
        var refundedCount = paymentRepository.countByTenantIdAndStatus(tenantId, PaymentStatus.REFUNDED);

        return new PaymentDashboardResponse(
                totalPayments + pendingCount + failedCount + refundedCount,
                totalPayments, pendingCount, failedCount,
                BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public WebhookResponse processWebhook(WebhookRequest request) {
        if (request.stripeSessionId() != null && request.event() != null) {
            var paymentOpt = paymentRepository.findByStripeSessionId(request.stripeSessionId());
            paymentOpt.ifPresent(payment -> {
                switch (request.event().toLowerCase()) {
                    case "checkout.session.completed":
                    case "payment_intent.succeeded":
                        payment.setStatus(PaymentStatus.COMPLETED);
                        payment.setPaidAt(Instant.now());
                        if (request.paymentIntentId() != null) {
                            payment.setStripePaymentIntentId(request.paymentIntentId());
                        }
                        paymentRepository.save(payment);
                        try {
                            finOpsService.createInvoiceFromBudget(payment.getBudgetId());
                        } catch (Exception e) {
                            payment.setErrorMessage("Invoice creation failed: " + e.getMessage());
                            paymentRepository.save(payment);
                        }
                        break;
                    case "checkout.session.expired":
                    case "payment_intent.payment_failed":
                        payment.setStatus(PaymentStatus.FAILED);
                        paymentRepository.save(payment);
                        break;
                }
            });
        }
        return new WebhookResponse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public ProviderSummaryResponse getProviders(UUID tenantId) {
        var stripeConfig = stripeConfigRepository.findByTenantId(tenantId).orElse(null);
        boolean stripeConfigured = stripeConfig != null;
        boolean stripeActive = stripeConfigured && Boolean.TRUE.equals(stripeConfig.getIsActive());
        String setupProvider = stripeActive ? "STRIPE" : "TRANSFER";

        boolean sepaMandateActive = false;
        try {
            finOpsService.getSepaMandate(tenantId);
            sepaMandateActive = true;
        } catch (Exception ignored) {}

        boolean gcMandateActive = false;
        String gcMandateStatus = null;
        try {
            var gcMandate = goCardlessService.findMandate(tenantId);
            if (gcMandate.isPresent()) {
                gcMandateStatus = gcMandate.get().status();
                gcMandateActive = "ACTIVE".equals(gcMandateStatus);
            }
        } catch (Exception ignored) {}

        String recurringProvider = gcMandateActive ? "GOCARDLESS"
                : sepaMandateActive ? "SEPA_MANUAL"
                : "TRANSFER";

        return new ProviderSummaryResponse(
                tenantId,
                new ProviderSummaryResponse.SetupProviders(setupProvider, stripeConfigured, stripeActive),
                new ProviderSummaryResponse.RecurringProviders(recurringProvider, sepaMandateActive, gcMandateActive, gcMandateStatus));
    }

    private StripeConfig findConfig(UUID tenantId) {
        return stripeConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Stripe not configured for tenant: " + tenantId));
    }

    private StripeConfigResponse toConfigResponse(StripeConfig config) {
        return new StripeConfigResponse(
                config.getId(), config.getTenantId(),
                Boolean.TRUE.equals(config.getIsActive()),
                config.getCreatedAt());
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(), payment.getTenantId(), payment.getBudgetId(),
                payment.getInvoiceId(), payment.getStripeSessionId(),
                payment.getStripePaymentIntentId(), payment.getAmount(),
                payment.getCurrency(), payment.getStatus().name(),
                payment.getCheckoutUrl(), payment.getPaidAt(),
                payment.getRefundedAt(), payment.getErrorMessage(),
                payment.getCreatedAt());
    }
}
