package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.shared.exception.MissingApiKeyException;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.amg.digitalitzacio.telegram.application.TenantTelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramBotClient {

    private final SystemConfigService sysConfig;
    private final TenantTelegramService tenantTelegramService;
    private final ChannelUsageService channelUsageService;

    /** Envia missatge usant el bot global de la plataforma */
    public boolean sendMessage(Long chatId, String text) {
        return sendWithToken(chatId, text, getPlatformToken());
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
        try {
            var client = RestClient.builder()
                    .baseUrl("https://api.telegram.org/bot" + token)
                    .build();
            var body = Map.of("chat_id", chatId, "text", text, "parse_mode", "HTML");
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
