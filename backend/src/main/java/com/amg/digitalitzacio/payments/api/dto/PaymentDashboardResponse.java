package com.amg.digitalitzacio.payments.api.dto;

import java.math.BigDecimal;

public record PaymentDashboardResponse(
        long totalPayments,
        long completedCount,
        long pendingCount,
        long failedCount,
        BigDecimal totalAmount,
        BigDecimal totalCompleted
) {}
