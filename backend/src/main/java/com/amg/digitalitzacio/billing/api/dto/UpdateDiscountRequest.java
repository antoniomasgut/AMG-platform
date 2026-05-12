package com.amg.digitalitzacio.billing.api.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateDiscountRequest {
    private BigDecimal value;
    private String label;
    private LocalDate validUntil;
    private Integer maxApplications;
}
