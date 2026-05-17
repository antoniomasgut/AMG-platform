package com.amg.digitalitzacio.agents.impl;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.Agent;
import com.amg.digitalitzacio.agents.domain.ScheduledAgentTask;
import com.amg.digitalitzacio.agents.domain.ScheduledAgentTaskRepository;
import com.amg.digitalitzacio.agents.domain.ScheduledTaskStatus;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import com.amg.digitalitzacio.vault.domain.CatalogServiceRepository;
import com.amg.digitalitzacio.vault.domain.CredentialFieldRepository;
import com.amg.digitalitzacio.vault.domain.TenantCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderAgent implements Agent {

    private static final String SLUG = "recordatori-24h";

    private final TenantChatLinkRepository chatLinkRepository;
    private final TenantCredentialRepository tenantCredentialRepository;
    private final CredentialFieldRepository credentialFieldRepository;
    private final CatalogServiceRepository catalogServiceRepository;
    private final ScheduledAgentTaskRepository taskRepository;
    private final TelegramBotClient telegramClient;
    private final VaultEncryption encryption;

    @Override
    public String getServiceSlug() {
        return SLUG;
    }

    @Override
    public String getDisplayName() {
        return "Recordatori 24h";
    }

    @Override
    public void onActivate(UUID tenantId) {
        var hoursRaw = getDecryptedCredential(tenantId, "hours_before");
        if (hoursRaw == null) {
            log.warn("ReminderAgent: hours_before no configurat per al tenant {}", tenantId);
            return;
        }

        var chatLink = chatLinkRepository.findByTenantId(tenantId);
        if (chatLink.isEmpty() || chatLink.get().getTelegramChatId() == null) {
            log.warn("ReminderAgent: tenant {} no té chat TG vinculat", tenantId);
            return;
        }

        try {
            var hoursBefore = Long.parseLong(hoursRaw);
            var scheduledAt = Instant.now().plus(hoursBefore, ChronoUnit.HOURS);
            var task = ScheduledAgentTask.builder()
                    .tenantId(tenantId)
                    .agentSlug(SLUG)
                    .taskType("SEND_REMINDER")
                    .payload("{\"type\":\"initial\"}")
                    .scheduledAt(scheduledAt)
                    .status(ScheduledTaskStatus.PENDING)
                    .build();
            taskRepository.save(task);
            log.info("ReminderAgent activat per al tenant {}, propera tasca: {}", tenantId, scheduledAt);

            // Send welcome message
            var template = getDecryptedCredential(tenantId, "reminder_template");
            var welcomeMsg = "✅ Agent Recordatori 24h activat!\n\n" +
                    (template != null ? "Plantilla: " + template : "");
            telegramClient.sendMessage(chatLink.get().getTelegramChatId(), welcomeMsg);
        } catch (NumberFormatException e) {
            log.warn("ReminderAgent: hours_before invalid per al tenant {}: {}", tenantId, hoursRaw);
        }
    }

    @Override
    public void onDeactivate(UUID tenantId) {
        log.info("ReminderAgent desactivat per al tenant {}", tenantId);
    }

    @Override
    public String handleMessage(UUID tenantId, String text, Long chatId) {
        var lower = text.toLowerCase();

        if (lower.contains("recordatori") || lower.contains("hora")) {
            var template = getDecryptedCredential(tenantId, "reminder_template");
            if (template != null) {
                return "🐶 " + template;
            }
            return "Tens un recordatori programat. En breu rebràs un missatge.";
        }

        if (lower.contains("ajuda") || lower.contains("help")) {
            return "Pots escriure \"recordatori\" per veure el teu pròxim recordatori, o contacta amb l'administrador.";
        }

        return null;
    }

    @Override
    public void executeTask(UUID tenantId, String taskType, String payload) {
        if (!"SEND_REMINDER".equals(taskType)) {
            log.warn("ReminderAgent: tipus de tasca desconegut: {}", taskType);
            return;
        }

        var chatLink = chatLinkRepository.findByTenantId(tenantId);
        if (chatLink.isEmpty() || chatLink.get().getTelegramChatId() == null) {
            log.warn("ReminderAgent: tenant {} no té chat TG vinculat", tenantId);
            return;
        }

        var template = getDecryptedCredential(tenantId, "reminder_template");
        if (template == null) {
            log.warn("ReminderAgent: plantilla no configurada per al tenant {}", tenantId);
            return;
        }

        var message = "🐶 RECORDATORI\n\n" + template;
        var sent = telegramClient.sendMessage(chatLink.get().getTelegramChatId(), message);
        if (sent) {
            log.info("ReminderAgent: recordatori enviat al tenant {}", tenantId);
        }
    }

    private String getDecryptedCredential(UUID tenantId, String fieldKey) {
        var serviceOpt = catalogServiceRepository.findBySlug(SLUG);
        if (serviceOpt.isEmpty()) return null;

        var fieldOpt = credentialFieldRepository.findByServiceIdAndKey(serviceOpt.get().getId(), fieldKey);
        if (fieldOpt.isEmpty()) return null;

        var tcOpt = tenantCredentialRepository.findByTenantIdAndFieldId(tenantId, fieldOpt.get().getId());
        if (tcOpt.isEmpty() || !tcOpt.get().getIsSet()) return null;

        return encryption.decrypt(tcOpt.get().getEncryptedValue());
    }
}
