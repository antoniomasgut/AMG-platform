package com.amg.digitalitzacio.metaads.application;

import com.amg.digitalitzacio.metaads.api.dto.CampaignStatsResponse;
import com.amg.digitalitzacio.metaads.api.dto.MetaAdsConfigRequest;
import com.amg.digitalitzacio.metaads.api.dto.MetaAdsConfigResponse;
import com.amg.digitalitzacio.metaads.domain.CampaignSpend;
import com.amg.digitalitzacio.metaads.domain.CampaignSpendRepository;
import com.amg.digitalitzacio.metaads.domain.MetaAdsConfig;
import com.amg.digitalitzacio.metaads.domain.MetaAdsConfigRepository;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetaAdsService {

    private static final String GRAPH_BASE = "https://graph.facebook.com";
    private static final String API_VERSION = "v19.0";

    private final MetaAdsConfigRepository configRepository;
    private final CampaignSpendRepository spendRepository;
    private final LeadRepository leadRepository;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    @Transactional
    public MetaAdsConfigResponse saveConfig(UUID tenantId, MetaAdsConfigRequest request) {
        var config = configRepository.findById(tenantId).orElse(new MetaAdsConfig());
        config.setTenantId(tenantId);
        if (request.adAccountId() != null) config.setAdAccountId(request.adAccountId());
        // Si el token ve buit, no sobreescribim l'anterior
        if (request.accessToken() != null && !request.accessToken().isBlank()) {
            config.setAccessToken(request.accessToken());
        }
        config.setEnabled(request.enabled());
        configRepository.save(config);
        return toResponse(config);
    }

    @Transactional(readOnly = true)
    public MetaAdsConfigResponse getConfig(UUID tenantId) {
        return configRepository.findById(tenantId)
                .map(this::toResponse)
                .orElse(new MetaAdsConfigResponse(tenantId, null, false, false, null, null));
    }

    @Transactional
    public int sync(UUID tenantId) {
        var config = configRepository.findById(tenantId).orElse(null);
        if (config == null || !config.isEnabled() || isBlank(config.getAccessToken()) || isBlank(config.getAdAccountId())) {
            log.warn("[MetaAds] Sync ignorat per tenant {} — config incompleta o desactivada", tenantId);
            return 0;
        }
        int saved = fetchAndStore(config);
        config.setLastSyncAt(java.time.Instant.now());
        configRepository.save(config);
        return saved;
    }

    @Transactional(readOnly = true)
    public CampaignStatsResponse getStats(UUID tenantId) {
        LocalDate from = LocalDate.now().minusDays(30);
        List<Object[]> spendRows = spendRepository.sumByCampaign(tenantId, from);
        List<Object[]> leadRows  = leadRepository.countByUtmCampaign(tenantId);

        // Mapa de leads per utm_campaign
        Map<String, Long> leadsByCampaign = new HashMap<>();
        for (Object[] row : leadRows) {
            String name = (String) row[0];
            Long count  = ((Number) row[1]).longValue();
            if (name != null) leadsByCampaign.put(name.toLowerCase(), count);
        }

        List<CampaignStatsResponse.CampaignRow> rows = new ArrayList<>();
        BigDecimal totalSpend   = BigDecimal.ZERO;
        long totalLeadsFromAds  = 0;

        for (Object[] row : spendRows) {
            String campaignId   = (String) row[0];
            String campaignName = (String) row[1];
            BigDecimal spend    = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            long impressions    = row[3] != null ? ((Number) row[3]).longValue() : 0;
            long clicks         = row[4] != null ? ((Number) row[4]).longValue() : 0;

            // Intenta fer coincidir per nom de campanya (case-insensitive)
            long leads = leadsByCampaign.getOrDefault(campaignName.toLowerCase(), 0L);
            Double cpl = (leads > 0 && spend.compareTo(BigDecimal.ZERO) > 0)
                    ? spend.divide(BigDecimal.valueOf(leads), 2, RoundingMode.HALF_UP).doubleValue()
                    : null;

            rows.add(new CampaignStatsResponse.CampaignRow(campaignId, campaignName, spend, impressions, clicks, leads, cpl));
            totalSpend = totalSpend.add(spend);
            totalLeadsFromAds += leads;
        }

        Double avgCpl = (totalLeadsFromAds > 0 && totalSpend.compareTo(BigDecimal.ZERO) > 0)
                ? totalSpend.divide(BigDecimal.valueOf(totalLeadsFromAds), 2, RoundingMode.HALF_UP).doubleValue()
                : null;

        return new CampaignStatsResponse(rows, totalSpend, totalLeadsFromAds, avgCpl, "last_30d");
    }

    int fetchAndStore(MetaAdsConfig config) {
        String accountId = config.getAdAccountId().startsWith("act_")
                ? config.getAdAccountId() : "act_" + config.getAdAccountId();
        String token = config.getAccessToken();

        String url = String.format("/%s/%s/insights?fields=campaign_name,campaign_id,spend,impressions,clicks&date_preset=yesterday&level=campaign&time_increment=1&access_token=%s",
                API_VERSION, accountId, token);

        try {
            var rc  = restClientBuilder.baseUrl(GRAPH_BASE).build();
            String raw = rc.get().uri(url).retrieve().body(String.class);

            JsonNode data = objectMapper.readTree(raw).path("data");
            if (!data.isArray()) {
                log.warn("[MetaAds] Resposta inesperada de Meta per tenant {}: {}", config.getTenantId(), raw);
                return 0;
            }

            int saved = 0;
            for (JsonNode item : data) {
                String campaignId   = item.path("campaign_id").asText(null);
                String campaignName = item.path("campaign_name").asText(null);
                String spendStr     = item.path("spend").asText("0");
                long impressions    = item.path("impressions").asLong(0);
                long clicks         = item.path("clicks").asLong(0);
                String dateStr      = item.path("date_start").asText(null);

                if (campaignId == null || dateStr == null) continue;
                LocalDate spendDate = LocalDate.parse(dateStr);

                if (spendRepository.existsByTenantIdAndCampaignIdAndSpendDate(config.getTenantId(), campaignId, spendDate)) {
                    continue;
                }

                CampaignSpend spend = new CampaignSpend();
                spend.setTenantId(config.getTenantId());
                spend.setCampaignId(campaignId);
                spend.setCampaignName(campaignName != null ? campaignName : campaignId);
                spend.setSpend(new BigDecimal(spendStr));
                spend.setImpressions(impressions);
                spend.setClicks(clicks);
                spend.setSpendDate(spendDate);
                spendRepository.save(spend);
                saved++;
            }
            log.info("[MetaAds] Tenant {} — {} registres de despesa guardats", config.getTenantId(), saved);
            return saved;
        } catch (Exception e) {
            log.error("[MetaAds] Error sincronitzant tenant {}: {}", config.getTenantId(), e.getMessage());
            return 0;
        }
    }

    private MetaAdsConfigResponse toResponse(MetaAdsConfig c) {
        return new MetaAdsConfigResponse(
                c.getTenantId(),
                c.getAdAccountId(),
                c.getAccessToken() != null && !c.getAccessToken().isBlank(),
                c.isEnabled(),
                c.getLastSyncAt(),
                c.getUpdatedAt()
        );
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
