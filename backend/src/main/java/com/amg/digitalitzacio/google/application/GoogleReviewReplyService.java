package com.amg.digitalitzacio.google.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
    private static final int TTL_MINUTES = 15;

    private final StringRedisTemplate redis;
    private final GoogleBusinessReviewSyncService reviewSyncService;

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
}
