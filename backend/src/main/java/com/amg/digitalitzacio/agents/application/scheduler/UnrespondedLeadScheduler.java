package com.amg.digitalitzacio.agents.application.scheduler;

import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnrespondedLeadScheduler {

    private static final String REDIS_KEY = "unresponded:alert:%s:%s";
    private static final int REDIS_TTL_HOURS = 24;
    private static final int LOOKBACK_DAYS = 3;

    private final TenantRepository tenantRepository;
    private final NexeServiceConfigService nexeConfigService;
    private final ConversationRepository conversationRepository;
    private final ContactIdentifierRepository identifierRepository;
    private final ContactRepository contactRepository;
    private final TelegramBotClient telegramBotClient;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 0 * * * *")
    public void alertUnrespondedLeads() {
        var tenants = tenantRepository.findByContractedPhasesContaining("F5");
        if (tenants.isEmpty()) return;

        for (var tenant : tenants) {
            try {
                processAlerts(tenant.getId(), tenant.getName());
            } catch (Exception e) {
                log.warn("[F5-Alert] Error processant alertes per tenant {}: {}", tenant.getId(), e.getMessage());
            }
        }
    }

    private void processAlerts(UUID tenantId, String tenantName) throws Exception {
        var configOpt = nexeConfigService.get(tenantId, "EQUIP");
        if (configOpt.isEmpty()) return;

        Map<String, Object> config = objectMapper.readValue(
                configOpt.get().getConfigJson(), new TypeReference<>() {});

        Object alertEnabled = config.get("unresponded_alert_enabled");
        if (!Boolean.TRUE.equals(alertEnabled) && !"true".equals(String.valueOf(alertEnabled))) return;

        Object groupIdRaw = config.get("telegram_group_id");
        if (groupIdRaw == null || String.valueOf(groupIdRaw).isBlank()) return;

        long groupId;
        try {
            groupId = Long.parseLong(String.valueOf(groupIdRaw));
        } catch (NumberFormatException e) {
            return;
        }

        int hoursThreshold = config.get("unresponded_hours_threshold") instanceof Number n ? n.intValue() : 4;

        Instant cutoff = Instant.now().minus(hoursThreshold, ChronoUnit.HOURS);
        Instant from   = Instant.now().minus(LOOKBACK_DAYS, ChronoUnit.DAYS);

        var identifiers = conversationRepository.findIdentifiersWithUnrespondedUserMessage(tenantId, from, cutoff);
        if (identifiers.isEmpty()) return;

        for (String identifier : identifiers) {
            String redisKey = REDIS_KEY.formatted(tenantId, identifier);
            if (Boolean.TRUE.equals(redis.hasKey(redisKey))) continue;

            try {
                String name = resolveContactName(tenantId, identifier);
                String alert = "⚠️ Lead sense resposta\n\n" +
                        "📞 " + name + " (" + identifier + ")\n" +
                        "⏱️ Fa més de " + hoursThreshold + "h sense resposta de l'agent.\n\n" +
                        "Revisa l'inbox al portal per respondre manualment.";
                telegramBotClient.sendMessage(groupId, alert);
                redis.opsForValue().set(redisKey, "1", REDIS_TTL_HOURS, TimeUnit.HOURS);
                log.info("[F5-Alert] Alerta enviada per {} (tenant {})", identifier, tenantId);
            } catch (Exception e) {
                log.warn("[F5-Alert] Error enviant alerta per {}: {}", identifier, e.getMessage());
            }
        }
    }

    private String resolveContactName(UUID tenantId, String identifier) {
        return identifierRepository.findByTenantIdAndIdentifier(tenantId, identifier)
                .flatMap(ci -> contactRepository.findById(ci.getContactId()))
                .map(Contact::getDisplayName)
                .filter(n -> n != null && !n.isBlank())
                .orElse(identifier);
    }
}
