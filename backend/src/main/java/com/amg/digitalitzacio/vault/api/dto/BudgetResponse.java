package com.amg.digitalitzacio.vault.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BudgetResponse(
    ProfileRef profile,
    List<BudgetPhase> phases,
    List<BudgetAddon> addons,
    BigDecimal total, BigDecimal totalCost, BigDecimal totalMargin
) {
    public record ProfileRef(UUID id, String name) {}
    public record BudgetPhase(PhaseRef phase, List<BudgetService> services,
                              BigDecimal phaseTotal, BigDecimal phaseCost, BigDecimal phaseMargin) {
        public record PhaseRef(UUID id, String name, Integer sortOrder) {}
        public record BudgetService(UUID id, String name, BigDecimal monthlyPrice, BigDecimal cost) {}
    }
    public record BudgetAddon(UUID id, String name, BigDecimal salePrice, BigDecimal cost) {}
}
