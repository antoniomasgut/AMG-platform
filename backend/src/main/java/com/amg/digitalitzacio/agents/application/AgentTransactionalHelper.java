package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Encapsula les dues fases transaccionals curtes de l'orquestrador d'agent.
 * Separat de ConversationalAgentService per evitar self-invocation (Spring AOP no
 * intercepta crides internes al mateix bean).
 *
 * Fase 1 — prepareIncoming: llegeix config + persista missatge USER + carrega context.
 * Fase 2 — persistResponse: persista resposta ASSISTANT + tasca programada (si n'hi ha).
 *
 * Les crides HTTP (IA, Telegram, WhatsApp, Email, Google Calendar) queden fora
 * d'ambdues transaccions, de manera que la connexió BD s'allibera abans que es facin.
 */
@Component
@RequiredArgsConstructor
class AgentTransactionalHelper {

    private final ConversationService conversationService;
    private final ContactService contactService;
    private final TenantChatLinkRepository tenantChatLinkRepository;
    private final TenantAIConfigRepository tenantAIConfigRepository;
    private final LeadRepository leadRepository;
    private final ChannelUsageService channelUsageService;
    private final ScheduledAgentTaskRepository taskRepository;

    /** Fase 1 per a canals externs (Telegram, WhatsApp, Email). */
    @Transactional
    Optional<IncomingPreparation> prepareIncoming(UUID tenantId, String identifier,
                                                   ConversationChannel channel, String text) {
        var chatLinkOpt = tenantChatLinkRepository.findByTenantId(tenantId);
        if (chatLinkOpt.isEmpty() || !Boolean.TRUE.equals(chatLinkOpt.get().getIsActive())) {
            return Optional.empty();
        }
        var chatLink = chatLinkOpt.get();

        boolean isNewContact = contactService.findOrCreate(tenantId, channel, identifier);
        touchLeadContactAt(tenantId, channel, identifier);
        conversationService.save(tenantId, identifier, channel, ConversationRole.USER, text, false);

        var context = conversationService.loadCustomerContext(tenantId, identifier, channel);
        var aiConfig = tenantAIConfigRepository.findById(tenantId).orElse(TenantAIConfig.defaultFor(tenantId));

        return Optional.of(new IncomingPreparation(
            chatLink.getAgentMode(),
            chatLink.getTelegramChatId(),
            chatLink.getWhatsappPhoneNumber(),
            chatLink.getWhatsappMetaPhoneNumberId(),
            aiConfig.getSenderEmail(), aiConfig.getSenderName(), aiConfig.getReplyToEmail(),
            context,
            aiConfig.getPreferredModel(),
            isNewContact
        ));
    }

    /** Fase 1 per al canal WIDGET (sense touchLeadContactAt ni notificació de contacte nou). */
    @Transactional
    Optional<IncomingPreparation> prepareWidgetMessage(UUID tenantId, String identifier, String text) {
        var chatLinkOpt = tenantChatLinkRepository.findByTenantId(tenantId);
        if (chatLinkOpt.isEmpty() || !Boolean.TRUE.equals(chatLinkOpt.get().getIsActive())) {
            return Optional.empty();
        }
        var chatLink = chatLinkOpt.get();

        contactService.findOrCreate(tenantId, ConversationChannel.WIDGET, identifier);
        conversationService.save(tenantId, identifier, ConversationChannel.WIDGET,
                ConversationRole.USER, text, false);

        var context = conversationService.loadCustomerContext(tenantId, identifier, ConversationChannel.WIDGET);
        var aiConfig = tenantAIConfigRepository.findById(tenantId).orElse(TenantAIConfig.defaultFor(tenantId));

        return Optional.of(new IncomingPreparation(
            chatLink.getAgentMode(),
            chatLink.getTelegramChatId(),
            null, null, null, null, null,
            context,
            aiConfig.getPreferredModel(),
            false
        ));
    }

    /** Fase 2: persista resposta ASSISTANT + tasca de recordatori (si n'hi ha). */
    @Transactional
    void persistResponse(UUID tenantId, String identifier, ConversationChannel channel,
                          String response, boolean pending, ScheduledAgentTask reminderTask) {
        conversationService.save(tenantId, identifier, channel, ConversationRole.ASSISTANT, response, pending);
        channelUsageService.record(tenantId, channel.name());
        if (reminderTask != null) {
            taskRepository.save(reminderTask);
        }
    }

    private void touchLeadContactAt(UUID tenantId, ConversationChannel channel, String identifier) {
        try {
            var opt = switch (channel) {
                case EMAIL -> leadRepository.findFirstByTenantIdAndEmail(tenantId, identifier);
                default    -> leadRepository.findFirstByTenantIdAndPhone(tenantId, identifier);
            };
            opt.ifPresent(lead -> {
                lead.setLastContactAt(Instant.now());
                leadRepository.save(lead);
            });
        } catch (Exception ignored) {}
    }
}
