package com.amg.digitalitzacio.vault.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Interfície per a cobraments (Stripe).
 * Implementada al Mòdul 09 Payments.
 */
public interface PaymentService {

    /**
     * Cobra un import per a una factura d'una fase.
     * @return result amb paymentStatus i paymentId
     */
    PaymentResult charge(UUID tenantId, String invoiceId, BigDecimal amount);

    /**
     * Reemborsa un pagament.
     */
    PaymentResult refund(String invoiceId);

    record PaymentResult(boolean success, String transactionId, String errorMessage) {}
}
