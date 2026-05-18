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

    @Value("${app.agents.email.resend-api-key:}")
    private String resendApiKey;

    @Value("${app.agents.email.resend-from-address:agent@amgdigital.com}")
    private String resendFromAddress;

    public void sendMessage(String toEmail, String subject, String text) {
        if (resendApiKey.isBlank()) {
            log.warn("Resend API key not configured for Email");
            return;
        }

        try {
            RestClient client = RestClient.builder()
                    .baseUrl("https://api.resend.com")
                    .defaultHeader("Authorization", "Bearer " + resendApiKey)
                    .build();

            Map<String, Object> body = Map.of(
                    "from", resendFromAddress,
                    "to", List.of(toEmail),
                    "subject", subject,
                    "text", text
            );

            client.post()
                    .uri("/emails")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", toEmail, e.getMessage());
        }
    }
}
