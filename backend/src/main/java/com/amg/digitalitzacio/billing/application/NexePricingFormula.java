package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.auth.domain.BusinessSector;
import com.amg.digitalitzacio.auth.domain.BusinessSize;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Font de veritat dels preus NexeLocal (model fórmula).
 * Preu mensual per fase = base × factor_sector × factor_mida
 * Setup per fase = mensual × 2
 */
@Component
public class NexePricingFormula {

    // Cost base mensual per fase (€)
    private static final Map<Integer, BigDecimal> PHASE_BASE = Map.of(
        1, new BigDecimal("40"),  // Captació (25) + Resposta immediata (15)
        2, new BigDecimal("20"),  // Agenda i cites (20)
        3, new BigDecimal("40"),  // Seguiment comercial (25) + Cobraments (15)
        4, new BigDecimal("15"),  // Fidelització (15)
        5, new BigDecimal("50")   // Operacions (20) + Documentació (30)
    );

    private static final Map<Integer, String> PHASE_LABELS = Map.of(
        1, "F1 — Captació i Agent IA",
        2, "F2 — Agenda i Cites",
        3, "F3 — Pressupostos i Cobraments",
        4, "F4 — Fidelització i Seguiment",
        5, "F5 — Equip i Documentació"
    );

    private static final BigDecimal SETUP_MULTIPLIER = new BigDecimal("2");

    public BigDecimal monthlyPerPhase(int phaseNumber, BusinessSector sector, BusinessSize size) {
        var base = PHASE_BASE.getOrDefault(phaseNumber, BigDecimal.ZERO);
        return base.multiply(sectorFactor(sector)).multiply(sizeFactor(size))
                   .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal setupPerPhase(int phaseNumber, BusinessSector sector, BusinessSize size) {
        return monthlyPerPhase(phaseNumber, sector, size)
                   .multiply(SETUP_MULTIPLIER).setScale(2, RoundingMode.HALF_UP);
    }

    public String phaseLabel(int phaseNumber) {
        return PHASE_LABELS.getOrDefault(phaseNumber, "Fase " + phaseNumber);
    }

    private BigDecimal sectorFactor(BusinessSector sector) {
        if (sector == null) return BigDecimal.ONE;
        return switch (sector) {
            case INMOBILIARIA -> new BigDecimal("1.5");
            case GESTORIA, ACADEMIA, AGENCIA_IA -> new BigDecimal("1.8");
            case FISIOTERAPEUTA, PSICOLEG, NUTRICIONISTA,
                 PERRUQUERIA, ESTETICA, VETERINARI, PERRUQUERIA_CANINA -> new BigDecimal("1.2");
            default -> BigDecimal.ONE; // Oficis: PINTOR, ELECTRICISTA, FONTANER, etc.
        };
    }

    private BigDecimal sizeFactor(BusinessSize size) {
        if (size == null) return BigDecimal.ONE;
        return switch (size) {
            case PETIT   -> new BigDecimal("1.5");
            case MITJA   -> new BigDecimal("2.0");
            case EMPRESA -> new BigDecimal("3.0");
            default      -> BigDecimal.ONE; // AUTONOMO
        };
    }
}
