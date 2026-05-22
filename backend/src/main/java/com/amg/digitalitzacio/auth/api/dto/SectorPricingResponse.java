package com.amg.digitalitzacio.auth.api.dto;

import java.math.BigDecimal;
import java.util.Collection;

public record SectorPricingResponse(
        String sector,
        String businessSize,
        BigDecimal setupPrice,
        BigDecimal priceF1,
        BigDecimal priceF2,
        BigDecimal priceF3,
        BigDecimal priceF4,
        BigDecimal priceF5
) {
    public static final BigDecimal PHASE_UPGRADE_PRICE = new BigDecimal("75.00");

    public record PhaseLookup(BigDecimal setup, BigDecimal monthly) {}

    // Calcula el mensual sumant els preus de les N primeres posicions (independentment de quines fases)
    public PhaseLookup forPhaseCount(int count) {
        BigDecimal[] tiers = { priceF1, priceF2, priceF3, priceF4, priceF5 };
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < count && i < tiers.length; i++) {
            if (tiers[i] != null) total = total.add(tiers[i]);
        }
        return new PhaseLookup(setupPrice, total);
    }

    // Calcula el mensual a partir d'una col·lecció de fases ("F1","F3",...)
    public PhaseLookup forPhases(Collection<String> phases) {
        return forPhaseCount(phases == null ? 0 : (int) phases.stream()
                .filter(p -> p != null && p.matches("F[1-5]"))
                .distinct().count());
    }
}
