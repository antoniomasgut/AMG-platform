package com.amg.digitalitzacio.shared.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@Slf4j
public class TelegramNotifier {

    private final WebClient webClient;
    private final String botToken;
    private final String chatId;

    public TelegramNotifier(
            WebClient.Builder webClientBuilder,
            @Value("${app.infraops.telegram.bot-token:}") String botToken,
            @Value("${app.infraops.telegram.chat-id:}") String chatId) {
        this.webClient = webClientBuilder.baseUrl("https://api.telegram.org").build();
        this.botToken = botToken;
        this.chatId = chatId;
    }

    public boolean isConfigured() {
        return botToken != null && !botToken.isBlank()
                && chatId != null && !chatId.isBlank();
    }

    public void send(String message) {
        if (!isConfigured()) {
            log.debug("TelegramNotifier: no configurat (BOT_TOKEN o CHAT_ID buit)");
            return;
        }
        try {
            webClient.post()
                    .uri("/bot{token}/sendMessage", botToken)
                    .bodyValue(Map.of(
                            "chat_id", chatId,
                            "text", message,
                            "parse_mode", "HTML"))
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(e -> log.error("TelegramNotifier: error enviant missatge: {}", e.getMessage()))
                    .subscribe(response -> log.debug("TelegramNotifier: missatge enviat OK"));
        } catch (Exception e) {
            log.error("TelegramNotifier: excepció inesperada: {}", e.getMessage());
        }
    }
}
