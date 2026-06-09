package com.amg.digitalitzacio.payments.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.payments.provider", havingValue = "mock", matchIfMissing = true)
public class StripeMockClient implements StripeClient {

    @Override
    public String createCheckoutSession(UUID budgetId, BigDecimal amount, String currency,
                                         String successUrl, String cancelUrl) {
        return "https://mock.stripe.com/checkout/" + UUID.randomUUID();
    }

    @Override
    public String checkPaymentStatus(String stripeSessionId) {
        return "complete";
    }

    @Override
    public String getReceiptUrl(String paymentIntentId) {
        return "https://mock.stripe.com/receipt/" + paymentIntentId;
    }

    @Override
    public void refundPayment(String paymentIntentId) {
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public String createOrGetCustomer(UUID tenantId, String email) {
        return "cus_mock_" + tenantId.toString().substring(0, 8);
    }

    @Override
    public String createSetupSession(String customerId, String successUrl, String cancelUrl) {
        return "https://mock.stripe.com/setup/" + UUID.randomUUID();
    }

    @Override
    public SavedPaymentMethod getPaymentMethodFromSession(String sessionId) {
        return new SavedPaymentMethod("pm_mock_" + UUID.randomUUID(), "visa", "4242", 12, 2027);
    }

    @Override
    public String chargeCustomer(String customerId, String paymentMethodId, long amountCents,
                                  String currency, String description) {
        return "pi_mock_" + UUID.randomUUID();
    }

    @Override
    public void detachPaymentMethod(String paymentMethodId) {}
}
