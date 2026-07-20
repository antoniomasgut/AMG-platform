package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.NexeServiceConfig;
import com.amg.digitalitzacio.agents.domain.NexeServiceConfigRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLink;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.social.domain.SocialMetaConfigRepository;
import com.amg.digitalitzacio.social.domain.SocialPost;
import com.amg.digitalitzacio.social.domain.SocialPostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocialSuggestionSchedulerTest {

    @Mock NexeServiceConfigRepository nexeConfigRepo;
    @Mock SocialFeatureService featureService;
    @Mock SocialContentGeneratorService contentGenerator;
    @Mock SocialAnalyticsService analyticsService;
    @Mock TenantChatLinkRepository chatLinkRepository;
    @Mock TenantRepository tenantRepository;
    @Mock TelegramBotClient telegramBotClient;
    @Mock SocialMetaConfigRepository metaConfigRepo;
    @Mock SocialPostRepository postRepository;

    private static final UUID TENANT_ID = UUID.randomUUID();

    private SocialSuggestionScheduler scheduler() {
        return new SocialSuggestionScheduler(nexeConfigRepo, featureService, contentGenerator,
                analyticsService, chatLinkRepository, tenantRepository, telegramBotClient,
                metaConfigRepo, postRepository);
    }

    private NexeServiceConfig tenantConfig() {
        var c = new NexeServiceConfig();
        c.setTenantId(TENANT_ID);
        c.setServiceKey("SOCIAL_PUBLISHER");
        return c;
    }

    private void stubChatLink() {
        var link = new TenantChatLink();
        link.setTenantId(TENANT_ID);
        link.setTelegramChatId(111L);
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(link));
    }

    // ─── P22: recordatori inactivitat setmanal ────────────────────────────────

    @Test
    void nudge_noPublicatEn7Dies_envia() {
        when(nexeConfigRepo.findByServiceKey("SOCIAL_PUBLISHER")).thenReturn(List.of(tenantConfig()));
        when(postRepository.findPublishedSince(eq(TENANT_ID), any())).thenReturn(List.of());
        stubChatLink();

        scheduler().nudgeInactiveTenants();

        verify(telegramBotClient).sendMessage(eq(111L),
            argThat(msg -> msg.contains("/publica") && msg.contains("7 dies")));
    }

    @Test
    void nudge_haTingutPublicacionRecent_noEnvia() {
        when(nexeConfigRepo.findByServiceKey("SOCIAL_PUBLISHER")).thenReturn(List.of(tenantConfig()));
        var recentPost = SocialPost.builder().id(UUID.randomUUID()).tenantId(TENANT_ID)
                .status("PUBLISHED").publishedAt(Instant.now()).build();
        when(postRepository.findPublishedSince(eq(TENANT_ID), any())).thenReturn(List.of(recentPost));

        scheduler().nudgeInactiveTenants();

        verify(telegramBotClient, never()).sendMessage(anyLong(), anyString());
    }

    @Test
    void nudge_senseChatLink_noPeta() {
        when(nexeConfigRepo.findByServiceKey("SOCIAL_PUBLISHER")).thenReturn(List.of(tenantConfig()));
        when(postRepository.findPublishedSince(eq(TENANT_ID), any())).thenReturn(List.of());
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());

        scheduler().nudgeInactiveTenants();

        verify(telegramBotClient, never()).sendMessage(anyLong(), anyString());
    }

    // ─── P23: neteja de posts encallats en PUBLISHING ─────────────────────────

    @Test
    void cleanup_postEnPublishing_marcaFailed() {
        var stuck = SocialPost.builder()
                .id(UUID.randomUUID()).tenantId(TENANT_ID)
                .status("PUBLISHING").scheduledAt(Instant.now().minusSeconds(600))
                .build();
        when(postRepository.findStuckPublishing(any())).thenReturn(List.of(stuck));

        scheduler().cleanupStuckPublishingPosts();

        verify(postRepository).save(argThat(p ->
            "FAILED".equals(p.getStatus()) && p.getErrorMessage() != null));
    }

    @Test
    void cleanup_sensePostsEncallats_noFaRes() {
        when(postRepository.findStuckPublishing(any())).thenReturn(List.of());

        scheduler().cleanupStuckPublishingPosts();

        verify(postRepository, never()).save(any());
    }
}
