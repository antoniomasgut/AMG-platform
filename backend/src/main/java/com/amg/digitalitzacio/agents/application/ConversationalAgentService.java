package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.application.channel.EmailChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.application.tools.AgentToolRegistry;
import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.shared.ai.AIProvider;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import com.amg.digitalitzacio.shared.ai.ToolCall;
import com.amg.digitalitzacio.shared.ai.ToolDefinition;
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
import java.util.List;
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
    private final TokenBudgetService tokenBudgetService;
    private final ChannelUsageService channelUsageService;
    private final ObjectMapper objectMapper;
    private final com.amg.digitalitzacio.billing.application.PostAcceptanceService postAcceptanceService;

    private static final Pattern BOOKING_TAG = Pattern.compile(
            "\\[CONFIRMA_CITA:(\\{.*?\\})]", Pattern.DOTALL);
    private static final String REMINDER_AGENT_SLUG = "appointment-reminder";
    // Límit per evitar DoS via prompts enormes a l'API d'IA
    private static final int MAX_INCOMING_TEXT_LENGTH = 4000;

    public void handleIncoming(UUID tenantId, String identifier, ConversationChannel channel, String text) {
        if (text != null && text.length() > MAX_INCOMING_TEXT_LENGTH) {
            log.warn("Missatge entrant massa llarg ({} chars) de {} — truncat", text.length(), identifier);
            text = text.substring(0, MAX_INCOMING_TEXT_LENGTH);
        }
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
            String aiResponse = callAI(tenantId, prep, systemPrompt, chatHistory, text);
            if (aiResponse == null || aiResponse.isBlank()) {
                log.warn("Cap provider ha retornat resposta per tenant {}", tenantId);
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
                case HYBRID -> notifyOperator(tenantId, prep, identifier, text, booking.cleanedResponse());
                case MANUAL -> notifyOperator(tenantId, prep, identifier, text, null);
                default -> log.warn("Agent mode desconegut: {}", prep.agentMode());
            }

        } catch (Exception e) {
            log.error("Error handling incoming message for tenantId={}, channel={}", tenantId, channel, e);
            postAcceptanceService.onAgentError(tenantId, e.getMessage());
        }
    }

    /**
     * Genera un esborrany de resposta per a un DM (Messenger/Instagram) sense enviar-lo (Mòdul 56 F2).
     * Persista el missatge USER i la resposta ASSISTANT com a pendent d'aprovació.
     * Retorna el text de l'esborrany, o null si l'agent no està actiu o no hi ha resposta.
     */
    public String generateDmDraft(UUID tenantId, String identifier, ConversationChannel channel, String text) {
        try {
            if (text != null && text.length() > MAX_INCOMING_TEXT_LENGTH) {
                text = text.substring(0, MAX_INCOMING_TEXT_LENGTH);
            }
            var prepOpt = helper.prepareIncoming(tenantId, identifier, channel, text);
            if (prepOpt.isEmpty()) return null;
            var prep = prepOpt.get();

            String systemPrompt = promptBuilder.build(tenantId, prep.context());
            var chatHistory = prep.context().recentMessages().stream()
                    .map(c -> new ChatMessage(c.getRole().name(), c.getContent()))
                    .toList();

            String aiResponse = callAI(tenantId, prep, systemPrompt, chatHistory, text);
            if (aiResponse == null || aiResponse.isBlank()) return null;

            var booking = processBookingTag(aiResponse, tenantId, identifier, channel);
            helper.persistResponse(tenantId, identifier, channel, booking.cleanedResponse(), true, booking.reminderTask());
            return booking.cleanedResponse();
        } catch (Exception e) {
            log.error("Error generant esborrany DM per tenant {}: {}", tenantId, e.getMessage());
            return null;
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
            if (prepOpt.isEmpty()) {
                log.warn("[PortalChat] Tenant {} — agent no actiu o sense TenantChatLink", tenantId);
                return null;
            }
            var prep = prepOpt.get();

            // Sense TX: prompt + IA
            String systemPrompt = promptBuilder.build(tenantId, prep.context());
            var chatHistory = prep.context().recentMessages().stream()
                    .map(c -> new ChatMessage(c.getRole().name(), c.getContent()))
                    .toList();

            log.info("[PortalChat] Tenant {} — model={} mode={} historial={} missatges",
                    tenantId, prep.preferredModel(), prep.agentMode(), chatHistory.size());

            String aiResponse = callAI(tenantId, prep, systemPrompt, chatHistory, text);
            if (aiResponse == null || aiResponse.isBlank()) {
                log.warn("[PortalChat] Tenant {} — callAI ha retornat null/buit", tenantId);
                return null;
            }

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
                notifyOperator(tenantId, prep, identifier, text, booking.cleanedResponse());
            } else if (prep.agentMode() == AgentMode.MANUAL) {
                notifyOperator(tenantId, prep, identifier, text, null);
            }

            return booking.cleanedResponse();

        } catch (Exception e) {
            log.error("Error processing widget message for tenantId={}", tenantId, e);
            return null;
        }
    }

    // ── Crida a la IA amb budget check i fallback ─────────────────────────────

    private String callAI(UUID tenantId, IncomingPreparation prep,
                          String systemPrompt, List<ChatMessage> history, String text) {
        boolean budgetOk = tokenBudgetService.canCallAI(tenantId);
        String primaryModel  = prep.preferredModel() != null ? prep.preferredModel() : aiProviderRouter.defaultModel();
        String fallbackModel = prep.fallbackModel();

        if (!budgetOk && fallbackModel == null) {
            log.warn("[Budget] Tenant {} sense pressupost ni fallback — missatge ignorat", tenantId);
            return null;
        }

        String modelToUse = budgetOk ? primaryModel : fallbackModel;
        if (!budgetOk) log.info("[Budget] Tenant {} usant fallback model '{}'", tenantId, modelToUse);

        var tools = toolRegistry.definitions();
        var executor = toolRegistry.executorFor(tenantId);

        var response = tryCallAI(modelToUse, systemPrompt, history, text, tools, executor);
        if (response != null && response.text() != null) {
            tokenBudgetService.record(tenantId, modelToUse, "chat", response);
            return response.text();
        }

        log.warn("[AI] Tenant {} — model '{}' ha retornat null/buit (tools={})",
                tenantId, modelToUse, tools.size());

        if (fallbackModel != null && !fallbackModel.equals(modelToUse)) {
            log.info("[AI] Reintentant amb fallback model '{}' per tenant {}", fallbackModel, tenantId);
            var fallbackResponse = tryCallAI(fallbackModel, systemPrompt, history, text, tools, executor);
            if (fallbackResponse != null && fallbackResponse.text() != null) {
                tokenBudgetService.record(tenantId, fallbackModel, "chat-fallback", fallbackResponse);
                return fallbackResponse.text();
            }
        }
        return null;
    }

    private AIProvider.AIResponse tryCallAI(String model, String systemPrompt, List<ChatMessage> history,
                                             String text, List<ToolDefinition> tools,
                                             java.util.function.Function<ToolCall, String> executor) {
        try {
            return aiProviderRouter.forModel(model).chatWithToolsTracked(systemPrompt, history, text, tools, executor);
        } catch (Exception e) {
            log.warn("[AI] Error amb model '{}': {}", model, e.getMessage());
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

    private void notifyOperator(UUID tenantId, IncomingPreparation prep, String identifier,
                                 String customerMessage, String suggestedResponse) {
        String msg = suggestedResponse != null
            ? "🤖 Missatge de %s:\n\n%s\n\n✍️ Resposta suggerida:\n%s\n\nAccepta o edita al portal."
                .formatted(identifier, customerMessage, suggestedResponse)
            : "📬 Missatge de %s:\n\n%s".formatted(identifier, customerMessage);
        try {
            if ("WHATSAPP".equals(prep.notificationChannel()) && prep.operatorPhone() != null) {
                String fromNumber = prep.whatsappPhoneNumber() != null ? prep.whatsappPhoneNumber() : "";
                whatsAppChannel.sendMessage(fromNumber, prep.operatorPhone(), msg);
                // Notificació WA a l'operador compta al budget igual que qualsevol missatge sortint
                channelUsageService.record(tenantId, ChannelUsageService.WHATSAPP);
            } else if (prep.telegramChatId() != null) {
                telegramBotClient.sendMessage(prep.telegramChatId(), msg);
                // Telegram és gratuït — no compta al budget WA
            }
        } catch (Exception e) {
            log.error("Error notificant operador: {}", e.getMessage());
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
