package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.application.ConversationalAgentService;
import com.amg.digitalitzacio.agents.application.InboundAssistService;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agents/email")
@RequiredArgsConstructor
@Slf4j
public class EmailWebhookController {

    private final ConversationalAgentService conversationalAgentService;
    private final InboundAssistService inboundAssistService;
    private final TenantChatLinkRepository chatLinkRepository;
    private final SystemConfigService systemConfigService;

    @Lazy @Autowired
    private EmailWebhookController self;

    /**
     * Webhook genèric — Mailgun envia tots els emails entrants aquí (multipart form).
     * Enruta al tenant correcte basant-se en l'adreça de destí (camp recipient/To).
     * Configurar a Mailgun: Routes → catch-all *@inbound.amgdl.com → forward webhook
     * URL: https://api.amgdl.com/api/v1/agents/email/inbound
     */
    @PostMapping("/inbound")
    public ResponseEntity<String> handleInbound(@RequestParam Map<String, String> params) {
        try {
            // Verificació de signatura Mailgun (timestamp + token + signature)
            String mailgunKey = systemConfigService.get("MAILGUN_WEBHOOK_SIGNING_KEY");
            if (mailgunKey != null && !mailgunKey.isBlank()) {
                String timestamp = params.get("timestamp");
                String token     = params.get("token");
                String signature = params.get("signature");
                if (!verifyMailgunSignature(timestamp, token, signature, mailgunKey)) {
                    log.warn("Email inbound: signatura Mailgun invalida");
                    return ResponseEntity.status(406).body("Invalid signature");
                }
            }

            // Mailgun: recipient (just email), sender (just email), body-plain
            // Fallback: To/From/Text per compatibilitat amb altres proveïdors
            String toRaw   = firstNonNullParam(params, "recipient", "To", "to");
            String fromRaw = firstNonNullParam(params, "sender", "From", "from");
            String text    = firstNonNullParam(params, "body-plain", "Text", "text", "TextBody");

            if (toRaw == null || fromRaw == null || text == null) {
                log.warn("Inbound email missing recipient/From/body-plain: keys={}", params.keySet());
                return ResponseEntity.ok("OK");
            }

            String toEmail   = extractEmail(toRaw);
            String fromEmail = extractEmail(fromRaw);

            chatLinkRepository.findByEmailAddressIgnoreCase(toEmail).ifPresentOrElse(
                link -> self.handleAsync(link.getTenantId(), fromEmail, text),
                () -> log.warn("No tenant found for inbound email address: {}", toEmail)
            );

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing inbound email: {}", e.getMessage());
            return ResponseEntity.ok("OK");
        }
    }

    /**
     * Webhook per tenant específic (URL directa amb tenantId).
     * URL: https://api.amgdl.com/api/v1/agents/email/webhook/{tenantId}
     */
    @PostMapping("/webhook/{tenantId}")
    public ResponseEntity<String> handleWebhook(
            @PathVariable UUID tenantId,
            @RequestParam Map<String, String> params) {
        try {
            String fromRaw = firstNonNullParam(params, "sender", "From", "from");
            String text    = firstNonNullParam(params, "body-plain", "Text", "text", "TextBody");

            if (fromRaw == null || text == null) {
                log.warn("Missing sender/body-plain in email webhook for tenant {}", tenantId);
                return ResponseEntity.ok("OK");
            }

            handleAsync(tenantId, extractEmail(fromRaw), text);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing email webhook for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.ok("OK");
        }
    }

    @Async
    protected void handleAsync(UUID tenantId, String customerEmail, String text) {
        log.info("Email received for tenant {}: from={}", tenantId, customerEmail);
        // Inbound Assist (Spec 59): si el tenant és HYBRID, esborrany + aprovació per Telegram.
        if (inboundAssistService.tryIntake(tenantId, ConversationChannel.EMAIL, customerEmail, null, text)) {
            return;
        }
        conversationalAgentService.handleIncoming(tenantId, customerEmail, ConversationChannel.EMAIL, text);
    }

    private String extractEmail(String raw) {
        if (raw == null) return null;
        // Format "Nom <email@domini.com>" → "email@domini.com"
        if (raw.contains("<") && raw.contains(">")) {
            return raw.replaceAll(".*<(.+?)>.*", "$1").trim().toLowerCase();
        }
        return raw.trim().toLowerCase();
    }

    private boolean verifyMailgunSignature(String timestamp, String token, String signature, String signingKey) {
        if (timestamp == null || token == null || signature == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal((timestamp + token).getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Mailgun HMAC verification error: {}", e.getMessage());
            return false;
        }
    }

    private String firstNonNullParam(Map<String, String> params, String... keys) {
        for (String key : keys) {
            String val = params.get(key);
            if (val != null && !val.isBlank()) return val;
        }
        return null;
    }
}
