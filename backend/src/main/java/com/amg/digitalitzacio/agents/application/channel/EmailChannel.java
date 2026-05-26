package com.amg.digitalitzacio.agents.application.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class EmailChannel {

    @Value("${app.agents.email.brevo-api-key:}")
    private String brevoApiKey;

    @Value("${app.agents.email.brevo-from-address:noreply@amgdl.com}")
    private String brevoFromAddress;

    public void sendMessage(String toEmail, String subject, String text) {
        if (brevoApiKey.isBlank()) {
            log.warn("Brevo API key not configured for Email channel");
            return;
        }

        try {
            RestClient client = RestClient.builder()
                    .baseUrl("https://api.brevo.com")
                    .defaultHeader("api-key", brevoApiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            Map<String, Object> body = Map.of(
                    "sender", Map.of("email", brevoFromAddress, "name", "AMG Digitalització"),
                    "to", List.of(Map.of("email", toEmail)),
                    "subject", subject,
                    "textContent", text
            );

            client.post()
                    .uri("/v3/smtp/email")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Email sent to {} via Brevo", toEmail);
        } catch (Exception e) {
            log.error("Error sending email to {} via Brevo: {}", toEmail, e.getMessage());
        }
    }
}
