package com.amg.digitalitzacio.billing.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DashboardResponse(
    int pendingBudgets,
    BudgetSummary lastBudget,
    BigDecimal totalSpent,
    List<PhaseSummary> recentPhases
) {
    public record BudgetSummary(UUID id, String budgetNumber, BigDecimal total, String status, Instant sentAt) {}
    public record PhaseSummary(String name, String status, BigDecimal amount) {}
}
