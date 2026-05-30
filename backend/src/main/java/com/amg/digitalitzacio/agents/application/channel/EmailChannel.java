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
        sendMessage(toEmail, subject, text, null, null);
    }

    public void sendMessage(String toEmail, String subject, String text, String fromEmail, String fromName) {
        sendMessage(toEmail, subject, text, fromEmail, fromName, null);
    }

    public void sendMessage(String toEmail, String subject, String text, String fromEmail, String fromName, String replyTo) {
        if (brevoApiKey.isBlank()) {
            log.warn("Brevo API key not configured for Email channel");
            return;
        }

        String effectiveFrom = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : brevoFromAddress;
        String effectiveName = (fromName != null && !fromName.isBlank()) ? fromName : "AMG Digitalitzacions";

        try {
            RestClient client = RestClient.builder()
                    .baseUrl("https://api.brevo.com")
                    .defaultHeader("api-key", brevoApiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            var body = new java.util.HashMap<String, Object>();
            body.put("sender", Map.of("email", effectiveFrom, "name", effectiveName));
            body.put("to", List.of(Map.of("email", toEmail)));
            body.put("subject", subject);
            body.put("textContent", text);
            if (replyTo != null && !replyTo.isBlank()) {
                body.put("replyTo", Map.of("email", replyTo));
            }

            client.post()
                    .uri("/v3/smtp/email")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Email sent to {} from {} replyTo={} via Brevo", toEmail, effectiveFrom, replyTo);
        } catch (Exception e) {
            log.error("Error sending email to {} via Brevo: {}", toEmail, e.getMessage());
        }
    }
}
