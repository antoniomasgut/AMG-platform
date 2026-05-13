package com.amg.digitalitzacio.payments.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID tenantId,
        UUID budgetId,
        UUID invoiceId,
        String stripeSessionId,
        String stripePaymentIntentId,
        BigDecimal amount,
        String currency,
        String status,
        String checkoutUrl,
        Instant paidAt,
        Instant refundedAt,
        String errorMessage,
        Instant createdAt
) {}
