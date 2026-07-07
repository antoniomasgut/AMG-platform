package com.amg.digitalitzacio.google.application;

import com.amg.digitalitzacio.google.domain.GoogleBusinessReviewRepository;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Estat conversacional del flux "respondre ressenya des de Telegram" (Mòdul 54).
 * Quan el tenant toca el botó "✍️ Respondre", desem a Redis quin reviewId espera
 * resposta per a aquell chat. El següent missatge de text es pren com a resposta.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleReviewReplyService {

    private static final String PENDING_KEY = "grev:pending:%s";
    private static final String DRAFT_KEY = "grev:draft:%s";
    private static final int TTL_MINUTES = 15;
    private static final String MODEL = "claude-haiku-4-5-20251001";

    private final StringRedisTemplate redis;
    private final GoogleBusinessReviewSyncService reviewSyncService;
    private final GoogleBusinessReviewRepository reviewRepo;
    private final AIProviderRouter aiRouter;

    /** Marca que aquest chat ha d'escriure una resposta per a la ressenya indicada */
    public void startReply(Long chatId, String reviewId) {
        redis.opsForValue().set(PENDING_KEY.formatted(chatId), reviewId, TTL_MINUTES, TimeUnit.MINUTES);
    }

    public boolean hasPending(Long chatId) {
        return Boolean.TRUE.equals(redis.hasKey(PENDING_KEY.formatted(chatId)));
    }

    /**
     * Consumeix la resposta pendent: publica el text a Google i esborra l'estat.
     * Retorna el missatge de confirmació per a Telegram.
     */
    public String submitReply(UUID tenantId, Long chatId, String text) {
        String key = PENDING_KEY.formatted(chatId);
        String reviewId = redis.opsForValue().get(key);
        if (reviewId == null) return null;
        redis.delete(key);

        try {
            reviewSyncService.replyToReview(tenantId, reviewId, text);
            return "✅ Resposta publicada a la ressenya de Google.";
        } catch (Exception e) {
            log.warn("Error publicant resposta de ressenya (tenant {}): {}", tenantId, e.getMessage());
            return "⚠️ No s'ha pogut publicar la resposta: " + e.getMessage();
        }
    }

    /** Genera un esborrany de resposta IA per a la ressenya i el desa a Redis (Mòdul 56 F1) */
    public String generateAndStoreDraft(Long chatId, UUID tenantId, String reviewId) {
        var review = reviewRepo.findByTenantIdAndReviewId(tenantId, reviewId).orElse(null);
        if (review == null) return null;

        String system = """
                Ets el responsable d'atenció al client d'un negoci local a Mallorca.
                Escriu una resposta breu i professional a una ressenya de Google.
                Usa el mateix idioma que la ressenya. To proper i agraït; si la ressenya és negativa,
                mostra empatia i ofereix solucionar-ho. Màxim 300 caràcters. Sense placeholders ni [claudàtors].
                Torna NOMÉS el text de la resposta.
                """;
        String user = "Valoració: " + (review.getRating() != null ? review.getRating() : "?") + "/5\n"
                + "Autor: " + (review.getAuthorName() != null ? review.getAuthorName() : "client") + "\n"
                + "Ressenya: \"" + (review.getComment() != null ? review.getComment() : "(sense text)") + "\"";

        String draft;
        try {
            draft = aiRouter.forModel(MODEL).chat(system, List.of(), user).trim();
        } catch (Exception e) {
            log.warn("Error generant resposta IA per ressenya (tenant {}): {}", tenantId, e.getMessage());
            return null;
        }
        if (draft.isBlank()) return null;
        redis.opsForValue().set(DRAFT_KEY.formatted(chatId), draft, TTL_MINUTES, TimeUnit.MINUTES);
        return draft;
    }

    /** Publica l'esborrany IA desat (Mòdul 56 F1) */
    public String publishDraft(UUID tenantId, Long chatId, String reviewId) {
        String key = DRAFT_KEY.formatted(chatId);
        String draft = redis.opsForValue().get(key);
        if (draft == null) return "⚠️ L'esborrany ha caducat. Torna a generar-lo.";
        redis.delete(key);
        try {
            reviewSyncService.replyToReview(tenantId, reviewId, draft);
            return "✅ Resposta publicada a la ressenya de Google.";
        } catch (Exception e) {
            log.warn("Error publicant esborrany IA (tenant {}): {}", tenantId, e.getMessage());
            return "⚠️ No s'ha pogut publicar la resposta: " + e.getMessage();
        }
    }
}
