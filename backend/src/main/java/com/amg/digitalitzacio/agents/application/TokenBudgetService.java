package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.ChannelUsageLogRepository;
import com.amg.digitalitzacio.agents.domain.TenantAIConfig;
import com.amg.digitalitzacio.agents.domain.TenantAIConfigRepository;
import com.amg.digitalitzacio.agents.domain.TokenUsageLog;
import com.amg.digitalitzacio.agents.domain.TokenUsageLogRepository;
import com.amg.digitalitzacio.shared.ai.AIProvider.AIResponse;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBudgetService {

    private final TokenUsageLogRepository usageLogRepository;
    private final ChannelUsageLogRepository channelUsageLogRepository;
    private final TenantAIConfigRepository aiConfigRepository;
    private final TelegramBotClient telegramBotClient;
    private final SystemConfigService sysConfig;

    /**
     * Preus per model en micros d'€ per token (1 micro = 0.000001 €).
     * inputMicrosPerToken, outputMicrosPerToken
     * Font: Anthropic/DeepSeek pricing (EUR/USD ≈ 0.93, juny 2026).
     * Actualitzar quan canviïn els preus.
     */
    public static final Map<String, long[]> MODEL_PRICES = Map.of(
        "claude-haiku-4-5-20251001", new long[]{ 740,  3_700 },
        "claude-haiku-4-5",          new long[]{ 740,  3_700 },
        "claude-sonnet-4-6",         new long[]{ 2_780, 13_890 },
        "claude-opus-4-7",           new long[]{ 13_890, 69_440 },
        "deepseek-chat",             new long[]{ 250,  1_020 },
        "deepseek-reasoner",         new long[]{ 510,  2_040 }
    );

    // Preu de fallback per a models desconeguts: assumim Haiku
    private static final long[] FALLBACK_PRICE = new long[]{ 740, 3_700 };

    public long calculateCostMicros(String model, int inputTokens, int outputTokens) {
        var price = MODEL_PRICES.getOrDefault(model, FALLBACK_PRICE);
        return (long) inputTokens * price[0] + (long) outputTokens * price[1];
    }

    private Instant startOfMonth() {
        return Instant.now()
                .atZone(ZoneOffset.UTC)
                .with(TemporalAdjusters.firstDayOfMonth())
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant();
    }

    /**
     * Comprova si el tenant pot fer una crida d'IA.
     * Si té pressupost en € configurat, compara el cost acumulat del mes.
     * Si no, retorna true (sense límit).
     */
    @Transactional
    public boolean canCallAI(UUID tenantId) {
        var config = aiConfigRepository.findById(tenantId).orElse(null);
        if (config == null) return true;

        Integer budgetCents = config.getMonthlyCostBudgetEurCents();
        if (budgetCents == null || budgetCents <= 0) return true;

        long budgetMicros = (long) budgetCents * 10_000; // 1 cent = 10.000 micros
        long usedMicros   = usageLogRepository.sumCostSince(tenantId, startOfMonth());
        if (usedMicros < budgetMicros) return true;

        log.warn("[CostBudget] Tenant {} ha superat el pressupost mensual ({}€ cents usats, límit {}€ cents)",
                tenantId, usedMicros / 10_000, budgetCents);
        return false;
    }

    /**
     * Retorna el cost en micros d'€ per un missatge WhatsApp sortint.
     * Tarifes configurable a System Config (límit superior conservador: compta Meta per missatge).
     */
    public long whatsappCostMicros(String channel) {
        if ("WHATSAPP_META".equals(channel)) {
            // Meta Cloud API amb WABA propi del tenant: Meta factura directament al
            // tenant, AMG no té cost per missatge. Si mai AMG passa a BSP (Meta
            // factura a AMG), posar el cost real a WHATSAPP_META_COST_EUR_MICROS.
            String val = sysConfig.get("WHATSAPP_META_COST_EUR_MICROS");
            return parseLong(val, 0);
        } else {
            String val = sysConfig.get("WHATSAPP_TWILIO_COST_EUR_MICROS");
            return parseLong(val, 55_000); // default €0.055/missatge (Twilio + Meta)
        }
    }

    /**
     * Comprova si el tenant pot enviar un missatge WhatsApp (dins del pressupost mensual en €).
     */
    @Transactional(readOnly = true)
    public boolean canSendWhatsapp(UUID tenantId) {
        var config = aiConfigRepository.findById(tenantId).orElse(null);
        if (config == null) return true;
        Integer budgetCents = config.getMonthlyWhatsappBudgetEurCents();
        if (budgetCents == null || budgetCents <= 0) return true;

        Instant start = startOfMonth();
        long budgetMicros = (long) budgetCents * 10_000;
        long usedMicros = channelUsageLogRepository.sumWhatsappCostBetween(tenantId, start, Instant.now());
        if (usedMicros < budgetMicros) return true;

        log.warn("[WaBudget] Tenant {} ha superat el pressupost WhatsApp ({}€ micros usats, límit {}€ cents)",
                tenantId, usedMicros, budgetCents);
        return false;
    }

    /**
     * Comprova si el tenant ha superat el pressupost de missatges WhatsApp/mes.
     */
    @Transactional
    public void checkAndAutoIncrementMessages(UUID tenantId, String channel) {
        var config = aiConfigRepository.findById(tenantId).orElse(null);
        if (config == null) return;
        int budget = config.getMonthlyMessageBudget() == null ? 0 : config.getMonthlyMessageBudget();
        if (budget <= 0) return;

        Instant start = startOfMonth();
        long used = channelUsageLogRepository.countByTenantAndChannel(tenantId, channel, start, Instant.now());
        if (used < budget) return;

        int increment = config.getOverageMessageIncrement() == null ? 0 : config.getOverageMessageIncrement();
        if (increment <= 0) return;

        int newBudget = budget + increment;
        config.setMonthlyMessageBudget(newBudget);
        aiConfigRepository.save(config);
        log.warn("[MsgBudget] Tenant {} ha superat el pressupost de missatges {}. Auto-increment: {} → {}",
                tenantId, channel, budget, newBudget);
        alertAdmin("📈 *Quota de missatges " + channel + " ampliada automàticament*\nTenant: `" + tenantId
                + "`\nÚs actual: " + used + " missatges\nNou límit: " + newBudget + " missatges");
    }

    /** Registra l'ús de tokens i el cost calculat. Envia alerta si s'acosta al límit. */
    @Transactional
    public void record(UUID tenantId, String model, String taskType, AIResponse response) {
        if (response == null || response.totalTokens() == 0) return;

        long costMicros = calculateCostMicros(model, response.inputTokens(), response.outputTokens());
        usageLogRepository.save(TokenUsageLog.of(
                tenantId, model, taskType,
                response.inputTokens(), response.outputTokens(), costMicros
        ));

        var config = aiConfigRepository.findById(tenantId).orElse(null);
        if (config == null) return;
        Integer budgetCents = config.getMonthlyCostBudgetEurCents();
        if (budgetCents == null || budgetCents <= 0) return;

        long budgetMicros = (long) budgetCents * 10_000;
        long usedMicros   = usageLogRepository.sumCostSince(tenantId, startOfMonth());
        int threshold     = config.getBudgetAlertThreshold() == null ? 80 : config.getBudgetAlertThreshold();
        double pct        = (usedMicros * 100.0) / budgetMicros;
        long prevMicros   = usedMicros - costMicros;
        double prevPct    = (prevMicros * 100.0) / budgetMicros;

        if (pct >= 100 && prevPct < 100) {
            log.warn("Tenant {} ha superat el pressupost mensual (€{})", tenantId, budgetCents / 100.0);
            alertAdmin("🔴 *Pressupost d'IA esgotat*\nTenant: `" + tenantId
                    + "`\nÚs: " + formatEur(usedMicros) + " / " + formatEur(budgetMicros)
                    + "\nS'activarà el model de backup si n'hi ha.");
        } else if (pct >= threshold && prevPct < threshold) {
            alertAdmin("⚠️ *Avís de pressupost d'IA*\nTenant: `" + tenantId
                    + "`\nÚs: " + formatEur(usedMicros) + " / " + formatEur(budgetMicros)
                    + " (" + (int) pct + "%)");
        }
    }

    /** Retorna l'ús del mes actual per a un tenant. */
    @Transactional(readOnly = true)
    public TokenUsageSummary summary(UUID tenantId) {
        var config = aiConfigRepository.findById(tenantId).orElse(TenantAIConfig.defaultFor(tenantId));
        Instant start    = startOfMonth();
        Instant now      = Instant.now();
        long usedTokens  = usageLogRepository.sumTokensSince(tenantId, start);
        long usedMicros  = usageLogRepository.sumCostSince(tenantId, start);
        Integer budgetCents = config.getMonthlyCostBudgetEurCents();
        long budgetMicros   = budgetCents != null && budgetCents > 0 ? (long) budgetCents * 10_000 : 0;
        int pct = (budgetMicros > 0) ? (int) Math.min(100, usedMicros * 100 / budgetMicros) : 0;

        long usedWaMicros = channelUsageLogRepository.sumWhatsappCostBetween(tenantId, start, now);
        Integer waBudgetCents = config.getMonthlyWhatsappBudgetEurCents();
        long waBudgetMicros = waBudgetCents != null && waBudgetCents > 0 ? (long) waBudgetCents * 10_000 : 0;
        int waPct = (waBudgetMicros > 0) ? (int) Math.min(100, usedWaMicros * 100 / waBudgetMicros) : 0;

        return new TokenUsageSummary(
                usedTokens, usedMicros, budgetMicros, pct,
                config.getPreferredModel(), config.getBudgetAlertThreshold(),
                budgetCents, usedWaMicros, waBudgetMicros, waPct, waBudgetCents
        );
    }

    private static String formatEur(long micros) {
        return String.format("€%.4f", micros / 1_000_000.0);
    }

    private static long parseLong(String val, long defaultVal) {
        if (val == null || val.isBlank()) return defaultVal;
        try { return Long.parseLong(val.trim()); } catch (NumberFormatException e) { return defaultVal; }
    }

    private void alertAdmin(String message) {
        try {
            String chatIdStr = sysConfig.get("TELEGRAM_CHAT_ID");
            if (chatIdStr != null && !chatIdStr.isBlank()) {
                telegramBotClient.sendMessage(Long.parseLong(chatIdStr.trim()), message);
            }
        } catch (Exception e) {
            log.error("No s'ha pogut enviar alerta de pressupost: {}", e.getMessage());
        }
    }

    public record TokenUsageSummary(
            long usedTokens,
            long usedCostMicros,
            long budgetMicros,
            int budgetPercent,
            String preferredModel,
            Integer alertThreshold,
            Integer monthlyCostBudgetEurCents,
            long usedWhatsappCostMicros,
            long whatsappBudgetMicros,
            int whatsappBudgetPercent,
            Integer monthlyWhatsappBudgetEurCents
    ) {
        public double usedCostEur()        { return usedCostMicros        / 1_000_000.0; }
        public double budgetEur()          { return budgetMicros          / 1_000_000.0; }
        public double usedWhatsappCostEur(){ return usedWhatsappCostMicros / 1_000_000.0; }
        public double whatsappBudgetEur()  { return whatsappBudgetMicros   / 1_000_000.0; }
    }
}
