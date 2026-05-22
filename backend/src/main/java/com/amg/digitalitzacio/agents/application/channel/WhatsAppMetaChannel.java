package com.amg.digitalitzacio.agents.application.channel;

import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class WhatsAppMetaChannel {

    private final SystemConfigService systemConfigService;

    public void sendMessage(String phoneNumberId, String toPhone, String text) {
        String accessToken = systemConfigService.get("WHATSAPP_META_ACCESS_TOKEN");
        if (accessToken == null || accessToken.isBlank() || phoneNumberId == null || phoneNumberId.isBlank()) {
            log.warn("WhatsApp Meta credentials not configured (phoneNumberId={}, token={})",
                    phoneNumberId, (accessToken == null || accessToken.isBlank()) ? "missing" : "present");
            return;
        }

        try {
            Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", toPhone,
                "type", "text",
                "text", Map.of("body", text)
            );

            RestClient.create()
                    .post()
                    .uri("https://graph.facebook.com/v19.0/{phoneNumberId}/messages", phoneNumberId)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("WhatsApp Meta message sent to {}", toPhone);
        } catch (Exception e) {
            log.error("Error sending WhatsApp Meta message to {}: {}", toPhone, e.getMessage());
        }
    }
}
