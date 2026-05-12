package com.amg.digitalitzacio.billing.api.dto;

import com.amg.digitalitzacio.billing.domain.BudgetStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BudgetResponse {
    private UUID id;
    private UUID tenantId;
    private String budgetNumber;
    private UUID profileId;
    private Integer version;
    private BudgetStatus status;
    private BigDecimal subtotal;
    private BigDecimal discountTotal;
    private BigDecimal total;
    private LocalDate validUntil;
    private String notes;
    private String clientNotes;
    private Instant sentAt;
    private Instant acceptedAt;
    private Instant rejectedAt;
    private String rejectedReason;
    private Instant createdAt;
    private Instant updatedAt;
    private List<BudgetLineResponse> lines;

    @Data
    @Builder
    public static class BudgetLineResponse {
        private UUID id;
        private UUID phaseId;
        private UUID serviceId;
        private String serviceName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal total;
        private Integer sortOrder;
    }

    @Data
    @Builder
    public static class BudgetSummary {
        private UUID id;
        private String budgetNumber;
        private BudgetStatus status;
        private BigDecimal total;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
