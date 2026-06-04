package com.amg.digitalitzacio.agents.application.scheduler;

import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.amg.digitalitzacio.agents.application.channel.EmailChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
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
public class ReactivationScheduler {

    private static final String REDIS_KEY = "reactivation:sent:%s:%s";
    private static final int REDIS_TTL_DAYS = 60;
    // 7-day lookback window (runs weekly)
    private static final int WINDOW_DAYS = 7;

    private final TenantRepository tenantRepository;
    private final NexeServiceConfigService nexeConfigService;
    private final ConversationRepository conversationRepository;
    private final TenantChatLinkRepository chatLinkRepository;
    private final ContactIdentifierRepository identifierRepository;
    private final ContactRepository contactRepository;
    private final WhatsAppChannel whatsAppChannel;
    private final WhatsAppMetaChannel whatsAppMetaChannel;
    private final EmailChannel emailChannel;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    // Every Monday at 09:00
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendReactivationMessages() {
        var tenants = tenantRepository.findByContractedPhasesContaining("F4");
        if (tenants.isEmpty()) return;

        log.info("[F4-Reactivation] Executant reactivació de clients per {} tenants", tenants.size());

        for (var tenant : tenants) {
            try {
                processReactivations(tenant.getId());
            } catch (Exception e) {
                log.warn("[F4-Reactivation] Error processant reactivació per tenant {}: {}", tenant.getId(), e.getMessage());
            }
        }
    }

    private void processReactivations(UUID tenantId) throws Exception {
        var configOpt = nexeConfigService.get(tenantId, "FIDELITZACIO");
        if (configOpt.isEmpty()) return;

        Map<String, Object> config = objectMapper.readValue(
                configOpt.get().getConfigJson(), new TypeReference<>() {});

        int reengagementMonths = config.get("reengagement_months") instanceof Number n ? n.intValue() : 0;
        if (reengagementMonths <= 0) return;

        String template = config.get("reengagement_template") instanceof String t && !t.isBlank()
                ? t
                : "Hola {{nom}}, fa temps que no et veiem! Si necessites alguna cosa, estem aquí. 😊";

        long inactiveDays = (long) reengagementMonths * 30;
        // Window: clients whose last message was between (inactiveDays + WINDOW_DAYS) and inactiveDays ago
        Instant windowEnd   = Instant.now().minus(inactiveDays, ChronoUnit.DAYS);
        Instant windowStart = windowEnd.minus(WINDOW_DAYS, ChronoUnit.DAYS);

        var identifiers = conversationRepository
                .findIdentifiersWithLastUserMessageInWindow(tenantId, windowStart, windowEnd);

        if (identifiers.isEmpty()) return;

        var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);

        for (String identifier : identifiers) {
            String redisKey = REDIS_KEY.formatted(tenantId, identifier);
            if (Boolean.TRUE.equals(redis.hasKey(redisKey))) continue;

            try {
                String name = resolveContactName(tenantId, identifier);
                String message = template
                        .replace("{{nom}}", name)
                        .replace("{{ultima_visita}}", "fa " + reengagementMonths + " mesos")
                        .replace("{{oferta}}", "");
                sendReactivation(chatLink, identifier, message);
                redis.opsForValue().set(redisKey, "1", REDIS_TTL_DAYS, TimeUnit.DAYS);
                log.info("[F4-Reactivation] Missatge de reactivació enviat a {} (tenant {})", identifier, tenantId);
            } catch (Exception e) {
                log.warn("[F4-Reactivation] Error enviant reactivació a {}: {}", identifier, e.getMessage());
            }
        }
    }

    private void sendReactivation(TenantChatLink chatLink, String identifier, String message) {
        if (identifier.contains("@")) {
            emailChannel.sendMessage(identifier, "Tornem a connectar!", message);
        } else if (chatLink != null && chatLink.getWhatsappMetaPhoneNumberId() != null
                && !chatLink.getWhatsappMetaPhoneNumberId().isBlank()) {
            whatsAppMetaChannel.sendMessage(chatLink.getWhatsappMetaPhoneNumberId(), identifier, message);
        } else if (chatLink != null && chatLink.getWhatsappPhoneNumber() != null
                && !chatLink.getWhatsappPhoneNumber().isBlank()) {
            whatsAppChannel.sendMessage(chatLink.getWhatsappPhoneNumber(), identifier, message);
        } else {
            log.warn("[F4-Reactivation] No hi ha canal configurat per enviar a {}", identifier);
        }
    }

    private String resolveContactName(UUID tenantId, String identifier) {
        return identifierRepository.findByTenantIdAndIdentifier(tenantId, identifier)
                .flatMap(ci -> contactRepository.findById(ci.getContactId()))
                .map(Contact::getDisplayName)
                .filter(n -> n != null && !n.isBlank())
                .orElse("client");
    }
}
