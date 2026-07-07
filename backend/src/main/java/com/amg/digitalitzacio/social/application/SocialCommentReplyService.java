package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Estat conversacional "respondre comentari des de Telegram" (Mòdul 55, feature 1).
 * Quan el tenant toca "✍️ Respondre" en un comentari, desem a Redis quin commentId
 * espera resposta; el següent missatge de text es pren com a resposta.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialCommentReplyService {

    private static final String PENDING_KEY = "cmt:pending:%s";
    private static final String DRAFT_KEY = "cmt:draft:%s";
    private static final int TTL_MINUTES = 15;
    private static final String MODEL = "claude-haiku-4-5-20251001";

    private final StringRedisTemplate redis;
    private final SocialCommentService commentService;
    private final AIProviderRouter aiRouter;

    public void startReply(Long chatId, String commentId) {
        redis.opsForValue().set(PENDING_KEY.formatted(chatId), commentId, TTL_MINUTES, TimeUnit.MINUTES);
    }

    public boolean hasPending(Long chatId) {
        return Boolean.TRUE.equals(redis.hasKey(PENDING_KEY.formatted(chatId)));
    }

    public String submitReply(UUID tenantId, Long chatId, String text) {
        String key = PENDING_KEY.formatted(chatId);
        String commentId = redis.opsForValue().get(key);
        if (commentId == null) return null;
        redis.delete(key);

        try {
            commentService.replyToComment(tenantId, commentId, text);
            return "✅ Resposta publicada al comentari de Facebook.";
        } catch (Exception e) {
            log.warn("Error publicant resposta de comentari (tenant {}): {}", tenantId, e.getMessage());
            return "⚠️ No s'ha pogut publicar la resposta: " + e.getMessage();
        }
    }

    /** Genera un esborrany de resposta IA per al comentari i el desa a Redis (Mòdul 56 F1) */
    public String generateAndStoreDraft(Long chatId, String commentId) {
        String commentText = commentService.storedCommentText(commentId);
        if (commentText == null || commentText.isBlank()) return null;

        String system = """
                Ets el community manager d'un negoci local a Mallorca.
                Escriu una resposta breu i amable a un comentari de Facebook.
                Usa el mateix idioma que el comentari. To proper i propera. Màxim 250 caràcters.
                Sense placeholders ni [claudàtors]. Torna NOMÉS el text de la resposta.
                """;
        String draft;
        try {
            draft = aiRouter.forModel(MODEL).chat(system, List.of(), "Comentari: \"" + commentText + "\"").trim();
        } catch (Exception e) {
            log.warn("Error generant resposta IA per comentari: {}", e.getMessage());
            return null;
        }
        if (draft.isBlank()) return null;
        redis.opsForValue().set(DRAFT_KEY.formatted(chatId), draft, TTL_MINUTES, TimeUnit.MINUTES);
        return draft;
    }

    /** Publica l'esborrany IA desat (Mòdul 56 F1) */
    public String publishDraft(UUID tenantId, Long chatId, String commentId) {
        String key = DRAFT_KEY.formatted(chatId);
        String draft = redis.opsForValue().get(key);
        if (draft == null) return "⚠️ L'esborrany ha caducat. Torna a generar-lo.";
        redis.delete(key);
        try {
            commentService.replyToComment(tenantId, commentId, draft);
            return "✅ Resposta publicada al comentari de Facebook.";
        } catch (Exception e) {
            log.warn("Error publicant esborrany IA de comentari (tenant {}): {}", tenantId, e.getMessage());
            return "⚠️ No s'ha pogut publicar la resposta: " + e.getMessage();
        }
    }
}
