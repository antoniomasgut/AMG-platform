package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.social.domain.LinkedInConnection;
import com.amg.digitalitzacio.social.domain.LinkedInConnectionRepository;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Publicació a LinkedIn via UGC Posts API (Mòdul 56 F4).
 * Publica com a autor personal (person URN) amb el token del tenant propietari.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LinkedInPublisherService {

    private static final String API_URL = "https://api.linkedin.com";

    private final LinkedInConnectionRepository connectionRepo;
    private final VaultEncryption vaultEncryption;
    private final ObjectMapper objectMapper;

    /** true si el tenant té una connexió LinkedIn activa */
    public boolean isConnected(UUID tenantId) {
        return connectionRepo.findByTenantId(tenantId).map(LinkedInConnection::isActive).orElse(false);
    }

    /**
     * Publica un post de només text a LinkedIn. Retorna l'ID del post (URN) o llança excepció.
     */
    public String publishText(UUID tenantId, String text) {
        LinkedInConnection conn = connectionRepo.findByTenantId(tenantId)
            .filter(LinkedInConnection::isActive)
            .orElseThrow(() -> new IllegalStateException("LinkedIn no connectat"));
        String token = vaultEncryption.decrypt(conn.getEncryptedAccessToken());

        Map<String, Object> payload = Map.of(
            "author", conn.getPersonUrn(),
            "lifecycleState", "PUBLISHED",
            "specificContent", Map.of(
                "com.linkedin.ugc.ShareContent", Map.of(
                    "shareCommentary", Map.of("text", text != null ? text : ""),
                    "shareMediaCategory", "NONE"
                )
            ),
            "visibility", Map.of("com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC")
        );

        var client = WebClient.builder().baseUrl(API_URL).build();
        try {
            String response = client.post()
                .uri("/v2/ugcPosts")
                .header("Authorization", "Bearer " + token)
                .header("X-Restli-Protocol-Version", "2.0.0")
                .header("Content-Type", "application/json")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .block();
            String id = extractId(response);
            log.info("Post LinkedIn publicat per tenant {}: {}", tenantId, id);
            return id != null ? id : "";
        } catch (Exception e) {
            log.warn("Error publicant a LinkedIn (tenant {}): {}", tenantId, e.getMessage());
            throw new RuntimeException("Error publicant a LinkedIn: " + e.getMessage());
        }
    }

    private String extractId(String response) {
        if (response == null) return null;
        try {
            var node = objectMapper.readTree(response);
            return node.path("id").asText(null);
        } catch (Exception e) {
            return null;
        }
    }
}
