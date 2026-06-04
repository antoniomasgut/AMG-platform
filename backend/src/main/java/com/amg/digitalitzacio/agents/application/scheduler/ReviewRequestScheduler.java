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
public class ReviewRequestScheduler {

    private static final String REDIS_KEY = "review:sent:%s:%s";
    private static final int REDIS_TTL_DAYS = 30;

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

    @Scheduled(cron = "0 0 10 * * *")
    public void requestReviews() {
        var tenants = tenantRepository.findByContractedPhasesContaining("F4");
        if (tenants.isEmpty()) return;

        log.info("[F4] Executant sol·licituds de ressenya per {} tenants", tenants.size());

        for (var tenant : tenants) {
            try {
                processReviewRequests(tenant.getId());
            } catch (Exception e) {
                log.warn("[F4] Error processant ressenyes per tenant {}: {}", tenant.getId(), e.getMessage());
            }
        }
    }

    private void processReviewRequests(UUID tenantId) throws Exception {
        var configOpt = nexeConfigService.get(tenantId, "FIDELITZACIO");
        if (configOpt.isEmpty()) return;

        Map<String, Object> config = objectMapper.readValue(
                configOpt.get().getConfigJson(), new TypeReference<>() {});

        // Frontend saves: google_reviews_url, followup_days, followup_template
        String reviewUrl = (String) config.get("google_reviews_url");
        if (reviewUrl == null || reviewUrl.isBlank()) return;

        int daysAfter = config.get("followup_days") instanceof Number n ? n.intValue() : 3;
        String template = config.get("followup_template") instanceof String t && !t.isBlank()
                ? t
                : "Hola! Esperem que tot hagi anat bé. Si has quedat content/a, t'agrairíem molt una ressenya: " + reviewUrl;

        Instant windowEnd   = Instant.now().minus(daysAfter, ChronoUnit.DAYS);
        Instant windowStart = windowEnd.minus(1, ChronoUnit.DAYS);

        var identifiers = conversationRepository
                .findDistinctIdentifiersByTenantIdAndCreatedAtBetween(tenantId, windowStart, windowEnd);

        if (identifiers.isEmpty()) return;

        var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);

        for (String identifier : identifiers) {
            String redisKey = REDIS_KEY.formatted(tenantId, identifier);
            if (Boolean.TRUE.equals(redis.hasKey(redisKey))) continue;

            try {
                String name = resolveContactName(tenantId, identifier);
                String message = template
                        .replace("{{nom}}", name)
                        .replace("{{url_ressenya}}", reviewUrl);
                sendReviewRequest(chatLink, identifier, message);
                redis.opsForValue().set(redisKey, "1", REDIS_TTL_DAYS, TimeUnit.DAYS);
                log.info("[F4] Sol·licitud de ressenya enviada a {} (tenant {})", identifier, tenantId);
            } catch (Exception e) {
                log.warn("[F4] Error enviant ressenya a {}: {}", identifier, e.getMessage());
            }
        }
    }

    private void sendReviewRequest(TenantChatLink chatLink, String identifier, String message) {
        if (identifier.contains("@")) {
            emailChannel.sendMessage(identifier, "Com has quedat del nostre servei?", message);
        } else if (chatLink != null && chatLink.getWhatsappMetaPhoneNumberId() != null
                && !chatLink.getWhatsappMetaPhoneNumberId().isBlank()) {
            whatsAppMetaChannel.sendMessage(chatLink.getWhatsappMetaPhoneNumberId(), identifier, message);
        } else if (chatLink != null && chatLink.getWhatsappPhoneNumber() != null
                && !chatLink.getWhatsappPhoneNumber().isBlank()) {
            whatsAppChannel.sendMessage(chatLink.getWhatsappPhoneNumber(), identifier, message);
        } else {
            log.warn("[F4] No hi ha canal configurat per enviar a {}", identifier);
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
