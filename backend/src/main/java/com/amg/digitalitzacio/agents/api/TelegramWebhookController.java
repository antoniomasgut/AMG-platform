package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.application.AbsenceRescheduleService;
import com.amg.digitalitzacio.agents.application.AgentRegistry;
import com.amg.digitalitzacio.agents.application.ConversationalAgentService;
import com.amg.digitalitzacio.agents.application.TeamGrowthService;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agents/telegram")
@RequiredArgsConstructor
@Slf4j
public class TelegramWebhookController {

    private final TenantChatLinkRepository chatLinkRepository;
    private final AgentRegistry agentRegistry;
    private final ConversationalAgentService conversationalAgentService;
    private final TeamGrowthService teamGrowthService;
    private final AbsenceRescheduleService absenceRescheduleService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload) {
        try {
            var message = extractMessage(payload);
            if (message == null) {
                return ResponseEntity.ok("ok");
            }

            var chatId = message.get("chat") instanceof Map<?, ?> chat
                    ? ((Number) chat.get("id")).longValue()
                    : null;
            var text = message.get("text") instanceof String t ? t.trim() : "";
            Long fromUserId = null;
            String firstName = "Usuari";
            if (message.get("from") instanceof Map<?, ?> from) {
                firstName = from.get("first_name") instanceof String fn ? fn : "Usuari";
                fromUserId = from.get("id") instanceof Number uid ? uid.longValue() : null;
            }

            if (chatId == null) {
                return ResponseEntity.ok("ok");
            }

            log.info("Missatge TG rebut de chat {}: {}", chatId, text);

            // Handle /start with link code
            if (text.startsWith("/start")) {
                var parts = text.split("\\s+");
                if (parts.length > 1) {
                    var linkCode = parts[1];
                    var linkOpt = chatLinkRepository.findByLinkCode(linkCode);
                    if (linkOpt.isPresent() && linkOpt.get().getIsActive()
                            && linkOpt.get().getLinkCodeExpiresAt() != null
                            && linkOpt.get().getLinkCodeExpiresAt().isAfter(Instant.now())) {
                        var link = linkOpt.get();
                        link.setTelegramChatId(chatId);
                        link.setLinkCode(null);
                        link.setLinkCodeExpiresAt(null);
                        chatLinkRepository.save(link);

                        log.info("Chat TG {} vinculat al tenant {}", chatId, link.getTenantId());
                        return ResponseEntity.ok(okTgReply(
                                chatId,
                                "Hola " + firstName + "! El teu compte s'ha vinculat correctament. ✅\n\nJa pots gestionar els teus serveis des d'aquest xat."
                        ));
                    }
                }
                return ResponseEntity.ok(okTgReply(
                        chatId,
                        "Hola " + firstName + "! 👋\n\nUtilitza el codi d'enllaç que t'ha proporcionat l'administrador per vincular el teu compte."
                ));
            }

            // Route message to agent by finding linked tenant
            var chatLinkOpt = chatLinkRepository.findByTelegramChatId(chatId);
            if (chatLinkOpt.isPresent() && chatLinkOpt.get().getIsActive()) {
                var link = chatLinkOpt.get();
                var tenantId = link.getTenantId();

                // Detecció de creixement d'equip (upsell F5 si 2a+ persona)
                if (fromUserId != null) {
                    teamGrowthService.recordAndCheck(tenantId, fromUserId, firstName, chatId);
                }

                // Comanda d'absència: /absencia [data]
                if (text.toLowerCase().startsWith("/absencia")) {
                    var reply = absenceRescheduleService.handleAbsenceCommand(
                            tenantId, text, chatId, fromUserId);
                    return ResponseEntity.ok(okTgReply(chatId, reply));
                }

                // Try to route to an agent
                boolean handled = false;
                for (var agent : agentRegistry.getAllAgents()) {
                    var response = agent.handleMessage(tenantId, text, chatId);
                    if (response != null && !response.isBlank()) {
                        return ResponseEntity.ok(okTgReply(chatId, response));
                    }
                    handled = true;
                }
                if (!handled) {
                    return ResponseEntity.ok(okTgReply(chatId,
                            "No s'ha pogut processar el missatge. Prova de nou més tard."));
                }
            }

            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage());
            return ResponseEntity.ok("ok");
        }
    }

    // Endpoint per-tenant: cada client té el seu bot amb la seva URL
    @PostMapping("/webhook/{tenantId}")
    public ResponseEntity<String> handleCustomerWebhook(
            @PathVariable UUID tenantId,
            @RequestBody Map<String, Object> payload) {
        try {
            var message = extractMessage(payload);
            if (message == null) return ResponseEntity.ok("ok");

            var chatId = message.get("chat") instanceof Map<?, ?> chat
                    ? ((Number) chat.get("id")).longValue() : null;
            var text = message.get("text") instanceof String t ? t.trim() : "";

            if (chatId == null || text.isBlank()) return ResponseEntity.ok("ok");

            log.info("Missatge TG client rebut per tenant={} de chat={}: {}", tenantId, chatId,
                    text.substring(0, Math.min(text.length(), 50)));

            handleCustomerAsync(tenantId, chatId, text);
        } catch (Exception e) {
            log.error("Error processing customer Telegram webhook: {}", e.getMessage());
        }
        return ResponseEntity.ok("ok");
    }

    @Async
    public void handleCustomerAsync(UUID tenantId, Long chatId, String text) {
        conversationalAgentService.handleIncoming(tenantId, chatId.toString(), ConversationChannel.TELEGRAM, text);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMessage(Map<String, Object> payload) {
        if (payload.get("message") instanceof Map<?, ?> msg) {
            return (Map<String, Object>) msg;
        }
        if (payload.get("edited_message") instanceof Map<?, ?> msg) {
            return (Map<String, Object>) msg;
        }
        return null;
    }

    private String okTgReply(Long chatId, String text) {
        return "{\"method\":\"sendMessage\",\"chat_id\":" + chatId + ",\"text\":" + JSONescape(text) + "}";
    }

    private String JSONescape(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
