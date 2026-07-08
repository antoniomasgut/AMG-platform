package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.FollowupLogRepository;
import com.amg.digitalitzacio.agents.domain.NexeServiceConfigRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLink;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.google.domain.GoogleBusinessReview;
import com.amg.digitalitzacio.google.domain.GoogleBusinessReviewRepository;
import com.amg.digitalitzacio.social.application.SocialFeatureService.SocialFeatures;
import com.amg.digitalitzacio.social.domain.SocialPost;
import com.amg.digitalitzacio.social.domain.SocialPostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocialProofPostSchedulerTest {

    @Mock NexeServiceConfigRepository nexeConfigRepo;
    @Mock SocialFeatureService featureService;
    @Mock SocialContentGeneratorService contentGenerator;
    @Mock SocialPostRepository postRepository;
    @Mock GoogleBusinessReviewRepository reviewRepository;
    @Mock TenantChatLinkRepository chatLinkRepository;
    @Mock TenantRepository tenantRepository;
    @Mock TelegramBotClient telegramBotClient;
    @Mock FollowupLogRepository followupLogRepository;

    @InjectMocks SocialProofPostScheduler scheduler;

    private static final UUID TENANT_ID = UUID.randomUUID();

    private void stubHappyPath(int reviewCount, int rating) {
        when(featureService.get(TENANT_ID))
                .thenReturn(new SocialFeatures(false, false, false, true, false));
        when(chatLinkRepository.findByTenantId(TENANT_ID))
                .thenReturn(Optional.of(TenantChatLink.builder()
                        .tenantId(TENANT_ID).telegramChatId(123L).build()));
        when(followupLogRepository.existsByTenantIdAndTypeAndEntityId(eq(TENANT_ID), anyString(), eq(TENANT_ID)))
                .thenReturn(false);
        var reviews = IntStream.range(0, reviewCount)
                .mapToObj(i -> GoogleBusinessReview.builder()
                        .tenantId(TENANT_ID).reviewId("r" + i).rating(rating).build())
                .toList();
        when(reviewRepository.findByTenantIdAndRatingGreaterThanEqualOrderByRatingDescReviewTimeDesc(TENANT_ID, 1))
                .thenReturn(reviews);
        when(tenantRepository.findById(TENANT_ID))
                .thenReturn(Optional.of(Tenant.builder().id(TENANT_ID).name("Perruqueria Test").slug("test").build()));
        when(postRepository.save(any())).thenAnswer(i -> {
            SocialPost p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });
    }

    @Test
    void happyPath_createsDraftAndSendsProposal() {
        stubHappyPath(10, 5);
        when(contentGenerator.generateCaption(eq("FACEBOOK"), anyString(), anyString()))
                .thenReturn("Caption IA");

        scheduler.proposeForTenant(TENANT_ID);

        verify(postRepository).save(argThat(p ->
                "DRAFT".equals(p.getStatus()) && TENANT_ID.equals(p.getTenantId())
                        && "Caption IA".equals(p.getCaption())));
        verify(telegramBotClient).sendMessageWithButtons(eq(123L), contains("Caption IA"),
                argThat(btns -> ((List<?>) btns).size() == 1));
        verify(followupLogRepository).save(argThat(f ->
                f.getType().startsWith("SOCPROOF_") && TENANT_ID.equals(f.getEntityId())));
    }

    @Test
    void aiFails_usesDeterministicCaption() {
        stubHappyPath(8, 4);
        when(contentGenerator.generateCaption(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("IA caiguda"));

        scheduler.proposeForTenant(TENANT_ID);

        verify(postRepository).save(argThat(p -> p.getCaption().contains("ressenyes a Google")));
        verify(telegramBotClient).sendMessageWithButtons(anyLong(), anyString(), any());
    }

    @Test
    void tooFewReviews_doesNothing() {
        stubHappyPath(4, 5);

        scheduler.proposeForTenant(TENANT_ID);

        verifyNoInteractions(postRepository, telegramBotClient);
    }

    @Test
    void lowAverage_doesNothing() {
        stubHappyPath(10, 3);

        scheduler.proposeForTenant(TENANT_ID);

        verifyNoInteractions(postRepository, telegramBotClient);
    }

    @Test
    void toggleOff_doesNothing() {
        stubHappyPath(10, 5);
        when(featureService.get(TENANT_ID))
                .thenReturn(new SocialFeatures(false, false, false, false, false));

        scheduler.proposeForTenant(TENANT_ID);

        verifyNoInteractions(postRepository, telegramBotClient, reviewRepository);
    }

    @Test
    void alreadyProposedThisMonth_doesNothing() {
        stubHappyPath(10, 5);
        when(followupLogRepository.existsByTenantIdAndTypeAndEntityId(eq(TENANT_ID), anyString(), eq(TENANT_ID)))
                .thenReturn(true);

        scheduler.proposeForTenant(TENANT_ID);

        verifyNoInteractions(postRepository, telegramBotClient);
        verify(followupLogRepository, never()).save(any());
    }

    @Test
    void noChatLink_doesNothing() {
        stubHappyPath(10, 5);
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());

        scheduler.proposeForTenant(TENANT_ID);

        verifyNoInteractions(postRepository, telegramBotClient);
    }
}
