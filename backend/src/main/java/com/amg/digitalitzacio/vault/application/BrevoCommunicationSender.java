package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.amg.digitalitzacio.vault.domain.CommunicationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrevoCommunicationSender implements CommunicationSender {

    private final SystemConfigService systemConfig;
    private final HttpClient http = HttpClient.newHttpClient();

    @Override
    public boolean send(String recipient, String subject, String body) {
        try {
            var apiKey = systemConfig.get("BREVO_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("BREVO_API_KEY not configured, cannot send email");
                return false;
            }
            var senderEmail = systemConfig.get("BREVO_SENDER_EMAIL");
            if (senderEmail == null || senderEmail.isBlank()) {
                senderEmail = "noreply@amgdl.com";
            }

            var json = """
                {
                    "sender": {"email": "%s"},
                    "to": [{"email": "%s"}],
                    "subject": "%s",
                    "htmlContent": "<p>%s</p>"
                }
                """.formatted(senderEmail, recipient, escape(subject), escape(body));

            var request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("api-key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201 || response.statusCode() == 200) {
                log.info("Brevo email sent to {}: {}", recipient, subject);
                return true;
            }
            log.warn("Brevo send failed: {} {}", response.statusCode(), response.body());
            return false;
        } catch (Exception e) {
            log.error("Brevo send error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public CommunicationChannel supportedChannel() {
        return CommunicationChannel.EMAIL;
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
