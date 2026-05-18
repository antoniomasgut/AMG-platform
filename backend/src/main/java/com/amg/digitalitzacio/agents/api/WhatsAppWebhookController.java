package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.application.ConversationalAgentService;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/agents/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private final ConversationalAgentService conversationalAgentService;

    @PostMapping("/webhook/{tenantId}")
    public ResponseEntity<String> handleWebhook(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String From,
            @RequestParam(required = false) String Body) {

        try {
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
    private void handleAsync(UUID tenantId, String customerPhone, String text) {
        conversationalAgentService.handleIncoming(tenantId, customerPhone, ConversationChannel.WHATSAPP, text);
    }
}
