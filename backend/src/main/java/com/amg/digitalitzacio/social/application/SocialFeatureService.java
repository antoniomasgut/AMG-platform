package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Toggles per tenant de les extensions socials (Mòdul 55).
 * Desats com a flags JSON dins la config SOCIAL_PUBLISHER de nexe_service_configs,
 * que també fa de gate d'activació del mòdul. Tots opt-in (default false).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SocialFeatureService {

    private static final String SERVICE_KEY = "SOCIAL_PUBLISHER";

    private final NexeServiceConfigService nexeConfigService;
    private final ObjectMapper objectMapper;

    public record SocialFeatures(
        boolean commentsToTelegram,
        boolean weeklyAnalytics,
        boolean aiSuggestions,
        boolean autoPostReviews
    ) {}

    /** true si el mòdul Social Publisher està activat per al tenant */
    public boolean isEnabled(UUID tenantId) {
        return nexeConfigService.get(tenantId, SERVICE_KEY).isPresent();
    }

    public SocialFeatures get(UUID tenantId) {
        var config = readConfig(tenantId);
        return new SocialFeatures(
            flag(config, "comments_to_telegram"),
            flag(config, "weekly_analytics"),
            flag(config, "ai_suggestions"),
            flag(config, "auto_post_reviews")
        );
    }

    /** Merge no destructiu: preserva la resta de claus de la config */
    public void update(UUID tenantId, SocialFeatures f) {
        var config = readConfig(tenantId);
        config.put("comments_to_telegram", f.commentsToTelegram());
        config.put("weekly_analytics", f.weeklyAnalytics());
        config.put("ai_suggestions", f.aiSuggestions());
        config.put("auto_post_reviews", f.autoPostReviews());
        try {
            nexeConfigService.save(tenantId, SERVICE_KEY, objectMapper.writeValueAsString(config));
        } catch (Exception e) {
            log.warn("Error desant toggles socials tenant {}: {}", tenantId, e.getMessage());
            throw new RuntimeException("No s'han pogut desar els toggles: " + e.getMessage());
        }
    }

    private Map<String, Object> readConfig(UUID tenantId) {
        return nexeConfigService.get(tenantId, SERVICE_KEY)
            .map(c -> {
                try {
                    return objectMapper.<Map<String, Object>>readValue(
                        c.getConfigJson(), new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    return new HashMap<String, Object>();
                }
            })
            .orElseGet(HashMap::new);
    }

    private boolean flag(Map<String, Object> config, String key) {
        return Boolean.TRUE.equals(config.get(key));
    }
}
