package com.amg.digitalitzacio.metaads.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.metaads.api.dto.CampaignResponse;
import com.amg.digitalitzacio.metaads.api.dto.PublishProgressResponse;
import com.amg.digitalitzacio.metaads.domain.*;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetaAdsPublishService {

    private static final String GRAPH_BASE = "https://graph.facebook.com";
    private static final String API_VERSION = "v19.0";

    private final AdCampaignRepository campaignRepository;
    private final MetaAdsConfigRepository configRepository;
    private final MetaAdsCampaignService campaignService;
    private final TenantChatLinkRepository chatLinkRepository;
    private final TelegramBotClient telegramBotClient;
    private final SystemConfigService sysConfig;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    @Transactional
    public PublishProgressResponse publish(UUID tenantId, UUID campaignId) {
        var campaign = campaignRepository.findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Campanya no trobada"));

        var config = configRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Meta Ads no configurat per aquest tenant"));

        String token = resolveToken(tenantId, config);
        if (token == null) throw new IllegalStateException("Token Meta Ads no configurat");

        String accountId = normalizeAccountId(config.getAdAccountId());
        List<String> steps = new ArrayList<>();

        try {
            // 1. Crear campanya a Meta
            String metaCampaignId = createMetaCampaign(accountId, token, campaign);
            campaign.setMetaCampaignId(metaCampaignId);
            campaign.setStatus(CampaignStatus.PENDING_REVIEW);
            campaign.setMetaError(null);
            campaignService.saveCampaign(campaign);
            steps.add("✓ Campanya creada a Meta");

            // 2. Per cada ad set
            for (AdSet adSet : campaignService.getAdSetsForCampaign(campaignId)) {
                if (adSet.getStatus() == CampaignStatus.ARCHIVED) continue;

                String metaAdSetId = createMetaAdSet(accountId, token, metaCampaignId, adSet);
                adSet.setMetaAdSetId(metaAdSetId);
                adSet.setStatus(CampaignStatus.PENDING_REVIEW);
                adSet.setMetaError(null);
                campaignService.saveAdSet(adSet);
                steps.add("✓ Ad Set \"" + adSet.getName() + "\" creat");

                // 3. Per cada anunci
                for (Ad ad : campaignService.getAdsForAdSet(adSet.getId())) {
                    if (ad.getStatus() == CampaignStatus.ARCHIVED) continue;

                    AdCreative creative = campaignService.getCreative(ad.getCreativeId());
                    if (creative == null) continue;

                    // Crear creatiu a Meta si no existeix
                    if (creative.getMetaCreativeId() == null) {
                        String metaCreativeId = createMetaCreative(accountId, token, creative, tenantId, config);
                        creative.setMetaCreativeId(metaCreativeId);
                        campaignService.saveCreative(creative);
                        steps.add("✓ Creatiu \"" + coalesce(creative.getName(), creative.getHeadline()) + "\" creat");
                    }

                    // Crear anunci a Meta
                    String metaAdId = createMetaAd(accountId, token, metaAdSetId, creative.getMetaCreativeId(), ad);
                    ad.setMetaAdId(metaAdId);
                    ad.setStatus(CampaignStatus.PENDING_REVIEW);
                    ad.setMetaError(null);
                    campaignService.saveAd(ad);
                    steps.add("✓ Anunci \"" + coalesce(ad.getName(), creative.getHeadline()) + "\" creat");
                }
            }

            steps.add("⏳ Pendent de revisió per Meta...");
            notifyTenant(tenantId, "⏳ Meta Ads: \"" + campaign.getName() + "\" enviada a revisió. Rebràs confirmació aviat.");

            return new PublishProgressResponse(CampaignStatus.PENDING_REVIEW.name(), steps, null);

        } catch (Exception e) {
            log.error("[MetaAds] Error publicant campanya {} del tenant {}: {}", campaignId, tenantId, e.getMessage());
            campaign.setStatus(CampaignStatus.ERROR);
            campaign.setMetaError(e.getMessage());
            campaignService.saveCampaign(campaign);
            steps.add("✕ Error: " + e.getMessage());
            return new PublishProgressResponse(CampaignStatus.ERROR.name(), steps, e.getMessage());
        }
    }

    @Transactional
    public CampaignResponse pause(UUID tenantId, UUID campaignId) {
        return setMetaStatus(tenantId, campaignId, "PAUSED", CampaignStatus.PAUSED);
    }

    @Transactional
    public CampaignResponse resume(UUID tenantId, UUID campaignId) {
        return setMetaStatus(tenantId, campaignId, "ACTIVE", CampaignStatus.ACTIVE);
    }

    @Transactional
    public CampaignResponse archive(UUID tenantId, UUID campaignId) {
        return setMetaStatus(tenantId, campaignId, "ARCHIVED", CampaignStatus.ARCHIVED);
    }

    private CampaignResponse setMetaStatus(UUID tenantId, UUID campaignId, String metaStatus, CampaignStatus localStatus) {
        var campaign = campaignRepository.findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Campanya no trobada"));

        if (campaign.getMetaCampaignId() != null) {
            var config = configRepository.findById(tenantId).orElse(null);
            if (config != null) {
                String token = resolveToken(tenantId, config);
                if (token != null) {
                    callMetaPatch(campaign.getMetaCampaignId(), token, Map.of("status", metaStatus));
                }
            }
        }

        campaign.setStatus(localStatus);
        return campaignService.toResponse(campaignRepository.save(campaign));
    }

    // ---- Meta API helpers ----

    private String createMetaCampaign(String accountId, String token, AdCampaign campaign) throws Exception {
        var body = new LinkedHashMap<String, Object>();
        body.put("name", campaign.getName());
        body.put("objective", campaign.getObjective().name());
        body.put("status", "PAUSED");
        body.put("special_ad_categories", List.of());
        if (campaign.getDailyBudget() != null)
            body.put("daily_budget", campaign.getDailyBudget().multiply(java.math.BigDecimal.valueOf(100)).longValue());
        if (campaign.getLifetimeBudget() != null)
            body.put("lifetime_budget", campaign.getLifetimeBudget().multiply(java.math.BigDecimal.valueOf(100)).longValue());

        String raw = postToMeta("/" + API_VERSION + "/" + accountId + "/campaigns", token, body);
        return objectMapper.readTree(raw).path("id").asText();
    }

    private String createMetaAdSet(String accountId, String token, String metaCampaignId, AdSet adSet) throws Exception {
        var body = new LinkedHashMap<String, Object>();
        body.put("name", adSet.getName());
        body.put("campaign_id", metaCampaignId);
        body.put("optimization_goal", coalesce(adSet.getOptimizationGoal(), "LEAD_GENERATION"));
        body.put("billing_event", coalesce(adSet.getBillingEvent(), "IMPRESSIONS"));
        body.put("status", "PAUSED");

        if (adSet.getDailyBudget() != null)
            body.put("daily_budget", adSet.getDailyBudget().multiply(java.math.BigDecimal.valueOf(100)).longValue());

        // Targeting
        var targeting = buildTargeting(adSet);
        body.put("targeting", targeting);

        if (adSet.getStartTime() != null) body.put("start_time", adSet.getStartTime().toString());
        if (adSet.getStopTime() != null) body.put("end_time", adSet.getStopTime().toString());

        String raw = postToMeta("/" + API_VERSION + "/" + accountId + "/adsets", token, body);
        return objectMapper.readTree(raw).path("id").asText();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildTargeting(AdSet adSet) throws Exception {
        var targeting = new LinkedHashMap<String, Object>();

        if (adSet.getAgeMin() != null) targeting.put("age_min", adSet.getAgeMin());
        if (adSet.getAgeMax() != null) targeting.put("age_max", adSet.getAgeMax());

        if (adSet.getGenders() != null && !"ALL".equals(adSet.getGenders())) {
            targeting.put("genders", "MALE".equals(adSet.getGenders()) ? List.of(1) : List.of(2));
        }

        if (adSet.getGeoLocationsJson() != null) {
            List<?> geoList = objectMapper.readValue(adSet.getGeoLocationsJson(), List.class);
            if (!geoList.isEmpty()) {
                targeting.put("geo_locations", Map.of("cities", geoList));
            }
        }

        if (adSet.getInterestsJson() != null) {
            List<?> interests = objectMapper.readValue(adSet.getInterestsJson(), List.class);
            if (!interests.isEmpty()) {
                targeting.put("flexible_spec", List.of(Map.of("interests", interests)));
            }
        }

        if (adSet.getPublisherPlatforms() != null) {
            targeting.put("publisher_platforms", Arrays.asList(adSet.getPublisherPlatforms().split(",")));
        }

        return targeting;
    }

    private String createMetaCreative(String accountId, String token, AdCreative creative,
                                      UUID tenantId, MetaAdsConfig config) throws Exception {
        // Necessitem el page_id del tenant (des de TenantChatLink)
        String pageId = resolvePageId(tenantId);

        var linkData = new LinkedHashMap<String, Object>();
        if (creative.getMetaImageHash() != null) linkData.put("image_hash", creative.getMetaImageHash());
        linkData.put("link", creative.getLinkUrl());
        if (creative.getBody() != null) linkData.put("message", creative.getBody());
        if (creative.getHeadline() != null) linkData.put("name", creative.getHeadline());
        if (creative.getDescription() != null) linkData.put("description", creative.getDescription());
        if (creative.getCallToAction() != null)
            linkData.put("call_to_action", Map.of("type", creative.getCallToAction()));

        var storySpec = Map.of(
            "page_id", pageId,
            "link_data", linkData
        );

        var body = new LinkedHashMap<String, Object>();
        body.put("name", coalesce(creative.getName(), creative.getHeadline(), "Creative"));
        body.put("object_story_spec", storySpec);

        String raw = postToMeta("/" + API_VERSION + "/" + accountId + "/adcreatives", token, body);
        return objectMapper.readTree(raw).path("id").asText();
    }

    private String createMetaAd(String accountId, String token, String metaAdSetId,
                                 String metaCreativeId, Ad ad) throws Exception {
        var body = new LinkedHashMap<String, Object>();
        body.put("name", coalesce(ad.getName(), "Ad"));
        body.put("adset_id", metaAdSetId);
        body.put("creative", Map.of("creative_id", metaCreativeId));
        body.put("status", "PAUSED");

        String raw = postToMeta("/" + API_VERSION + "/" + accountId + "/ads", token, body);
        return objectMapper.readTree(raw).path("id").asText();
    }

    private String postToMeta(String path, String token, Object body) throws Exception {
        String jsonBody = objectMapper.writeValueAsString(body);
        var rc = restClientBuilder.baseUrl(GRAPH_BASE).build();
        return rc.post()
                .uri(path + "?access_token=" + token)
                .header("Content-Type", "application/json")
                .body(jsonBody)
                .retrieve()
                .body(String.class);
    }

    private void callMetaPatch(String metaId, String token, Map<String, Object> fields) {
        try {
            String jsonBody = objectMapper.writeValueAsString(fields);
            var rc = restClientBuilder.baseUrl(GRAPH_BASE).build();
            rc.post()
                .uri("/" + API_VERSION + "/" + metaId + "?access_token=" + token)
                .header("Content-Type", "application/json")
                .body(jsonBody)
                .retrieve()
                .body(String.class);
        } catch (Exception e) {
            log.error("[MetaAds] Error canviant estat a Meta per {}: {}", metaId, e.getMessage());
        }
    }

    // ---- Sync d'estats per al job ----

    @Transactional
    public void syncStatus(AdCampaign campaign) {
        var config = configRepository.findById(campaign.getTenantId()).orElse(null);
        if (config == null || campaign.getMetaCampaignId() == null) return;

        String token = resolveToken(campaign.getTenantId(), config);
        if (token == null) return;

        try {
            var rc = restClientBuilder.baseUrl(GRAPH_BASE).build();
            String raw = rc.get()
                    .uri("/" + API_VERSION + "/" + campaign.getMetaCampaignId()
                            + "?fields=status,review_feedback&access_token=" + token)
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(raw);
            String metaStatus = node.path("status").asText("");

            CampaignStatus newStatus = switch (metaStatus) {
                case "ACTIVE" -> CampaignStatus.ACTIVE;
                case "PAUSED" -> CampaignStatus.PAUSED;
                case "DISAPPROVED" -> CampaignStatus.REJECTED;
                case "ARCHIVED" -> CampaignStatus.ARCHIVED;
                case "IN_PROCESS", "WITH_ISSUES" -> CampaignStatus.PENDING_REVIEW;
                default -> null;
            };

            if (newStatus == null || newStatus == campaign.getStatus()) return;

            campaign.setStatus(newStatus);

            if (newStatus == CampaignStatus.ACTIVE) {
                notifyTenant(campaign.getTenantId(),
                    "✅ Meta Ads: \"" + campaign.getName() + "\" activada. Pressupost: "
                    + (campaign.getDailyBudget() != null ? campaign.getDailyBudget() + "€/dia" : "—"));
            } else if (newStatus == CampaignStatus.REJECTED) {
                String feedback = node.path("review_feedback").path("global").asText("Motiu no especificat");
                campaign.setMetaError(feedback);
                notifyTenant(campaign.getTenantId(),
                    "❌ Meta Ads: \"" + campaign.getName() + "\" rebutjada. Motiu: " + feedback);
            }

            campaignService.saveCampaign(campaign);

        } catch (Exception e) {
            log.warn("[MetaAds] Error sincronitzant estat de campanya {}: {}", campaign.getId(), e.getMessage());
        }
    }

    // ---- Utilitats ----

    private String resolveToken(UUID tenantId, MetaAdsConfig config) {
        if (config.getAccessToken() != null && !config.getAccessToken().isBlank())
            return config.getAccessToken();
        return sysConfig.get("META_ADS_MANAGEMENT_TOKEN");
    }

    private String normalizeAccountId(String id) {
        if (id == null) return null;
        return id.startsWith("act_") ? id : "act_" + id;
    }

    private String resolvePageId(UUID tenantId) {
        return chatLinkRepository.findByTenantId(tenantId)
                .map(cl -> cl.getMetaPageId())
                .orElseThrow(() -> new IllegalStateException("Meta Page ID no configurat al tenant"));
    }

    private void notifyTenant(UUID tenantId, String message) {
        try {
            chatLinkRepository.findByTenantId(tenantId).ifPresent(cl -> {
                if (cl.getTelegramChatId() != null)
                    telegramBotClient.sendMessage(cl.getTelegramChatId(), message);
            });
        } catch (Exception e) {
            log.warn("[MetaAds] Error enviant notificació Telegram al tenant {}: {}", tenantId, e.getMessage());
        }
    }

    @SafeVarargs
    private static <T> T coalesce(T... values) {
        for (T v : values) if (v != null) return v;
        return null;
    }
}
