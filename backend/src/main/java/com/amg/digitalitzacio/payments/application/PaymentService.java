package com.amg.digitalitzacio.payments.application;

import com.amg.digitalitzacio.gocardless.api.dto.ProviderSummaryResponse;
import com.amg.digitalitzacio.payments.api.dto.*;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface PaymentService {
    StripeConfigResponse configure(StripeConfigRequest request);
    StripeConfigResponse getConfig(UUID tenantId);
    PaymentResponse createCheckoutSession(UUID budgetId);
    PaymentResponse getPayment(UUID paymentId, UUID currentTenantId);
    String getReceiptUrl(UUID paymentId, UUID currentTenantId);
    PaymentResponse refundPayment(UUID paymentId);
    Page<PaymentResponse> listPayments(UUID tenantId, String status, int page, int size);
    PaymentDashboardResponse getDashboard(UUID tenantId);
    WebhookResponse processWebhook(WebhookRequest request);
    ProviderSummaryResponse getProviders(UUID tenantId);
}
