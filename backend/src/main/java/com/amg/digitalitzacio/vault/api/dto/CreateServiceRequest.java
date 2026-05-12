package com.amg.digitalitzacio.vault.api.dto;

import com.amg.digitalitzacio.vault.domain.ServiceType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateServiceRequest {
    private String name;
    private String slug;
    private String description;
    private ServiceType type;
    private Boolean isAddon;
    private BigDecimal cost;
    private BigDecimal salePrice;
    private Integer sortOrder;
}
