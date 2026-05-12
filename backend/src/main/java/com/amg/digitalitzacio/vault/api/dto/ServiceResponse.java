package com.amg.digitalitzacio.vault.api.dto;

import com.amg.digitalitzacio.vault.domain.ServiceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ServiceResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private ServiceType type;
    private boolean isAddon;
    private BigDecimal cost;
    private BigDecimal salePrice;
    private Integer sortOrder;
}
