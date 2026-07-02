package com.amg.digitalitzacio.auth.application;

import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final SystemConfigService systemConfigService;

    public void sendEmail(String to, String subject, String textContent) {
        String apiKey = systemConfigService.get("BREVO_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.error("BREVO_API_KEY not configured — email not sent to {}", to);
            return;
        }
        String fromAddress = systemConfigService.get("BREVO_SENDER_EMAIL");
        if (fromAddress == null || fromAddress.isBlank()) fromAddress = "noreply@amgdl.com";

        try {
            RestClient client = RestClient.builder()
                    .baseUrl("https://api.brevo.com")
                    .defaultHeader("api-key", apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            Map<String, Object> body = Map.of(
                    "sender", Map.of("email", fromAddress, "name", "AMG Digitalització"),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "textContent", textContent
            );

            client.post().uri("/v3/smtp/email").body(body).retrieve().toBodilessEntity();
            log.info("Email sent to {} — subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email send failed: " + e.getMessage(), e);
        }
    }

    public void sendEmailHtml(String to, String subject, String htmlContent) {
        String apiKey = systemConfigService.get("BREVO_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.error("BREVO_API_KEY not configured — email not sent to {}", to);
            return;
        }
        String fromAddress = systemConfigService.get("BREVO_SENDER_EMAIL");
        if (fromAddress == null || fromAddress.isBlank()) fromAddress = "noreply@amgdl.com";

        try {
            RestClient client = RestClient.builder()
                    .baseUrl("https://api.brevo.com")
                    .defaultHeader("api-key", apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            Map<String, Object> body = Map.of(
                    "sender", Map.of("email", fromAddress, "name", "AMG Digitalització"),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", htmlContent
            );

            client.post().uri("/v3/smtp/email").body(body).retrieve().toBodilessEntity();
            log.info("HTML email sent to {} — subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email send failed: " + e.getMessage(), e);
        }
    }

    public void sendPasswordResetEmail(String to, String resetLink) {
        String apiKey = systemConfigService.get("BREVO_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            log.error("BREVO_API_KEY not configured — password reset email not sent to {}", to);
            return;
        }
        String fromAddress = systemConfigService.get("BREVO_SENDER_EMAIL");
        if (fromAddress == null || fromAddress.isBlank()) {
            fromAddress = "noreply@amgdl.com";
        }

        String text = """
                Has sol·licitat restablir la teva contrasenya.

                Fes clic en aquest enllaç per restablir-la:
                %s

                Si no has sol·licitat aquest canvi, ignora aquest missatge.

                L'enllaç és vàlid durant 30 minuts.
                """.formatted(resetLink);

        try {
            RestClient client = RestClient.builder()
                    .baseUrl("https://api.brevo.com")
                    .defaultHeader("api-key", apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            Map<String, Object> body = Map.of(
                    "sender", Map.of("email", fromAddress, "name", "AMG Digitalització"),
                    "to", List.of(Map.of("email", to)),
                    "subject", "AMG Digitalització — Recuperació de contrasenya",
                    "textContent", text
            );

            client.post()
                    .uri("/v3/smtp/email")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Password reset email sent to {} via Brevo", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {} via Brevo: {}", to, e.getMessage());
        }
    }
}
