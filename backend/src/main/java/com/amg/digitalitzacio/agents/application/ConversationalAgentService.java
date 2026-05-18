package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.application.channel.EmailChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.ConversationRole;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ConversationalAgentService {

    private final ConversationService conversationService;
    private final PromptBuilder promptBuilder;
    private final TenantChatLinkRepository tenantChatLinkRepository;
    private final TelegramBotClient telegramBotClient;
    private final WhatsAppChannel whatsAppChannel;
    private final EmailChannel emailChannel;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${app.agents.conversational.claude-model:claude-haiku-4-5-20251001}")
    private String claudeModel;

    @Value("${app.agents.conversational.anthropic-api-key:${ANTHROPIC_API_KEY:}}")
    private String anthropicApiKey;

    public void handleIncoming(UUID tenantId, String customerIdentifier, ConversationChannel channel, String text) {
        try {
            log.info("Handling incoming message for tenantId: {}, customerIdentifier: {}, channel: {}",
                    tenantId, customerIdentifier, channel);

            // 1. Save user message
            conversationService.save(tenantId, customerIdentifier, channel, ConversationRole.USER, text, false);

            // 2. Load history
            var history = conversationService.loadHistory(tenantId, customerIdentifier, channel);
            log.debug("Loaded {} messages from history", history.size());

            // 3. Build system prompt
            String systemPrompt = promptBuilder.build(tenantId);

            // 4. Call Claude API
            String assistantResponse = callClaudeApi(systemPrompt, history, text);
            if (assistantResponse == null || assistantResponse.isBlank()) {
                log.warn("Claude API returned empty response for tenant {}", tenantId);
                return;
            }

            // 5. Load TenantChatLink to check agentMode
            var chatLinkOpt = tenantChatLinkRepository.findByTenantId(tenantId);
            if (chatLinkOpt.isEmpty()) {
                log.warn("TenantChatLink not found for tenant {}", tenantId);
                return;
            }

            var chatLink = chatLinkOpt.get();
            var agentMode = chatLink.getAgentMode();

            // 6. Handle based on agentMode
            switch (agentMode) {
                case AUTO:
                    sendViaChannel(chatLink.getWhatsappPhoneNumber(), customerIdentifier, channel, assistantResponse);
                    conversationService.save(tenantId, customerIdentifier, channel, ConversationRole.ASSISTANT,
                            assistantResponse, false);
                    log.info("Auto mode: response sent immediately");
                    break;

                case HYBRID:
                    conversationService.save(tenantId, customerIdentifier, channel, ConversationRole.ASSISTANT,
                            assistantResponse, true);
                    notifyTenantViaInternalTelegram(chatLink.getTelegramChatId(), customerIdentifier, text, assistantResponse);
                    log.info("Hybrid mode: response pending approval, tenant notified");
                    break;

                case MANUAL:
                    notifyTenantViaInternalTelegram(chatLink.getTelegramChatId(), customerIdentifier, text, null);
                    log.info("Manual mode: tenant notified only");
                    break;

                default:
                    log.warn("Unknown agent mode: {}", agentMode);
            }

        } catch (Exception e) {
            log.error("Error handling incoming message for tenantId: {}, customerIdentifier: {}, channel: {}",
                    tenantId, customerIdentifier, channel, e);
        }
    }

    private String callClaudeApi(String systemPrompt, java.util.List<com.amg.digitalitzacio.agents.domain.Conversation> history, String userText) {
        try {
            RestClient client = restClientBuilder
                    .baseUrl("https://api.anthropic.com")
                    .build();

            List<Map<String, String>> messages = new ArrayList<>();
            for (var msg : history) {
                messages.add(Map.of("role", msg.getRole().name().toLowerCase(), "content", msg.getContent()));
            }
            messages.add(Map.of("role", "user", "content", userText));

            Map<String, Object> requestBody = Map.of(
                    "model", claudeModel,
                    "max_tokens", 1024,
                    "system", systemPrompt,
                    "messages", messages
            );

            var response = client.post()
                    .uri("/v1/messages")
                    .header("x-api-key", anthropicApiKey)
                    .header("anthropic-version", "2023-06-01")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("content")) {
                var content = (java.util.List<?>) response.get("content");
                if (!content.isEmpty()) {
                    var first = (Map<?, ?>) content.get(0);
                    return (String) first.get("text");
                }
            }

            log.warn("Claude API response missing content");
            return null;

        } catch (Exception e) {
            log.error("Error calling Claude API: {}", e.getMessage(), e);
            return null;
        }
    }

    private void sendViaChannel(String fromNumber, String customerIdentifier, ConversationChannel channel, String text) {
        try {
            switch (channel) {
                case WHATSAPP:
                    whatsAppChannel.sendMessage(fromNumber != null ? fromNumber : "", customerIdentifier, text);
                    break;
                case TELEGRAM:
                    telegramBotClient.sendMessage(Long.parseLong(customerIdentifier), text);
                    break;
                case EMAIL:
                    emailChannel.sendMessage(customerIdentifier, "Response from Agent", text);
                    break;
                default:
                    log.warn("Unsupported channel: {}", channel);
            }
        } catch (Exception e) {
            log.error("Error sending message via channel {} to {}: {}", channel, customerIdentifier, e.getMessage());
        }
    }

    private void notifyTenantViaInternalTelegram(Long telegramChatId, String customerIdentifier, String customerMessage, String suggestedResponse) {
        if (telegramChatId == null) {
            log.warn("Telegram chat ID not set for tenant, cannot notify");
            return;
        }

        try {
            String notification;
            if (suggestedResponse != null) {
                // HYBRID mode: suggest response for approval
                notification = String.format("🤖 Missatge de %s:\n\n%s\n\n✍️ Resposta suggerida:\n%s\n\nAccepta o edita al portal.",
                        customerIdentifier, customerMessage, suggestedResponse);
            } else {
                // MANUAL mode: just notify
                notification = String.format("📬 Missatge de %s:\n\n%s",
                        customerIdentifier, customerMessage);
            }

            telegramBotClient.sendMessage(telegramChatId, notification);
            log.debug("Tenant notified via Telegram chat {}", telegramChatId);
        } catch (Exception e) {
            log.error("Error notifying tenant via Telegram: {}", e.getMessage());
        }
    }
}
