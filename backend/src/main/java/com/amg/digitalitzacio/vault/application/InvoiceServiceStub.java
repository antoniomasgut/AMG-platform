package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.vault.domain.InvoiceStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stub per a InvoiceService — fins que s'implementi el Mòdul 08.
 * Les implementacions reals aniran a FinOps (Holded + Verifactu).
 */
@Service
public class InvoiceServiceStub implements InvoiceService {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public String createInvoice(UUID tenantId, UUID phaseId, BigDecimal amount) {
        var invoiceId = "INV-STUB-" + counter.incrementAndGet();
        return invoiceId;
    }

    @Override
    public void updateInvoiceStatus(String invoiceId, InvoiceStatus status) {
        // no-op
    }
}
