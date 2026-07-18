package com.amg.digitalitzacio.content.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.content.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Automatització del Content Planner (Spec 58 §5). Els guards brief_sent_at / reminder_sent_at
 * i la transició d'estat garanteixen idempotència (no reenviaments duplicats).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ContentPlanScheduler {

    private final ContentPlanRepository planRepository;
    private final ContentPlanItemRepository itemRepository;
    private final TenantChatLinkRepository chatLinkRepository;
    private final TelegramBotClient telegramBotClient;
    private final ContentPlanPublishService publishService;

    /** Dilluns 10:00 — envia el brief de la setmana en curs per cada pla ACTIVE. */
    @Scheduled(cron = "0 0 10 * * MON")
    public void sendWeeklyBriefs() {
        LocalDate today = LocalDate.now();
        for (ContentPlan plan : planRepository.findByStatus(ContentPlanStatus.ACTIVE)) {
            var link = chatLinkRepository.findByTenantId(plan.getTenantId());
            if (link.isEmpty()) continue;
            itemRepository.findByPlanIdOrderByWeekNumberAsc(plan.getId()).stream()
                    .filter(i -> i.getStatus() == ContentItemStatus.PLANNED)
                    .filter(i -> inCurrentWeek(i.getPhotoDeadline(), today))
                    .findFirst()
                    .ifPresent(item -> {
                        try {
                            item.setStatus(ContentItemStatus.PHOTO_REQUESTED);
                            item.setBriefSentAt(Instant.now());
                            itemRepository.save(item);
                            telegramBotClient.sendMessage(link.get().getTelegramChatId(), briefMessage(item));
                        } catch (Exception e) {
                            log.error("Error enviant brief per item {}: {}", item.getId(), e.getMessage());
                        }
                    });
        }
    }

    /** Dimecres 10:00 — recordatori dels items amb foto demanada i encara sense foto. */
    @Scheduled(cron = "0 0 10 * * WED")
    public void sendReminders() {
        for (ContentPlanItem item : itemRepository
                .findByStatusAndMediaUrlIsNullAndReminderSentAtIsNull(ContentItemStatus.PHOTO_REQUESTED)) {
            try {
                var link = chatLinkRepository.findByTenantId(item.getTenantId());
                if (link.isEmpty()) continue;
                item.setReminderSentAt(Instant.now());
                itemRepository.save(item);
                telegramBotClient.sendMessage(link.get().getTelegramChatId(), reminderMessage(item));
            } catch (Exception e) {
                log.error("Error enviant recordatori per item {}: {}", item.getId(), e.getMessage());
            }
        }
    }

    /** Diari 08:30 — reintenta els items amb publicació fallida. */
    @Scheduled(cron = "0 30 8 * * *")
    public void retryFailed() {
        for (ContentPlanItem item : itemRepository.findByStatus(ContentItemStatus.FAILED)) {
            try {
                publishService.publishItem(item);
            } catch (Exception e) {
                log.warn("Reintent de publicació fallit per item {}: {}", item.getId(), e.getMessage());
            }
        }
    }

    private boolean inCurrentWeek(LocalDate date, LocalDate today) {
        if (date == null) return false;
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return !date.isBefore(weekStart) && !date.isAfter(weekEnd);
    }

    private String briefMessage(ContentPlanItem item) {
        return "📸 <b>Aquesta setmana toca</b>: " + pillarLabel(item.getPillar()) + "\n\n"
                + item.getBriefText()
                + (item.getExampleText() != null && !item.getExampleText().isBlank()
                        ? "\n<i>" + item.getExampleText() + "</i>" : "")
                + "\n\n🗓️ Envia'ns la foto abans de <b>" + item.getPhotoDeadline() + "</b>.";
    }

    private String reminderMessage(ContentPlanItem item) {
        return "😊 Encara ens falta la foto de <b>" + pillarLabel(item.getPillar())
                + "</b> per publicar aquesta setmana. Quan puguis, envia-nos-la — amb una del mòbil ja n'hi ha prou!";
    }

    private String pillarLabel(ContentPillar pillar) {
        if (pillar == null) return "";
        return switch (pillar) {
            case NOVELTY -> "Novetat";
            case COMBINE -> "Com combinar-ho";
            case SHOP -> "La botiga";
            case SOCIAL_PROOF -> "Prova social";
        };
    }
}
