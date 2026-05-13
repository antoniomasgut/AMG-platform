package com.amg.digitalitzacio.finops.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID tenantId,
        UUID budgetId,
        String holdedInvoiceId,
        String invoiceNumber,
        String status,
        BigDecimal amount,
        BigDecimal taxAmount,
        String currency,
        String verifactuStatus,
        String invoicePdfUrl,
        Instant dueDate,
        Instant paidAt,
        String errorMessage,
        Instant createdAt
) {}
