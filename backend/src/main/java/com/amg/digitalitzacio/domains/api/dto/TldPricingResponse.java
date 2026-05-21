package com.amg.digitalitzacio.domains.api.dto;

import java.math.BigDecimal;

// Resposta amb les tarifes d'un TLD
public record TldPricingResponse(
        String tld,
        BigDecimal costRegister,
        BigDecimal costRenew,
        BigDecimal saleRegister,
        BigDecimal saleRenew,
        boolean isActive
) {}
