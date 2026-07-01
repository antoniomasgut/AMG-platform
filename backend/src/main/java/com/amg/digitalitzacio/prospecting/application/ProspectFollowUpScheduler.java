package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.prospecting.domain.*;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Comprova cada dia a les 8:00 quins prospects CONTACTED porten N dies sense resposta
 * i envia recordatoris de follow-up (màx 3 per prospect).
 * Només actua sobre campanyes amb autoSequenceEnabled = true.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProspectFollowUpScheduler {

    private static final int MAX_FOLLOWUPS = 3;

    private final ProspectRepository prospectRepository;
    private final ProspectCampaignRepository campaignRepository;
    private final TelegramBotClient telegramBotClient;
    private final SystemConfigService sysConfig;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void runFollowUpCheck() {
        var campaigns = campaignRepository.findByAutoSequenceEnabled(true);
        if (campaigns.isEmpty()) return;

        log.info("Follow-up scheduler: comprovant {} campanyes actives", campaigns.size());

        // Índex campaignId → campanya per accés ràpid
        Map<UUID, ProspectCampaign> campaignMap = campaigns.stream()
                .collect(Collectors.toMap(ProspectCampaign::getId, c -> c));

        // Talla conservadora: busca prospects que porten almenys followupDays1 dies
        int minDays = campaigns.stream()
                .mapToInt(c -> c.getFollowupDays1() != null ? c.getFollowupDays1() : 3)
                .min().orElse(3);

        Instant cutoff = Instant.now().minus(minDays, ChronoUnit.DAYS);
        var candidates = prospectRepository.findContactedWithPendingFollowup(cutoff);

        int processed = 0;
        for (var prospect : candidates) {
            var campaign = campaignMap.get(prospect.getCampaignId());
            if (campaign == null) continue;

            int followupCount = prospect.getFollowupCount() != null ? prospect.getFollowupCount() : 0;
            if (followupCount >= MAX_FOLLOWUPS) continue;

            // Determina quants dies han passat des del darrer contacte
            Instant lastContact = prospect.getLastFollowupAt() != null
                    ? prospect.getLastFollowupAt()
                    : prospect.getPitchSentAt();
            if (lastContact == null) continue;

            long daysSince = ChronoUnit.DAYS.between(lastContact, Instant.now());

            // Comprova si toca el follow-up N
            int nextFollowup = followupCount + 1;
            int requiredDays = getDaysForFollowup(campaign, nextFollowup);

            if (daysSince < requiredDays) continue;

            // Registra el follow-up
            prospect.setFollowupCount(followupCount + 1);
            prospect.setLastFollowupAt(Instant.now());
            prospectRepository.save(prospect);

            // Notifica per Telegram al canal de la plataforma
            sendFollowUpNotification(prospect, nextFollowup);
            processed++;
        }

        if (processed > 0) {
            log.info("Follow-up scheduler: {} notificacions enviades", processed);
        }
    }

    private int getDaysForFollowup(ProspectCampaign campaign, int followupNumber) {
        return switch (followupNumber) {
            case 1 -> campaign.getFollowupDays1() != null ? campaign.getFollowupDays1() : 3;
            case 2 -> campaign.getFollowupDays2() != null ? campaign.getFollowupDays2() : 7;
            case 3 -> campaign.getFollowupDays3() != null ? campaign.getFollowupDays3() : 14;
            default -> Integer.MAX_VALUE;
        };
    }

    private void sendFollowUpNotification(Prospect prospect, int followupNumber) {
        try {
            String chatIdStr = sysConfig.get("TELEGRAM_CHAT_ID");
            if (chatIdStr == null || chatIdStr.isBlank()) return;

            Long chatId = Long.parseLong(chatIdStr.trim());
            String text = String.format(
                "🔁 <b>Follow-up #%d</b> — %s\n\n" +
                "Porta %d dies sense resposta al pitch inicial.\n" +
                "Sector: %s | Municipi: %s\n" +
                (prospect.getDemoUrl() != null ? "Demo: " + prospect.getDemoUrl() : ""),
                followupNumber,
                escapeHtml(prospect.getName()),
                ChronoUnit.DAYS.between(
                    prospect.getPitchSentAt() != null ? prospect.getPitchSentAt() : Instant.now(),
                    Instant.now()),
                prospect.getSector() != null ? prospect.getSector() : "—",
                prospect.getCity() != null ? prospect.getCity() : "—"
            );
            telegramBotClient.sendMessage(chatId, text);
        } catch (Exception e) {
            log.warn("Error notificant follow-up per prospect {}: {}", prospect.getId(), e.getMessage());
        }
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
