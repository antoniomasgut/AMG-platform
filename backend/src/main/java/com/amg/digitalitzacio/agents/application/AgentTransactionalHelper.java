package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.*;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
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
    private final TenantRepository tenantRepository;
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
        // L'agent no respon fins que SUPER_ADMIN posa el tenant en marxa (activePhases explícit)
        var tenantCheck = tenantRepository.findById(tenantId).orElse(null);
        if (tenantCheck == null) return Optional.empty();
        var activePhasesCheck = tenantCheck.getActivePhases();
        if (activePhasesCheck == null || activePhasesCheck.isBlank() || !activePhasesCheck.contains("F1")) {
            return Optional.empty();
        }
        var chatLink = chatLinkOpt.get();

        boolean isNewContact = contactService.findOrCreate(tenantId, channel, identifier);
        contactService.extractAndLinkContact(tenantId, channel, identifier, text);
        touchLeadContactAt(tenantId, channel, identifier);
        conversationService.save(tenantId, identifier, channel, ConversationRole.USER, text, false);

        var context = conversationService.loadCustomerContext(tenantId, identifier, channel);
        var aiConfig = tenantAIConfigRepository.findById(tenantId).orElse(TenantAIConfig.defaultFor(tenantId));
        var tenant   = tenantRepository.findById(tenantId).orElse(null);
        String notifChannel = (tenant != null && tenant.getPreferredChannel() != null)
                ? tenant.getPreferredChannel().name() : "TELEGRAM";
        String operatorPhone = tenant != null ? tenant.getContactPhone() : null;

        return Optional.of(new IncomingPreparation(
            chatLink.modeFor(channel),
            chatLink.getTelegramChatId(),
            chatLink.getWhatsappPhoneNumber(),
            chatLink.getWhatsappMetaPhoneNumberId(),
            aiConfig.getSenderEmail(), aiConfig.getSenderName(), aiConfig.getReplyToEmail(),
            context,
            resolveChannelModel(aiConfig, channel),
            aiConfig.getFallbackModel(),
            isNewContact,
            notifChannel,
            operatorPhone
        ));
    }

    /** Fase 1 per al canal WIDGET (sense touchLeadContactAt ni notificació de contacte nou). */
    @Transactional
    Optional<IncomingPreparation> prepareWidgetMessage(UUID tenantId, String identifier, String text) {
        var chatLinkOpt = tenantChatLinkRepository.findByTenantId(tenantId);
        if (chatLinkOpt.isEmpty() || !Boolean.TRUE.equals(chatLinkOpt.get().getIsActive())) {
            return Optional.empty();
        }
        // Igual que prepareIncoming: requereix F1 activa explícitament
        var tenantCheck = tenantRepository.findById(tenantId).orElse(null);
        if (tenantCheck == null) return Optional.empty();
        var activePhasesCheck = tenantCheck.getActivePhases();
        if (activePhasesCheck == null || activePhasesCheck.isBlank() || !activePhasesCheck.contains("F1")) {
            return Optional.empty();
        }
        var chatLink = chatLinkOpt.get();

        contactService.findOrCreate(tenantId, ConversationChannel.WIDGET, identifier);
        contactService.extractAndLinkContact(tenantId, ConversationChannel.WIDGET, identifier, text);
        conversationService.save(tenantId, identifier, ConversationChannel.WIDGET,
                ConversationRole.USER, text, false);

        var context = conversationService.loadCustomerContext(tenantId, identifier, ConversationChannel.WIDGET);
        var aiConfig = tenantAIConfigRepository.findById(tenantId).orElse(TenantAIConfig.defaultFor(tenantId));
        var tenant   = tenantRepository.findById(tenantId).orElse(null);
        String notifChannel = (tenant != null && tenant.getPreferredChannel() != null)
                ? tenant.getPreferredChannel().name() : "TELEGRAM";
        String operatorPhone = tenant != null ? tenant.getContactPhone() : null;

        return Optional.of(new IncomingPreparation(
            chatLink.modeFor(ConversationChannel.WIDGET),
            chatLink.getTelegramChatId(),
            null, null, null, null, null,
            context,
            aiConfig.getChatModel() != null ? aiConfig.getChatModel() : aiConfig.getPreferredModel(),
            aiConfig.getFallbackModel(),
            false,
            notifChannel,
            operatorPhone
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

    private String resolveChannelModel(TenantAIConfig cfg, ConversationChannel channel) {
        String channelSpecific = switch (channel) {
            case WIDGET                              -> cfg.getChatModel();
            case WHATSAPP, WHATSAPP_META            -> cfg.getWhatsappModel();
            case EMAIL                              -> cfg.getEmailModel();
            default                                 -> null;
        };
        return channelSpecific != null ? channelSpecific : cfg.getPreferredModel();
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
