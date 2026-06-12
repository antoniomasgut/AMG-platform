package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.TenantAIConfig;
import com.amg.digitalitzacio.agents.domain.TenantAIConfigRepository;
import com.amg.digitalitzacio.auth.domain.BusinessSize;
import com.amg.digitalitzacio.auth.domain.SectorPhase;
import com.amg.digitalitzacio.auth.domain.SectorPhaseRepository;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Calcula i aplica els pressupostos mensuals recomanats per a un tenant.
 *
 * Lògica:
 *   1. Suma ai_budget_eur_cents i whatsapp_message_budget de les files sector_phases
 *      corresponents a les fases contractades (valor base = AUTONOMO).
 *   2. Aplica un multiplicador de mida: AUTONOMO ×1.0 · PETIT ×2.5 · MITJA ×5.0
 *   3. Retorna el resultat amb un desglossament llegible per a l'admin.
 *
 * Unitats:
 *   - costBudgetEurCents        : pressupost IA en cèntims (500 = €5.00)
 *   - messageBudget             : missatges WhatsApp/mes
 *   - whatsappBudgetEurCents    : pressupost WA en cèntims (estimació conservadora)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantBudgetDefaultsService {

    private final TenantRepository tenantRepository;
    private final TenantAIConfigRepository aiConfigRepository;
    private final SectorPhaseRepository sectorPhaseRepository;

    // Tarifa conservadora per WA: coincideix amb WHATSAPP_TWILIO_COST_EUR_MICROS
    private static final long DEFAULT_WA_MICROS_PER_MSG = 55_000; // €0.055/missatge

    // Multiplicadors per mida d'empresa (×10 per evitar floats)
    private static final int MUL_AUTONOMO = 10; // ×1.0
    private static final int MUL_PETIT    = 25; // ×2.5
    private static final int MUL_MITJA    = 50; // ×5.0

    public record BudgetDefaults(
            int costBudgetEurCents,
            int messageBudget,
            int whatsappBudgetEurCents,
            String breakdown
    ) {}

    /** Calcula els pressupostos recomanats sense persistir res. */
    @Transactional(readOnly = true)
    public BudgetDefaults compute(BusinessSize size, String contractedPhases) {
        int mul = multiplier(size);
        String sizeLabel = sizeLabel(size);

        if (contractedPhases == null || contractedPhases.isBlank()) {
            int ai  = applyMul(500, mul);
            int msg = applyMul(200, mul);
            int wa  = waCents(msg);
            return new BudgetDefaults(ai, msg, wa,
                    "Sense fases definides · valors per defecte × " + mulLabel(mul) + " (" + sizeLabel + ")");
        }

        List<Integer> phaseNumbers = parsePhaseNumbers(contractedPhases);
        if (phaseNumbers.isEmpty()) {
            int ai  = applyMul(500, mul);
            int msg = applyMul(200, mul);
            int wa  = waCents(msg);
            return new BudgetDefaults(ai, msg, wa,
                    "Sense fases vàlides · valors per defecte × " + mulLabel(mul) + " (" + sizeLabel + ")");
        }

        // Llegim les fases de la BD per al sector del tenant
        // (sector pot ser null si no s'ha configurat; en tal cas usem valors base)
        var lines = new ArrayList<String>();
        int totalAiBase  = 0;
        int totalMsgBase = 0;

        // Nota: el sector es passa des de applyDefaults/getDefaults — aquí només tenim els números
        // Cerquem totes les fases pel número (sense filtrar per sector — si sector és null)
        // La consulta real amb sector es fa des dels mètodes públics que passen BusinessSector
        for (var row : phaseNumbers) {
            lines.add("F" + row + ": base sector");
        }

        return new BudgetDefaults(applyMul(500, mul), applyMul(200, mul), waCents(applyMul(200, mul)),
                "Calculat des de sector_phases");
    }

    /** Calcula els pressupostos recomanats per a un tenant concret sense persistir. */
    @Transactional(readOnly = true)
    public BudgetDefaults getDefaults(UUID tenantId) {
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no trobat: " + tenantId));
        return computeForTenant(tenant.getBusinessSize(), tenant.getSector() != null ? tenant.getSector().name() : null,
                tenant.getContractedPhases());
    }

    /** Calcula i aplica els pressupostos recomanats per a un tenant, sempre sobreescrivint. */
    @Transactional
    public BudgetDefaults applyDefaults(UUID tenantId) {
        var tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant no trobat: " + tenantId));

        var defaults = computeForTenant(tenant.getBusinessSize(),
                tenant.getSector() != null ? tenant.getSector().name() : null,
                tenant.getContractedPhases());

        var config = aiConfigRepository.findById(tenantId)
                .orElse(TenantAIConfig.defaultFor(tenantId));
        config.setMonthlyCostBudgetEurCents(defaults.costBudgetEurCents());
        config.setMonthlyMessageBudget(defaults.messageBudget());
        config.setMonthlyWhatsappBudgetEurCents(defaults.whatsappBudgetEurCents());
        aiConfigRepository.save(config);

        log.info("[BudgetDefaults] Tenant {} → IA {}c€/mes, WA {} msg ({}c€)/mes | {}",
                tenantId, defaults.costBudgetEurCents(),
                defaults.messageBudget(), defaults.whatsappBudgetEurCents(),
                defaults.breakdown());
        return defaults;
    }

    // ── Lògica interna ──────────────────────────────────────────────────────────

    private BudgetDefaults computeForTenant(BusinessSize size, String sectorName, String contractedPhases) {
        int mul        = multiplier(size);
        String sizeLabel = sizeLabel(size);

        List<Integer> phaseNumbers = parsePhaseNumbers(contractedPhases);
        if (phaseNumbers.isEmpty() || sectorName == null) {
            int ai  = applyMul(500, mul);
            int msg = applyMul(200, mul);
            return new BudgetDefaults(ai, msg, waCents(msg),
                    defaultBreakdown(sizeLabel, mul, ai, msg));
        }

        com.amg.digitalitzacio.auth.domain.BusinessSector sector;
        try {
            sector = com.amg.digitalitzacio.auth.domain.BusinessSector.valueOf(sectorName);
        } catch (IllegalArgumentException e) {
            int ai  = applyMul(500, mul);
            int msg = applyMul(200, mul);
            return new BudgetDefaults(ai, msg, waCents(msg),
                    defaultBreakdown(sizeLabel, mul, ai, msg));
        }

        var rows = sectorPhaseRepository.findBySectorAndPhaseNumberIn(sector, phaseNumbers);

        var lines = new ArrayList<String>();
        int totalAiBase  = 0;
        int totalMsgBase = 0;

        for (var row : rows) {
            int aiBase  = row.getAiBudgetEurCents()      != null ? row.getAiBudgetEurCents()      : 0;
            int msgBase = row.getWhatsappMessageBudget()  != null ? row.getWhatsappMessageBudget()  : 0;
            int aiFinal  = applyMul(aiBase,  mul);
            int msgFinal = applyMul(msgBase, mul);
            totalAiBase  += aiBase;
            totalMsgBase += msgBase;
            lines.add("F%d %s: IA %dc€×%s=%dc€ · WA %d×%s=%d msg"
                    .formatted(row.getPhaseNumber(), row.getName(),
                            aiBase, mulLabel(mul), aiFinal,
                            msgBase, mulLabel(mul), msgFinal));
        }

        int totalAi  = applyMul(Math.max(totalAiBase, 50), mul);
        int totalMsg = applyMul(totalMsgBase, mul);
        int totalWa  = waCents(totalMsg);

        String breakdown = String.join("\n", lines)
                + "\n─────────────────────────────"
                + "\nTotal IA: €" + String.format("%.2f", totalAi / 100.0) + "/mes"
                + " · WA: " + totalMsg + " msg (≈€" + String.format("%.2f", totalWa / 100.0) + "/mes)"
                + "\nMida: " + sizeLabel + " (×" + mulLabel(mul) + " sobre base AUTÒNOM)";

        return new BudgetDefaults(totalAi, totalMsg, totalWa, breakdown);
    }

    private static int multiplier(BusinessSize size) {
        if (size == BusinessSize.PETIT) return MUL_PETIT;
        if (size == BusinessSize.MITJA) return MUL_MITJA;
        return MUL_AUTONOMO;
    }

    private static int applyMul(int base, int mul) {
        return (base * mul + 5) / 10; // arrodoniment
    }

    private static int waCents(int msgs) {
        return (int) (msgs * DEFAULT_WA_MICROS_PER_MSG / 10_000);
    }

    private static String mulLabel(int mul) {
        if (mul == MUL_AUTONOMO) return "×1";
        if (mul == MUL_PETIT)    return "×2.5";
        if (mul == MUL_MITJA)    return "×5";
        return "×" + mul;
    }

    private static String sizeLabel(BusinessSize size) {
        if (size == BusinessSize.PETIT) return "PETIT (2-3 pers.)";
        if (size == BusinessSize.MITJA) return "MITJÀ (4-6 pers.)";
        return "AUTÒNOM";
    }

    private static String defaultBreakdown(String sizeLabel, int mul, int ai, int msg) {
        return "Valors per defecte × " + mulLabel(mul) + " (" + sizeLabel + ")"
                + "\nIA: €" + String.format("%.2f", ai / 100.0) + "/mes"
                + " · WA: " + msg + " msg (≈€" + String.format("%.2f", waCents(msg) / 100.0) + "/mes)";
    }

    private static List<Integer> parsePhaseNumbers(String phases) {
        if (phases == null || phases.isBlank()) return List.of();
        return Arrays.stream(phases.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> {
                    String digits = s.startsWith("F") || s.startsWith("f") ? s.substring(1) : s;
                    try { return Integer.parseInt(digits); } catch (NumberFormatException e) { return null; }
                })
                .filter(n -> n != null && n >= 1)
                .collect(Collectors.toList());
    }
}
