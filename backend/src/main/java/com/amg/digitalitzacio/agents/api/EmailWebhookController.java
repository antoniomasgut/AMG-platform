package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.application.ConversationalAgentService;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
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

    @PostMapping("/webhook/{tenantId}")
    public ResponseEntity<String> handleWebhook(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, Object> payload) {

        try {
            Object fromObj = payload.get("from");
            Object textObj = payload.get("text");

            if (fromObj == null || textObj == null) {
                log.warn("Missing from or text in email webhook payload for tenant {}", tenantId);
                return ResponseEntity.ok("OK");
            }

            String customerEmail = fromObj.toString();
            String text = textObj.toString();

            log.info("Email message received for tenant {}: from={}, text={}",
                    tenantId, customerEmail, text.substring(0, Math.min(text.length(), 50)));

            // Handle async
            handleAsync(tenantId, customerEmail, text);

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing email webhook for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.ok("OK");
        }
    }

    @Async
    private void handleAsync(UUID tenantId, String customerEmail, String text) {
        conversationalAgentService.handleIncoming(tenantId, customerEmail, ConversationChannel.EMAIL, text);
    }
}
