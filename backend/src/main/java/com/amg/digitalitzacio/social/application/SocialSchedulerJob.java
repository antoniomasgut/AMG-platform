package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.social.domain.SocialPost;
import com.amg.digitalitzacio.social.domain.SocialPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Publica els posts SCHEDULED quan arriba el seu scheduledAt.
 * S'executa cada minut.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SocialSchedulerJob {

    private final SocialPostRepository postRepository;
    private final SocialPublisherOrchestrator orchestrator;

    @Scheduled(cron = "0 * * * * *")
    public void publishScheduledPosts() {
        List<SocialPost> due = postRepository.findDueScheduled(Instant.now());
        if (due.isEmpty()) return;

        log.info("SocialScheduler: {} posts programats per publicar", due.size());

        for (SocialPost post : due) {
            post.setStatus("PUBLISHING");
            postRepository.save(post);

            try {
                var draft = new java.util.HashMap<String, String>();
                draft.put("tenantId",  post.getTenantId().toString());
                draft.put("step",      "PUBLISHING");
                draft.put("ig", "INSTAGRAM".equals(post.getNetwork()) ? "1" : "0");
                draft.put("fb", "FACEBOOK".equals(post.getNetwork()) ? "1" : "0");
                draft.put("gb", "GOOGLE_BUSINESS".equals(post.getNetwork()) ? "1" : "0");
                draft.put("postType",  post.getPostType());
                draft.put("caption",   post.getCaption() != null ? post.getCaption() : "");
                if (post.getMediaUrl() != null) draft.put("mediaUrl", post.getMediaUrl());

                orchestrator.publishAsync(post.getTenantId(), null, draft);
                postRepository.delete(post);
            } catch (Exception e) {
                log.error("Error publicant post programat {}: {}", post.getId(), e.getMessage());
                post.setStatus("FAILED");
                post.setErrorMessage(e.getMessage());
                postRepository.save(post);
            }
        }
    }
}
