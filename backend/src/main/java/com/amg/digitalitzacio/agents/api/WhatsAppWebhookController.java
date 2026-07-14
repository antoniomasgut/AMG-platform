package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.application.ConversationalAgentService;
import com.amg.digitalitzacio.agents.application.InboundAssistService;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agents/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private final ConversationalAgentService conversationalAgentService;
    private final InboundAssistService inboundAssistService;
    private final SystemConfigService systemConfigService;

    @PostMapping("/webhook/{tenantId}")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-Twilio-Signature", required = false) String twilioSignature,
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String From,
            @RequestParam(required = false) String Body,
            HttpServletRequest request) {

        try {
            String authToken = systemConfigService.get("TWILIO_AUTH_TOKEN");
            if (authToken != null && !authToken.isBlank()) {
                if (!verifyTwilioSignature(request, twilioSignature, authToken, From, Body)) {
                    log.warn("Twilio webhook: signatura invàlida per tenant {}", tenantId);
                    return ResponseEntity.status(401).body("<Response/>");
                }
            } else {
                log.warn("TWILIO_AUTH_TOKEN no configurat — webhook sense verificació");
            }

            if (From == null || From.isBlank() || Body == null || Body.isBlank()) {
                log.warn("Missing From or Body parameter for WhatsApp webhook");
                return ResponseEntity.ok("<Response/>");
            }

            // Remove "whatsapp:" prefix if present
            String customerPhone = From.replace("whatsapp:", "").trim();

            log.info("WhatsApp message received for tenant {}: from={}, body={}",
                    tenantId, customerPhone, Body.substring(0, Math.min(Body.length(), 50)));

            // Handle async to return immediately
            handleAsync(tenantId, customerPhone, Body);

            return ResponseEntity.ok("<Response/>");
        } catch (Exception e) {
            log.error("Error processing WhatsApp webhook for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.ok("<Response/>");
        }
    }

    @Async
    protected void handleAsync(UUID tenantId, String customerPhone, String text) {
        // Inbound Assist (Spec 59): si el tenant és HYBRID, esborrany + aprovació per Telegram.
        if (inboundAssistService.tryIntake(tenantId, ConversationChannel.WHATSAPP, customerPhone, null, text)) {
            return;
        }
        conversationalAgentService.handleIncoming(tenantId, customerPhone, ConversationChannel.WHATSAPP, text);
    }

    private boolean verifyTwilioSignature(HttpServletRequest request, String signature,
                                           String authToken, String from, String body) {
        if (signature == null || signature.isBlank()) return false;
        try {
            String scheme = request.getHeader("X-Forwarded-Proto");
            if (scheme == null) scheme = request.getScheme();
            String host = request.getHeader("X-Forwarded-Host");
            if (host == null) host = request.getHeader("Host");
            String url = scheme + "://" + host + request.getRequestURI();

            // Ordena i concatena els paràmetres POST (ordre alfabètic)
            Map<String, String> params = new TreeMap<>();
            if (from != null) params.put("From", from);
            if (body != null) params.put("Body", body);
            StringBuilder sb = new StringBuilder(url);
            params.forEach((k, v) -> sb.append(k).append(v));

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(authToken.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            String expected = Base64.getEncoder().encodeToString(
                    mac.doFinal(sb.toString().getBytes(StandardCharsets.UTF_8)));

            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error verificant signatura Twilio: {}", e.getMessage());
            return false;
        }
    }
}
