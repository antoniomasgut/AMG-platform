package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.NotificationOutbox;
import com.amg.digitalitzacio.agents.domain.NotificationOutboxRepository;
import com.amg.digitalitzacio.shared.exception.MissingApiKeyException;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.amg.digitalitzacio.telegram.application.TenantTelegramService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class TelegramBotClient {

    private final SystemConfigService sysConfig;
    private final TenantTelegramService tenantTelegramService;
    private final ChannelUsageService channelUsageService;
    private final NotificationOutboxRepository outboxRepository;
    private static final ObjectMapper JSON = new ObjectMapper();

    public TelegramBotClient(SystemConfigService sysConfig,
                             TenantTelegramService tenantTelegramService,
                             @Lazy ChannelUsageService channelUsageService,
                             NotificationOutboxRepository outboxRepository) {
        this.sysConfig = sysConfig;
        this.tenantTelegramService = tenantTelegramService;
        this.channelUsageService = channelUsageService;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Envia missatge usant el bot global de la plataforma.
     * Si falla, el missatge queda a la cua de reintents (notification_outbox).
     */
    public boolean sendMessage(Long chatId, String text) {
        boolean sent = sendWithToken(chatId, text, getPlatformToken());
        if (!sent) enqueue(chatId, text, null);
        return sent;
    }

    /**
     * Envia missatge del bot de plataforma amb botons inline.
     * Cada botó és un Map amb "text" + ("callback_data" o "url").
     * Exemple: List.of(Map.of("text","✓ Fet","callback_data","lead_done:ID"))
     */
    public boolean sendMessageWithButtons(Long chatId, String text, List<Map<String, String>> buttons) {
        String markup = buildInlineKeyboard(buttons);
        boolean sent = sendRaw(chatId, text, markup, getPlatformToken());
        if (!sent) enqueue(chatId, text, markup);
        return sent;
    }

    /** Envia missatge amb keyboard multi-fila. rows = llista de files, cada fila = llista de botons. */
    public boolean sendMessageWithRows(Long chatId, String text, List<List<Map<String, String>>> rows) {
        try {
            String markup = JSON.writeValueAsString(Map.of("inline_keyboard", rows));
            boolean sent = sendRaw(chatId, text, markup, getPlatformToken());
            if (!sent) enqueue(chatId, text, markup);
            return sent;
        } catch (Exception e) {
            log.warn("Error enviant missatge amb files de botons: {}", e.getMessage());
            return sendMessage(chatId, text);
        }
    }

    /** Respon un callback_query (treu l'spinner del botó i mostra un toast a Telegram). */
    public void answerCallbackQuery(String callbackQueryId, String text) {
        try {
            var client = RestClient.builder()
                    .baseUrl("https://api.telegram.org/bot" + getPlatformToken())
                    .build();
            client.post().uri("/answerCallbackQuery")
                    .body(Map.of("callback_query_id", callbackQueryId, "text", text))
                    .retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("Error responent callback query: {}", e.getMessage());
        }
    }

    /** Reintenta un missatge de l'outbox. Retorna true si s'ha enviat. */
    public boolean retryOutboxEntry(NotificationOutbox entry) {
        String token = getPlatformTokenOrNull();
        if (token == null) return false;
        return sendRaw(entry.getChatId(), entry.getMessage(), entry.getReplyMarkup(), token);
    }

    private void enqueue(Long chatId, String text, String replyMarkup) {
        try {
            var entry = new NotificationOutbox();
            entry.setChatId(chatId);
            entry.setMessage(text);
            entry.setReplyMarkup(replyMarkup);
            outboxRepository.save(entry);
            log.info("Notificació TG encuada per reintent (chat {})", chatId);
        } catch (Exception e) {
            log.error("No s'ha pogut encuar la notificació TG: {}", e.getMessage());
        }
    }

    private String buildInlineKeyboard(List<Map<String, String>> buttons) {
        try {
            return JSON.writeValueAsString(Map.of("inline_keyboard", List.of(buttons)));
        } catch (Exception e) {
            return null;
        }
    }

    /** Envia missatge usant el bot configurat per al tenant (o el global com a fallback) */
    public boolean sendMessageForTenant(UUID tenantId, Long chatId, String text) {
        String token = tenantTelegramService.getDecryptedToken(tenantId);
        if (token == null) {
            token = getPlatformTokenOrNull();
        }
        if (token == null) {
            log.warn("Cap bot Telegram configurat per tenant {} ni globalment — missatge no enviat", tenantId);
            return false;
        }
        boolean sent = sendWithToken(chatId, text, token);
        if (sent) channelUsageService.record(tenantId, ChannelUsageService.TELEGRAM);
        return sent;
    }

    private boolean sendWithToken(Long chatId, String text, String token) {
        return sendRaw(chatId, text, null, token);
    }

    private boolean sendRaw(Long chatId, String text, String replyMarkup, String token) {
        try {
            var client = RestClient.builder()
                    .baseUrl("https://api.telegram.org/bot" + token)
                    .build();
            var body = new java.util.HashMap<String, Object>();
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("parse_mode", "HTML");
            if (replyMarkup != null && !replyMarkup.isBlank()) {
                body.put("reply_markup", JSON.readTree(replyMarkup));
            }
            client.post().uri("/sendMessage").body(body).retrieve().toBodilessEntity();
            log.debug("Missatge TG enviat a chat {}", chatId);
            return true;
        } catch (Exception e) {
            log.error("Error enviant missatge TG a chat {}: {}", chatId, e.getMessage());
            return false;
        }
    }

    private String getPlatformToken() {
        String token = sysConfig.get("TELEGRAM_BOT_TOKEN");
        if (token == null || token.isBlank()) {
            throw new MissingApiKeyException("Telegram Bot", "TELEGRAM_BOT_TOKEN");
        }
        return token;
    }

    private String getPlatformTokenOrNull() {
        String token = sysConfig.get("TELEGRAM_BOT_TOKEN");
        return (token != null && !token.isBlank()) ? token : null;
    }
}
