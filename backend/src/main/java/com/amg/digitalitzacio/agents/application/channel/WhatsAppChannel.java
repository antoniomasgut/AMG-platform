package com.amg.digitalitzacio.agents.application.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Slf4j
public class WhatsAppChannel {

    @Value("${app.agents.whatsapp.twilio-account-sid:}")
    private String twilioAccountSid;

    @Value("${app.agents.whatsapp.twilio-auth-token:}")
    private String twilioAuthToken;

    public void sendMessage(String fromNumber, String toPhone, String text) {
        if (twilioAccountSid.isBlank() || twilioAuthToken.isBlank()) {
            log.warn("Twilio credentials not configured for WhatsApp");
            return;
        }

        try {
            String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", twilioAccountSid);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("From", "whatsapp:" + fromNumber);
            body.add("To", "whatsapp:" + toPhone);
            body.add("Body", text);

            RestClient client = RestClient.builder()
                    .baseUrl("https://api.twilio.com")
                    .defaultHeader("Authorization", "Basic " + java.util.Base64.getEncoder()
                            .encodeToString((twilioAccountSid + ":" + twilioAuthToken).getBytes()))
                    .build();

            client.post()
                    .uri("/2010-04-01/Accounts/{sid}/Messages.json", twilioAccountSid)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("WhatsApp message sent to {}", toPhone);
        } catch (Exception e) {
            log.error("Error sending WhatsApp message to {}: {}", toPhone, e.getMessage());
        }
    }
}
