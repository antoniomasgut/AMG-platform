package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.FollowupLog;
import com.amg.digitalitzacio.agents.domain.FollowupLogRepository;
import com.amg.digitalitzacio.agents.domain.NexeServiceConfigRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.google.domain.GoogleBusinessReviewRepository;
import com.amg.digitalitzacio.social.domain.SocialPost;
import com.amg.digitalitzacio.social.domain.SocialPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Post mensual de social proof (Mòdul 57 F3).
 * El dia 1 de cada mes proposa al Telegram del tenant un post amb la valoració
 * agregada de Google («⭐ 4,9/5 amb 32 ressenyes») llest per publicar amb un clic
 * (reutilitza el flux de publicació gpub: del Mòdul 55).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialProofPostScheduler {

    private static final String SERVICE_KEY = "SOCIAL_PUBLISHER";
    private static final int MIN_REVIEWS = 5;
    private static final double MIN_AVG_RATING = 4.0;

    private final NexeServiceConfigRepository nexeConfigRepo;
    private final SocialFeatureService featureService;
    private final SocialContentGeneratorService contentGenerator;
    private final SocialPostRepository postRepository;
    private final GoogleBusinessReviewRepository reviewRepository;
    private final TenantChatLinkRepository chatLinkRepository;
    private final TenantRepository tenantRepository;
    private final TelegramBotClient telegramBotClient;
    private final FollowupLogRepository followupLogRepository;

    /** Dia 1 de cada mes a les 11:00 */
    @Scheduled(cron = "0 0 11 1 * *")
    public void proposeMonthlyProofPosts() {
        var configs = nexeConfigRepo.findByServiceKey(SERVICE_KEY);
        log.info("Social proof mensual: {} tenants amb Social Publisher", configs.size());
        for (var config : configs) {
            try {
                proposeForTenant(config.getTenantId());
            } catch (Exception e) {
                log.warn("Social proof mensual fallat per tenant {}: {}", config.getTenantId(), e.getMessage());
            }
        }
    }

    void proposeForTenant(UUID tenantId) {
        if (!featureService.get(tenantId).autoPostReviews()) return;

        var chatLink = chatLinkRepository.findByTenantId(tenantId).orElse(null);
        if (chatLink == null || chatLink.getTelegramChatId() == null) return;

        // Dedupe: una proposta per tenant i mes
        var dedupeType = "SOCPROOF_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        if (followupLogRepository.existsByTenantIdAndTypeAndEntityId(tenantId, dedupeType, tenantId)) return;

        var reviews = reviewRepository
            .findByTenantIdAndRatingGreaterThanEqualOrderByRatingDescReviewTimeDesc(tenantId, 1);
        if (reviews.size() < MIN_REVIEWS) return;

        double avg = reviews.stream().mapToInt(r -> r.getRating()).average().orElse(0);
        if (avg < MIN_AVG_RATING) return;

        var tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) return;
        String businessName = tenant.getName() != null ? tenant.getName() : "el negoci";

        String avgStr = String.format(Locale.forLanguageTag("ca"), "%.1f", avg);
        String caption = generateCaption(businessName, avgStr, reviews.size());

        var post = postRepository.save(SocialPost.builder()
            .tenantId(tenantId)
            .network("FACEBOOK")
            .postType("TEXT")
            .caption(caption)
            .status("DRAFT")
            .build());

        String text = "🌟 <b>Proposta del mes: presumeix de les teves ressenyes</b>\n\n" + caption
            + "\n\nVols publicar-ho a xarxes?";
        var button = Map.of("text", "✅ Publicar", "callback_data", "gpub:" + post.getId());
        telegramBotClient.sendMessageWithButtons(chatLink.getTelegramChatId(), text, List.of(button));

        var entry = new FollowupLog();
        entry.setTenantId(tenantId);
        entry.setType(dedupeType);
        entry.setEntityId(tenantId);
        followupLogRepository.save(entry);
        log.info("Social proof mensual proposat al tenant {} ({} ressenyes, mitjana {})", tenantId, reviews.size(), avgStr);
    }

    private String generateCaption(String businessName, String avgStr, int count) {
        var brief = "Post mensual de social proof per a " + businessName + ": mitjana de " + avgStr
            + "/5 estrelles amb " + count + " ressenyes a Google. To agraït i proper, convida a conèixer el negoci.";
        try {
            var caption = contentGenerator.generateCaption("FACEBOOK", businessName, brief);
            if (caption != null && !caption.isBlank()) return caption;
        } catch (Exception e) {
            log.debug("IA no disponible per al caption de social proof: {}", e.getMessage());
        }
        return "⭐ " + avgStr + "/5 amb " + count + " ressenyes a Google!\n"
            + "Gràcies per la confiança — seguim treballant cada dia per merèixer-la. 🙌";
    }
}
