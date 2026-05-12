package com.amg.digitalitzacio.vault.application;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stub per a PaymentService — fins que s'implementi el Mòdul 09.
 * Les implementacions reals aniran a Payments (Stripe).
 */
@Service
public class PaymentServiceStub implements PaymentService {

    @Override
    public PaymentResult charge(UUID tenantId, String invoiceId, BigDecimal amount) {
        return new PaymentResult(true, "TXN-STUB-" + invoiceId, null);
    }

    @Override
    public PaymentResult refund(String invoiceId) {
        return new PaymentResult(true, "REF-STUB-" + invoiceId, null);
    }
}
