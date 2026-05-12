package com.amg.digitalitzacio.billing.api.dto;

import com.amg.digitalitzacio.billing.domain.AppliesTo;
import com.amg.digitalitzacio.billing.domain.DiscountType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateDiscountRequest {
    private DiscountType type;
    private BigDecimal value;
    private AppliesTo appliesTo;
    private UUID referenceId;
    private UUID tenantId;
    private String label;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private Integer maxApplications;
}
