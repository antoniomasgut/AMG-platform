package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.application.ConversationalAgentService;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * Webhook genèric — Brevo envia tots els emails entrants aquí.
     * Enruta al tenant correcte basant-se en l'adreça de destí (camp To).
     * Configurar a Brevo: Inbound Parsing → webhook = https://api.amgdl.com/api/v1/agents/email/inbound
     */
    @PostMapping("/inbound")
    public ResponseEntity<String> handleInbound(@RequestBody Map<String, Object> payload) {
        try {
            String toRaw   = firstNonNull(payload, "To", "to");
            String fromRaw = firstNonNull(payload, "From", "from");
            String text    = firstNonNull(payload, "Text", "text", "TextBody");

            if (toRaw == null || fromRaw == null || text == null) {
                log.warn("Inbound email missing To/From/Text: {}", payload.keySet());
                return ResponseEntity.ok("OK");
            }

            String toEmail   = extractEmail(toRaw);
            String fromEmail = extractEmail(fromRaw);

            chatLinkRepository.findByEmailAddressIgnoreCase(toEmail).ifPresentOrElse(
                link -> handleAsync(link.getTenantId(), fromEmail, text),
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
     * Útil per a tenants amb domini propi que configuren el seu propi inbound a Brevo.
     * URL: https://api.amgdl.com/api/v1/agents/email/webhook/{tenantId}
     */
    @PostMapping("/webhook/{tenantId}")
    public ResponseEntity<String> handleWebhook(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, Object> payload) {
        try {
            String fromRaw = firstNonNull(payload, "From", "from");
            String text    = firstNonNull(payload, "Text", "text", "TextBody");

            if (fromRaw == null || text == null) {
                log.warn("Missing From or Text in email webhook for tenant {}", tenantId);
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

    private String firstNonNull(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object val = payload.get(key);
            if (val != null && !val.toString().isBlank()) return val.toString();
        }
        return null;
    }
}
