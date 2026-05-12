package com.amg.digitalitzacio.vault.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AddonResponse {
    private boolean approvalRequired;
    private BigDecimal salePrice;
    private String status;
}
