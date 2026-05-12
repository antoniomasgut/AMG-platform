package com.amg.digitalitzacio.vault.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BudgetResponse {
    private ProfileRef profile;
    private List<PhaseBudget> phases;
    private List<ServiceResponse> addons;
    private BigDecimal total;
    private BigDecimal totalCost;
    private BigDecimal totalMargin;

    @Data
    @Builder
    public static class ProfileRef {
        private UUID id;
        private String name;
    }

    @Data
    @Builder
    public static class PhaseBudget {
        private PhaseRef phase;
        private List<ServiceResponse> services;
        private BigDecimal phaseTotal;
        private BigDecimal phaseCost;
        private BigDecimal phaseMargin;

        @Data
        @Builder
        public static class PhaseRef {
            private UUID id;
            private String name;
            private int sortOrder;
        }
    }
}
