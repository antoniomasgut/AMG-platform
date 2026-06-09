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

    /** Inici del mes actual en UTC */
    private Instant startOfMonth() {
        return Instant.now()
                .atZone(ZoneOffset.UTC)
                .with(TemporalAdjusters.firstDayOfMonth())
                .truncatedTo(ChronoUnit.DAYS)
                .toInstant();
    }

    /**
     * Comprova si el tenant pot fer una crida d'IA.
     * Si el pressupost s'ha superat, l'incrementa automàticament en lloc de bloquejar.
     * Sempre retorna true si hi ha un overageTokenIncrement configurat.
     */
    @Transactional
    public boolean canCallAI(UUID tenantId) {
        var config = aiConfigRepository.findById(tenantId).orElse(null);
        if (config == null) return true;
        int budget = config.getMonthlyTokenBudget() == null ? 0 : config.getMonthlyTokenBudget();
        if (budget <= 0) return true;

        long used = usageLogRepository.sumTokensSince(tenantId, startOfMonth());
        if (used < budget) return true;

        int increment = config.getOverageTokenIncrement() == null ? 0 : config.getOverageTokenIncrement();
        if (increment <= 0) return false; // sense auto-increment: bloqueja

        int newBudget = budget + increment;
        config.setMonthlyTokenBudget(newBudget);
        aiConfigRepository.save(config);
        log.warn("[TokenBudget] Tenant {} ha superat el pressupost. Auto-increment: {} → {} tokens",
                tenantId, budget, newBudget);
        alertAdmin("📈 *Quota de tokens ampliada automàticament*\nTenant: `" + tenantId
                + "`\nÚs actual: " + used + " tokens\nNou límit: " + newBudget + " tokens");
        return true;
    }

    /**
     * Comprova si el tenant ha superat el pressupost de missatges WhatsApp/mes.
     * Si s'ha superat, l'incrementa automàticament.
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

    /** Registra l'ús de tokens i envia alerta si s'acosta o supera el límit. */
    @Transactional
    public void record(UUID tenantId, String model, String taskType, AIResponse response) {
        if (response == null || response.totalTokens() == 0) return;

        usageLogRepository.save(TokenUsageLog.of(
                tenantId, model, taskType,
                response.inputTokens(), response.outputTokens()
        ));

        var config = aiConfigRepository.findById(tenantId).orElse(null);
        if (config == null) return;
        int budget = config.getMonthlyTokenBudget() == null ? 0 : config.getMonthlyTokenBudget();
        if (budget <= 0) return;

        long used = usageLogRepository.sumTokensSince(tenantId, startOfMonth());
        int threshold = config.getBudgetAlertThreshold() == null ? 80 : config.getBudgetAlertThreshold();

        double pct = (used * 100.0) / budget;

        if (pct >= 100) {
            log.warn("Tenant {} ha superat el pressupost mensual de tokens ({}/{})", tenantId, used, budget);
            alertAdmin("🔴 *Pressupost de tokens esgotat*\nTenant: `" + tenantId + "`\nÚs: " + used + "/" + budget + " tokens\nL'agent ha quedat suspès fins al mes vinent.");
        } else if (pct >= threshold && (used - response.totalTokens()) * 100.0 / budget < threshold) {
            // Primer cop que creua el threshold aquest mes
            alertAdmin("⚠️ *Avís de tokens*\nTenant: `" + tenantId + "`\nÚs: " + used + "/" + budget + " tokens (" + (int) pct + "%)");
        }
    }

    /** Retorna l'ús del mes actual per a un tenant. */
    @Transactional(readOnly = true)
    public TokenUsageSummary summary(UUID tenantId) {
        var config = aiConfigRepository.findById(tenantId).orElse(TenantAIConfig.defaultFor(tenantId));
        long used = usageLogRepository.sumTokensSince(tenantId, startOfMonth());
        int budget = config.getMonthlyTokenBudget() == null ? 0 : config.getMonthlyTokenBudget();
        int pct = (budget > 0) ? (int) Math.min(100, used * 100 / budget) : 0;
        return new TokenUsageSummary(used, budget, pct,
                config.getPreferredModel(), config.getReasoningModel(),
                config.getBudgetAlertThreshold());
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
            int monthlyBudget,
            int budgetPercent,
            String chatModel,
            String reasoningModel,
            Integer alertThreshold
    ) {}
}
