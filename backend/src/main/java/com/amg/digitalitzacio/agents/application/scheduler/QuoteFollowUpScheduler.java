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
public class QuoteFollowUpScheduler {

    private static final String REDIS_KEY = "quote:followup:%s:%s";
    private static final int REDIS_TTL_DAYS = 30;
    // Lookback window: 3-day buffer around the configured followup day
    private static final int WINDOW_DAYS = 3;

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

    @Scheduled(cron = "0 0 11 * * *")
    public void sendQuoteFollowUps() {
        var tenants = tenantRepository.findByContractedPhasesContaining("F3");
        if (tenants.isEmpty()) return;

        log.info("[F3] Executant follow-up de pressupostos per {} tenants", tenants.size());

        for (var tenant : tenants) {
            try {
                processFollowUps(tenant.getId());
            } catch (Exception e) {
                log.warn("[F3] Error processant follow-ups per tenant {}: {}", tenant.getId(), e.getMessage());
            }
        }
    }

    private void processFollowUps(UUID tenantId) throws Exception {
        var configOpt = nexeConfigService.get(tenantId, "PRESSUPOSTOS");
        if (configOpt.isEmpty()) return;

        Map<String, Object> config = objectMapper.readValue(
                configOpt.get().getConfigJson(), new TypeReference<>() {});

        Object enabled = config.get("quote_followup_enabled");
        if (!Boolean.TRUE.equals(enabled) && !"true".equals(String.valueOf(enabled))) return;

        int followupDays = config.get("quote_followup_days") instanceof Number n ? n.intValue() : 3;
        String template = config.get("quote_followup_message") instanceof String t && !t.isBlank()
                ? t
                : "Hola {{nom}}! Et volem recordar que fa uns dies et vam enviar informació sobre el nostre servei. " +
                  "Estem aquí si tens alguna pregunta o vols continuar. 😊";

        // Look for quotes sent between (followupDays + WINDOW_DAYS) and followupDays ago
        Instant cutoff = Instant.now().minus(followupDays, ChronoUnit.DAYS);
        Instant from   = cutoff.minus(WINDOW_DAYS, ChronoUnit.DAYS);

        var identifiers = conversationRepository.findIdentifiersWithUnansweredQuote(tenantId, from, cutoff);
        if (identifiers.isEmpty()) return;

        var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);

        for (String identifier : identifiers) {
            String redisKey = REDIS_KEY.formatted(tenantId, identifier);
            if (Boolean.TRUE.equals(redis.hasKey(redisKey))) continue;

            try {
                String name = resolveContactName(tenantId, identifier);
                String message = template.replace("{{nom}}", name);
                sendFollowUp(chatLink, identifier, message);
                redis.opsForValue().set(redisKey, "1", REDIS_TTL_DAYS, TimeUnit.DAYS);
                log.info("[F3] Follow-up de pressupost enviat a {} (tenant {})", identifier, tenantId);
            } catch (Exception e) {
                log.warn("[F3] Error enviant follow-up a {}: {}", identifier, e.getMessage());
            }
        }
    }

    private void sendFollowUp(TenantChatLink chatLink, String identifier, String message) {
        if (identifier.contains("@")) {
            emailChannel.sendMessage(identifier, "Seguiment del teu pressupost", message);
        } else if (chatLink != null && chatLink.getWhatsappMetaPhoneNumberId() != null
                && !chatLink.getWhatsappMetaPhoneNumberId().isBlank()) {
            whatsAppMetaChannel.sendMessage(chatLink.getWhatsappMetaPhoneNumberId(), identifier, message);
        } else if (chatLink != null && chatLink.getWhatsappPhoneNumber() != null
                && !chatLink.getWhatsappPhoneNumber().isBlank()) {
            whatsAppChannel.sendMessage(chatLink.getWhatsappPhoneNumber(), identifier, message);
        } else {
            log.warn("[F3] No hi ha canal configurat per enviar a {}", identifier);
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
