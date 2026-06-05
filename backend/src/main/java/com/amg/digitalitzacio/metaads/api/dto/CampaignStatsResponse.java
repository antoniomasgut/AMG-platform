package com.amg.digitalitzacio.metaads.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record CampaignStatsResponse(
    List<CampaignRow> campaigns,
    BigDecimal totalSpend,
    long totalLeadsFromAds,
    Double avgCpl,
    String period
) {
    public record CampaignRow(
        String campaignId,
        String campaignName,
        BigDecimal spend,
        long impressions,
        long clicks,
        long leads,
        Double cpl
    ) {}
}
