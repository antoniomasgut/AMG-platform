package com.amg.digitalitzacio.finops.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.finops.provider", havingValue = "mock", matchIfMissing = true)
public class FinOpsMockClient implements FinOpsClient {

    @Override
    public String createContact(String tenantName, String email, String phone, String nif) {
        return "mock-contact-" + UUID.randomUUID();
    }

    @Override
    public void updateContact(String holdedContactId, String tenantName, String email, String phone) {
    }

    @Override
    public boolean contactExists(String holdedContactId) {
        return holdedContactId != null && holdedContactId.startsWith("mock-contact-");
    }

    @Override
    public String createInvoice(String holdedContactId, BigDecimal amount, BigDecimal taxAmount,
                                String description, String dueDate) {
        return "mock-inv-" + UUID.randomUUID();
    }

    @Override
    public String getInvoiceStatus(String holdedInvoiceId) {
        return "paid";
    }

    @Override
    public String getInvoicePdfUrl(String holdedInvoiceId) {
        return "https://mock.holded.com/invoices/" + holdedInvoiceId + "/pdf";
    }

    @Override
    public void cancelInvoice(String holdedInvoiceId) {
    }

    @Override
    public boolean isConnected() {
        return true;
    }
}
