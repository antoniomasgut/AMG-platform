package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.application.ConversationalAgentService;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agents/email")
@RequiredArgsConstructor
@Slf4j
public class EmailWebhookController {

    private final ConversationalAgentService conversationalAgentService;
    private final TenantChatLinkRepository chatLinkRepository;

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

    private String firstNonNullParam(Map<String, String> params, String... keys) {
        for (String key : keys) {
            String val = params.get(key);
            if (val != null && !val.isBlank()) return val;
        }
        return null;
    }
}
