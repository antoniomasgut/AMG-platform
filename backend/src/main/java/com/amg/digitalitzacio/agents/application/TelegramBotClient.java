package com.amg.digitalitzacio.agents.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Slf4j
public class TelegramBotClient {

    private final RestClient restClient;

    public TelegramBotClient(@Value("${app.agents.telegram.bot-token}") String botToken) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + botToken)
                .build();
    }

    public boolean sendMessage(Long chatId, String text) {
        try {
            var body = Map.of("chat_id", chatId, "text", text, "parse_mode", "HTML");
            restClient.post()
                    .uri("/sendMessage")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Missatge TG enviat a chat {}: {}", chatId, text.substring(0, Math.min(text.length(), 50)));
            return true;
        } catch (Exception e) {
            log.error("Error enviant missatge TG a chat {}: {}", chatId, e.getMessage());
            return false;
        }
    }
}
