package com.amg.digitalitzacio.agents.impl;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.application.channel.EmailChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.Agent;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.TenantChatLink;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderAgent implements Agent {

    static final String SLUG = "appointment-reminder";

    private final TenantChatLinkRepository chatLinkRepository;
    private final WhatsAppChannel whatsAppChannel;
    private final WhatsAppMetaChannel whatsAppMetaChannel;
    private final EmailChannel emailChannel;
    private final TelegramBotClient telegramBotClient;
    private final ObjectMapper objectMapper;

    @Override public String getServiceSlug()  { return SLUG; }
    @Override public String getDisplayName()  { return "Recordatori de cita"; }
    @Override public void onActivate(UUID tenantId)   {}
    @Override public void onDeactivate(UUID tenantId) {}
    @Override public String handleMessage(UUID tenantId, String text, Long chatId) { return null; }

    @Override
    public void executeTask(UUID tenantId, String taskType, String payload) {
        if (!"SEND_REMINDER".equals(taskType)) return;

        try {
            Map<String, Object> p = objectMapper.readValue(payload, new TypeReference<>() {});
            String identifier = (String) p.get("identifier");
            String channelName = (String) p.get("channel");
            String name = (String) p.get("name");
            String date = (String) p.get("date");
            String time = (String) p.get("time");

            if (identifier == null || channelName == null) {
                log.warn("[Reminder] Payload incomplet per tenant {}: {}", tenantId, payload);
                return;
            }

            ConversationChannel channel = ConversationChannel.valueOf(channelName);
            String message = buildReminderMessage(name, date, time);

            var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
            sendReminder(chatLink, channel, identifier, message);
            log.info("[Reminder] Recordatori enviat a {} via {} (tenant {})", identifier, channel, tenantId);
        } catch (Exception e) {
            log.error("[Reminder] Error enviant recordatori per tenant {}: {}", tenantId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String buildReminderMessage(String name, String date, String time) {
        return "🗓️ Recordatori de cita\n\n" +
               "Hola! Et recordem que tens una cita programada:\n" +
               "📌 " + (name != null ? name : "Cita") + "\n" +
               "📅 " + (date != null ? date : "") + " a les " + (time != null ? time : "") + "\n\n" +
               "Si necessites canviar-la o cancel·lar-la, contacta amb nosaltres. Fins aviat! 👋";
    }

    private void sendReminder(TenantChatLink chatLink, ConversationChannel channel,
                               String identifier, String message) {
        try {
            switch (channel) {
                case WHATSAPP -> {
                    String from = chatLink != null ? chatLink.getWhatsappPhoneNumber() : "";
                    whatsAppChannel.sendMessage(from != null ? from : "", identifier, message);
                }
                case WHATSAPP_META -> {
                    String phoneId = chatLink != null ? chatLink.getWhatsappMetaPhoneNumberId() : "";
                    whatsAppMetaChannel.sendMessage(phoneId != null ? phoneId : "", identifier, message);
                }
                case TELEGRAM -> telegramBotClient.sendMessage(Long.parseLong(identifier), message);
                case EMAIL -> emailChannel.sendMessage(identifier, "Recordatori de cita", message);
                default -> log.warn("[Reminder] Canal no suportat per recordatoris: {}", channel);
            }
        } catch (Exception e) {
            log.error("[Reminder] Error enviant via {}: {}", channel, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
