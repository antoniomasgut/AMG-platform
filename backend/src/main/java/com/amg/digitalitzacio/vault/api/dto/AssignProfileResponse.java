package com.amg.digitalitzacio.vault.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AssignProfileResponse(
    UUID profileId,
    List<PhaseSummary> phases,
    BigDecimal totalPrice
) {
    public record PhaseSummary(UUID phaseId, String name, Integer sortOrder, String approvalStatus, int totalServices, BigDecimal totalPrice) {}
}
