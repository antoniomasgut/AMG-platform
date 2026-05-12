package com.amg.digitalitzacio.billing.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateBudgetRequest {
    private UUID profileId;
    private List<UUID> addonIds;
    private String notes;
    private Integer validUntilDays;
}
