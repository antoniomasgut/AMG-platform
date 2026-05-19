package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.shared.exception.MissingApiKeyException;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramBotClient {

    private final SystemConfigService sysConfig;

    private RestClient buildClient() {
        String token = sysConfig.get("TELEGRAM_BOT_TOKEN");
        if (token == null || token.isBlank()) {
            throw new MissingApiKeyException("Telegram Bot", "TELEGRAM_BOT_TOKEN");
        }
        return RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + token)
                .build();
    }

    public boolean sendMessage(Long chatId, String text) {
        try {
            var body = Map.of("chat_id", chatId, "text", text, "parse_mode", "HTML");
            buildClient().post()
                    .uri("/sendMessage")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Missatge TG enviat a chat {}: {}", chatId, text.substring(0, Math.min(text.length(), 50)));
            return true;
        } catch (MissingApiKeyException e) {
            log.warn("Bot Telegram no configurat — missatge no enviat a chat {}", chatId);
            return false;
        } catch (Exception e) {
            log.error("Error enviant missatge TG a chat {}: {}", chatId, e.getMessage());
            return false;
        }
    }
}
