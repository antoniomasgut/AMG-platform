package com.amg.digitalitzacio.metaads.api.dto;

public record MetaAdsConfigRequest(
    String adAccountId,
    String accessToken,
    boolean enabled
) {}
