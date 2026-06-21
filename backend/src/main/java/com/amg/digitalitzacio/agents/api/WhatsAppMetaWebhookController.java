package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.application.ConversationalAgentService;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@RestController
@RequestMapping("/api/v1/agents/whatsapp-meta/webhook")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppMetaWebhookController {

    private final ConversationalAgentService conversationalAgentService;
    private final TenantChatLinkRepository tenantChatLinkRepository;
    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper;

    // Meta verifica el webhook amb un GET inicial
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String verifyToken,
            @RequestParam("hub.challenge") String challenge) {

        String expectedToken = systemConfigService.get("WHATSAPP_META_VERIFY_TOKEN");
        if (expectedToken == null) expectedToken = "";

        if ("subscribe".equals(mode) && expectedToken.equals(verifyToken)) {
            log.info("WhatsApp Meta webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }

        log.warn("WhatsApp Meta webhook verification failed — token mismatch");
        return ResponseEntity.status(403).body("Forbidden");
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String hubSignature,
            @RequestBody byte[] rawBody) {
        String appSecret = systemConfigService.get("META_APP_SECRET");
        if (appSecret != null && !appSecret.isBlank()) {
            if (!verifyMetaSignature(rawBody, hubSignature, appSecret)) {
                log.warn("WhatsApp Meta webhook: signatura X-Hub-Signature-256 invàlida");
                return ResponseEntity.status(401).body("Unauthorized");
            }
        } else {
            log.warn("META_APP_SECRET no configurat — WhatsApp Meta webhook sense verificació HMAC");
        }

        JsonNode payload;
        try {
            payload = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            log.error("Error parsejant payload WhatsApp Meta: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Bad Request");
        }

        try {
            if (!"whatsapp_business_account".equals(payload.path("object").asText())) {
                return ResponseEntity.ok("ok");
            }

            for (JsonNode entry : payload.path("entry")) {
                for (JsonNode change : entry.path("changes")) {
                    JsonNode value = change.path("value");
                    String phoneNumberId = value.path("metadata").path("phone_number_id").asText();

                    for (JsonNode message : value.path("messages")) {
                        if (!"text".equals(message.path("type").asText())) continue;

                        String from = message.path("from").asText();
                        String text = message.path("text").path("body").asText();

                        if (from.isBlank() || text.isBlank()) continue;

                        log.info("WhatsApp Meta message received: phoneNumberId={}, from={}", phoneNumberId, from);

                        tenantChatLinkRepository.findByWhatsappMetaPhoneNumberId(phoneNumberId)
                                .ifPresentOrElse(
                                        link -> handleAsync(link.getTenantId(), from, text),
                                        () -> log.warn("No tenant found for whatsapp_meta phone_number_id={}", phoneNumberId)
                                );
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing WhatsApp Meta webhook: {}", e.getMessage());
        }

        return ResponseEntity.ok("ok");
    }

    @Async
    private void handleAsync(java.util.UUID tenantId, String from, String text) {
        conversationalAgentService.handleIncoming(tenantId, from, ConversationChannel.WHATSAPP_META, text);
    }

    private boolean verifyMetaSignature(byte[] body, String signature, String appSecret) {
        if (signature == null || !signature.startsWith("sha256=")) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error verificant signatura Meta: {}", e.getMessage());
            return false;
        }
    }
}
