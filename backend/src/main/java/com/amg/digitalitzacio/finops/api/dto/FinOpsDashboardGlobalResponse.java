package com.amg.digitalitzacio.finops.api.dto;

import java.math.BigDecimal;

public record FinOpsDashboardGlobalResponse(
        BigDecimal totalInvoiced,
        BigDecimal totalPaid,
        BigDecimal totalPending,
        BigDecimal totalOverdue,
        int totalInvoices,
        int activeTenants,
        int configuredTenants
) {}
