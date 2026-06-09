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

    // Pagament automàtic: guardar mètode i cobrar
    String createOrGetCustomer(UUID tenantId, String email);
    String createSetupSession(String customerId, String successUrl, String cancelUrl);
    SavedPaymentMethod getPaymentMethodFromSession(String sessionId);
    String chargeCustomer(String customerId, String paymentMethodId, long amountCents,
                          String currency, String description);
    void detachPaymentMethod(String paymentMethodId);

    record SavedPaymentMethod(String paymentMethodId, String brand, String lastFour,
                               Integer expMonth, Integer expYear) {}
}
