package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.vault.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.util.UUID;

public interface InvoiceService {
    UUID createInvoice(UUID tenantId, UUID phaseId, BigDecimal amount);
    void updateInvoiceStatus(UUID invoiceId, InvoiceStatus status);
}
