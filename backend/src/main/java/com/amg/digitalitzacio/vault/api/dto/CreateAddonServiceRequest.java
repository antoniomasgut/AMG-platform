package com.amg.digitalitzacio.vault.api.dto;

import com.amg.digitalitzacio.vault.domain.ServiceType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateAddonServiceRequest {
    private String name;
    private String slug;
    private String description;
    private ServiceType type;
    private BigDecimal cost;
    private BigDecimal salePrice;
}
