package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.application.channel.EmailChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.ConversationRole;
import com.amg.digitalitzacio.agents.domain.TenantAIConfig;
import com.amg.digitalitzacio.agents.domain.TenantAIConfigRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import com.amg.digitalitzacio.shared.ai.ChatMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ConversationalAgentService {

    private final ConversationService conversationService;
    private final ContactService contactService;
    private final PromptBuilder promptBuilder;
    private final TenantChatLinkRepository tenantChatLinkRepository;
    private final TenantAIConfigRepository tenantAIConfigRepository;
    private final LeadRepository leadRepository;
    private final TelegramBotClient telegramBotClient;
    private final WhatsAppChannel whatsAppChannel;
    private final WhatsAppMetaChannel whatsAppMetaChannel;
    private final EmailChannel emailChannel;
    private final AIProviderRouter aiProviderRouter;
    private final ChannelUsageService channelUsageService;
    private final NexeServiceConfigService nexeServiceConfigService;
    private final GoogleCalendarService googleCalendarService;
    private final ObjectMapper objectMapper;

    private static final Pattern BOOKING_TAG = Pattern.compile(
            "\\[CONFIRMA_CITA:(\\{.*?\\})]", Pattern.DOTALL);

    public void handleIncoming(UUID tenantId, String customerIdentifier, ConversationChannel channel, String text) {
        try {
            log.info("Handling incoming message for tenantId={}, channel={}", tenantId, channel);

            var chatLinkOpt = tenantChatLinkRepository.findByTenantId(tenantId);
            if (chatLinkOpt.isEmpty()) {
                log.warn("TenantChatLink not found for tenant {}", tenantId);
                return;
            }
            var chatLink = chatLinkOpt.get();

            if (!Boolean.TRUE.equals(chatLink.getIsActive())) {
                log.info("Agent is inactive for tenant {} — message ignored", tenantId);
                return;
            }

            contactService.findOrCreate(tenantId, channel, customerIdentifier);
            touchLeadContactAt(tenantId, channel, customerIdentifier);

            conversationService.save(tenantId, customerIdentifier, channel, ConversationRole.USER, text, false);

            var context = conversationService.loadCustomerContext(tenantId, customerIdentifier, channel);
            String systemPrompt = promptBuilder.build(tenantId, context);

            var aiConfig = tenantAIConfigRepository.findById(tenantId)
                    .orElse(TenantAIConfig.defaultFor(tenantId));
            String model = aiConfig.getPreferredModel() != null
                    ? aiConfig.getPreferredModel()
                    : aiProviderRouter.defaultModel();

            var chatHistory = context.recentMessages().stream()
                    .map(c -> new ChatMessage(c.getRole().name(), c.getContent()))
                    .toList();

            var provider = aiProviderRouter.forModel(model);
            String assistantResponse = provider.chat(systemPrompt, chatHistory, text);

            if (assistantResponse == null || assistantResponse.isBlank()) {
                log.warn("AI provider '{}' returned empty response for tenant {}", provider.providerName(), tenantId);
                return;
            }

            // Extreu el tag de booking si existeix i crea l'event al Google Calendar
            String cleanedResponse = processBookingTag(assistantResponse, tenantId);

            switch (chatLink.getAgentMode()) {
                case AUTO:
                    String senderId = channel == ConversationChannel.WHATSAPP_META
                            ? chatLink.getWhatsappMetaPhoneNumberId()
                            : chatLink.getWhatsappPhoneNumber();
                    sendViaChannel(senderId, customerIdentifier, channel, cleanedResponse,
                            aiConfig.getSenderEmail(), aiConfig.getSenderName(), aiConfig.getReplyToEmail());
                    channelUsageService.record(tenantId, channel.name());
                    conversationService.save(tenantId, customerIdentifier, channel, ConversationRole.ASSISTANT, cleanedResponse, false);
                    break;
                case HYBRID:
                    conversationService.save(tenantId, customerIdentifier, channel, ConversationRole.ASSISTANT, cleanedResponse, true);
                    notifyTenantViaInternalTelegram(chatLink.getTelegramChatId(), customerIdentifier, text, cleanedResponse);
                    break;
                case MANUAL:
                    notifyTenantViaInternalTelegram(chatLink.getTelegramChatId(), customerIdentifier, text, null);
                    break;
                default:
                    log.warn("Unknown agent mode: {}", chatLink.getAgentMode());
            }

        } catch (Exception e) {
            log.error("Error handling incoming message for tenantId={}, channel={}", tenantId, channel, e);
        }
    }

    private void sendViaChannel(String fromNumber, String customerIdentifier, ConversationChannel channel, String text) {
        sendViaChannel(fromNumber, customerIdentifier, channel, text, null, null, null);
    }

    private void sendViaChannel(String fromNumber, String customerIdentifier, ConversationChannel channel, String text,
                                String senderEmail, String senderName, String replyToEmail) {
        try {
            switch (channel) {
                case WHATSAPP      -> whatsAppChannel.sendMessage(fromNumber != null ? fromNumber : "", customerIdentifier, text);
                case WHATSAPP_META -> whatsAppMetaChannel.sendMessage(fromNumber != null ? fromNumber : "", customerIdentifier, text);
                case TELEGRAM      -> telegramBotClient.sendMessage(Long.parseLong(customerIdentifier), text);
                case EMAIL         -> emailChannel.sendMessage(customerIdentifier, "Resposta del teu agent", text, senderEmail, senderName, replyToEmail);
                default            -> log.warn("Unsupported channel: {}", channel);
            }
        } catch (Exception e) {
            log.error("Error sending via channel {} to {}: {}", channel, customerIdentifier, e.getMessage());
        }
    }

    private void notifyTenantViaInternalTelegram(Long telegramChatId, String customerIdentifier, String customerMessage, String suggestedResponse) {
        if (telegramChatId == null) return;
        try {
            String msg = suggestedResponse != null
                ? "🤖 Missatge de %s:\n\n%s\n\n✍️ Resposta suggerida:\n%s\n\nAccepta o edita al portal.".formatted(customerIdentifier, customerMessage, suggestedResponse)
                : "📬 Missatge de %s:\n\n%s".formatted(customerIdentifier, customerMessage);
            telegramBotClient.sendMessage(telegramChatId, msg);
        } catch (Exception e) {
            log.error("Error notifying tenant via Telegram: {}", e.getMessage());
        }
    }

    private void touchLeadContactAt(UUID tenantId, ConversationChannel channel, String identifier) {
        try {
            var opt = switch (channel) {
                case EMAIL -> leadRepository.findFirstByTenantIdAndEmail(tenantId, identifier);
                default    -> leadRepository.findFirstByTenantIdAndPhone(tenantId, identifier);
            };
            opt.ifPresent(lead -> {
                lead.setLastContactAt(Instant.now());
                leadRepository.save(lead);
            });
        } catch (Exception ignored) {}
    }

    /** Extreu el tag [CONFIRMA_CITA:{...}] de la resposta, crea l'event al Calendar i retorna el text net. */
    private String processBookingTag(String response, UUID tenantId) {
        Matcher m = BOOKING_TAG.matcher(response);
        if (!m.find()) return response;

        String cleaned = BOOKING_TAG.matcher(response).replaceAll("").strip();

        try {
            String json = m.group(1);
            Map<String, Object> booking = objectMapper.readValue(json, new TypeReference<>() {});

            String agendaJson = nexeServiceConfigService.getAllAsMap(tenantId).get("AGENDA");
            if (agendaJson == null) return cleaned;

            Map<String, Object> agenda = objectMapper.readValue(agendaJson, new TypeReference<>() {});
            String calType = String.valueOf(agenda.get("calendar_type"));
            if (!"google".equals(calType) && !"google_oauth".equals(calType)) return cleaned;
            String calId = (String) agenda.get("google_calendar_id");
            if (calId == null || calId.isBlank()) return cleaned;

            String date = (String) booking.get("date");
            String time = (String) booking.get("time");
            if (date == null || time == null) return cleaned;

            LocalDateTime start = LocalDateTime.parse(date + "T" + time,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            int duration = booking.get("duration") instanceof Number n ? n.intValue() : 60;
            String name  = booking.get("name") instanceof String s ? s : "Client";
            String notes = booking.get("notes") instanceof String s ? s : "";

            if ("google_oauth".equals(calType)) {
                String refreshToken = (String) agenda.get("google_refresh_token");
                googleCalendarService.createEventOAuth(refreshToken, calId, "Cita: " + name, start, duration, notes);
            } else {
                googleCalendarService.createEvent(calId, "Cita: " + name, start, duration, notes);
            }
        } catch (Exception e) {
            log.warn("Could not process booking tag: {}", e.getMessage());
        }
        return cleaned;
    }
}
