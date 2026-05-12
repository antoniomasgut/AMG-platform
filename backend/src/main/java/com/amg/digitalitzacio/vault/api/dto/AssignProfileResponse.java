package com.amg.digitalitzacio.vault.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AssignProfileResponse {
    private UUID profileId;
    private List<PhaseSummary> phases;
    private BigDecimal totalPrice;

    @Data
    @Builder
    public static class PhaseSummary {
        private UUID phaseId;
        private String name;
        private int sortOrder;
        private String approvalStatus;
        private int totalServices;
        private BigDecimal totalPrice;
    }
}
