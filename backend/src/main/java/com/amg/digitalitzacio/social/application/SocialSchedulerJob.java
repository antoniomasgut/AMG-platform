package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.social.domain.SocialPost;
import com.amg.digitalitzacio.social.domain.SocialPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Publica els posts SCHEDULED quan arriba el seu scheduledAt.
 * S'executa cada minut. Si un post falla, avisa el tenant per Telegram
 * (un post programat que mor en silenci només es veuria entrant al portal).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SocialSchedulerJob {

    private static final Map<String, String> NETWORK_LABELS = Map.of(
        "INSTAGRAM", "Instagram", "FACEBOOK", "Facebook",
        "GOOGLE_BUSINESS", "Google Business", "LINKEDIN", "LinkedIn");

    private final SocialPostRepository postRepository;
    private final SocialPublisherOrchestrator orchestrator;
    private final TenantChatLinkRepository chatLinkRepository;
    private final TelegramBotClient telegramBotClient;

    @Scheduled(cron = "0 * * * * *")
    public void publishScheduledPosts() {
        List<SocialPost> due = postRepository.findDueScheduled(Instant.now());
        if (due.isEmpty()) return;

        log.info("SocialScheduler: {} posts programats per publicar", due.size());

        for (SocialPost post : due) {
            post.setStatus("PUBLISHING");
            postRepository.save(post);
            // publishNow és síncron: actualitza l'estat del post existent (PUBLISHED o FAILED)
            orchestrator.publishNow(post);

            if ("FAILED".equals(post.getStatus())) {
                notifyFailure(post);
            }
        }
    }

    private void notifyFailure(SocialPost post) {
        try {
            var chatLink = chatLinkRepository.findByTenantId(post.getTenantId()).orElse(null);
            if (chatLink == null || chatLink.getTelegramChatId() == null) return;

            String caption = post.getCaption() == null ? ""
                : post.getCaption().length() > 60 ? post.getCaption().substring(0, 57) + "…" : post.getCaption();
            telegramBotClient.sendMessage(chatLink.getTelegramChatId(),
                "⚠️ <b>No s'ha pogut publicar el post programat</b>\n"
                + "Xarxa: " + NETWORK_LABELS.getOrDefault(post.getNetwork(), post.getNetwork()) + "\n"
                + (caption.isBlank() ? "" : "Text: \"" + caption + "\"\n")
                + "Motiu: " + (post.getErrorMessage() != null ? post.getErrorMessage() : "error desconegut")
                + "\n\nPots tornar-lo a programar amb <code>/publica</code>.");
        } catch (Exception e) {
            log.warn("No s'ha pogut avisar el tenant {} de la fallada del post {}: {}",
                post.getTenantId(), post.getId(), e.getMessage());
        }
    }
}
