package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.social.domain.SocialPost;
import com.amg.digitalitzacio.social.domain.SocialPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * P42: envia una notificació Telegram al tenant 24h després de publicar,
 * amb les mètriques de rendiment del post (reach, likes, comentaris).
 * Evita duplicats via performanceNotifiedAt.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialPostPerformanceNotifier {

    private static final ZoneId ZONE_ES = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZONE_ES);

    private static final Map<String, String> NETWORK_LABEL = Map.of(
        "INSTAGRAM", "Instagram", "FACEBOOK", "Facebook",
        "GOOGLE_BUSINESS", "Google Business", "LINKEDIN", "LinkedIn");

    private final SocialPostRepository postRepository;
    private final SocialAnalyticsService analyticsService;
    private final TelegramBotClient telegramBotClient;
    private final TenantChatLinkRepository chatLinkRepository;

    /** Cada hora, cerca posts publicats fa 22-26h sense notificació enviada. */
    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    public void notifyPerformance() {
        Instant now = Instant.now();
        Instant from = now.minus(Duration.ofHours(26));
        Instant to   = now.minus(Duration.ofHours(22));

        var posts = postRepository.findPublishedWithoutPerformanceNotification(from, to);
        if (posts.isEmpty()) return;

        // Sincronitza les mètriques abans de notificar (si no s'han sincronitzat prou recentment)
        posts.stream()
            .map(SocialPost::getTenantId)
            .distinct()
            .forEach(tenantId -> {
                try { analyticsService.syncMetrics(tenantId); }
                catch (Exception e) { log.warn("Sync mètriques P42 fallit per {}: {}", tenantId, e.getMessage()); }
            });

        // Refresca els posts per tenir les mètriques actualitzades
        var refreshed = postRepository.findPublishedWithoutPerformanceNotification(from, to);

        for (SocialPost p : refreshed) {
            try {
                sendPerformanceNotification(p);
                p.setPerformanceNotifiedAt(now);
                postRepository.save(p);
            } catch (Exception e) {
                log.warn("Error notificant rendiment post {}: {}", p.getId(), e.getMessage());
            }
        }
    }

    private void sendPerformanceNotification(SocialPost p) {
        UUID tenantId = p.getTenantId();
        Long chatId = chatLinkRepository.findByTenantId(tenantId)
            .map(link -> link.getTelegramChatId())
            .orElse(null);
        if (chatId == null) return; // tenant no vinculat a Telegram

        String label   = NETWORK_LABEL.getOrDefault(p.getNetwork(), p.getNetwork());
        String pubDate = p.getPublishedAt() != null ? FMT.format(p.getPublishedAt()) : "?";
        String cap     = p.getCaption() != null && !p.getCaption().isBlank()
            ? (p.getCaption().length() > 60 ? p.getCaption().substring(0, 57) + "…" : p.getCaption())
            : "(sense text)";

        boolean hasMetrics = p.getReach() != null || p.getLikes() != null || p.getComments() != null;
        String metricsText;
        if (hasMetrics) {
            metricsText = "\n📊 <b>Resultats:</b>\n"
                + (p.getReach()    != null ? "• Abast: "       + p.getReach()    + " persones\n" : "")
                + (p.getLikes()    != null ? "• M'agrada: "    + p.getLikes()    + "\n" : "")
                + (p.getComments() != null ? "• Comentaris: "  + p.getComments() + "\n" : "");
        } else {
            metricsText = "\n<i>Les mètriques encara no estan disponibles. "
                + "Escriu <code>/stats-social</code> per actualitzar-les.</i>";
        }

        String msg = "📣 <b>Com ha anat el teu post d'ahir?</b>\n"
            + label + " · " + pubDate + "\n"
            + "<i>\"" + cap + "\"</i>"
            + metricsText;

        telegramBotClient.sendMessageForTenant(tenantId, chatId, msg);
    }
}
