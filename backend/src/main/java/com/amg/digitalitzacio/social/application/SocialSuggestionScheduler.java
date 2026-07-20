package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.NexeServiceConfigRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.social.domain.SocialMetaConfigRepository;
import com.amg.digitalitzacio.social.domain.SocialPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
    private final SocialMetaConfigRepository metaConfigRepo;
    private final SocialPostRepository postRepository;

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
                // P3: les mètriques es sincronitzen a les 9:00 (digest); a les 10:00 ja són fresques
                String performance = analyticsService.buildPerformanceContext(tenantId);
                String idea = contentGenerator.generateWeeklyIdea(businessName, sector, dateContext(), performance);
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

    /** Cada dia a les 23:00: sincronitza mètriques de tots els tenants (P6 — dades fresques al portal) */
    @Scheduled(cron = "0 0 23 * * *")
    public void dailyMetricsSync() {
        var configs = nexeConfigRepo.findByServiceKey(SERVICE_KEY);
        log.debug("Social daily sync: {} tenants", configs.size());
        for (var config : configs) {
            try {
                analyticsService.syncMetrics(config.getTenantId());
            } catch (Exception e) {
                log.debug("Error sync mètriques tenant {}: {}", config.getTenantId(), e.getMessage());
            }
        }
    }

    /** Cada dia a les 08:00: avisa els tenants amb token Meta a caducar en 7 dies (P12) */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkTokenExpiry() {
        var configs = nexeConfigRepo.findByServiceKey(SERVICE_KEY);
        var limit = Instant.now().plus(7, ChronoUnit.DAYS);

        for (var config : configs) {
            var tenantId = config.getTenantId();
            try {
                var mc = metaConfigRepo.findByTenantId(tenantId).orElse(null);
                if (mc == null || mc.getTokenExpiresAt() == null) continue;
                if (mc.getTokenExpiresAt().isAfter(limit)) continue;

                var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
                if (chatLink == null || chatLink.getTelegramChatId() == null) continue;

                long daysLeft = ChronoUnit.DAYS.between(Instant.now(), mc.getTokenExpiresAt());
                String msg = daysLeft <= 0
                    ? "⚠️ <b>El token de Facebook/Instagram ha caducat.</b>\n"
                      + "Les publicacions a xarxes fallaran fins que el reconnectis.\n"
                      + "Vés a /portal → Social → Meta per reconnectar."
                    : "⚠️ <b>El token de Facebook/Instagram caduca en " + daysLeft + " dies.</b>\n"
                      + "Reconnecta'l aviat per evitar interrupcions.\n"
                      + "Vés a /portal → Social → Meta per renovar-lo.";

                telegramBotClient.sendMessage(chatLink.getTelegramChatId(), msg);
            } catch (Exception e) {
                log.debug("Error comprovant expiració de token per tenant {}: {}", tenantId, e.getMessage());
            }
        }
    }

    /**
     * Cada dijous a les 11:00 (P22): si no ha publicat res en els últims 7 dies,
     * envia un recordatori per activar el tenant i evitar inactivitat prolongada.
     */
    @Scheduled(cron = "0 0 11 * * THU")
    public void nudgeInactiveTenants() {
        var configs = nexeConfigRepo.findByServiceKey(SERVICE_KEY);
        var since7d = Instant.now().minus(7, ChronoUnit.DAYS);

        for (var config : configs) {
            var tenantId = config.getTenantId();
            try {
                var posts = postRepository.findPublishedSince(tenantId, since7d);
                if (!posts.isEmpty()) continue;

                var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
                if (chatLink == null || chatLink.getTelegramChatId() == null) continue;

                telegramBotClient.sendMessage(chatLink.getTelegramChatId(),
                    "📅 Fa més de 7 dies que no publiques a xarxes socials.\n"
                    + "Publicar de forma regular millora el teu abast i la fidelitat dels clients.\n\n"
                    + "Escriu <code>/publica</code> per crear un post ràpid!");
            } catch (Exception e) {
                log.debug("Error comprovant inactivitat del tenant {}: {}", tenantId, e.getMessage());
            }
        }
    }

    /**
     * Cada hora (P23): reverteix posts encallats en estat PUBLISHING (app crash enmig d'una publicació).
     * Un post en PUBLISHING durant més de 5 minuts és definitament un error.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupStuckPublishingPosts() {
        var stuckSince = Instant.now().minus(5, ChronoUnit.MINUTES);
        var stuck = postRepository.findStuckPublishing(stuckSince);
        if (stuck.isEmpty()) return;

        log.warn("Social cleanup: {} posts encallats en PUBLISHING, revertint a FAILED", stuck.size());
        for (var post : stuck) {
            post.setStatus("FAILED");
            post.setErrorMessage("La publicació va quedar interrompuda (reinici del servidor)");
            postRepository.save(post);
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
