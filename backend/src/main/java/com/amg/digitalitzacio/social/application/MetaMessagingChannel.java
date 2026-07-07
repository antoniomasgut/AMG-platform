package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.social.domain.SocialMetaConfig;
import com.amg.digitalitzacio.social.domain.SocialMetaConfigRepository;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Enviament de missatges directes de Messenger / Instagram via Graph API
 * usant el token de pàgina del tenant (Mòdul 56 F2).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetaMessagingChannel {

    private static final String GRAPH_URL = "https://graph.facebook.com";
    private static final String API_VERSION = "v22.0";

    private final SocialMetaConfigRepository metaConfigRepo;
    private final VaultEncryption vaultEncryption;

    /** Envia un DM a un destinatari (PSID de Messenger o IGSID d'Instagram) */
    public boolean sendMessage(UUID tenantId, String recipientId, String text) {
        SocialMetaConfig meta = metaConfigRepo.findByTenantId(tenantId).orElse(null);
        if (meta == null || meta.getPageAccessTokenEncrypted() == null || meta.getFacebookPageId() == null) {
            log.warn("Meta messaging: configuració incompleta per tenant {}", tenantId);
            return false;
        }
        String token = vaultEncryption.decrypt(meta.getPageAccessTokenEncrypted());
        var client = WebClient.builder().baseUrl(GRAPH_URL).build();
        try {
            client.post()
                .uri("/{v}/{pageId}/messages", API_VERSION, meta.getFacebookPageId())
                .bodyValue(Map.of(
                    "recipient", Map.of("id", recipientId),
                    "messaging_type", "RESPONSE",
                    "message", Map.of("text", text),
                    "access_token", token
                ))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(12))
                .block();
            return true;
        } catch (Exception e) {
            log.warn("Error enviant DM Meta a {} (tenant {}): {}", recipientId, tenantId, e.getMessage());
            return false;
        }
    }
}
