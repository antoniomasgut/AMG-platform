package com.amg.digitalitzacio.billing.api.dto;

import com.amg.digitalitzacio.billing.domain.AppliesTo;
import com.amg.digitalitzacio.billing.domain.DiscountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class DiscountResponse {
    private UUID id;
    private UUID tenantId;
    private DiscountType type;
    private BigDecimal value;
    private AppliesTo appliesTo;
    private UUID referenceId;
    private String label;
    private Boolean isActive;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private Integer maxApplications;
    private Integer appliedCount;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
