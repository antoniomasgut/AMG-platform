package com.amg.digitalitzacio.content.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.TenantChatLink;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.content.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContentPlanSchedulerTest {

    @Mock ContentPlanRepository planRepository;
    @Mock ContentPlanItemRepository itemRepository;
    @Mock TenantChatLinkRepository chatLinkRepository;
    @Mock TelegramBotClient telegramBotClient;
    @Mock ContentPlanPublishService publishService;
    @InjectMocks ContentPlanScheduler scheduler;

    private final UUID TENANT = UUID.randomUUID();

    private TenantChatLink linkTo(long chatId) {
        TenantChatLink link = mock(TenantChatLink.class);
        when(link.getTelegramChatId()).thenReturn(chatId);
        return link;
    }

    @Test
    void sendWeeklyBriefs_transitionsItemAndSendsBrief() {
        UUID planId = UUID.randomUUID();
        ContentPlan plan = ContentPlan.builder().id(planId).tenantId(TENANT).status(ContentPlanStatus.ACTIVE).build();
        LocalDate thisWeekThu = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .plusDays(3);
        ContentPlanItem item = ContentPlanItem.builder().id(UUID.randomUUID()).planId(planId).tenantId(TENANT)
                .weekNumber(1).pillar(ContentPillar.NOVELTY).briefText("Foto novetat")
                .status(ContentItemStatus.PLANNED).photoDeadline(thisWeekThu).build();

        TenantChatLink link = linkTo(123L);
        when(planRepository.findByStatus(ContentPlanStatus.ACTIVE)).thenReturn(List.of(plan));
        when(chatLinkRepository.findByTenantId(TENANT)).thenReturn(Optional.of(link));
        when(itemRepository.findByPlanIdOrderByWeekNumberAsc(planId)).thenReturn(List.of(item));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.sendWeeklyBriefs();

        assertThat(item.getStatus()).isEqualTo(ContentItemStatus.PHOTO_REQUESTED);
        assertThat(item.getBriefSentAt()).isNotNull();
        verify(telegramBotClient).sendMessage(eq(123L), anyString());
    }

    @Test
    void sendWeeklyBriefs_noChatLink_skips() {
        ContentPlan plan = ContentPlan.builder().id(UUID.randomUUID()).tenantId(TENANT).status(ContentPlanStatus.ACTIVE).build();
        when(planRepository.findByStatus(ContentPlanStatus.ACTIVE)).thenReturn(List.of(plan));
        when(chatLinkRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());

        scheduler.sendWeeklyBriefs();

        verifyNoInteractions(telegramBotClient);
    }

    @Test
    void sendReminders_marksAndSends() {
        ContentPlanItem item = ContentPlanItem.builder().id(UUID.randomUUID()).tenantId(TENANT)
                .pillar(ContentPillar.SHOP).status(ContentItemStatus.PHOTO_REQUESTED).build();
        TenantChatLink link = linkTo(456L);
        when(itemRepository.findByStatusAndMediaUrlIsNullAndReminderSentAtIsNull(ContentItemStatus.PHOTO_REQUESTED))
                .thenReturn(List.of(item));
        when(chatLinkRepository.findByTenantId(TENANT)).thenReturn(Optional.of(link));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.sendReminders();

        assertThat(item.getReminderSentAt()).isNotNull();
        verify(telegramBotClient).sendMessage(eq(456L), anyString());
    }

    @Test
    void retryFailed_republishesFailedItems() {
        ContentPlanItem item = ContentPlanItem.builder().id(UUID.randomUUID()).tenantId(TENANT)
                .status(ContentItemStatus.FAILED).build();
        when(itemRepository.findByStatus(ContentItemStatus.FAILED)).thenReturn(List.of(item));

        scheduler.retryFailed();

        verify(publishService).publishItem(item);
    }
}
