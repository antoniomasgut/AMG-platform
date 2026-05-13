package com.amg.digitalitzacio.finops.application;

import java.math.BigDecimal;

public interface FinOpsClient {
    String createContact(String tenantName, String email, String phone, String nif);
    void updateContact(String holdedContactId, String tenantName, String email, String phone);
    boolean contactExists(String holdedContactId);

    String createInvoice(String holdedContactId, BigDecimal amount, BigDecimal taxAmount,
                         String description, String dueDate);
    String getInvoiceStatus(String holdedInvoiceId);
    String getInvoicePdfUrl(String holdedInvoiceId);
    void cancelInvoice(String holdedInvoiceId);

    boolean isConnected();
}
