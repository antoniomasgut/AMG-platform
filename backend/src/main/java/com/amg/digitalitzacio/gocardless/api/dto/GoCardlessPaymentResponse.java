package com.amg.digitalitzacio.gocardless.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GoCardlessPaymentResponse(
        UUID id,
        UUID tenantId,
        UUID monthlyInvoiceId,
        String gcPaymentId,
        BigDecimal amount,
        String status,
        LocalDate chargeDate,
        Instant paidOutAt,
        String failureReason,
        Instant createdAt
) {}
