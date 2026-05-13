package com.amg.digitalitzacio.vault.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class PaymentServiceStub implements PaymentService {
    @Override
    public PaymentResult charge(UUID tenantId, UUID invoiceId, BigDecimal amount) {
        log.info("STUB: Payment charged — tenantId={}, invoiceId={}, amount={}", tenantId, invoiceId, amount);
        return new PaymentResult(true, "STUB-TXN-" + UUID.randomUUID(), null);
    }

    @Override
    public PaymentResult refund(UUID tenantId, UUID invoiceId) {
        log.info("STUB: Payment refunded — tenantId={}, invoiceId={}", tenantId, invoiceId);
        return new PaymentResult(true, "STUB-TXN-" + UUID.randomUUID(), null);
    }
}
