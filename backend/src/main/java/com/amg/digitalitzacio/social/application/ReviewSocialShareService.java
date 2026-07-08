package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.google.domain.GoogleBusinessReviewRepository;
import com.amg.digitalitzacio.social.domain.SocialPost;
import com.amg.digitalitzacio.social.domain.SocialPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Converteix una ressenya 5★ de Google en un post social (Mòdul 55, feature 4).
 * Genera un caption a partir del text de la ressenya, crea un draft i demana
 * confirmació al tenant per Telegram abans de publicar.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewSocialShareService {

    private final GoogleBusinessReviewRepository reviewRepo;
    private final SocialContentGeneratorService contentGenerator;
    private final SocialPostRepository postRepository;
    private final TenantRepository tenantRepository;
    private final TelegramBotClient telegramBotClient;
    private final SocialPublisherOrchestrator orchestrator;

    /** Genera el caption des de la ressenya, crea un draft i envia la previsualització */
    @Async
    public void preview(UUID tenantId, Long chatId, String reviewId) {
        var review = reviewRepo.findByTenantIdAndReviewId(tenantId, reviewId).orElse(null);
        if (review == null) {
            telegramBotClient.sendMessage(chatId, "⚠️ No he trobat la ressenya.");
            return;
        }

        var tenant = tenantRepository.findById(tenantId).orElse(null);
        String businessName = tenant != null && tenant.getName() != null ? tenant.getName() : "el negoci";
        String brief = "Comparteix aquesta ressenya de 5 estrelles com a testimoni. "
                + "Autor: " + (review.getAuthorName() != null ? review.getAuthorName() : "un client")
                + ". Ressenya: \"" + (review.getComment() != null ? review.getComment() : "Excel·lent servei") + "\"";

        String caption = contentGenerator.generateCaption("FACEBOOK", businessName, brief);
        if (caption == null || caption.isBlank()) caption = brief;

        var post = postRepository.save(SocialPost.builder()
                .tenantId(tenantId)
                .network("FACEBOOK")
                .postType("TEXT")
                .caption(caption)
                .status("DRAFT")
                .build());

        String text = "📢 <b>Compartir ressenya a Facebook</b>\n\n" + caption
                + "\n\nVols publicar-ho?";
        var button = Map.of("text", "✅ Publicar", "callback_data", "gpub:" + post.getId());
        telegramBotClient.sendMessageWithButtons(chatId, text, List.of(button));
    }

    /** Publica el draft creat des de la ressenya. Verifica que el post pertany al tenant del xat. */
    @Async
    public void publish(UUID tenantId, Long chatId, UUID postId) {
        var post = postRepository.findById(postId).orElse(null);
        if (post == null || !"DRAFT".equals(post.getStatus()) || !post.getTenantId().equals(tenantId)) {
            telegramBotClient.sendMessage(chatId, "⚠️ Aquest post ja no està disponible.");
            return;
        }
        orchestrator.publishNow(post);
        var refreshed = postRepository.findById(postId).orElse(post);
        if ("PUBLISHED".equals(refreshed.getStatus())) {
            telegramBotClient.sendMessage(chatId, "✅ Ressenya publicada a Facebook.");
        } else {
            telegramBotClient.sendMessage(chatId, "⚠️ No s'ha pogut publicar: "
                    + (refreshed.getErrorMessage() != null ? refreshed.getErrorMessage() : "error desconegut"));
        }
    }
}
