package com.amg.digitalitzacio.agents.application.scheduler;

import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.amg.digitalitzacio.agents.domain.ScheduledAgentTask;
import com.amg.digitalitzacio.agents.domain.ScheduledAgentTaskRepository;
import com.amg.digitalitzacio.agents.domain.ScheduledTaskStatus;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderScheduler {

    private static final String AGENT_SLUG  = "appointment-reminder";
    private static final String TASK_TYPE   = "SEND_REMINDER";
    private static final String REDIS_KEY   = "booking:reminder:%s:%s";
    private static final int    REDIS_TTL_DAYS = 2;
    private static final int    DEFAULT_HOURS  = 24;
    private static final int    WINDOW_MARGIN_MINUTES = 60;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("Europe/Madrid"));
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Europe/Madrid"));

    private final TenantRepository            tenantRepository;
    private final NexeServiceConfigService    nexeConfigService;
    private final BookingTokenRepository      bookingTokenRepository;
    private final LeadRepository              leadRepository;
    private final ScheduledAgentTaskRepository taskRepository;
    private final StringRedisTemplate         redis;
    private final ObjectMapper                objectMapper;

    @Scheduled(fixedRate = 1_800_000)
    public void scheduleUpcomingReminders() {
        var tenants = tenantRepository.findByContractedPhasesContaining("F2");
        if (tenants.isEmpty()) return;

        log.debug("[F2-Reminder] Comprovant recordatoris de cites per {} tenants", tenants.size());

        for (var tenant : tenants) {
            // Fase suspesa o desactivada: no executar l'automatisme (nomes contractada no basta)
            if (!tenant.isPhaseActive("F2")) continue;
            try {
                processReminders(tenant.getId());
            } catch (Exception e) {
                log.warn("[F2-Reminder] Error processant recordatoris per tenant {}: {}", tenant.getId(), e.getMessage());
            }
        }
    }

    private void processReminders(UUID tenantId) throws Exception {
        int hoursBefore = readHoursBefore(tenantId);

        Instant windowFrom = Instant.now().plus(hoursBefore, ChronoUnit.HOURS)
                .minus(WINDOW_MARGIN_MINUTES, ChronoUnit.MINUTES);
        Instant windowTo   = Instant.now().plus(hoursBefore, ChronoUnit.HOURS)
                .plus(WINDOW_MARGIN_MINUTES, ChronoUnit.MINUTES);

        var bookings = bookingTokenRepository.findConfirmedWithMeetingInWindow(tenantId, windowFrom, windowTo);
        if (bookings.isEmpty()) return;

        log.info("[F2-Reminder] {} cites pròximes per tenant {}", bookings.size(), tenantId);

        for (var booking : bookings) {
            String redisKey = REDIS_KEY.formatted(tenantId, booking.getId());
            if (Boolean.TRUE.equals(redis.hasKey(redisKey))) continue;

            try {
                createReminderTask(tenantId, booking);
                redis.opsForValue().set(redisKey, "1", REDIS_TTL_DAYS, TimeUnit.DAYS);
                log.info("[F2-Reminder] Tasca de recordatori creada per booking {} (tenant {})", booking.getId(), tenantId);
            } catch (Exception e) {
                log.warn("[F2-Reminder] Error creant recordatori per booking {}: {}", booking.getId(), e.getMessage());
            }
        }
    }

    private void createReminderTask(UUID tenantId, BookingToken booking) throws Exception {
        String identifier = resolveIdentifier(tenantId, booking);
        if (identifier == null) {
            log.debug("[F2-Reminder] No s'ha trobat identificador per booking {} (tenant {})", booking.getId(), tenantId);
            return;
        }

        String channel  = identifier.contains("@") ? "EMAIL" : "WHATSAPP_META";
        String dateStr  = booking.getMeetingAt() != null ? DATE_FMT.format(booking.getMeetingAt()) : "";
        String timeStr  = booking.getMeetingAt() != null ? TIME_FMT.format(booking.getMeetingAt()) : "";

        Map<String, String> payload = Map.of(
                "identifier", identifier,
                "channel",    channel,
                "name",       booking.getLeadName() != null ? booking.getLeadName() : "",
                "date",       dateStr,
                "time",       timeStr
        );

        var task = ScheduledAgentTask.builder()
                .tenantId(tenantId)
                .agentSlug(AGENT_SLUG)
                .taskType(TASK_TYPE)
                .payload(objectMapper.writeValueAsString(payload))
                .scheduledAt(Instant.now())
                .status(ScheduledTaskStatus.PENDING)
                .build();

        taskRepository.save(task);
    }

    private String resolveIdentifier(UUID tenantId, BookingToken booking) {
        // Prioritza telèfon del lead (WhatsApp); si no, usa l'email del token
        if (booking.getLeadId() != null) {
            var lead = leadRepository.findById(booking.getLeadId()).orElse(null);
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

    private int readHoursBefore(UUID tenantId) {
        try {
            var configOpt = nexeConfigService.get(tenantId, "AGENDA");
            if (configOpt.isEmpty()) return DEFAULT_HOURS;
            Map<String, Object> config = objectMapper.readValue(configOpt.get().getConfigJson(), new TypeReference<>() {});
            return config.get("reminder_hours_before") instanceof Number n ? n.intValue() : DEFAULT_HOURS;
        } catch (Exception e) {
            return DEFAULT_HOURS;
        }
    }
}
