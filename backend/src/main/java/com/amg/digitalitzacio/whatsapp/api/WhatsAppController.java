package com.amg.digitalitzacio.whatsapp.api;

import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.amg.digitalitzacio.whatsapp.api.dto.WhatsAppConfigResponse;
import com.amg.digitalitzacio.whatsapp.api.dto.WhatsAppConnectRequest;
import com.amg.digitalitzacio.whatsapp.api.dto.WhatsAppTestMessageRequest;
import com.amg.digitalitzacio.whatsapp.application.WhatsAppBusinessOrchestrator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppController {

    private final WhatsAppBusinessOrchestrator orchestrator;
    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper;

    /** Retorna la config WABA del tenant */
    @GetMapping("/tenants/{tenantId}/config")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public WhatsAppConfigResponse getConfig(@PathVariable UUID tenantId) {
        return orchestrator.getConfig(tenantId);
    }

    /** Connecta manualment (Phone Number ID + access token + WABA ID opcionals) */
    @PostMapping("/tenants/{tenantId}/connect")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public WhatsAppConfigResponse connect(@PathVariable UUID tenantId,
                                          @RequestBody WhatsAppConnectRequest request) {
        return orchestrator.connect(tenantId, request);
    }

    /** Verifica el token amb Meta i marca el webhook com a registrat */
    @PostMapping("/tenants/{tenantId}/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public WhatsAppConfigResponse verify(@PathVariable UUID tenantId) {
        return orchestrator.verify(tenantId);
    }

    /** Envia un missatge de prova al número indicat */
    @PostMapping("/tenants/{tenantId}/test")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> sendTest(@PathVariable UUID tenantId,
                                         @RequestBody WhatsAppTestMessageRequest req) {
        orchestrator.sendTestMessage(tenantId, req.toPhone());
        return ResponseEntity.ok().build();
    }

    /** Desconnecta el WABA del tenant */
    @DeleteMapping("/tenants/{tenantId}/config")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public void disconnect(@PathVariable UUID tenantId) {
        orchestrator.disconnect(tenantId);
    }

    /** GET webhook — verificació de Meta (challenge) */
    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        String expectedToken = systemConfigService.get("WHATSAPP_WEBHOOK_SECRET");
        if ("subscribe".equals(mode) && verifyToken != null
                && verifyToken.equals(expectedToken) && challenge != null) {
            log.info("WhatsApp webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }
        log.warn("WhatsApp webhook verification failed — mode={}, token_present={}", mode, verifyToken != null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    /** POST webhook — rep missatges i events de Meta */
    @PostMapping("/webhook")
    public ResponseEntity<String> receiveWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String hubSignature,
            @RequestBody(required = false) byte[] rawBody) {
        if (rawBody == null || rawBody.length == 0) return ResponseEntity.ok("ok");

        String appSecret = systemConfigService.get("WHATSAPP_WEBHOOK_SECRET");
        if (appSecret != null && !appSecret.isBlank()) {
            if (!verifyHmacSignature(rawBody, hubSignature, appSecret)) {
                log.warn("WhatsApp webhook: signatura X-Hub-Signature-256 invàlida");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }
        } else {
            log.warn("WHATSAPP_WEBHOOK_SECRET no configurat — WhatsApp webhook sense verificació HMAC");
        }

        JsonNode payload;
        try {
            payload = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            log.error("Error parsejant payload WhatsApp: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Bad Request");
        }

        try {
            var entries = payload.path("entry");
            for (JsonNode entry : entries) {
                for (JsonNode change : entry.path("changes")) {
                    var value = change.path("value");
                    String phoneNumberId = value.path("metadata").path("phone_number_id").asText(null);
                    if (phoneNumberId == null) continue;

                    for (JsonNode message : value.path("messages")) {
                        String from = message.path("from").asText(null);
                        String type = message.path("type").asText("text");
                        if (from == null) continue;

                        String text = switch (type) {
                            case "text" -> message.path("text").path("body").asText(null);
                            case "image", "audio", "video", "document" -> "[" + type + "]";
                            default -> null;
                        };

                        if (text != null) {
                            orchestrator.handleWebhookMessage(phoneNumberId, from, text);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing WhatsApp webhook: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok("ok");
    }

    private boolean verifyHmacSignature(byte[] body, String signature, String secret) {
        if (signature == null || !signature.startsWith("sha256=")) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error verificant signatura WhatsApp HMAC: {}", e.getMessage());
            return false;
        }
    }
}
