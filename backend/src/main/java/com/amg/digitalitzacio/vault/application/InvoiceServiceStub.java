package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.vault.domain.InvoiceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class InvoiceServiceStub implements InvoiceService {
    @Override
    public UUID createInvoice(UUID tenantId, UUID phaseId, BigDecimal amount) {
        UUID invoiceId = UUID.randomUUID();
        log.info("STUB: Invoice created — id={}, tenantId={}, phaseId={}, amount={}", invoiceId, tenantId, phaseId, amount);
        return invoiceId;
    }

    @Override
    public void updateInvoiceStatus(UUID invoiceId, InvoiceStatus status) {
        log.info("STUB: Invoice status updated — id={}, status={}", invoiceId, status);
    }
}
