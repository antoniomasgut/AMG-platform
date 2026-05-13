package com.amg.digitalitzacio.finops.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConditionalOnProperty(name = "app.finops.provider", havingValue = "holded")
@Slf4j
public class FinOpsHoldedClient implements FinOpsClient {

    @Override
    public String createContact(String tenantName, String email, String phone, String nif) {
        throw new UnsupportedOperationException("Holded real client not yet implemented");
    }

    @Override
    public void updateContact(String holdedContactId, String tenantName, String email, String phone) {
        throw new UnsupportedOperationException("Holded real client not yet implemented");
    }

    @Override
    public boolean contactExists(String holdedContactId) {
        throw new UnsupportedOperationException("Holded real client not yet implemented");
    }

    @Override
    public String createInvoice(String holdedContactId, BigDecimal amount, BigDecimal taxAmount,
                                String description, String dueDate) {
        throw new UnsupportedOperationException("Holded real client not yet implemented");
    }

    @Override
    public String getInvoiceStatus(String holdedInvoiceId) {
        throw new UnsupportedOperationException("Holded real client not yet implemented");
    }

    @Override
    public String getInvoicePdfUrl(String holdedInvoiceId) {
        throw new UnsupportedOperationException("Holded real client not yet implemented");
    }

    @Override
    public void cancelInvoice(String holdedInvoiceId) {
        throw new UnsupportedOperationException("Holded real client not yet implemented");
    }

    @Override
    public boolean isConnected() {
        return false;
    }
}
