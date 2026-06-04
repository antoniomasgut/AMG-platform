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
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbsenceRescheduleService {

    private final ScheduledAgentTaskRepository taskRepository;
    private final AbsenceRecordRepository absenceRepository;
    private final TenantChatLinkRepository chatLinkRepository;
    private final TenantRepository tenantRepository;
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

        var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
        boolean hasF4 = hasPhase(tenant, "F4");

        // Busca totes les tasques de recordatori pendents del dia indicat
        var dayStart = absenceDate.atStartOfDay(ZoneId.of("Europe/Madrid")).toInstant();
        var dayEnd   = absenceDate.plusDays(1).atStartOfDay(ZoneId.of("Europe/Madrid")).toInstant();
        var tasks = taskRepository.findByTenantIdAndAgentSlugAndStatusAndScheduledAtBetween(
                tenantId, "appointment-reminder", ScheduledTaskStatus.PENDING, dayStart, dayEnd);

        int notified = 0;
        for (var task : tasks) {
            try {
                var payload = objectMapper.readValue(task.getPayload(), Map.class);
                var identifier = (String) payload.get("identifier");
                var channelName = (String) payload.get("channel");
                var name = (String) payload.get("name");
                var time = (String) payload.get("time");

                if (identifier != null && channelName != null) {
                    var msg = buildCancellationMessage(name, absenceDate.toString(), time);
                    sendMessage(chatLink, ConversationChannel.valueOf(channelName), identifier, msg);
                    notified++;
                }

                // Marca la tasca original com cancel·lada
                task.setStatus(ScheduledTaskStatus.CANCELLED);
                taskRepository.save(task);

                // Crea tasca de seguiment si F4 contractada
                if (hasF4) {
                    var followUpPayload = objectMapper.writeValueAsString(Map.of(
                        "identifier",    identifier != null ? identifier : "",
                        "channel",       channelName != null ? channelName : "WHATSAPP",
                        "name",          name != null ? name : "",
                        "originalDate",  absenceDate.toString(),
                        "originalTime",  time != null ? time : ""
                    ));
                    taskRepository.save(ScheduledAgentTask.builder()
                        .tenantId(tenantId)
                        .agentSlug("reschedule-pending")
                        .taskType("RESCHEDULE_FOLLOWUP")
                        .payload(followUpPayload)
                        .scheduledAt(Instant.now().plusSeconds(86400)) // +24h
                        .status(ScheduledTaskStatus.PENDING)
                        .build());
                }
            } catch (Exception e) {
                log.warn("Error processant tasca {} en absència: {}", task.getId(), e.getMessage());
            }
        }

        // Registra l'absència
        absenceRepository.save(AbsenceRecord.builder()
            .tenantId(tenantId)
            .absenceDate(absenceDate)
            .triggeredBy(triggeredBy)
            .affectedCount(tasks.size())
            .notifiedCount(notified)
            .build());

        // Resum per al grup
        return buildSummary(absenceDate, tasks.size(), notified, hasF4);
    }

    private LocalDate parseDate(String raw) {
        if (raw.isBlank() || raw.equalsIgnoreCase("avui")) return LocalDate.now();
        if (raw.equalsIgnoreCase("demà") || raw.equalsIgnoreCase("dema")) return LocalDate.now().plusDays(1);
        return LocalDate.parse(raw);
    }

    private boolean hasPhase(Tenant tenant, String phase) {
        var phases = tenant.getContractedPhases();
        return phases != null && phases.contains(phase);
    }

    private String buildCancellationMessage(String name, String date, String time) {
        var greeting = name != null && !name.isBlank() ? "Hola " + name + "," : "Hola,";
        var timeStr  = time != null && !time.isBlank() ? " a les " + time : "";
        return greeting + " la teva cita del " + date + timeStr +
               " s'ha hagut de cancel·lar per motius inesperats.\n\n" +
               "Et contactarem en breu per trobar un nou dia. Disculpa les molèsties. 🙏";
    }

    private String buildSummary(LocalDate date, int affected, int notified, boolean hasF4) {
        var sb = new StringBuilder();
        sb.append("📋 Absència registrada: ").append(date).append("\n");
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
