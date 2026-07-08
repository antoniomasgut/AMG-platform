package com.amg.digitalitzacio.google.application;

import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.FollowupLog;
import com.amg.digitalitzacio.agents.domain.FollowupLogRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.google.domain.GoogleBusinessReview;
import com.amg.digitalitzacio.google.domain.GoogleBusinessReviewRepository;
import com.amg.digitalitzacio.google.domain.GoogleModuleConfig;
import com.amg.digitalitzacio.google.domain.GoogleModuleConfigRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sincronitza ressenyes de Google Business Profile (My Business API v4.9)
 * per a tenants amb businessEnabled=true i businessLocationId configurat.
 * Les ressenyes es cachen a google_business_reviews per al bloc 'reviews' de les landings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleBusinessReviewSyncService {

    private static final String GBP_BASE = "https://mybusiness.googleapis.com/v4";

    private final GoogleModuleConfigRepository configRepo;
    private final GoogleBusinessReviewRepository reviewRepo;
    private final GoogleTokenService tokenService;
    private final ObjectMapper objectMapper;
    private final TelegramBotClient telegramBotClient;
    private final TenantChatLinkRepository chatLinkRepository;
    private final NexeServiceConfigService nexeConfigService;
    private final FollowupLogRepository followupLogRepository;

    /** Sync diari complet a les 03:00 (full-resync de l'històric) */
    @Scheduled(cron = "0 0 3 * * *")
    public void syncAll() {
        for (var config : enabledConfigs()) {
            try {
                sync(config.getTenantId());
            } catch (Exception e) {
                log.warn("GBP sync failed for tenant {}: {}", config.getTenantId(), e.getMessage());
            }
        }
    }

    /** Detecció de ressenyes noves cada hora + notificació Telegram al tenant (Mòdul 54) */
    @Scheduled(cron = "0 0 * * * *")
    public void syncAndNotifyAll() {
        for (var config : enabledConfigs()) {
            try {
                sync(config.getTenantId());
                notifyNewReviews(config.getTenantId());
            } catch (Exception e) {
                log.warn("GBP sync+notify failed for tenant {}: {}", config.getTenantId(), e.getMessage());
            }
        }
    }

    private List<GoogleModuleConfig> enabledConfigs() {
        var configs = configRepo.findAll().stream()
            .filter(GoogleModuleConfig::isBusinessEnabled)
            .filter(c -> c.getBusinessLocationId() != null && !c.getBusinessLocationId().isBlank())
            .toList();
        log.info("GBP review sync: {} tenants enabled", configs.size());
        return configs;
    }

    /** Envia una notificació Telegram (amb botó "Respondre") per cada ressenya no notificada */
    @Transactional
    public void notifyNewReviews(UUID tenantId) {
        var pending = reviewRepo.findByTenantIdAndNotifiedAtIsNullOrderByReviewTimeDesc(tenantId);
        if (pending.isEmpty()) return;

        var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
        Long chatId = chatLink != null ? chatLink.getTelegramChatId() : null;

        // Mòdul 55 feature 4: compartir automàticament ressenyes 5★ a xarxes (opt-in)
        boolean autoPostReviews = autoPostReviewsEnabled(tenantId);

        for (var review : pending) {
            if (chatId != null) {
                var text = buildReviewMessage(review);
                var buttons = new java.util.ArrayList<Map<String, String>>();
                buttons.add(Map.of("text", "✍️ Respondre", "callback_data", "grev:" + review.getReviewId()));
                buttons.add(Map.of("text", "🤖 Suggerir resposta", "callback_data", "grevai:" + review.getReviewId()));
                if (autoPostReviews && review.getRating() != null && review.getRating() == 5) {
                    buttons.add(Map.of("text", "📢 Compartir a xarxes", "callback_data", "gshare:" + review.getReviewId()));
                }
                telegramBotClient.sendMessageWithButtons(chatId, text, buttons);
            }
            review.setNotifiedAt(Instant.now());
            reviewRepo.save(review);
        }
        log.info("GBP: {} ressenyes noves notificades al tenant {}", pending.size(), tenantId);
    }

    private String buildReviewMessage(GoogleBusinessReview r) {
        boolean negative = isNegative(r);
        var stars = "★".repeat(r.getRating()) + "☆".repeat(5 - r.getRating());
        var sb = new StringBuilder();
        if (negative) {
            sb.append("🚨 <b>Ressenya negativa a Google</b> · ");
        } else {
            sb.append("⭐ <b>Nova ressenya a Google</b> · ");
        }
        sb.append(stars).append(" (").append(r.getRating()).append("/5)\n");
        if (r.getAuthorName() != null && !r.getAuthorName().isBlank()) {
            sb.append("👤 ").append(escapeHtml(r.getAuthorName())).append("\n");
        }
        if (r.getComment() != null && !r.getComment().isBlank()) {
            String c = r.getComment().length() > 300 ? r.getComment().substring(0, 297) + "…" : r.getComment();
            sb.append("\"").append(escapeHtml(c)).append("\"\n");
        }
        if (negative) {
            sb.append("\n⏱️ Respondre-la aviat i amb bon to és la millor manera de protegir la teva reputació — la resposta la veu tothom que llegeixi la ressenya.");
        } else {
            sb.append("\nToca <b>✍️ Respondre</b> per contestar-la des d'aquí.");
        }
        return sb.toString();
    }

    private boolean isNegative(GoogleBusinessReview r) {
        return r.getRating() != null && r.getRating() <= 3;
    }

    /**
     * Recordatori de ressenyes negatives sense resposta (Mòdul 57 F2).
     * Cada dia a les 09:45: ressenyes ≤3★ notificades fa més de 48h i encara sense reply
     * → un únic recordatori Telegram (dedupe via followup_logs NEGREV_48H).
     */
    @Scheduled(cron = "0 45 9 * * *")
    public void remindUnansweredNegativeReviews() {
        for (var config : enabledConfigs()) {
            var tenantId = config.getTenantId();
            try {
                var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
                if (chatLink == null || chatLink.getTelegramChatId() == null) continue;

                var threshold = Instant.now().minus(java.time.Duration.ofHours(48));
                var unanswered = reviewRepo
                    .findByTenantIdAndRatingLessThanEqualAndReplyIsNullAndNotifiedAtBefore(tenantId, 3, threshold);

                for (var review : unanswered) {
                    if (followupLogRepository.existsByTenantIdAndTypeAndEntityId(tenantId, "NEGREV_48H", review.getId())) {
                        continue;
                    }
                    var text = "⏰ <b>Recordatori</b> · Aquesta ressenya negativa segueix sense resposta des de fa 2 dies:\n\n"
                        + buildReviewMessage(review);
                    var buttons = List.of(
                        Map.of("text", "✍️ Respondre", "callback_data", "grev:" + review.getReviewId()),
                        Map.of("text", "🤖 Suggerir resposta", "callback_data", "grevai:" + review.getReviewId()));
                    telegramBotClient.sendMessageWithButtons(chatLink.getTelegramChatId(), text, buttons);

                    var entry = new FollowupLog();
                    entry.setTenantId(tenantId);
                    entry.setType("NEGREV_48H");
                    entry.setEntityId(review.getId());
                    followupLogRepository.save(entry);
                }
            } catch (Exception e) {
                log.warn("Recordatori de ressenyes negatives fallat per tenant {}: {}", tenantId, e.getMessage());
            }
        }
    }

    private String escapeHtml(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Llegeix el toggle `auto_post_reviews` de la config SOCIAL_PUBLISHER del tenant */
    private boolean autoPostReviewsEnabled(UUID tenantId) {
        try {
            return nexeConfigService.get(tenantId, "SOCIAL_PUBLISHER")
                .map(c -> {
                    try {
                        var json = objectMapper.readTree(c.getConfigJson());
                        return json.path("auto_post_reviews").asBoolean(false);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    /** Sincronització manual per a un tenant concret */
    @Transactional
    public int sync(UUID tenantId) {
        var config = configRepo.findById(tenantId)
            .filter(GoogleModuleConfig::isBusinessEnabled)
            .filter(c -> c.getBusinessLocationId() != null)
            .orElseThrow(() -> new IllegalStateException("Google Business Profile no configurat per tenant " + tenantId));

        GoogleTokenService.GoogleCredentials creds;
        try {
            creds = tokenService.getValidCredentials(tenantId);
        } catch (Exception e) {
            throw new IllegalStateException("Token Google no disponible: " + e.getMessage());
        }

        var client = WebClient.builder().baseUrl(GBP_BASE).build();
        var locationName = config.getBusinessLocationId();
        if (!locationName.startsWith("accounts/")) {
            locationName = "accounts/-/locations/" + locationName;
        }

        try {
            var response = client.get()
                .uri("/{locationName}/reviews?pageSize=50", locationName)
                .header("Authorization", "Bearer " + creds.accessToken())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(java.time.Duration.ofSeconds(15))
                .block();

            if (response == null) return 0;
            var root = objectMapper.readTree(response);
            var reviews = root.path("reviews");
            if (!reviews.isArray()) return 0;

            // Primer sync del tenant: marca l'històric com a notificat per no fer spam
            boolean backfill = reviewRepo.countByTenantId(tenantId) == 0;

            int synced = 0;
            for (JsonNode r : reviews) {
                try {
                    upsertReview(tenantId, r, backfill);
                    synced++;
                } catch (Exception e) {
                    log.debug("Failed to upsert review: {}", e.getMessage());
                }
            }
            log.info("GBP sync: {} reviews synced for tenant {}", synced, tenantId);
            return synced;

        } catch (Exception e) {
            log.warn("GBP API error for tenant {}: {}", tenantId, e.getMessage());
            throw new RuntimeException("Error sincronitzant ressenyes: " + e.getMessage());
        }
    }

    /**
     * Publica una resposta a una ressenya de Google Business (PUT reviewReply)
     * i actualitza la còpia local. Retorna true si s'ha publicat.
     */
    @Transactional
    public boolean replyToReview(UUID tenantId, String reviewId, String text) {
        if (text == null || text.isBlank()) return false;

        var config = configRepo.findById(tenantId)
            .filter(GoogleModuleConfig::isBusinessEnabled)
            .filter(c -> c.getBusinessLocationId() != null)
            .orElseThrow(() -> new IllegalStateException("Google Business Profile no configurat per tenant " + tenantId));

        var review = reviewRepo.findByTenantIdAndReviewId(tenantId, reviewId)
            .orElseThrow(() -> new IllegalStateException("Ressenya no trobada: " + reviewId));

        GoogleTokenService.GoogleCredentials creds = tokenService.getValidCredentials(tenantId);

        var locationName = config.getBusinessLocationId();
        if (!locationName.startsWith("accounts/")) {
            locationName = "accounts/-/locations/" + locationName;
        }

        var client = WebClient.builder().baseUrl(GBP_BASE).build();
        try {
            client.put()
                .uri("/{loc}/reviews/{reviewId}/reply", locationName, reviewId)
                .header("Authorization", "Bearer " + creds.accessToken())
                .bodyValue(Map.of("comment", text))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(java.time.Duration.ofSeconds(15))
                .block();
        } catch (Exception e) {
            log.warn("GBP reply error tenant {} review {}: {}", tenantId, reviewId, e.getMessage());
            throw new RuntimeException("Error publicant la resposta: " + e.getMessage());
        }

        review.setReply(text);
        reviewRepo.save(review);
        log.info("GBP: resposta publicada per tenant {} a la ressenya {}", tenantId, reviewId);
        return true;
    }

    /** Retorna les ressenyes cacheades per al renderer de landings */
    public List<GoogleBusinessReview> getReviews(UUID tenantId, int minRating, int maxItems) {
        var reviews = reviewRepo
            .findByTenantIdAndRatingGreaterThanEqualOrderByRatingDescReviewTimeDesc(tenantId, minRating);
        return reviews.stream().limit(maxItems).toList();
    }

    private void upsertReview(UUID tenantId, JsonNode r, boolean backfill) {
        var reviewId = r.path("reviewId").asText(null);
        if (reviewId == null) return;

        var existingOpt = reviewRepo.findByTenantIdAndReviewId(tenantId, reviewId);
        boolean isNew = existingOpt.isEmpty();
        var existing = existingOpt
            .orElseGet(() -> GoogleBusinessReview.builder().tenantId(tenantId).reviewId(reviewId).build());

        // Ressenya nova (i no és el primer sync de backfill) → pendent de notificar
        if (isNew && backfill) {
            existing.setNotifiedAt(Instant.now());
        }

        var ratingStr = r.path("starRating").asText("ONE");
        existing.setRating(starRatingToInt(ratingStr));
        existing.setAuthorName(r.path("reviewer").path("displayName").asText(null));
        existing.setComment(r.path("comment").asText(null));

        var replyNode = r.path("reviewReply");
        if (!replyNode.isMissingNode() && !replyNode.isNull()) {
            existing.setReply(replyNode.path("comment").asText(null));
        }

        var timeStr = r.path("updateTime").asText(null);
        if (timeStr != null) {
            try { existing.setReviewTime(Instant.parse(timeStr)); }
            catch (DateTimeParseException ignored) {}
        }
        existing.setSyncedAt(Instant.now());
        reviewRepo.save(existing);
    }

    private int starRatingToInt(String star) {
        return switch (star) {
            case "FIVE" -> 5;
            case "FOUR" -> 4;
            case "THREE" -> 3;
            case "TWO" -> 2;
            default -> 1;
        };
    }
}
