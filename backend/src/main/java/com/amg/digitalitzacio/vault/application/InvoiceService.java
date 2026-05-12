package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.vault.domain.InvoiceStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Interfície per a facturació (Verifactu).
 * Implementada al Mòdul 08 FinOps.
 */
public interface InvoiceService {

    /**
     * Crea una factura per una fase aprovada.
     * @return invoiceId (string) per referència futura
     */
    String createInvoice(UUID tenantId, UUID phaseId, BigDecimal amount);

    /**
     * Actualitza l'estat d'una factura.
     */
    void updateInvoiceStatus(String invoiceId, InvoiceStatus status);
}
