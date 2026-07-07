package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.NexeServiceConfigRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Suggeriments de contingut proactius (Mòdul 55, feature 3).
 * Cada dilluns proposa una idea de publicació al Telegram del tenant,
 * si té el toggle `ai_suggestions` actiu i un xat de Telegram vinculat.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialSuggestionScheduler {

    private static final String SERVICE_KEY = "SOCIAL_PUBLISHER";

    private final NexeServiceConfigRepository nexeConfigRepo;
    private final SocialFeatureService featureService;
    private final SocialContentGeneratorService contentGenerator;
    private final SocialAnalyticsService analyticsService;
    private final TenantChatLinkRepository chatLinkRepository;
    private final TenantRepository tenantRepository;
    private final TelegramBotClient telegramBotClient;

    /** Cada dilluns a les 10:00 */
    @Scheduled(cron = "0 0 10 * * MON")
    public void sendWeeklySuggestions() {
        var configs = nexeConfigRepo.findByServiceKey(SERVICE_KEY);
        log.info("Social suggestions: {} tenants amb Social Publisher", configs.size());

        for (var config : configs) {
            var tenantId = config.getTenantId();
            try {
                if (!featureService.get(tenantId).aiSuggestions()) continue;

                var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
                if (chatLink == null || chatLink.getTelegramChatId() == null) continue;

                var tenant = tenantRepository.findById(tenantId).orElse(null);
                if (tenant == null) continue;

                String businessName = tenant.getName() != null ? tenant.getName() : "el teu negoci";
                String sector = tenant.getSector() != null ? tenant.getSector().name() : "general";
                String idea = contentGenerator.generateWeeklyIdea(businessName, sector, dateContext());
                if (idea == null || idea.isBlank()) continue;

                String text = "💡 <b>Idea per aquesta setmana</b>\n\n" + idea
                        + "\n\nVols que la preparem per publicar?";
                var button = Map.of("text", "📢 Crear aquest post", "callback_data", "sugg:new");
                telegramBotClient.sendMessageWithButtons(chatLink.getTelegramChatId(), text, List.of(button));
            } catch (Exception e) {
                log.warn("Error enviant suggeriment social al tenant {}: {}", tenantId, e.getMessage());
            }
        }
    }

    /** Cada dilluns a les 09:00: sincronitza mètriques i envia el resum setmanal (feature 2) */
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklyDigests() {
        var configs = nexeConfigRepo.findByServiceKey(SERVICE_KEY);
        for (var config : configs) {
            var tenantId = config.getTenantId();
            try {
                if (!featureService.get(tenantId).weeklyAnalytics()) continue;

                var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
                if (chatLink == null || chatLink.getTelegramChatId() == null) continue;

                analyticsService.syncMetrics(tenantId);
                String digest = analyticsService.buildWeeklyDigest(tenantId);
                if (digest == null) continue;

                telegramBotClient.sendMessage(chatLink.getTelegramChatId(), digest);
            } catch (Exception e) {
                log.warn("Error enviant resum setmanal al tenant {}: {}", tenantId, e.getMessage());
            }
        }
    }

    private String dateContext() {
        var now = LocalDate.now();
        String season = switch (now.getMonthValue()) {
            case 12, 1, 2 -> "hivern";
            case 3, 4, 5 -> "primavera";
            case 6, 7, 8 -> "estiu (temporada alta turística a Mallorca)";
            default -> "tardor";
        };
        return "Setmana del " + now + ", " + season;
    }
}
