package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbsenceRescheduleService {

    private final ScheduledAgentTaskRepository taskRepository;
    private final AbsenceRecordRepository absenceRepository;
    private final TenantChatLinkRepository chatLinkRepository;
    private final TenantRepository tenantRepository;
    private final NexeServiceConfigService nexeConfigService;
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
     * Cancela totes les cites pendents d'un dia, notifica els clients i crea seguiments F4.
     * Retorna [affected, notified].
     */
    private int[] runCancellationCascade(UUID tenantId, LocalDate date, CancellationReason reason,
                                          TenantChatLink chatLink, Tenant tenant, Long triggeredBy) {
        boolean hasF4 = hasPhase(tenant, "F4");
        var dayStart = date.atStartOfDay(ZoneId.of("Europe/Madrid")).toInstant();
        var dayEnd   = date.plusDays(1).atStartOfDay(ZoneId.of("Europe/Madrid")).toInstant();
        var tasks = taskRepository.findByTenantIdAndAgentSlugAndStatusAndScheduledAtBetween(
                tenantId, "appointment-reminder", ScheduledTaskStatus.PENDING, dayStart, dayEnd);

        int notified = 0;
        for (var task : tasks) {
            try {
                @SuppressWarnings("unchecked")
                var payload = (Map<String, Object>) objectMapper.readValue(task.getPayload(), Map.class);
                var identifier = (String) payload.get("identifier");
                var channelName = (String) payload.get("channel");
                var name = (String) payload.get("name");
                var time = (String) payload.get("time");

                if (identifier != null && channelName != null) {
                    var msg = buildCancellationMessage(name, date.toString(), time, reason);
                    sendMessage(chatLink, ConversationChannel.valueOf(channelName), identifier, msg);
                    notified++;
                }

                task.setStatus(ScheduledTaskStatus.CANCELLED);
                taskRepository.save(task);

                if (hasF4) {
                    var followUpPayload = objectMapper.writeValueAsString(Map.of(
                        "identifier",   identifier != null ? identifier : "",
                        "channel",      channelName != null ? channelName : "WHATSAPP",
                        "name",         name != null ? name : "",
                        "originalDate", date.toString(),
                        "originalTime", time != null ? time : ""
                    ));
                    taskRepository.save(ScheduledAgentTask.builder()
                        .tenantId(tenantId)
                        .agentSlug("reschedule-pending")
                        .taskType("RESCHEDULE_FOLLOWUP")
                        .payload(followUpPayload)
                        .scheduledAt(Instant.now().plusSeconds(86400))
                        .status(ScheduledTaskStatus.PENDING)
                        .build());
                }
            } catch (Exception e) {
                log.warn("Error processant tasca {} en cancel·lació: {}", task.getId(), e.getMessage());
            }
        }

        absenceRepository.save(AbsenceRecord.builder()
            .tenantId(tenantId)
            .absenceDate(date)
            .triggeredBy(triggeredBy)
            .affectedCount(tasks.size())
            .notifiedCount(notified)
            .build());

        return new int[]{ tasks.size(), notified };
    }

    private LocalDate parseDate(String raw) {
        if (raw.isBlank() || raw.equalsIgnoreCase("avui")) return LocalDate.now();
        if (raw.equalsIgnoreCase("demà") || raw.equalsIgnoreCase("dema")) return LocalDate.now().plusDays(1);
        return LocalDate.parse(raw);
    }

    private boolean hasPhase(Tenant tenant, String phase) {
        return tenant.isPhaseActive(phase);
    }

    private String buildCancellationMessage(String name, String date, String time, CancellationReason reason) {
        var greeting = name != null && !name.isBlank() ? "Hola " + name + "," : "Hola,";
        var timeStr  = time != null && !time.isBlank() ? " a les " + time : "";
        var body = reason == CancellationReason.HOLIDAY
                ? " s'ha hagut de cancel·lar per festiu.\n\nEt contactarem en breu per trobar un nou dia. Gràcies per la comprensió. 🙏"
                : " s'ha hagut de cancel·lar per motius inesperats.\n\nEt contactarem en breu per trobar un nou dia. Disculpa les molèsties. 🙏";
        return greeting + " la teva cita del " + date + timeStr + body;
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
