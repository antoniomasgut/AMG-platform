package com.amg.digitalitzacio.auth.api.dto;

import java.math.BigDecimal;

public record SectorPricingResponse(
        String sector,
        String businessSize,
        BigDecimal setupPrice,
        BigDecimal monthlyF1,
        BigDecimal monthlyF1f2,
        BigDecimal monthlyF1f2f3,
        BigDecimal monthlyComplete
) {
    // Preu fix d'ampliació de fase per a clients existents (Opció B)
    public static final BigDecimal PHASE_UPGRADE_PRICE = new BigDecimal("75.00");

    public record PhaseLookup(BigDecimal setup, BigDecimal monthly) {}

    public PhaseLookup forPhaseCount(int count) {
        return switch (count) {
            case 0 -> new PhaseLookup(setupPrice, BigDecimal.ZERO);
            case 1 -> new PhaseLookup(setupPrice, monthlyF1);
            case 2 -> new PhaseLookup(setupPrice, monthlyF1f2);
            case 3 -> new PhaseLookup(setupPrice, monthlyF1f2f3);
            default -> new PhaseLookup(setupPrice, monthlyComplete);
        };
    }
}
