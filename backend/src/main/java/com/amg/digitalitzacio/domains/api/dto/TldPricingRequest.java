package com.amg.digitalitzacio.domains.api.dto;

import java.math.BigDecimal;

// Petició d'actualització de tarifes d'un TLD
public record TldPricingRequest(
        BigDecimal saleRegister,
        BigDecimal saleRenew,
        boolean isActive
) {}
