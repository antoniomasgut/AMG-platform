package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.api.dto.*;
import com.amg.digitalitzacio.agents.application.ContactService;
import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.application.channel.EmailChannel;
import com.amg.digitalitzacio.agents.domain.Conversation;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.agents.domain.ConversationRepository;
import com.amg.digitalitzacio.agents.domain.ConversationRole;
import com.amg.digitalitzacio.agents.domain.TenantAIConfig;
import com.amg.digitalitzacio.agents.domain.TenantAIConfigRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLink;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agents/conversational")
@RequiredArgsConstructor
@Slf4j
public class ConversationalAgentController {

    private final ConversationRepository conversationRepository;
    private final ContactService contactService;
    private final TenantChatLinkRepository tenantChatLinkRepository;
    private final TenantAIConfigRepository tenantAIConfigRepository;
    private final AIProviderRouter aiProviderRouter;
    private final TelegramBotClient telegramBotClient;
    private final SystemConfigService systemConfigService;
    private final com.amg.digitalitzacio.agents.application.AgentHealthService agentHealthService;
    private final com.amg.digitalitzacio.agents.application.ConversationalAgentService conversationalAgentService;
    private final EmailChannel emailChannel;
    private final TenantRepository tenantRepository;

    @GetMapping("/{tenantId}/conversations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ConversationResponse>> getConversations(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            var principal = getPrincipal();
            validateTenantAccess(tenantId, principal);

            var conversations = conversationRepository.findByTenantIdOrderByCreatedAtDesc(
                    tenantId, PageRequest.of(page, size));

            var responses = conversations.stream()
                    .map(c -> new ConversationResponse(
                            c.getId(),
                            c.getCustomerIdentifier(),
                            c.getChannel(),
                            c.getRole(),
                            c.getContent(),
                            c.getPendingApproval(),
                            c.getCreatedAt()
                    ))
                    .toList();

            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching conversations for tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{tenantId}/pending/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getPendingCount(@PathVariable UUID tenantId) {
        try {
            var principal = getPrincipal();
            validateTenantAccess(tenantId, principal);
            long count = conversationRepository.countByTenantIdAndPendingApprovalTrue(tenantId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error counting pending for tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{tenantId}/pending")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PendingResponseDto>> getPending(@PathVariable UUID tenantId) {
        try {
            var principal = getPrincipal();
            validateTenantAccess(tenantId, principal);

            var pending = conversationRepository.findByTenantIdAndPendingApprovalTrueOrderByCreatedAtDesc(tenantId);

            var responses = pending.stream()
                    .filter(c -> c.getRole() == ConversationRole.ASSISTANT)
                    .map(assistantMsg -> {
                        // Cerca el missatge USER precedent amb una sola query puntual
                        String customerMessage = conversationRepository
                            .findTop1UserMessageBefore(
                                tenantId,
                                assistantMsg.getCustomerIdentifier(),
                                assistantMsg.getChannel(),
                                assistantMsg.getCreatedAt())
                            .map(Conversation::getContent)
                            .orElse("");

                        return new PendingResponseDto(
                                assistantMsg.getId(),
                                assistantMsg.getCustomerIdentifier(),
                                assistantMsg.getChannel(),
                                customerMessage,
                                assistantMsg.getContent(),
                                assistantMsg.getCreatedAt()
                        );
                    })
                    .toList();

            return ResponseEntity.ok(responses);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching pending for tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{tenantId}/pending/{id}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> approvePending(
            @PathVariable UUID tenantId,
            @PathVariable Long id) {
        try {
            var principal = getPrincipal();
            validateTenantAccess(tenantId, principal);

            contactService.approveAndSend(tenantId, id, null);

            log.info("Approved pending response {} for tenant {}", id, tenantId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error approving pending {} for tenant {}", id, tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{tenantId}/pending/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> editPending(
            @PathVariable UUID tenantId,
            @PathVariable Long id,
            @Valid @RequestBody EditPendingRequest request) {
        try {
            var principal = getPrincipal();
            validateTenantAccess(tenantId, principal);

            if (request.content() == null || request.content().isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            contactService.approveAndSend(tenantId, id, request.content());

            log.info("Edited and approved pending response {} for tenant {}", id, tenantId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error editing pending {} for tenant {}", id, tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{tenantId}/pending/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> discardPending(
            @PathVariable UUID tenantId,
            @PathVariable Long id) {
        try {
            var principal = getPrincipal();
            validateTenantAccess(tenantId, principal);

            var conversation = conversationRepository.findById(id)
                    .filter(c -> c.getTenantId().equals(tenantId) && c.getPendingApproval())
                    .orElseThrow(() -> new IllegalArgumentException("Conversation not found or not pending"));

            conversationRepository.delete(conversation);

            log.info("Discarded pending response {} for tenant {}", id, tenantId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error discarding pending {} for tenant {}", id, tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{tenantId}/mode")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateMode(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AgentModeRequest request) {
        try {
            var principal = getPrincipal();
            validateTenantAccess(tenantId, principal);

            var chatLink = tenantChatLinkRepository.findByTenantId(tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Chat link not found"));

            chatLink.setAgentMode(request.mode());
            tenantChatLinkRepository.save(chatLink);

            log.info("Updated agent mode to {} for tenant {}", request.mode(), tenantId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating mode for tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{tenantId}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgentStatusResponse> getStatus(@PathVariable UUID tenantId) {
        try {
            var principal = getPrincipal();
            validateTenantAccess(tenantId, principal);

            var chatLink = tenantChatLinkRepository.findByTenantId(tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Chat link not found"));

            var response = new AgentStatusResponse(
                    chatLink.getAgentMode(),
                    chatLink.getTelegramChatId() != null,
                    chatLink.getWhatsappPhoneNumber() != null && !chatLink.getWhatsappPhoneNumber().isBlank(),
                    chatLink.getEmailAddress() != null && !chatLink.getEmailAddress().isBlank()
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error fetching status for tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** Retorna la configuració de canals d'un tenant (Telegram, WhatsApp, mode) */
    @GetMapping("/{tenantId}/channels")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<com.amg.digitalitzacio.agents.api.dto.ChannelsResponse> getChannels(@PathVariable UUID tenantId) {
        String botUsername = systemConfigService.get("TELEGRAM_BOT_USERNAME");
        if (botUsername == null || botUsername.isBlank()) botUsername = "AMGDL_Bot";
        String telegramBotLink = "https://t.me/" + botUsername;

        var chatLink = tenantChatLinkRepository.findByTenantId(tenantId).orElse(null);
        if (chatLink == null) {
            return ResponseEntity.ok(new com.amg.digitalitzacio.agents.api.dto.ChannelsResponse(
                    tenantId, "AUTO", false, false, null, telegramBotLink, null, null, null, null));
        }
        return ResponseEntity.ok(new com.amg.digitalitzacio.agents.api.dto.ChannelsResponse(
                tenantId,
                chatLink.getAgentMode().name(),
                chatLink.getIsActive(),
                chatLink.getTelegramChatId() != null,
                chatLink.getTelegramChatId(),
                telegramBotLink,
                chatLink.getWhatsappPhoneNumber(),
                chatLink.getWhatsappMetaPhoneNumberId(),
                chatLink.getEmailAddress(),
                chatLink.getMetaPageId()
        ));
    }

    /** Actualitza la configuració de canals d'un tenant */
    @PutMapping("/{tenantId}/channels")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<com.amg.digitalitzacio.agents.api.dto.ChannelsResponse> updateChannels(
            @PathVariable UUID tenantId,
            @Valid @RequestBody com.amg.digitalitzacio.agents.api.dto.UpdateChannelsRequest request) {
        var chatLink = tenantChatLinkRepository.findByTenantId(tenantId)
                .orElseGet(() -> com.amg.digitalitzacio.agents.domain.TenantChatLink.builder()
                        .tenantId(tenantId).build());
        if (request.agentMode() != null)
            chatLink.setAgentMode(com.amg.digitalitzacio.agents.domain.AgentMode.valueOf(request.agentMode()));
        if (request.isActive() != null)
            chatLink.setIsActive(request.isActive());
        if (request.whatsappPhoneNumber() != null)
            chatLink.setWhatsappPhoneNumber(request.whatsappPhoneNumber().isBlank() ? null : request.whatsappPhoneNumber());
        if (request.whatsappMetaPhoneNumberId() != null)
            chatLink.setWhatsappMetaPhoneNumberId(request.whatsappMetaPhoneNumberId().isBlank() ? null : request.whatsappMetaPhoneNumberId());
        if (request.emailAddress() != null)
            chatLink.setEmailAddress(request.emailAddress().isBlank() ? null : request.emailAddress());
        if (request.metaPageId() != null)
            chatLink.setMetaPageId(request.metaPageId().isBlank() ? null : request.metaPageId());
        chatLink = tenantChatLinkRepository.save(chatLink);

        String botUsername = systemConfigService.get("TELEGRAM_BOT_USERNAME");
        if (botUsername == null || botUsername.isBlank()) botUsername = "AMGDL_Bot";

        return ResponseEntity.ok(new com.amg.digitalitzacio.agents.api.dto.ChannelsResponse(
                tenantId,
                chatLink.getAgentMode().name(),
                chatLink.getIsActive(),
                chatLink.getTelegramChatId() != null,
                chatLink.getTelegramChatId(),
                "https://t.me/" + botUsername,
                chatLink.getWhatsappPhoneNumber(),
                chatLink.getWhatsappMetaPhoneNumberId(),
                chatLink.getEmailAddress(),
                chatLink.getMetaPageId()
        ));
    }

    /** Retorna els models disponibles (Anthropic, DeepSeek, Ollama) */
    @GetMapping("/models")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<List<AIProviderRouter.ModelInfo>> getAvailableModels() {
        return ResponseEntity.ok(aiProviderRouter.availableModels());
    }

    /** Retorna la configuració d'IA d'un tenant */
    @GetMapping("/{tenantId}/ai-config")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<TenantAIConfig> getAIConfig(@PathVariable UUID tenantId) {
        var config = tenantAIConfigRepository.findById(tenantId)
                .orElse(TenantAIConfig.defaultFor(tenantId));
        return ResponseEntity.ok(config);
    }

    /** Actualitza el model d'IA d'un tenant */
    @PutMapping("/{tenantId}/ai-config")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<TenantAIConfig> updateAIConfig(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AIConfigRequest request) {
        var config = tenantAIConfigRepository.findById(tenantId)
                .orElse(TenantAIConfig.defaultFor(tenantId));
        if (request.preferredModel() != null && !request.preferredModel().isBlank())
            config.setPreferredModel(request.preferredModel());
        if (request.maxTokens() != null)
            config.setMaxTokens(request.maxTokens());
        if (request.temperature() != null)
            config.setTemperature(request.temperature());
        if (request.reasoningModel() != null)
            config.setReasoningModel(request.reasoningModel().isBlank() ? null : request.reasoningModel());
        if (request.monthlyTokenBudget() != null)
            config.setMonthlyTokenBudget(request.monthlyTokenBudget());
        if (request.budgetAlertThreshold() != null)
            config.setBudgetAlertThreshold(request.budgetAlertThreshold());
        if (request.senderEmail() != null)
            config.setSenderEmail(request.senderEmail().isBlank() ? null : request.senderEmail());
        if (request.senderName() != null)
            config.setSenderName(request.senderName().isBlank() ? null : request.senderName());
        if (request.replyToEmail() != null)
            config.setReplyToEmail(request.replyToEmail().isBlank() ? null : request.replyToEmail());
        return ResponseEntity.ok(tenantAIConfigRepository.save(config));
    }

    /** Test directe d'un model: envia un missatge i retorna la resposta */
    @PostMapping("/test-model")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, String>> testModel(@Valid @RequestBody AIModelTestRequest request) {
        try {
            var provider = aiProviderRouter.forModel(request.model());
            String response = provider.chat(
                    request.systemPrompt() != null ? request.systemPrompt() : "Ets un assistent útil.",
                    List.of(),
                    request.message()
            );
            return ResponseEntity.ok(Map.of(
                    "model", request.model(),
                    "provider", provider.providerName(),
                    "response", response != null ? response : "(sense resposta)"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Retorna les instruccions d'activació per canal (Telegram, WhatsApp) */
    @GetMapping("/{tenantId}/activation-instructions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getActivationInstructions(@PathVariable UUID tenantId) {
        try {
            var chatLink = tenantChatLinkRepository.findByTenantId(tenantId).orElse(null);

            String botUsername = systemConfigService.get("TELEGRAM_BOT_USERNAME");
            if (botUsername == null || botUsername.isBlank()) {
                botUsername = "AMGDL_Bot";
            }

            boolean telegramLinked = chatLink != null && chatLink.getTelegramChatId() != null;
            boolean whatsappConfigured = chatLink != null && chatLink.getWhatsappPhoneNumber() != null
                    && !chatLink.getWhatsappPhoneNumber().isBlank();

            Map<String, Object> telegram = new LinkedHashMap<>();
            telegram.put("active", telegramLinked);
            telegram.put("configured", telegramLinked);
            telegram.put("instructions", telegramLinked
                    ? "Els vostres clients poden parlar amb el bot a t.me/" + botUsername
                    : null);
            telegram.put("link", telegramLinked ? "https://t.me/" + botUsername : null);

            Map<String, Object> whatsapp = new LinkedHashMap<>();
            whatsapp.put("active", whatsappConfigured);
            whatsapp.put("configured", whatsappConfigured);
            whatsapp.put("instructions", null);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("telegram", telegram);
            result.put("whatsapp", whatsapp);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error fetching activation instructions for tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** Activa el bot per al tenant */
    @PostMapping("/{tenantId}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<com.amg.digitalitzacio.agents.api.dto.ChannelsResponse> activate(@PathVariable UUID tenantId) {
        try {
            var chatLink = tenantChatLinkRepository.findByTenantId(tenantId)
                    .orElseGet(() -> TenantChatLink.builder().tenantId(tenantId).build());

            boolean hasChannel = chatLink.getTelegramChatId() != null
                    || (chatLink.getWhatsappPhoneNumber() != null && !chatLink.getWhatsappPhoneNumber().isBlank())
                    || (chatLink.getEmailAddress() != null && !chatLink.getEmailAddress().isBlank());

            if (!hasChannel) {
                return ResponseEntity.badRequest().build();
            }

            chatLink.setIsActive(true);
            chatLink = tenantChatLinkRepository.save(chatLink);

            if (chatLink.getTelegramChatId() != null) {
                String botUsername = systemConfigService.get("TELEGRAM_BOT_USERNAME");
                if (botUsername == null || botUsername.isBlank()) {
                    botUsername = "AMGDL_Bot";
                }
                String msg = "✅ Bot activat\n\n"
                        + "El vostre assistent virtual ja és en línia. Els vostres clients poden escriure-us i rebran resposta automàtica.\n\n"
                        + "Canal Telegram actiu: t.me/" + botUsername;
                telegramBotClient.sendMessage(chatLink.getTelegramChatId(), msg);
            }

            String activateBotUsername = systemConfigService.get("TELEGRAM_BOT_USERNAME");
            if (activateBotUsername == null || activateBotUsername.isBlank()) activateBotUsername = "AMGDL_Bot";
            log.info("Bot activat per al tenant {}", tenantId);
            return ResponseEntity.ok(new com.amg.digitalitzacio.agents.api.dto.ChannelsResponse(
                    tenantId,
                    chatLink.getAgentMode().name(),
                    chatLink.getIsActive(),
                    chatLink.getTelegramChatId() != null,
                    chatLink.getTelegramChatId(),
                    "https://t.me/" + activateBotUsername,
                    chatLink.getWhatsappPhoneNumber(),
                    chatLink.getWhatsappMetaPhoneNumberId(),
                    chatLink.getEmailAddress(),
                    chatLink.getMetaPageId()
            ));
        } catch (Exception e) {
            log.error("Error activant bot per al tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /** Desactiva el bot per al tenant */
    @PostMapping("/{tenantId}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<com.amg.digitalitzacio.agents.api.dto.ChannelsResponse> deactivate(@PathVariable UUID tenantId) {
        try {
            var chatLink = tenantChatLinkRepository.findByTenantId(tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Chat link not found"));

            chatLink.setIsActive(false);
            chatLink = tenantChatLinkRepository.save(chatLink);

            String deactivateBotUsername = systemConfigService.get("TELEGRAM_BOT_USERNAME");
            if (deactivateBotUsername == null || deactivateBotUsername.isBlank()) deactivateBotUsername = "AMGDL_Bot";
            log.info("Bot desactivat per al tenant {}", tenantId);
            return ResponseEntity.ok(new com.amg.digitalitzacio.agents.api.dto.ChannelsResponse(
                    tenantId,
                    chatLink.getAgentMode().name(),
                    chatLink.getIsActive(),
                    chatLink.getTelegramChatId() != null,
                    chatLink.getTelegramChatId(),
                    "https://t.me/" + deactivateBotUsername,
                    chatLink.getWhatsappPhoneNumber(),
                    chatLink.getWhatsappMetaPhoneNumberId(),
                    chatLink.getEmailAddress(),
                    chatLink.getMetaPageId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error desactivant bot per al tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{tenantId}/health")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<AgentHealthResponse> getAgentHealth(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(agentHealthService.calculate(tenantId));
    }

    @PostMapping("/{tenantId}/email/test")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, Object>> testEmail(@PathVariable UUID tenantId) {
        var principal = getPrincipal();
        String recipientEmail = principal.email();

        var aiConfig = tenantAIConfigRepository.findByTenantId(tenantId).orElse(null);
        String senderEmail = aiConfig != null && aiConfig.getSenderEmail() != null ? aiConfig.getSenderEmail() : null;
        String senderName  = aiConfig != null && aiConfig.getSenderName()  != null ? aiConfig.getSenderName()  : "AMG Bot";
        String replyTo     = aiConfig != null && aiConfig.getReplyToEmail() != null ? aiConfig.getReplyToEmail() : null;

        try {
            emailChannel.sendMessage(
                recipientEmail,
                "Prova de configuració d'email — AMG",
                "Hola!\n\nEls emails del bot s'envien correctament.\n\nRemitent configurat: " +
                (senderEmail != null ? senderEmail : "noreply@amgdl.com (per defecte)") +
                "\nRespostes a: " + (replyTo != null ? replyTo : "no configurat") +
                "\n\nPer respondre a aquest email pots comprovar que arriba a l'adreça correcta.\n\nAMG Digitalització",
                senderEmail, senderName, replyTo
            );
            return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Email de prova enviat a " + recipientEmail + ". Comprova la safata d'entrada. " +
                           "Fes clic a 'Respondre' per verificar que el Reply-To és correcte."
            ));
        } catch (Exception e) {
            log.error("Email test failed for tenant {}: {}", tenantId, e.getMessage());
            return ResponseEntity.ok(Map.of(
                "ok", false,
                "message", "Error enviant l'email: " + e.getMessage() +
                           ". Comprova que BREVO_API_KEY està configurada a Admin → API Keys del Sistema."
            ));
        }
    }

    record PortalChatRequest(String message, String sessionId) {}
    record PortalChatResponse(String reply, boolean agentActive) {}

    @PostMapping("/{tenantId}/portal-chat")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<PortalChatResponse> portalChat(
            @PathVariable UUID tenantId,
            @RequestBody PortalChatRequest req) {
        if (req.message() == null || req.message().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String sessionId = req.sessionId() != null ? req.sessionId() : UUID.randomUUID().toString();
        String reply = conversationalAgentService.processWidgetMessage(tenantId, "portal:" + sessionId, req.message());
        if (reply == null) {
            return ResponseEntity.ok(new PortalChatResponse("L'agent no està actiu o no s'ha configurat encara.", false));
        }
        return ResponseEntity.ok(new PortalChatResponse(reply, true));
    }

    private UserPrincipal getPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new IllegalArgumentException("Not authenticated");
        }
        return (UserPrincipal) auth.getPrincipal();
    }

    private void validateTenantAccess(UUID requestedTenantId, UserPrincipal principal) {
        if (!requestedTenantId.equals(principal.tenantId())) {
            log.warn("Unauthorized access attempt: user {} tried to access tenant {}", principal.id(), requestedTenantId);
            throw new IllegalArgumentException("Unauthorized");
        }
    }
}
