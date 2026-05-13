package com.amg.digitalitzacio.payments.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.payments.provider", havingValue = "stripe")
@Slf4j
public class StripeRealClient implements StripeClient {

    @Override
    public String createCheckoutSession(UUID budgetId, BigDecimal amount, String currency,
                                         String successUrl, String cancelUrl) {
        throw new UnsupportedOperationException("Stripe real client not yet implemented");
    }

    @Override
    public String checkPaymentStatus(String stripeSessionId) {
        throw new UnsupportedOperationException("Stripe real client not yet implemented");
    }

    @Override
    public String getReceiptUrl(String paymentIntentId) {
        throw new UnsupportedOperationException("Stripe real client not yet implemented");
    }

    @Override
    public void refundPayment(String paymentIntentId) {
        throw new UnsupportedOperationException("Stripe real client not yet implemented");
    }

    @Override
    public boolean isConnected() {
        return false;
    }
}
