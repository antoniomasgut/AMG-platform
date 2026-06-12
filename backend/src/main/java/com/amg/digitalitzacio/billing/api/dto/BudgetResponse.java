package com.amg.digitalitzacio.billing.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BudgetResponse(
    UUID id, String budgetNumber, String status,
    List<BudgetPhase> phases, List<BudgetAddon> addons,
    BigDecimal subtotal, BigDecimal discountTotal, BigDecimal total,
    BigDecimal monthlyTotal,
    Instant sentAt, Instant acceptedAt, Instant rejectedAt,
    String rejectionUrl, String acceptanceUrl, LocalDate validUntil, Instant createdAt,
    UUID profileId, List<UUID> phaseIds, String notes, String clientNotes,
    UUID tenantId, String tenantName,
    String recommendation, List<UUID> recommendedPhaseIds,
    List<Integer> phaseNumbers,
    String sector, String businessSize,
    List<CustomLine> customLines,
    UUID leadId, Integer offerPercent
) {
    public record BudgetPhase(String name, Integer sortOrder, List<BudgetLine> lines,
                               BigDecimal phaseTotal, BigDecimal phaseMonthlyTotal,
                               UUID phaseId, String phaseKey) {
        public record BudgetLine(String serviceName, BigDecimal setupPrice, BigDecimal monthlyPrice) {}
    }
    public record BudgetAddon(String serviceName, BigDecimal unitPrice) {}
    public record CustomLine(String description, Integer quantity, BigDecimal unitPrice,
                              BigDecimal monthlyPrice, BigDecimal total) {}
}
