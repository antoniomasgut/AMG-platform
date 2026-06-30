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
            // publishNow és síncron: actualitza l'estat del post existent (PUBLISHED o FAILED)
            orchestrator.publishNow(post);
        }
    }
}
