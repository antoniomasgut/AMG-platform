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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * F4 — Campanyes estacionals: missatges puntuals (estiu, Nadal, rebaixes...)
 * definits pel tenant a la config FIDELITZACIO (`seasonal_campaigns`).
 * Cada campanya té data d'enviament i missatge; s'envia als contactes amb
 * activitat en els últims 12 mesos, un sol cop (dedupe Redis per campanya).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SeasonalCampaignScheduler {

    private static final String REDIS_KEY = "seasonal:sent:%s:%s:%s"; // tenant : campanya : identificador
    private static final int REDIS_TTL_DAYS = 90;
    private static final int AUDIENCE_MONTHS = 12;

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

    @Scheduled(cron = "0 30 10 * * *")
    public void sendSeasonalCampaigns() {
        var tenants = tenantRepository.findByContractedPhasesContaining("F4");
        if (tenants.isEmpty()) return;

        for (var tenant : tenants) {
            // Fase suspesa o desactivada: no executar l'automatisme
            if (!tenant.isPhaseActive("F4")) continue;
            try {
                processCampaigns(tenant.getId());
            } catch (Exception e) {
                log.warn("[F4-Seasonal] Error processant campanyes per tenant {}: {}",
                        tenant.getId(), e.getMessage());
            }
        }
    }

    private void processCampaigns(UUID tenantId) throws Exception {
        var configOpt = nexeConfigService.get(tenantId, "FIDELITZACIO");
        if (configOpt.isEmpty()) return;

        Map<String, Object> config = objectMapper.readValue(
                configOpt.get().getConfigJson(), new TypeReference<>() {});

        if (!(config.get("seasonal_campaigns") instanceof List<?> campaigns) || campaigns.isEmpty()) return;

        String today = LocalDate.now().toString(); // YYYY-MM-DD
        for (Object c : campaigns) {
            if (!(c instanceof Map<?, ?> campaign)) continue;
            String sendDate = campaign.get("send_date") instanceof String d ? d : null;
            String message  = campaign.get("message") instanceof String m ? m : null;
            String id       = campaign.get("id") instanceof String i ? i : null;
            String name     = campaign.get("name") instanceof String n ? n : "campanya";
            if (sendDate == null || message == null || message.isBlank() || id == null) continue;
            if (!today.equals(sendDate)) continue;

            sendCampaign(tenantId, id, name, message);
        }
    }

    private void sendCampaign(UUID tenantId, String campaignId, String campaignName, String template) {
        // Audiència: contactes amb activitat els últims 12 mesos
        Instant windowEnd = Instant.now();
        Instant windowStart = windowEnd.minus(AUDIENCE_MONTHS * 30L, ChronoUnit.DAYS);
        var identifiers = conversationRepository
                .findIdentifiersWithLastUserMessageInWindow(tenantId, windowStart, windowEnd);
        if (identifiers.isEmpty()) {
            log.info("[F4-Seasonal] Campanya '{}' sense audiència (tenant {})", campaignName, tenantId);
            return;
        }

        var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
        int sent = 0;
        for (String identifier : identifiers) {
            String redisKey = REDIS_KEY.formatted(tenantId, campaignId, identifier);
            if (Boolean.TRUE.equals(redis.hasKey(redisKey))) continue;
            try {
                String contactName = resolveContactName(tenantId, identifier);
                String message = template.replace("{{nom}}", contactName);
                deliver(chatLink, identifier, campaignName, message);
                redis.opsForValue().set(redisKey, "1", REDIS_TTL_DAYS, TimeUnit.DAYS);
                sent++;
            } catch (Exception e) {
                log.warn("[F4-Seasonal] Error enviant campanya a {}: {}", identifier, e.getMessage());
            }
        }
        log.info("[F4-Seasonal] Campanya '{}' enviada a {}/{} contactes (tenant {})",
                campaignName, sent, identifiers.size(), tenantId);
    }

    private void deliver(TenantChatLink chatLink, String identifier, String subject, String message) {
        if (identifier.contains("@")) {
            emailChannel.sendMessage(identifier, subject, message);
        } else if (chatLink != null && chatLink.getWhatsappMetaPhoneNumberId() != null
                && !chatLink.getWhatsappMetaPhoneNumberId().isBlank()) {
            whatsAppMetaChannel.sendMessage(chatLink.getWhatsappMetaPhoneNumberId(), identifier, message);
        } else if (chatLink != null && chatLink.getWhatsappPhoneNumber() != null
                && !chatLink.getWhatsappPhoneNumber().isBlank()) {
            whatsAppChannel.sendMessage(chatLink.getWhatsappPhoneNumber(), identifier, message);
        } else {
            log.warn("[F4-Seasonal] Sense canal per enviar a {}", identifier);
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
