package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.engine.domain.LandingVisitDailyRepository;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.leads.domain.LeadSource;
import com.amg.digitalitzacio.social.domain.SocialMetaConfigRepository;
import com.amg.digitalitzacio.social.domain.SocialPost;
import com.amg.digitalitzacio.social.domain.SocialPostRepository;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocialAnalyticsServiceTest {

    @Mock SocialPostRepository postRepository;
    @Mock SocialMetaConfigRepository metaConfigRepo;
    @Mock VaultEncryption vaultEncryption;
    @Mock LandingVisitDailyRepository visitDailyRepository;
    @Mock LeadRepository leadRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final UUID TENANT_ID = UUID.randomUUID();

    private SocialAnalyticsService service() {
        return new SocialAnalyticsService(postRepository, metaConfigRepo, vaultEncryption,
                objectMapper, visitDailyRepository, leadRepository);
    }

    private SocialPost post(String type, String caption, Integer reach, Integer likes) {
        return SocialPost.builder()
                .id(UUID.randomUUID()).tenantId(TENANT_ID)
                .network("INSTAGRAM").postType(type).caption(caption)
                .status("PUBLISHED").publishedAt(Instant.now())
                .reach(reach).likes(likes)
                .build();
    }

    // ─── buildPerformanceContext (P3) ────────────────────────────────────────

    @Test
    void performanceContextDestacaElMillorPost() {
        when(postRepository.findPublishedSince(eq(TENANT_ID), any())).thenReturn(List.of(
            post("PHOTO", "Foto del local renovat", 400, 30),
            post("PHOTO", "Una altra foto", 100, 5),
            post("TEXT", "Text sense mèdia", 50, 2)));

        String ctx = service().buildPerformanceContext(TENANT_ID);

        assertThat(ctx).contains("Foto del local renovat");
        assertThat(ctx).contains("PHOTO");
        // PHOTO té 2 posts → mitjana inclosa; TEXT només 1 → exclosa
        assertThat(ctx).contains("PHOTO: 2 posts");
        assertThat(ctx).doesNotContain("TEXT: 1 posts");
    }

    @Test
    void performanceContextSenseMetriquesRetornaNull() {
        when(postRepository.findPublishedSince(eq(TENANT_ID), any())).thenReturn(List.of(
            post("PHOTO", "Sense mètriques", null, null)));

        assertThat(service().buildPerformanceContext(TENANT_ID)).isNull();
    }

    @Test
    void performanceContextSensePostsRetornaNull() {
        when(postRepository.findPublishedSince(eq(TENANT_ID), any())).thenReturn(List.of());
        assertThat(service().buildPerformanceContext(TENANT_ID)).isNull();
    }

    // ─── buildWeeklyDigest amb secció de trànsit (P2) ────────────────────────

    @Test
    void digestInclouTransitPerFontILeads() {
        when(postRepository.findPublishedSince(eq(TENANT_ID), any())).thenReturn(List.of(
            post("PHOTO", "Un post", 100, 10)));
        var igViews = sourceViews("instagram", 42);
        var directViews = sourceViews("direct", 300);
        when(visitDailyRepository.sumByTenantSince(eq(TENANT_ID), any()))
            .thenReturn(List.of(directViews, igViews));
        when(leadRepository.countByTenantIdAndSourceSince(eq(TENANT_ID), eq(LeadSource.INSTAGRAM), any()))
            .thenReturn(3L);
        when(leadRepository.countByTenantIdAndSourceSince(eq(TENANT_ID), eq(LeadSource.FACEBOOK), any()))
            .thenReturn(0L);

        String digest = service().buildWeeklyDigest(TENANT_ID);

        assertThat(digest).contains("Trànsit des de xarxes");
        assertThat(digest).contains("Instagram: <b>42</b> visites");
        assertThat(digest).doesNotContain("direct");         // el trànsit directe no és social
        assertThat(digest).contains("Contactes nous des de xarxes: <b>3</b>");
    }

    @Test
    void digestSenseTransitSocialNoAfegeixSeccio() {
        when(postRepository.findPublishedSince(eq(TENANT_ID), any())).thenReturn(List.of(
            post("PHOTO", "Un post", 100, 10)));
        when(visitDailyRepository.sumByTenantSince(eq(TENANT_ID), any()))
            .thenReturn(List.of(sourceViews("direct", 300)));
        when(leadRepository.countByTenantIdAndSourceSince(eq(TENANT_ID), any(), any())).thenReturn(0L);

        String digest = service().buildWeeklyDigest(TENANT_ID);

        assertThat(digest).doesNotContain("Trànsit des de xarxes");
    }

    private LandingVisitDailyRepository.SourceViews sourceViews(String source, long views) {
        return new LandingVisitDailyRepository.SourceViews() {
            @Override public String getSource() { return source; }
            @Override public long getViews() { return views; }
        };
    }
}
