package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.social.domain.SocialMetaConfig;
import com.amg.digitalitzacio.social.domain.SocialMetaConfigRepository;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Comentaris de xarxes → Telegram + resposta (Mòdul 55, feature 1).
 * Rep comentaris del webhook de Meta (camp feed), avisa el tenant per Telegram
 * i permet respondre'ls des d'allà via Graph API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialCommentService {

    private static final String GRAPH_URL = "https://graph.facebook.com";
    private static final String API_VERSION = "v22.0";

    private final SocialMetaConfigRepository metaConfigRepo;
    private final SocialFeatureService featureService;
    private final TenantChatLinkRepository chatLinkRepository;
    private final TelegramBotClient telegramBotClient;
    private final VaultEncryption vaultEncryption;
    private final StringRedisTemplate redis;

    /** Processa un comentari entrant d'una pàgina de Facebook */
    public void processFacebookComment(String pageId, String commentId, String message,
                                       String fromId, String fromName) {
        if (commentId == null) return;

        var meta = metaConfigRepo.findByFacebookPageId(pageId).orElse(null);
        if (meta == null) return;

        // Evita notificar les respostes de la mateixa pàgina
        if (fromId != null && fromId.equals(pageId)) return;

        var tenantId = meta.getTenantId();
        if (!featureService.get(tenantId).commentsToTelegram()) return;

        var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
        if (chatLink == null || chatLink.getTelegramChatId() == null) return;

        var sb = new StringBuilder();
        sb.append("💬 <b>Nou comentari a Facebook</b>\n");
        if (fromName != null && !fromName.isBlank()) {
            sb.append("👤 ").append(escapeHtml(fromName)).append("\n");
        }
        if (message != null && !message.isBlank()) {
            String m = message.length() > 400 ? message.substring(0, 397) + "…" : message;
            sb.append("\"").append(escapeHtml(m)).append("\"\n");
        }
        sb.append("\nToca <b>✍️ Respondre</b> per contestar des d'aquí.");

        // Desa el text del comentari perquè el suggeriment IA el pugui reutilitzar (Mòdul 56 F1)
        if (message != null && !message.isBlank()) {
            redis.opsForValue().set("cmt:text:" + commentId, message, 1, TimeUnit.HOURS);
        }

        var buttons = List.of(
            Map.of("text", "✍️ Respondre", "callback_data", "cmt:" + commentId),
            Map.of("text", "🤖 Suggerir resposta", "callback_data", "cmtai:" + commentId)
        );
        telegramBotClient.sendMessageWithButtons(chatLink.getTelegramChatId(), sb.toString(), buttons);
    }

    /** Recupera el text desat d'un comentari (per al suggeriment IA) */
    public String storedCommentText(String commentId) {
        return redis.opsForValue().get("cmt:text:" + commentId);
    }

    /** Publica una resposta a un comentari de Facebook */
    public boolean replyToComment(UUID tenantId, String commentId, String text) {
        if (text == null || text.isBlank()) return false;

        SocialMetaConfig meta = metaConfigRepo.findByTenantId(tenantId).orElse(null);
        if (meta == null || meta.getPageAccessTokenEncrypted() == null) {
            throw new IllegalStateException("Configuració Meta no disponible");
        }
        String token = vaultEncryption.decrypt(meta.getPageAccessTokenEncrypted());

        var client = WebClient.builder().baseUrl(GRAPH_URL).build();
        try {
            client.post()
                .uri("/{v}/{commentId}/comments", API_VERSION, commentId)
                .bodyValue(Map.of("message", text, "access_token", token))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(12))
                .block();
        } catch (Exception e) {
            log.warn("Error responent comentari {} (tenant {}): {}", commentId, tenantId, e.getMessage());
            throw new RuntimeException("Error publicant la resposta: " + e.getMessage());
        }
        log.info("Comentari {} respost pel tenant {}", commentId, tenantId);
        return true;
    }

    /** Resol el tenant propietari d'una pàgina de Facebook (per al webhook) */
    public UUID tenantForPage(String pageId) {
        return metaConfigRepo.findByFacebookPageId(pageId).map(SocialMetaConfig::getTenantId).orElse(null);
    }

    private String escapeHtml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
