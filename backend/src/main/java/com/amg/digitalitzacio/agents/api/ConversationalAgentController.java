package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.api.dto.*;
import com.amg.digitalitzacio.agents.application.ContactService;
import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.Conversation;
import com.amg.digitalitzacio.agents.domain.ConversationRepository;
import com.amg.digitalitzacio.agents.domain.ConversationRole;
import com.amg.digitalitzacio.agents.domain.TenantAIConfig;
import com.amg.digitalitzacio.agents.domain.TenantAIConfigRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLink;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
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

    @GetMapping("/{tenantId}/conversations")
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

    @GetMapping("/{tenantId}/pending")
    public ResponseEntity<List<PendingResponseDto>> getPending(@PathVariable UUID tenantId) {
        try {
            var principal = getPrincipal();
            validateTenantAccess(tenantId, principal);

            var pending = conversationRepository.findByTenantIdAndPendingApprovalTrueOrderByCreatedAtDesc(tenantId);

            var responses = pending.stream()
                    .filter(c -> c.getRole() == ConversationRole.ASSISTANT)
                    .map(assistantMsg -> {
                        // Find preceding user message
                        String customerMessage = "";
                        var allMsgs = conversationRepository.findByTenantIdOrderByCreatedAtDesc(
                                tenantId, PageRequest.of(0, 1000));
                        for (int i = 0; i < allMsgs.size() - 1; i++) {
                            if (allMsgs.get(i).getId().equals(assistantMsg.getId()) &&
                                    i + 1 < allMsgs.size() &&
                                    allMsgs.get(i + 1).getRole() == ConversationRole.USER) {
                                customerMessage = allMsgs.get(i + 1).getContent();
                                break;
                            }
                        }

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
    public ResponseEntity<Void> editPending(
            @PathVariable UUID tenantId,
            @PathVariable Long id,
            @RequestBody EditPendingRequest request) {
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
    public ResponseEntity<Void> updateMode(
            @PathVariable UUID tenantId,
            @RequestBody AgentModeRequest request) {
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
        var chatLink = tenantChatLinkRepository.findByTenantId(tenantId).orElse(null);
        if (chatLink == null) {
            return ResponseEntity.ok(new com.amg.digitalitzacio.agents.api.dto.ChannelsResponse(
                    tenantId, "AUTO", false, false, null, null, null));
        }
        return ResponseEntity.ok(new com.amg.digitalitzacio.agents.api.dto.ChannelsResponse(
                tenantId,
                chatLink.getAgentMode().name(),
                chatLink.getIsActive(),
                chatLink.getTelegramChatId() != null,
                chatLink.getTelegramChatId(),
                chatLink.getWhatsappPhoneNumber(),
                chatLink.getWhatsappMetaPhoneNumberId()
        ));
    }

    /** Actualitza la configuració de canals d'un tenant */
    @PutMapping("/{tenantId}/channels")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<com.amg.digitalitzacio.agents.api.dto.ChannelsResponse> updateChannels(
            @PathVariable UUID tenantId,
            @RequestBody com.amg.digitalitzacio.agents.api.dto.UpdateChannelsRequest request) {
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
        chatLink = tenantChatLinkRepository.save(chatLink);
        return ResponseEntity.ok(new com.amg.digitalitzacio.agents.api.dto.ChannelsResponse(
                tenantId,
                chatLink.getAgentMode().name(),
                chatLink.getIsActive(),
                chatLink.getTelegramChatId() != null,
                chatLink.getTelegramChatId(),
                chatLink.getWhatsappPhoneNumber(),
                chatLink.getWhatsappMetaPhoneNumberId()
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
            @RequestBody AIConfigRequest request) {
        var config = tenantAIConfigRepository.findById(tenantId)
                .orElse(TenantAIConfig.defaultFor(tenantId));
        if (request.preferredModel() != null && !request.preferredModel().isBlank())
            config.setPreferredModel(request.preferredModel());
        if (request.maxTokens() != null)
            config.setMaxTokens(request.maxTokens());
        if (request.temperature() != null)
            config.setTemperature(request.temperature());
        return ResponseEntity.ok(tenantAIConfigRepository.save(config));
    }

    /** Test directe d'un model: envia un missatge i retorna la resposta */
    @PostMapping("/test-model")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Map<String, String>> testModel(@RequestBody AIModelTestRequest request) {
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

            log.info("Bot activat per al tenant {}", tenantId);
            return ResponseEntity.ok(new com.amg.digitalitzacio.agents.api.dto.ChannelsResponse(
                    tenantId,
                    chatLink.getAgentMode().name(),
                    chatLink.getIsActive(),
                    chatLink.getTelegramChatId() != null,
                    chatLink.getTelegramChatId(),
                    chatLink.getWhatsappPhoneNumber(),
                    chatLink.getWhatsappMetaPhoneNumberId()
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

            log.info("Bot desactivat per al tenant {}", tenantId);
            return ResponseEntity.ok(new com.amg.digitalitzacio.agents.api.dto.ChannelsResponse(
                    tenantId,
                    chatLink.getAgentMode().name(),
                    chatLink.getIsActive(),
                    chatLink.getTelegramChatId() != null,
                    chatLink.getTelegramChatId(),
                    chatLink.getWhatsappPhoneNumber(),
                    chatLink.getWhatsappMetaPhoneNumberId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error desactivant bot per al tenant {}", tenantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
