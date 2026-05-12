package com.amg.digitalitzacio.billing.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class DashboardResponse {
    private long totalBudgets;
    private long draftCount;
    private long sentCount;
    private long acceptedCount;
    private long rejectedCount;
    private long expiredCount;
    private long cancelledCount;
    private BigDecimal totalAcceptedAmount;
    private BigDecimal totalPendingAmount;

    @Data
    @Builder
    public static class PhaseSummary {
        private UUID phaseId;
        private String phaseName;
        private BigDecimal amount;
    }
}
