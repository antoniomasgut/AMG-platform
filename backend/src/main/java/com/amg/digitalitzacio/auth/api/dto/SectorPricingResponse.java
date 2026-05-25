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
        BigDecimal priceF5,
        BigDecimal setupF2,
        BigDecimal setupF3,
        BigDecimal setupF4,
        BigDecimal setupF5
) {
    public static final BigDecimal PHASE_UPGRADE_PRICE = new BigDecimal("75.00");

    public record PhaseLookup(BigDecimal setup, BigDecimal monthly) {}

    // Setup acumulat per a N fases seleccionades (F1=setupPrice, F2..F5=setupFn)
    public PhaseLookup forPhaseCount(int count) {
        BigDecimal[] tiers   = { priceF1, priceF2, priceF3, priceF4, priceF5 };
        BigDecimal[] setups  = { setupPrice, safe(setupF2), safe(setupF3), safe(setupF4), safe(setupF5) };
        BigDecimal monthly = BigDecimal.ZERO;
        BigDecimal totalSetup = BigDecimal.ZERO;
        for (int i = 0; i < count && i < tiers.length; i++) {
            if (tiers[i] != null) monthly = monthly.add(tiers[i]);
            totalSetup = totalSetup.add(setups[i]);
        }
        return new PhaseLookup(totalSetup, monthly);
    }

    // Setup per a una fase concreta (pel seu índex ordinal 0-based)
    public BigDecimal setupForOrdinal(int ordinal) {
        BigDecimal[] setups = { setupPrice, safe(setupF2), safe(setupF3), safe(setupF4), safe(setupF5) };
        return ordinal >= 0 && ordinal < setups.length ? setups[ordinal] : BigDecimal.ZERO;
    }

    public PhaseLookup forPhases(Collection<String> phases) {
        return forPhaseCount(phases == null ? 0 : (int) phases.stream()
                .filter(p -> p != null && p.matches("F[1-5]"))
                .distinct().count());
    }

    private static BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
