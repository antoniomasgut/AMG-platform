package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.application.channel.EmailChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.ConversationRole;
import com.amg.digitalitzacio.agents.domain.TenantAIConfig;
import com.amg.digitalitzacio.agents.domain.TenantAIConfigRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import com.amg.digitalitzacio.shared.ai.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ConversationalAgentService {

    private final ConversationService conversationService;
    private final ContactService contactService;
    private final PromptBuilder promptBuilder;
    private final TenantChatLinkRepository tenantChatLinkRepository;
    private final TenantAIConfigRepository tenantAIConfigRepository;
    private final TelegramBotClient telegramBotClient;
    private final WhatsAppChannel whatsAppChannel;
    private final WhatsAppMetaChannel whatsAppMetaChannel;
    private final EmailChannel emailChannel;
    private final AIProviderRouter aiProviderRouter;

    public void handleIncoming(UUID tenantId, String customerIdentifier, ConversationChannel channel, String text) {
        try {
            log.info("Handling incoming message for tenantId={}, channel={}", tenantId, channel);

            var chatLinkOpt = tenantChatLinkRepository.findByTenantId(tenantId);
            if (chatLinkOpt.isEmpty()) {
                log.warn("TenantChatLink not found for tenant {}", tenantId);
                return;
            }
            var chatLink = chatLinkOpt.get();

            if (!Boolean.TRUE.equals(chatLink.getIsActive())) {
                log.info("Agent is inactive for tenant {} — message ignored", tenantId);
                return;
            }

            contactService.findOrCreate(tenantId, channel, customerIdentifier);

            conversationService.save(tenantId, customerIdentifier, channel, ConversationRole.USER, text, false);

            var context = conversationService.loadCustomerContext(tenantId, customerIdentifier, channel);
            String systemPrompt = promptBuilder.build(tenantId, context);

            String model = tenantAIConfigRepository.findById(tenantId)
                    .map(TenantAIConfig::getPreferredModel)
                    .orElse(aiProviderRouter.defaultModel());

            var chatHistory = context.recentMessages().stream()
                    .map(c -> new ChatMessage(c.getRole().name(), c.getContent()))
                    .toList();

            var provider = aiProviderRouter.forModel(model);
            String assistantResponse = provider.chat(systemPrompt, chatHistory, text);

            if (assistantResponse == null || assistantResponse.isBlank()) {
                log.warn("AI provider '{}' returned empty response for tenant {}", provider.providerName(), tenantId);
                return;
            }

            switch (chatLink.getAgentMode()) {
                case AUTO:
                    String senderId = channel == ConversationChannel.WHATSAPP_META
                            ? chatLink.getWhatsappMetaPhoneNumberId()
                            : chatLink.getWhatsappPhoneNumber();
                    sendViaChannel(senderId, customerIdentifier, channel, assistantResponse);
                    conversationService.save(tenantId, customerIdentifier, channel, ConversationRole.ASSISTANT, assistantResponse, false);
                    break;
                case HYBRID:
                    conversationService.save(tenantId, customerIdentifier, channel, ConversationRole.ASSISTANT, assistantResponse, true);
                    notifyTenantViaInternalTelegram(chatLink.getTelegramChatId(), customerIdentifier, text, assistantResponse);
                    break;
                case MANUAL:
                    notifyTenantViaInternalTelegram(chatLink.getTelegramChatId(), customerIdentifier, text, null);
                    break;
                default:
                    log.warn("Unknown agent mode: {}", chatLink.getAgentMode());
            }

        } catch (Exception e) {
            log.error("Error handling incoming message for tenantId={}, channel={}", tenantId, channel, e);
        }
    }

    private void sendViaChannel(String fromNumber, String customerIdentifier, ConversationChannel channel, String text) {
        try {
            switch (channel) {
                case WHATSAPP      -> whatsAppChannel.sendMessage(fromNumber != null ? fromNumber : "", customerIdentifier, text);
                case WHATSAPP_META -> whatsAppMetaChannel.sendMessage(fromNumber != null ? fromNumber : "", customerIdentifier, text);
                case TELEGRAM      -> telegramBotClient.sendMessage(Long.parseLong(customerIdentifier), text);
                case EMAIL         -> emailChannel.sendMessage(customerIdentifier, "Response from Agent", text);
                default            -> log.warn("Unsupported channel: {}", channel);
            }
        } catch (Exception e) {
            log.error("Error sending via channel {} to {}: {}", channel, customerIdentifier, e.getMessage());
        }
    }

    private void notifyTenantViaInternalTelegram(Long telegramChatId, String customerIdentifier, String customerMessage, String suggestedResponse) {
        if (telegramChatId == null) return;
        try {
            String msg = suggestedResponse != null
                ? "🤖 Missatge de %s:\n\n%s\n\n✍️ Resposta suggerida:\n%s\n\nAccepta o edita al portal.".formatted(customerIdentifier, customerMessage, suggestedResponse)
                : "📬 Missatge de %s:\n\n%s".formatted(customerIdentifier, customerMessage);
            telegramBotClient.sendMessage(telegramChatId, msg);
        } catch (Exception e) {
            log.error("Error notifying tenant via Telegram: {}", e.getMessage());
        }
    }
}
