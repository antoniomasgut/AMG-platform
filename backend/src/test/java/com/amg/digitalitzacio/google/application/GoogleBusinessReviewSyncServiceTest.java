package com.amg.digitalitzacio.google.application;

import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.FollowupLogRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLink;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.google.domain.GoogleBusinessReview;
import com.amg.digitalitzacio.google.domain.GoogleBusinessReviewRepository;
import com.amg.digitalitzacio.google.domain.GoogleModuleConfig;
import com.amg.digitalitzacio.google.domain.GoogleModuleConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GoogleBusinessReviewSyncServiceTest {

    @Mock GoogleModuleConfigRepository configRepo;
    @Mock GoogleBusinessReviewRepository reviewRepo;
    @Mock GoogleTokenService tokenService;
    @Mock TelegramBotClient telegramBotClient;
    @Mock TenantChatLinkRepository chatLinkRepository;
    @Mock NexeServiceConfigService nexeConfigService;
    @Mock FollowupLogRepository followupLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private GoogleBusinessReviewSyncService service;

    private static final UUID TENANT_ID = UUID.randomUUID();

    private GoogleBusinessReviewSyncService service() {
        if (service == null) {
            service = new GoogleBusinessReviewSyncService(configRepo, reviewRepo, tokenService,
                    objectMapper, telegramBotClient, chatLinkRepository, nexeConfigService,
                    followupLogRepository);
        }
        return service;
    }

    private void stubChatLink() {
        when(chatLinkRepository.findByTenantId(TENANT_ID))
                .thenReturn(Optional.of(TenantChatLink.builder()
                        .tenantId(TENANT_ID).telegramChatId(555L).build()));
    }

    private GoogleBusinessReview review(int rating) {
        return GoogleBusinessReview.builder()
                .id(UUID.randomUUID()).tenantId(TENANT_ID).reviewId("rev-" + rating)
                .rating(rating).authorName("Un Client").comment("Comentari de prova").build();
    }

    private void stubEnabledConfig() {
        var config = new GoogleModuleConfig();
        config.setTenantId(TENANT_ID);
        config.setBusinessEnabled(true);
        config.setBusinessLocationId("123");
        when(configRepo.findAll()).thenReturn(List.of(config));
    }

    // ── Notificació de ressenya negativa (Mòdul 57 F2) ────────────────────────

    @Test
    void notify_negativeReview_usesAlertHeader() {
        stubChatLink();
        when(nexeConfigService.get(eq(TENANT_ID), anyString())).thenReturn(Optional.empty());
        when(reviewRepo.findByTenantIdAndNotifiedAtIsNullOrderByReviewTimeDesc(TENANT_ID))
                .thenReturn(List.of(review(2)));

        service().notifyNewReviews(TENANT_ID);

        verify(telegramBotClient).sendMessageWithButtons(eq(555L),
                contains("🚨"), anyList());
    }

    @Test
    void notify_positiveReview_usesStandardHeader() {
        stubChatLink();
        when(nexeConfigService.get(eq(TENANT_ID), anyString())).thenReturn(Optional.empty());
        when(reviewRepo.findByTenantIdAndNotifiedAtIsNullOrderByReviewTimeDesc(TENANT_ID))
                .thenReturn(List.of(review(5)));

        service().notifyNewReviews(TENANT_ID);

        verify(telegramBotClient).sendMessageWithButtons(eq(555L),
                contains("⭐ <b>Nova ressenya"), anyList());
    }

    // ── Recordatori 48h (Mòdul 57 F2) ──────────────────────────────────────────

    @Test
    void reminder_unansweredNegative_sendsOnceAndLogs() {
        stubEnabledConfig();
        stubChatLink();
        var r = review(1);
        r.setNotifiedAt(Instant.now().minus(3, ChronoUnit.DAYS));
        when(reviewRepo.findByTenantIdAndRatingLessThanEqualAndReplyIsNullAndNotifiedAtBefore(
                eq(TENANT_ID), eq(3), any())).thenReturn(List.of(r));
        when(followupLogRepository.existsByTenantIdAndTypeAndEntityId(TENANT_ID, "NEGREV_48H", r.getId()))
                .thenReturn(false);

        service().remindUnansweredNegativeReviews();

        verify(telegramBotClient).sendMessageWithButtons(eq(555L), contains("Recordatori"), anyList());
        verify(followupLogRepository).save(argThat(f ->
                "NEGREV_48H".equals(f.getType()) && r.getId().equals(f.getEntityId())));
    }

    @Test
    void reminder_alreadySent_skips() {
        stubEnabledConfig();
        stubChatLink();
        var r = review(2);
        r.setNotifiedAt(Instant.now().minus(3, ChronoUnit.DAYS));
        when(reviewRepo.findByTenantIdAndRatingLessThanEqualAndReplyIsNullAndNotifiedAtBefore(
                eq(TENANT_ID), eq(3), any())).thenReturn(List.of(r));
        when(followupLogRepository.existsByTenantIdAndTypeAndEntityId(TENANT_ID, "NEGREV_48H", r.getId()))
                .thenReturn(true);

        service().remindUnansweredNegativeReviews();

        verify(telegramBotClient, never()).sendMessageWithButtons(anyLong(), anyString(), anyList());
        verify(followupLogRepository, never()).save(any());
    }

    @Test
    void reminder_noChatLink_skipsTenant() {
        stubEnabledConfig();
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());

        service().remindUnansweredNegativeReviews();

        verifyNoInteractions(telegramBotClient);
        verify(reviewRepo, never())
                .findByTenantIdAndRatingLessThanEqualAndReplyIsNullAndNotifiedAtBefore(any(), anyInt(), any());
    }
}
