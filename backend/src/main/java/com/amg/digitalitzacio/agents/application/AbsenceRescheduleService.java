package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.booking.application.AvailabilityService;
import com.amg.digitalitzacio.booking.application.BookingService;
import com.amg.digitalitzacio.booking.domain.BookingToken;
import com.amg.digitalitzacio.booking.domain.BookingTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbsenceRescheduleService {

    private static final ZoneId TZ = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter SLOT_FMT =
            DateTimeFormatter.ofPattern("EEEE d/MM 'a les' HH:mm", new java.util.Locale("ca"));

    private final ScheduledAgentTaskRepository taskRepository;
    private final AbsenceRecordRepository absenceRepository;
    private final TenantChatLinkRepository chatLinkRepository;
    private final TenantRepository tenantRepository;
    private final NexeServiceConfigService nexeConfigService;
    private final BookingTokenRepository bookingTokenRepository;
    private final BookingService bookingService;
    private final AvailabilityService availabilityService;
    private final WhatsAppChannel whatsAppChannel;
    private final WhatsAppMetaChannel whatsAppMetaChannel;
    private final TelegramBotClient telegramBotClient;
    private final ObjectMapper objectMapper;

    /**
     * Processa la comanda /absencia del grup de Telegram.
     * Exemple: "/absencia 2026-06-10" o "/absencia demà"
     */
    @Transactional
    public String handleAbsenceCommand(UUID tenantId, String commandText, Long chatId, Long triggeredBy) {
        var dateArg = commandText.replaceFirst("(?i)/absencia\\s*", "").trim();
        LocalDate absenceDate;
        try {
            absenceDate = parseDate(dateArg);
        } catch (Exception e) {
            return "⚠️ Format de data no reconegut. Exemples:\n/absencia 2026-06-10\n/absencia avui\n/absencia demà";
        }

        var tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) return "Error intern: tenant no trobat.";
        if (!hasPhase(tenant, "F2")) return "ℹ️ La gestió d'absències requereix tenir l'Agenda (F2) activada.";

        var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
        var result = runCancellationCascade(tenantId, absenceDate, CancellationReason.ABSENCE, chatLink, tenant, triggeredBy);
        return buildSummary("📋 Absència registrada", absenceDate, result[0], result[1], hasPhase(tenant, "F4"));
    }

    /**
     * Processa la comanda /festiu del grup de Telegram.
     * Afegeix el dia als holidays de la config AGENDA i notifica les cites ja assignades.
     */
    @Transactional
    public String handleHolidayCommand(UUID tenantId, String commandText) {
        var dateArg = commandText.replaceFirst("(?i)/festiu\\s*", "").trim();
        LocalDate holidayDate;
        try {
            holidayDate = parseDate(dateArg);
        } catch (Exception e) {
            return "⚠️ Format de data no reconegut. Exemples:\n/festiu 2026-08-15\n/festiu avui\n/festiu demà";
        }

        var dateStr = holidayDate.toString();

        // 1. Afegeix a la config AGENDA
        boolean alreadyMarked = false;
        try {
            var configOpt = nexeConfigService.get(tenantId, "AGENDA");
            Map<String, Object> configMap = configOpt.isPresent()
                    ? objectMapper.readValue(configOpt.get().getConfigJson(), Map.class)
                    : new LinkedHashMap<>();
            @SuppressWarnings("unchecked")
            var holidays = (List<String>) configMap.computeIfAbsent("holidays", k -> new ArrayList<>());
            if (holidays.contains(dateStr)) {
                alreadyMarked = true;
            } else {
                holidays.add(dateStr);
                Collections.sort(holidays);
                configMap.put("holidays", holidays);
                nexeConfigService.save(tenantId, "AGENDA", objectMapper.writeValueAsString(configMap));
            }
        } catch (Exception e) {
            log.error("Error afegint festiu a config AGENDA tenant {}: {}", tenantId, e.getMessage());
            return "Error intern en afegir el festiu.";
        }

        // 2. Cancel·la les cites ja assignades aquell dia
        var tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) return "Error intern: tenant no trobat.";
        if (!hasPhase(tenant, "F2")) return "ℹ️ La gestió de festius requereix tenir l'Agenda (F2) activada.";
        if (alreadyMarked) return "ℹ️ El dia " + dateStr + " ja estava marcat com a festiu.";

        var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
        var result = runCancellationCascade(tenantId, holidayDate, CancellationReason.HOLIDAY, chatLink, tenant, null);

        if (alreadyMarked && result[0] == 0) {
            return "ℹ️ El dia " + dateStr + " ja estava marcat com a festiu i no hi havia cites pendents.";
        }
        return buildSummary("📅 Festiu registrat", holidayDate, result[0], result[1], hasPhase(tenant, "F4"));
    }

    private enum CancellationReason { ABSENCE, HOLIDAY }

    /**
     * Cancela totes les cites confirmades d'un dia, proposa horaris alternatius als clients
     * amb un nou link de reserva, i crea seguiment F4 si cal.
     * Retorna [affected, notified].
     */
    private int[] runCancellationCascade(UUID tenantId, LocalDate date, CancellationReason reason,
                                          TenantChatLink chatLink, Tenant tenant, Long triggeredBy) {
        boolean hasF4 = hasPhase(tenant, "F4");
        var dayStart = date.atStartOfDay(TZ).toInstant();
        var dayEnd   = date.plusDays(1).atStartOfDay(TZ).toInstant();

        // 1. Cancel·la les tasques de recordatori del dia (evita enviar reminders a cites cancel·lades)
        var tasks = taskRepository.findByTenantIdAndAgentSlugAndStatusAndScheduledAtBetween(
                tenantId, "appointment-reminder", ScheduledTaskStatus.PENDING, dayStart, dayEnd);
        tasks.forEach(t -> { t.setStatus(ScheduledTaskStatus.CANCELLED); taskRepository.save(t); });

        // 2. Troba i reagenda cada BookingToken confirmat del dia
        var bookings = bookingTokenRepository
                .findByTenantIdAndMeetingAtBetween(tenantId, dayStart, dayEnd)
                .stream()
                .filter(BookingToken::isConfirmed)
                .toList();

        // Precalcula slots alternatius (reutilitza per a totes les cites del dia)
        var settings = bookingService.getOrCreate(tenantId);
        var alternativeSlots = findNextSlots(tenantId, date, settings, 3);

        int notified = 0;
        for (var booking : bookings) {
            try {
                String originalTime = booking.getMeetingAt() != null
                        ? booking.getMeetingAt().atZone(TZ).toLocalTime()
                          .format(DateTimeFormatter.ofPattern("HH:mm"))
                        : "";

                // Crea un nou token de reserva per al client
                var rebookToken = createRebookToken(tenantId, booking);

                // Construeix missatge amb link + horaris suggerits
                String identifier = resolveIdentifier(booking);
                ConversationChannel channel = resolveChannel(booking, chatLink);
                if (identifier != null && channel != null) {
                    var msg = buildRescheduleMessage(
                            booking.getRecipientName() != null ? booking.getRecipientName() : booking.getLeadName(),
                            date.toString(), originalTime, reason,
                            rebookToken, alternativeSlots);
                    sendMessage(chatLink, channel, identifier, msg);
                    notified++;

                    // F4: seguiment 48h si el client no ha reboocat
                    if (hasF4) {
                        var followUpPayload = objectMapper.writeValueAsString(Map.of(
                            "identifier",    identifier,
                            "channel",       channel.name(),
                            "name",          booking.getLeadName() != null ? booking.getLeadName() : "",
                            "originalDate",  date.toString(),
                            "originalTime",  originalTime
                        ));
                        taskRepository.save(ScheduledAgentTask.builder()
                            .tenantId(tenantId)
                            .agentSlug("reschedule-pending")
                            .taskType("RESCHEDULE_FOLLOWUP")
                            .payload(followUpPayload)
                            .scheduledAt(Instant.now().plusSeconds(172800)) // 48h
                            .status(ScheduledTaskStatus.PENDING)
                            .build());
                    }
                }

                // Cancel·la la cita original
                booking.setConfirmed(false);
                booking.setMeetingAt(null);
                bookingTokenRepository.save(booking);

            } catch (Exception e) {
                log.warn("Error reagendant booking {} (tenant {}): {}", booking.getId(), tenantId, e.getMessage());
            }
        }

        absenceRepository.save(AbsenceRecord.builder()
            .tenantId(tenantId)
            .absenceDate(date)
            .triggeredBy(triggeredBy)
            .affectedCount(bookings.size())
            .notifiedCount(notified)
            .build());

        return new int[]{ bookings.size(), notified };
    }

    private BookingToken createRebookToken(UUID tenantId, BookingToken original) {
        var token = new BookingToken();
        token.setTenantId(tenantId);
        token.setLeadId(original.getLeadId());
        token.setLeadName(original.getLeadName());
        token.setLeadEmail(original.getLeadEmail());
        token.setRecipientPhone(original.getRecipientPhone());
        token.setRecipientName(original.getRecipientName());
        token.setBookingLabel(original.getBookingLabel());
        token.setToken(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        token.setExpiresAt(Instant.now().plus(Duration.ofDays(14)));
        token.setConfirmed(false);
        return bookingTokenRepository.save(token);
    }

    private List<String> findNextSlots(UUID tenantId, LocalDate excludeDate,
                                        com.amg.digitalitzacio.booking.domain.MeetingSettings settings,
                                        int maxSlots) {
        List<String> result = new ArrayList<>();
        LocalDate cursor = LocalDate.now(TZ).plusDays(1);
        LocalDate limit  = cursor.plusDays(14);
        while (!cursor.isAfter(limit) && result.size() < maxSlots) {
            if (!cursor.equals(excludeDate)) {
                var slots = availabilityService.getSlotsForDay(settings, cursor);
                for (var slot : slots) {
                    if (result.size() >= maxSlots) break;
                    result.add(cursor.atTime(slot).format(SLOT_FMT));
                }
            }
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    private String buildRescheduleMessage(String name, String date, String time,
                                           CancellationReason reason,
                                           BookingToken rebookToken,
                                           List<String> slots) {
        var greeting = name != null && !name.isBlank() ? "Hola " + name + "," : "Hola,";
        var timeStr  = time != null && !time.isBlank() ? " a les " + time : "";
        var motiu = reason == CancellationReason.HOLIDAY ? "per festiu" : "per un imprevist";
        var rebookUrl = "https://amgdl.com/ca/book/" + rebookToken.getToken();

        var sb = new StringBuilder();
        sb.append(greeting).append(" la teva cita del ").append(date).append(timeStr)
          .append(" s'ha hagut de cancel·lar ").append(motiu).append(".\n\n");
        sb.append("Pots reservar un nou dia aquí:\n🔗 ").append(rebookUrl).append("\n");

        if (!slots.isEmpty()) {
            sb.append("\nAlguns horaris disponibles els propers dies:\n");
            slots.forEach(s -> sb.append("• ").append(s).append("\n"));
        }

        sb.append("\nDisculpa les molèsties. 🙏");
        return sb.toString();
    }

    private String resolveIdentifier(BookingToken b) {
        if (b.getRecipientPhone() != null && !b.getRecipientPhone().isBlank()) return b.getRecipientPhone();
        if (b.getLeadEmail() != null && !b.getLeadEmail().isBlank()) return b.getLeadEmail();
        return null;
    }

    private ConversationChannel resolveChannel(BookingToken b, TenantChatLink chatLink) {
        if (b.getRecipientPhone() != null && !b.getRecipientPhone().isBlank()) {
            if (chatLink != null && chatLink.getWhatsappMetaPhoneNumberId() != null
                    && !chatLink.getWhatsappMetaPhoneNumberId().isBlank()) {
                return ConversationChannel.WHATSAPP_META;
            }
            return ConversationChannel.WHATSAPP;
        }
        if (b.getLeadEmail() != null && !b.getLeadEmail().isBlank()) return ConversationChannel.EMAIL;
        return null;
    }

    private LocalDate parseDate(String raw) {
        if (raw.isBlank() || raw.equalsIgnoreCase("avui")) return LocalDate.now();
        if (raw.equalsIgnoreCase("demà") || raw.equalsIgnoreCase("dema")) return LocalDate.now().plusDays(1);
        return LocalDate.parse(raw);
    }

    private boolean hasPhase(Tenant tenant, String phase) {
        return tenant.isPhaseActive(phase);
    }

    private String buildSummary(String header, LocalDate date, int affected, int notified, boolean hasF4) {
        var sb = new StringBuilder();
        sb.append(header).append(": ").append(date).append("\n");
        sb.append("• Cites afectades: ").append(affected).append("\n");
        sb.append("• Pacients notificats: ").append(notified).append("\n");
        if (affected > notified) {
            sb.append("⚠️ ").append(affected - notified).append(" cita(es) sense notificar (revisar manualment)\n");
        }
        if (hasF4 && affected > 0) {
            sb.append("• Seguiment de reprogramació programat per a 24h\n");
        } else if (!hasF4 && affected > 0) {
            sb.append("ℹ️ Activa F4 per programar el seguiment automàtic de reprogramació\n");
        }
        return sb.toString();
    }

    private void sendMessage(TenantChatLink chatLink, ConversationChannel channel,
                              String identifier, String message) {
        try {
            switch (channel) {
                case WHATSAPP -> {
                    var from = chatLink != null ? chatLink.getWhatsappPhoneNumber() : "";
                    whatsAppChannel.sendMessage(from, identifier, message);
                }
                case WHATSAPP_META -> {
                    var phoneId = chatLink != null ? chatLink.getWhatsappMetaPhoneNumberId() : "";
                    whatsAppMetaChannel.sendMessage(phoneId, identifier, message);
                }
                case TELEGRAM -> telegramBotClient.sendMessage(Long.parseLong(identifier), message);
                default -> log.warn("Canal {} no suportat per cancel·lació d'absència", channel);
            }
        } catch (Exception e) {
            log.warn("Error enviant cancel·lació a {}: {}", identifier, e.getMessage());
        }
    }
}
