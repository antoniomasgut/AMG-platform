package com.amg.digitalitzacio.agents.application.scheduler;

import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.amg.digitalitzacio.agents.application.channel.EmailChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.FollowupLog;
import com.amg.digitalitzacio.agents.domain.FollowupLogRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLink;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.documents.delivery.domain.SecureDocumentToken;
import com.amg.digitalitzacio.documents.delivery.domain.SecureDocumentTokenRepository;
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

/**
 * F3 → F4: Quan un pressupost ha estat acceptat fa X dies, envia seguiment/ressenya.
 * Per a sectors sense F2 (pintor, electricista...), és l'únic mecanisme de seguiment postvenda automàtic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostServiceFollowUpScheduler {

    private static final String REDIS_KEY      = "followup:service:%s:%s";
    private static final int    REDIS_TTL_DAYS = 60;
    private static final int    DEFAULT_FOLLOWUP_DAYS = 7;
    private static final int    WINDOW_MARGIN_HOURS   = 12;

    private final TenantRepository              tenantRepository;
    private final NexeServiceConfigService      nexeConfigService;
    private final SecureDocumentTokenRepository documentTokenRepository;
    private final TenantChatLinkRepository      chatLinkRepository;
    private final WhatsAppChannel               whatsAppChannel;
    private final WhatsAppMetaChannel           whatsAppMetaChannel;
    private final EmailChannel                  emailChannel;
    private final StringRedisTemplate           redis;
    private final ObjectMapper                  objectMapper;
    private final FollowupLogRepository         followupLogRepository;

    @Scheduled(cron = "0 30 11 * * *")
    public void sendPostServiceFollowUps() {
        var tenants = tenantRepository.findByContractedPhasesContaining("F4");
        for (var tenant : tenants) {
            if (!tenant.isPhaseActive("F3") || !tenant.isPhaseActive("F4")) continue;
            try {
                processFollowUps(tenant.getId());
            } catch (Exception e) {
                log.warn("[F3→F4] Error processant seguiment post-servei per tenant {}: {}",
                    tenant.getId(), e.getMessage());
            }
        }
    }

    private void processFollowUps(UUID tenantId) throws Exception {
        Map<String, Object> config = readFidelitzacioConfig(tenantId);

        String reviewUrl = (String) config.get("google_reviews_url");
        int followupDays = config.get("followup_days") instanceof Number n ? n.intValue() : DEFAULT_FOLLOWUP_DAYS;
        String template  = config.get("followup_template") instanceof String t && !t.isBlank() ? t
            : "Hola {{nom}}! Esperem que tot hagi anat bé amb el servei. Si has quedat content/a, t'agrairíem molt una ressenya: {{url_ressenya}}";

        if (reviewUrl == null || reviewUrl.isBlank()) return;

        Instant windowEnd   = Instant.now().minus(followupDays, ChronoUnit.DAYS);
        Instant windowStart = windowEnd.minus(WINDOW_MARGIN_HOURS, ChronoUnit.HOURS);

        var docs = documentTokenRepository.findBudgetsAcceptedBetween(tenantId, windowStart, windowEnd);
        if (docs.isEmpty()) return;

        log.info("[F3→F4] {} pressupostos acceptats per seguiment (tenant {})", docs.size(), tenantId);

        var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);

        for (var doc : docs) {
            String redisKey = REDIS_KEY.formatted(tenantId, doc.getId());
            if (Boolean.TRUE.equals(redis.hasKey(redisKey))) continue;

            String identifier = resolveIdentifier(doc);
            if (identifier == null) continue;

            try {
                String name    = doc.getRecipientName() != null ? doc.getRecipientName().split(" ")[0] : "client";
                String message = template
                    .replace("{{nom}}", name)
                    .replace("{{url_ressenya}}", reviewUrl);
                send(chatLink, identifier, message);
                redis.opsForValue().set(redisKey, "1", REDIS_TTL_DAYS, TimeUnit.DAYS);
                var logEntry = new FollowupLog();
                logEntry.setTenantId(tenantId);
                logEntry.setType("SERVICE");
                logEntry.setEntityId(doc.getId());
                logEntry.setContact(identifier);
                followupLogRepository.save(logEntry);
                log.info("[F3→F4] Seguiment post-servei enviat a {} (doc {}, tenant {})",
                    identifier, doc.getId(), tenantId);
            } catch (Exception e) {
                log.warn("[F3→F4] Error enviant seguiment a {}: {}", identifier, e.getMessage());
            }
        }
    }

    private String resolveIdentifier(SecureDocumentToken doc) {
        if (doc.getRecipientPhone() != null && !doc.getRecipientPhone().isBlank()) {
            return doc.getRecipientPhone();
        }
        if (doc.getRecipientEmail() != null && !doc.getRecipientEmail().isBlank()) {
            return doc.getRecipientEmail();
        }
        return null;
    }

    private void send(TenantChatLink chatLink, String identifier, String message) {
        if (identifier.contains("@")) {
            emailChannel.sendMessage(identifier, "Com ha anat el servei?", message);
        } else if (chatLink != null && chatLink.getWhatsappMetaPhoneNumberId() != null
                && !chatLink.getWhatsappMetaPhoneNumberId().isBlank()) {
            whatsAppMetaChannel.sendMessage(chatLink.getWhatsappMetaPhoneNumberId(), identifier, message);
        } else if (chatLink != null && chatLink.getWhatsappPhoneNumber() != null
                && !chatLink.getWhatsappPhoneNumber().isBlank()) {
            whatsAppChannel.sendMessage(chatLink.getWhatsappPhoneNumber(), identifier, message);
        } else {
            log.warn("[F3→F4] No hi ha canal configurat per enviar a {}", identifier);
        }
    }

    private Map<String, Object> readFidelitzacioConfig(UUID tenantId) {
        try {
            var opt = nexeConfigService.get(tenantId, "FIDELITZACIO");
            if (opt.isEmpty()) return Map.of();
            return objectMapper.readValue(opt.get().getConfigJson(), new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
