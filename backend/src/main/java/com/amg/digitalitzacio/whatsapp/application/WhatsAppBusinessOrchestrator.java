package com.amg.digitalitzacio.whatsapp.application;

import com.amg.digitalitzacio.agents.application.ConversationalAgentService;
import com.amg.digitalitzacio.agents.application.InboundAssistService;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import com.amg.digitalitzacio.whatsapp.api.dto.WhatsAppConfigResponse;
import com.amg.digitalitzacio.whatsapp.api.dto.WhatsAppConnectRequest;
import com.amg.digitalitzacio.whatsapp.domain.WhatsAppConnectionStatus;
import com.amg.digitalitzacio.whatsapp.domain.WhatsAppWabaConfig;
import com.amg.digitalitzacio.whatsapp.domain.WhatsAppWabaConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WhatsAppBusinessOrchestrator {

    private final WhatsAppWabaConfigRepository configRepository;
    private final MetaWhatsAppClient metaClient;
    private final VaultEncryption vaultEncryption;
    private final SystemConfigService systemConfigService;
    private final ConversationalAgentService conversationalAgentService;
    private final InboundAssistService inboundAssistService;

    @Transactional(readOnly = true)
    public WhatsAppConfigResponse getConfig(UUID tenantId) {
        var config = configRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WhatsApp no configurat per al tenant: " + tenantId));
        return toResponse(config);
    }

    public WhatsAppConfigResponse connect(UUID tenantId, WhatsAppConnectRequest req) {
        if (req.phoneNumberId() == null || req.phoneNumberId().isBlank()
                || req.accessToken() == null || req.accessToken().isBlank()) {
            throw new IllegalArgumentException("phoneNumberId i accessToken són obligatoris");
        }

        var config = configRepository.findByTenantId(tenantId)
                .orElseGet(() -> WhatsAppWabaConfig.builder().tenantId(tenantId).build());

        config.setPhoneNumberId(req.phoneNumberId());
        config.setAccessTokenEncrypted(vaultEncryption.encrypt(req.accessToken()));
        config.setStatus(WhatsAppConnectionStatus.PENDING);
        config.setWebhookRegistered(false);

        if (req.wabaId() != null && !req.wabaId().isBlank()) {
            config.setWabaId(req.wabaId());
        }

        config = configRepository.save(config);
        log.info("WhatsApp WABA config saved for tenant {} (phoneNumberId={})", tenantId, req.phoneNumberId());
        return toResponse(config);
    }

    public WhatsAppConfigResponse verify(UUID tenantId) {
        var config = configRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WhatsApp no configurat per al tenant: " + tenantId));

        String token = vaultEncryption.decrypt(config.getAccessTokenEncrypted());
        boolean valid = metaClient.verifyToken(config.getPhoneNumberId(), token);

        if (!valid) {
            config.setStatus(WhatsAppConnectionStatus.ERROR);
            configRepository.save(config);
            throw new IllegalStateException("Token d'accés invàlid o phoneNumberId incorrecte");
        }

        // Obté info del WABA si tenim el wabaId
        if (config.getWabaId() != null && !config.getWabaId().isBlank()) {
            try {
                var info = metaClient.fetchWabaInfo(token, config.getWabaId());
                config.setDisplayPhoneNumber(info.displayPhone());
                config.setBusinessName(info.businessName());
            } catch (Exception e) {
                log.warn("Could not fetch WABA info for tenant {}: {}", tenantId, e.getMessage());
            }
        }

        config.setStatus(WhatsAppConnectionStatus.CONNECTED);
        config.setWebhookRegistered(true);
        config.setConnectedAt(Instant.now());
        config = configRepository.save(config);

        log.info("WhatsApp WABA verified for tenant {} — CONNECTED", tenantId);
        return toResponse(config);
    }

    public void disconnect(UUID tenantId) {
        var config = configRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WhatsApp no configurat per al tenant: " + tenantId));
        config.setStatus(WhatsAppConnectionStatus.DISCONNECTED);
        config.setWebhookRegistered(false);
        configRepository.save(config);
        log.info("WhatsApp WABA disconnected for tenant {}", tenantId);
    }

    public void sendTestMessage(UUID tenantId, String toPhone) {
        var config = configRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("WhatsApp no configurat per al tenant: " + tenantId));

        if (config.getStatus() != WhatsAppConnectionStatus.CONNECTED) {
            throw new IllegalStateException("WhatsApp no connectat — verifica primer la connexió");
        }

        String token = vaultEncryption.decrypt(config.getAccessTokenEncrypted());
        metaClient.sendTextMessage(config.getPhoneNumberId(), token, toPhone,
                "✅ Missatge de prova des d'AMG Digitalitzacions. El vostre WhatsApp Business és operatiu.");
        log.info("Test WhatsApp sent to {} for tenant {}", toPhone, tenantId);
    }

    /**
     * Punt d'entrada del webhook de Meta — enruta el missatge al tenant correcte
     * pel phoneNumberId i el passa a ConversationalAgentService.
     */
    public void handleWebhookMessage(String phoneNumberId, String from, String text) {
        var configOpt = configRepository.findByPhoneNumberId(phoneNumberId);
        if (configOpt.isEmpty()) {
            log.warn("Incoming WhatsApp message for unknown phoneNumberId={}", phoneNumberId);
            return;
        }
        var config = configOpt.get();
        if (config.getStatus() != WhatsAppConnectionStatus.CONNECTED) {
            log.info("WhatsApp message ignored — tenant {} not CONNECTED", config.getTenantId());
            return;
        }
        // Inbound Assist (Spec 59): si el tenant és HYBRID, esborrany + aprovació per Telegram.
        if (inboundAssistService.tryIntake(config.getTenantId(), ConversationChannel.WHATSAPP_META, from, null, text)) {
            return;
        }
        conversationalAgentService.handleIncoming(
                config.getTenantId(), from, ConversationChannel.WHATSAPP_META, text);
    }

    private WhatsAppConfigResponse toResponse(WhatsAppWabaConfig c) {
        return new WhatsAppConfigResponse(
                c.getId(), c.getTenantId(), c.getWabaId(), c.getPhoneNumberId(),
                c.getDisplayPhoneNumber(), c.getBusinessName(),
                c.getStatus().name(), c.getWebhookRegistered(),
                c.getConnectedAt(), c.getCreatedAt());
    }
}
