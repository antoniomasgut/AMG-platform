package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.application.channel.EmailChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.application.tools.AgentToolRegistry;
import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import com.amg.digitalitzacio.shared.ai.ChatMessage;
import com.amg.digitalitzacio.shared.notification.NotificationEvent;
import com.amg.digitalitzacio.shared.notification.TenantNotificationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orquestrador principal de l'agent conversacional. Sense @Transactional de classe:
 * les dues transaccions curtes (preparació + persistència) es gestionen via
 * AgentTransactionalHelper per evitar que la connexió BD quedi bloquejada durant
 * les crides HTTP externes (IA, Telegram, WhatsApp, Email, Google Calendar).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationalAgentService {

    private final AgentTransactionalHelper helper;
    private final PromptBuilder promptBuilder;
    private final NexeServiceConfigService nexeServiceConfigService;
    private final TelegramBotClient telegramBotClient;
    private final WhatsAppChannel whatsAppChannel;
    private final WhatsAppMetaChannel whatsAppMetaChannel;
    private final EmailChannel emailChannel;
    private final AIProviderRouter aiProviderRouter;
    private final TenantNotificationService notificationService;
    private final GoogleCalendarService googleCalendarService;
    private final AgentToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    private static final Pattern BOOKING_TAG = Pattern.compile(
            "\\[CONFIRMA_CITA:(\\{.*?\\})]", Pattern.DOTALL);
    private static final String REMINDER_AGENT_SLUG = "appointment-reminder";

    public void handleIncoming(UUID tenantId, String identifier, ConversationChannel channel, String text) {
        try {
            log.info("Handling incoming message for tenantId={}, channel={}", tenantId, channel);

            // TX 1: valida agent + persista USER + carrega context (connexió BD alliberada en sortir)
            var prepOpt = helper.prepareIncoming(tenantId, identifier, channel, text);
            if (prepOpt.isEmpty()) {
                log.info("Agent inactiu o no configurat per tenant {} — missatge ignorat", tenantId);
                return;
            }
            var prep = prepOpt.get();

            if (prep.isNewContact()) {
                NotificationEvent evt = channel == ConversationChannel.EMAIL
                        ? NotificationEvent.EMAIL_NEW : NotificationEvent.WHATSAPP_NEW;
                String subject = channel == ConversationChannel.EMAIL ? extractEmailSubject(text) : "";
                notificationService.notify(tenantId, evt, Map.of(
                        "identifier", identifier, "subject", subject, "message", text));
            }

            // Sense TX: construcció del prompt (BD pròpia per servei)
            String systemPrompt = promptBuilder.build(tenantId, prep.context());
            var chatHistory = prep.context().recentMessages().stream()
                    .map(c -> new ChatMessage(c.getRole().name(), c.getContent()))
                    .toList();

            // Sense TX: crida HTTP a l'API d'IA (pot trigar 5-30 s)
            String model = prep.preferredModel() != null ? prep.preferredModel() : aiProviderRouter.defaultModel();
            var provider = aiProviderRouter.forModel(model);
            var tools = toolRegistry.definitions();
            String aiResponse = provider.chatWithTools(systemPrompt, chatHistory, text, tools,
                    toolRegistry.executorFor(tenantId));
            if (aiResponse == null || aiResponse.isBlank()) {
                log.warn("AI provider '{}' ha retornat resposta buida per tenant {}", provider.providerName(), tenantId);
                return;
            }

            // Sense TX: processa tag de booking + crida HTTP a Google Calendar si cal
            var booking = processBookingTag(aiResponse, tenantId, identifier, channel);
            if (booking.notificationData() != null) {
                notificationService.notify(tenantId, NotificationEvent.BOOKING_CONFIRMED, booking.notificationData());
            }

            // TX 2: persista ASSISTANT + tasca de recordatori (connexió BD alliberada en sortir)
            boolean pending = prep.agentMode() == AgentMode.HYBRID;
            helper.persistResponse(tenantId, identifier, channel, booking.cleanedResponse(),
                    pending, booking.reminderTask());

            // Sense TX: enviament via canal extern
            switch (prep.agentMode()) {
                case AUTO -> {
                    String senderId = channel == ConversationChannel.WHATSAPP_META
                            ? prep.whatsappMetaPhoneNumberId()
                            : prep.whatsappPhoneNumber();
                    sendViaChannel(senderId, identifier, channel, booking.cleanedResponse(),
                            prep.senderEmail(), prep.senderName(), prep.replyToEmail());
                }
                case HYBRID -> notifyTenantViaInternalTelegram(
                        prep.telegramChatId(), identifier, text, booking.cleanedResponse());
                case MANUAL -> notifyTenantViaInternalTelegram(
                        prep.telegramChatId(), identifier, text, null);
                default -> log.warn("Agent mode desconegut: {}", prep.agentMode());
            }

        } catch (Exception e) {
            log.error("Error handling incoming message for tenantId={}, channel={}", tenantId, channel, e);
        }
    }

    /**
     * Punt d'entrada per al canal WIDGET. Sempre retorna una resposta immediata.
     * Retorna null si l'agent no està actiu.
     */
    public String processWidgetMessage(UUID tenantId, String widgetSessionId, String text) {
        try {
            String identifier = "wgt:" + widgetSessionId;

            // TX 1
            var prepOpt = helper.prepareWidgetMessage(tenantId, identifier, text);
            if (prepOpt.isEmpty()) return null;
            var prep = prepOpt.get();

            // Sense TX: prompt + IA
            String systemPrompt = promptBuilder.build(tenantId, prep.context());
            var chatHistory = prep.context().recentMessages().stream()
                    .map(c -> new ChatMessage(c.getRole().name(), c.getContent()))
                    .toList();

            String model = prep.preferredModel() != null ? prep.preferredModel() : aiProviderRouter.defaultModel();
            var provider = aiProviderRouter.forModel(model);
            var tools = toolRegistry.definitions();
            String aiResponse = provider.chatWithTools(systemPrompt, chatHistory, text, tools,
                    toolRegistry.executorFor(tenantId));
            if (aiResponse == null || aiResponse.isBlank()) return null;

            // Sense TX: booking tag (no hi ha recordatori per WIDGET)
            var booking = processBookingTag(aiResponse, tenantId, identifier, ConversationChannel.WIDGET);
            if (booking.notificationData() != null) {
                notificationService.notify(tenantId, NotificationEvent.BOOKING_CONFIRMED, booking.notificationData());
            }

            // TX 2
            boolean pending = prep.agentMode() == AgentMode.HYBRID;
            helper.persistResponse(tenantId, identifier, ConversationChannel.WIDGET,
                    booking.cleanedResponse(), pending, booking.reminderTask());

            // Sense TX: notificació interna
            if (prep.agentMode() == AgentMode.HYBRID) {
                notifyTenantViaInternalTelegram(prep.telegramChatId(), identifier, text, booking.cleanedResponse());
            } else if (prep.agentMode() == AgentMode.MANUAL) {
                notifyTenantViaInternalTelegram(prep.telegramChatId(), identifier, text, null);
            }

            return booking.cleanedResponse();

        } catch (Exception e) {
            log.error("Error processing widget message for tenantId={}", tenantId, e);
            return null;
        }
    }

    // ── Enviament per canal ───────────────────────────────────────────────────

    private void sendViaChannel(String fromNumber, String identifier, ConversationChannel channel, String text) {
        sendViaChannel(fromNumber, identifier, channel, text, null, null, null);
    }

    private void sendViaChannel(String fromNumber, String identifier, ConversationChannel channel, String text,
                                 String senderEmail, String senderName, String replyToEmail) {
        try {
            switch (channel) {
                case WHATSAPP      -> whatsAppChannel.sendMessage(fromNumber != null ? fromNumber : "", identifier, text);
                case WHATSAPP_META -> whatsAppMetaChannel.sendMessage(fromNumber != null ? fromNumber : "", identifier, text);
                case TELEGRAM      -> telegramBotClient.sendMessage(Long.parseLong(identifier), text);
                case EMAIL         -> emailChannel.sendMessage(identifier, "Resposta del teu agent", text, senderEmail, senderName, replyToEmail);
                default            -> log.warn("Canal no suportat per enviament: {}", channel);
            }
        } catch (Exception e) {
            log.error("Error enviant via {} a {}: {}", channel, identifier, e.getMessage());
        }
    }

    private void notifyTenantViaInternalTelegram(Long telegramChatId, String identifier,
                                                  String customerMessage, String suggestedResponse) {
        if (telegramChatId == null) return;
        try {
            String msg = suggestedResponse != null
                ? "🤖 Missatge de %s:\n\n%s\n\n✍️ Resposta suggerida:\n%s\n\nAccepta o edita al portal."
                    .formatted(identifier, customerMessage, suggestedResponse)
                : "📬 Missatge de %s:\n\n%s".formatted(identifier, customerMessage);
            telegramBotClient.sendMessage(telegramChatId, msg);
        } catch (Exception e) {
            log.error("Error notificant tenant via Telegram intern: {}", e.getMessage());
        }
    }

    // ── Processament tag de booking ───────────────────────────────────────────

    private record BookingProcessed(
        String cleanedResponse,
        ScheduledAgentTask reminderTask,
        Map<String, String> notificationData
    ) {}

    private BookingProcessed processBookingTag(String response, UUID tenantId,
                                                String identifier, ConversationChannel channel) {
        Matcher m = BOOKING_TAG.matcher(response);
        if (!m.find()) return new BookingProcessed(response, null, null);

        String cleaned = BOOKING_TAG.matcher(response).replaceAll("").strip();

        try {
            Map<String, Object> booking = objectMapper.readValue(m.group(1), new TypeReference<>() {});

            // NexeServiceConfigService té @Transactional(readOnly=true) propi
            String agendaJson = nexeServiceConfigService.getAllAsMap(tenantId).get("AGENDA");
            if (agendaJson == null) return new BookingProcessed(cleaned, null, null);

            Map<String, Object> agenda = objectMapper.readValue(agendaJson, new TypeReference<>() {});
            String calType = String.valueOf(agenda.get("calendar_type"));
            if (!"google".equals(calType) && !"google_oauth".equals(calType))
                return new BookingProcessed(cleaned, null, null);

            String calId = (String) agenda.get("google_calendar_id");
            if (calId == null || calId.isBlank()) return new BookingProcessed(cleaned, null, null);

            String date = (String) booking.get("date");
            String time = (String) booking.get("time");
            if (date == null || time == null) return new BookingProcessed(cleaned, null, null);

            LocalDateTime start = LocalDateTime.parse(date + "T" + time,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            int duration = booking.get("duration") instanceof Number n ? n.intValue() : 60;
            String name  = booking.get("name")  instanceof String s ? s : "Client";
            String notes = booking.get("notes") instanceof String s ? s : "";

            // Crida HTTP externa: Google Calendar
            if ("google_oauth".equals(calType)) {
                String refreshToken = (String) agenda.get("google_refresh_token");
                googleCalendarService.createEventOAuth(refreshToken, calId, "Cita: " + name, start, duration, notes);
            } else {
                googleCalendarService.createEvent(calId, "Cita: " + name, start, duration, notes);
            }

            // Construeix la tasca de recordatori (es persistirà a TX 2)
            ScheduledAgentTask reminderTask = channel != ConversationChannel.WIDGET
                    ? buildReminderTask(tenantId, identifier, channel, name, date, time, agenda)
                    : null;

            return new BookingProcessed(cleaned, reminderTask, Map.of(
                    "nom_client", name, "data", date, "hora", time,
                    "duracio", String.valueOf(duration), "notes", notes));

        } catch (Exception e) {
            log.warn("No s'ha pogut processar el tag de booking: {}", e.getMessage());
            return new BookingProcessed(cleaned, null, null);
        }
    }

    private ScheduledAgentTask buildReminderTask(UUID tenantId, String identifier, ConversationChannel channel,
                                                  String name, String date, String time,
                                                  Map<String, Object> agenda) {
        try {
            int hoursBefore = agenda.get("reminder_hours_before") instanceof Number n ? n.intValue() : 24;
            LocalDateTime eventStart = LocalDateTime.parse(date + "T" + time,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            Instant scheduledAt = eventStart
                    .atZone(java.time.ZoneId.of("Europe/Madrid"))
                    .toInstant()
                    .minusSeconds(hoursBefore * 3600L);
            if (scheduledAt.isBefore(Instant.now())) return null;

            String payload = objectMapper.writeValueAsString(Map.of(
                    "identifier", identifier, "channel", channel.name(),
                    "name", name, "date", date, "time", time));

            return ScheduledAgentTask.builder()
                    .tenantId(tenantId)
                    .agentSlug(REMINDER_AGENT_SLUG)
                    .taskType("SEND_REMINDER")
                    .payload(payload)
                    .scheduledAt(scheduledAt)
                    .status(ScheduledTaskStatus.PENDING)
                    .build();
        } catch (Exception e) {
            log.warn("[Booking] No s'ha pogut construir el recordatori: {}", e.getMessage());
            return null;
        }
    }

    private String extractEmailSubject(String text) {
        if (text == null) return "";
        int nl = text.indexOf('\n');
        String firstLine = nl > 0 ? text.substring(0, nl).strip() : text.strip();
        return firstLine.length() > 80 ? firstLine.substring(0, 80) + "…" : firstLine;
    }
}
