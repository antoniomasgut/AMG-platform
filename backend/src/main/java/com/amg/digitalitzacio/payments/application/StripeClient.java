package com.amg.digitalitzacio.payments.application;

import java.math.BigDecimal;
import java.util.UUID;

public interface StripeClient {
    String createCheckoutSession(UUID budgetId, BigDecimal amount, String currency,
                                  String successUrl, String cancelUrl);
    String checkPaymentStatus(String stripeSessionId);
    String getReceiptUrl(String paymentIntentId);
    void refundPayment(String paymentIntentId);
    boolean isConnected();
}
