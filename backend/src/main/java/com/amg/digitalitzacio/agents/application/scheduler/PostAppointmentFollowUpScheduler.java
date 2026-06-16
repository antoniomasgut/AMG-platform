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
import com.amg.digitalitzacio.booking.domain.BookingToken;
import com.amg.digitalitzacio.booking.domain.BookingTokenRepository;
import com.amg.digitalitzacio.leads.domain.Lead;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
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
 * F2 → F4: Quan una cita ha passat, si el tenant té F4 activa, envia missatge de seguiment/ressenya.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostAppointmentFollowUpScheduler {

    private static final String REDIS_KEY      = "followup:appt:%s:%s";
    private static final int    REDIS_TTL_DAYS = 60;
    private static final int    DEFAULT_FOLLOWUP_DAYS = 3;
    private static final int    WINDOW_MARGIN_HOURS   = 12;

    private final TenantRepository         tenantRepository;
    private final NexeServiceConfigService nexeConfigService;
    private final BookingTokenRepository   bookingTokenRepository;
    private final LeadRepository           leadRepository;
    private final TenantChatLinkRepository chatLinkRepository;
    private final WhatsAppChannel          whatsAppChannel;
    private final WhatsAppMetaChannel      whatsAppMetaChannel;
    private final EmailChannel             emailChannel;
    private final StringRedisTemplate      redis;
    private final ObjectMapper             objectMapper;
    private final FollowupLogRepository    followupLogRepository;

    @Scheduled(cron = "0 0 11 * * *")
    public void sendPostAppointmentFollowUps() {
        var tenants = tenantRepository.findByContractedPhasesContaining("F4");
        for (var tenant : tenants) {
            if (!tenant.isPhaseActive("F2") || !tenant.isPhaseActive("F4")) continue;
            try {
                processFollowUps(tenant.getId());
            } catch (Exception e) {
                log.warn("[F2→F4] Error processant seguiment post-cita per tenant {}: {}",
                    tenant.getId(), e.getMessage());
            }
        }
    }

    private void processFollowUps(UUID tenantId) throws Exception {
        Map<String, Object> config = readFidelitzacioConfig(tenantId);

        String reviewUrl = (String) config.get("google_reviews_url");
        int followupDays = config.get("followup_days") instanceof Number n ? n.intValue() : DEFAULT_FOLLOWUP_DAYS;
        String template  = config.get("followup_template") instanceof String t && !t.isBlank() ? t
            : "Hola {{nom}}! Esperem que la teva cita hagi anat molt bé. Si has quedat satisfet/a, t'agrairíem una ressenya: {{url_ressenya}}";

        if (reviewUrl == null || reviewUrl.isBlank()) return;

        Instant windowEnd   = Instant.now().minus(followupDays, ChronoUnit.DAYS);
        Instant windowStart = windowEnd.minus(WINDOW_MARGIN_HOURS, ChronoUnit.HOURS);

        var bookings = bookingTokenRepository.findConfirmedWithMeetingInWindow(tenantId, windowStart, windowEnd);
        if (bookings.isEmpty()) return;

        log.info("[F2→F4] {} cites per seguiment (tenant {})", bookings.size(), tenantId);

        var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);

        for (var booking : bookings) {
            String redisKey = REDIS_KEY.formatted(tenantId, booking.getId());
            if (Boolean.TRUE.equals(redis.hasKey(redisKey))) continue;

            String identifier = resolveIdentifier(booking);
            if (identifier == null) continue;

            try {
                String name    = resolveName(booking);
                String message = template
                    .replace("{{nom}}", name)
                    .replace("{{url_ressenya}}", reviewUrl);
                send(chatLink, identifier, message);
                redis.opsForValue().set(redisKey, "1", REDIS_TTL_DAYS, TimeUnit.DAYS);
                var logEntry = new FollowupLog();
                logEntry.setTenantId(tenantId);
                logEntry.setType("APPOINTMENT");
                logEntry.setEntityId(booking.getId());
                logEntry.setContact(identifier);
                followupLogRepository.save(logEntry);
                log.info("[F2→F4] Seguiment post-cita enviat a {} (booking {}, tenant {})",
                    identifier, booking.getId(), tenantId);
            } catch (Exception e) {
                log.warn("[F2→F4] Error enviant seguiment a {}: {}", identifier, e.getMessage());
            }
        }
    }

    private String resolveIdentifier(BookingToken booking) {
        if (booking.getRecipientPhone() != null && !booking.getRecipientPhone().isBlank()) {
            return booking.getRecipientPhone();
        }
        if (booking.getLeadId() != null) {
            Lead lead = leadRepository.findById(booking.getLeadId()).orElse(null);
            if (lead != null && lead.getPhone() != null && !lead.getPhone().isBlank()
                    && Boolean.TRUE.equals(lead.getHasWhatsapp())) {
                return lead.getPhone();
            }
        }
        if (booking.getLeadEmail() != null && !booking.getLeadEmail().isBlank()) {
            return booking.getLeadEmail();
        }
        return null;
    }

    private String resolveName(BookingToken booking) {
        String name = booking.getLeadName();
        if (name != null && !name.isBlank()) return name.split(" ")[0];
        return "client";
    }

    private void send(TenantChatLink chatLink, String identifier, String message) {
        if (identifier.contains("@")) {
            emailChannel.sendMessage(identifier, "Com ha anat?", message);
        } else if (chatLink != null && chatLink.getWhatsappMetaPhoneNumberId() != null
                && !chatLink.getWhatsappMetaPhoneNumberId().isBlank()) {
            whatsAppMetaChannel.sendMessage(chatLink.getWhatsappMetaPhoneNumberId(), identifier, message);
        } else if (chatLink != null && chatLink.getWhatsappPhoneNumber() != null
                && !chatLink.getWhatsappPhoneNumber().isBlank()) {
            whatsAppChannel.sendMessage(chatLink.getWhatsappPhoneNumber(), identifier, message);
        } else {
            log.warn("[F2→F4] No hi ha canal configurat per enviar a {}", identifier);
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
