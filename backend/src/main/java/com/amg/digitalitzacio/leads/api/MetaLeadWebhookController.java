package com.amg.digitalitzacio.leads.api;

import com.amg.digitalitzacio.leads.application.MetaLeadWebhookService;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Webhook de Meta Lead Ads.
 *
 * Configuració a Facebook App:
 *   URL:          https://api.amgdl.com/api/v1/leads/meta-webhook
 *   Verify token: valor de META_WEBHOOK_VERIFY_TOKEN a SystemConfig
 *   Subscripcions: leadgen
 *
 * Per a cada tenant:
 *   - Configura metaPageId a TenantChatLink (camp "meta_page_id")
 *   - Afegeix META_PAGE_ACCESS_TOKEN_{PAGE_ID} a SystemConfig
 *   - O bé deixa META_PAGE_ACCESS_TOKEN per a una sola pàgina
 */
@RestController
@RequestMapping("/api/v1/leads/meta-webhook")
@RequiredArgsConstructor
@Slf4j
public class MetaLeadWebhookController {

    private final SystemConfigService sysConfig;
    private final MetaLeadWebhookService metaLeadWebhookService;
    private final com.amg.digitalitzacio.social.application.SocialCommentService socialCommentService;
    private final com.amg.digitalitzacio.social.application.SocialDmService socialDmService;
    private final ObjectMapper objectMapper;

    /** Verificació del webhook per part de Meta (han de rebre el challenge de tornada). */
    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        String expected = sysConfig.get("META_WEBHOOK_VERIFY_TOKEN");
        if ("subscribe".equals(mode) && token.equals(expected)) {
            log.info("Meta webhook verificat correctament");
            return ResponseEntity.ok(challenge);
        }
        log.warn("Meta webhook: token de verificació incorrecte");
        return ResponseEntity.status(403).body("Forbidden");
    }

    /** Recepció d'events de nous leads des de Meta. */
    @PostMapping
    public ResponseEntity<String> receive(
            @RequestBody String rawBody,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature) {

        String appSecret = sysConfig.get("META_APP_SECRET");
        if (appSecret != null && !appSecret.isBlank()) {
            if (signature == null || !verifyHmacSignature(rawBody, signature, appSecret)) {
                log.warn("Meta webhook: HMAC signature invàlida o absent");
                return ResponseEntity.status(401).body("Unauthorized");
            }
        } else {
            log.warn("Meta webhook: META_APP_SECRET no configurat, HMAC no verificat");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(rawBody, Map.class);
            processPayload(payload);
        } catch (Exception e) {
            log.error("Error processant webhook Meta leads: {}", e.getMessage());
        }
        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    private boolean verifyHmacSignature(String body, String signature, String appSecret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            String expected = "sha256=" + HexFormat.of().formatHex(hmacBytes);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error verificant HMAC signature: {}", e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void processPayload(Map<String, Object> payload) {
        var object = String.valueOf(payload.get("object"));
        boolean isPage = "page".equals(object);
        boolean isInstagram = "instagram".equals(object);
        if (!isPage && !isInstagram) return;

        var entries = (List<Map<String, Object>>) payload.getOrDefault("entry", List.of());
        for (var entry : entries) {
            var entryId = String.valueOf(entry.get("id"));
            var changes = (List<Map<String, Object>>) entry.getOrDefault("changes", List.of());
            for (var change : changes) {
                var field = String.valueOf(change.get("field"));
                var value = (Map<String, Object>) change.get("value");
                if (value == null) continue;

                if ("leadgen".equals(field)) {
                    var leadgenId = String.valueOf(value.get("leadgen_id"));
                    var formId = String.valueOf(value.get("form_id"));
                    metaLeadWebhookService.processLead(entryId, leadgenId, formId);
                } else if ("feed".equals(field)) {
                    processFeedComment(entryId, value);
                }
            }

            // DMs (Mòdul 56 F2): arriben a entry[].messaging[], no a changes[]
            var messaging = (List<Map<String, Object>>) entry.getOrDefault("messaging", List.of());
            for (var event : messaging) {
                processMessagingEvent(entryId, event, isInstagram);
            }
        }
    }

    /** Missatges directes de Messenger / Instagram (Mòdul 56 F2) */
    @SuppressWarnings("unchecked")
    private void processMessagingEvent(String entryId, Map<String, Object> event, boolean isInstagram) {
        var messageObj = (Map<String, Object>) event.get("message");
        if (messageObj == null) return;
        // Ignora els ecos dels missatges enviats per la pròpia pàgina/compte
        if (Boolean.TRUE.equals(messageObj.get("is_echo"))) return;

        var text = messageObj.get("text") != null ? String.valueOf(messageObj.get("text")) : null;
        if (text == null || text.isBlank()) return;

        String senderId = null;
        if (event.get("sender") instanceof Map<?, ?> sender && sender.get("id") != null) {
            senderId = String.valueOf(sender.get("id"));
        }
        if (senderId == null) return;

        try {
            if (isInstagram) {
                socialDmService.handleInstagramDm(entryId, senderId, null, text);
            } else {
                socialDmService.handleMessengerDm(entryId, senderId, null, text);
            }
        } catch (Exception e) {
            log.warn("Error processant DM (entry {}): {}", entryId, e.getMessage());
        }
    }

    /** Comentaris nous d'una pàgina (Mòdul 55, feature 1) */
    @SuppressWarnings("unchecked")
    private void processFeedComment(String pageId, Map<String, Object> value) {
        // Només comentaris afegits (no likes, edicions ni esborrats)
        if (!"comment".equals(String.valueOf(value.get("item")))) return;
        if (!"add".equals(String.valueOf(value.get("verb")))) return;

        var commentId = value.get("comment_id") != null ? String.valueOf(value.get("comment_id")) : null;
        var message = value.get("message") != null ? String.valueOf(value.get("message")) : null;
        String fromId = null, fromName = null;
        if (value.get("from") instanceof Map<?, ?> from) {
            fromId = from.get("id") != null ? String.valueOf(from.get("id")) : null;
            fromName = from.get("name") != null ? String.valueOf(from.get("name")) : null;
        }
        try {
            socialCommentService.processFacebookComment(pageId, commentId, message, fromId, fromName);
        } catch (Exception e) {
            log.warn("Error processant comentari de la pàgina {}: {}", pageId, e.getMessage());
        }
    }
}
