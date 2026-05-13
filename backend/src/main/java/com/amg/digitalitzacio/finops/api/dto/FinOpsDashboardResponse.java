package com.amg.digitalitzacio.finops.api.dto;

import java.math.BigDecimal;

public record FinOpsDashboardResponse(
        BigDecimal totalInvoiced,
        BigDecimal totalPaid,
        BigDecimal totalPending,
        BigDecimal totalOverdue,
        int invoiceCount,
        int paidCount,
        int pendingCount,
        int overdueCount
) {}
