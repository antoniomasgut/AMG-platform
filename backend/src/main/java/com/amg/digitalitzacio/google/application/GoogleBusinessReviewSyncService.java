package com.amg.digitalitzacio.google.application;

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

    /** Sync diari automàtic a les 03:00 */
    @Scheduled(cron = "0 0 3 * * *")
    public void syncAll() {
        var configs = configRepo.findAll().stream()
            .filter(GoogleModuleConfig::isBusinessEnabled)
            .filter(c -> c.getBusinessLocationId() != null && !c.getBusinessLocationId().isBlank())
            .toList();

        log.info("GBP review sync: {} tenants to sync", configs.size());
        for (var config : configs) {
            try {
                sync(config.getTenantId());
            } catch (Exception e) {
                log.warn("GBP sync failed for tenant {}: {}", config.getTenantId(), e.getMessage());
            }
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

            int synced = 0;
            for (JsonNode r : reviews) {
                try {
                    upsertReview(tenantId, r);
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

    /** Retorna les ressenyes cacheades per al renderer de landings */
    public List<GoogleBusinessReview> getReviews(UUID tenantId, int minRating, int maxItems) {
        var reviews = reviewRepo
            .findByTenantIdAndRatingGreaterThanEqualOrderByRatingDescReviewTimeDesc(tenantId, minRating);
        return reviews.stream().limit(maxItems).toList();
    }

    private void upsertReview(UUID tenantId, JsonNode r) {
        var reviewId = r.path("reviewId").asText(null);
        if (reviewId == null) return;

        var existing = reviewRepo.findByTenantIdAndReviewId(tenantId, reviewId)
            .orElseGet(() -> GoogleBusinessReview.builder().tenantId(tenantId).reviewId(reviewId).build());

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
