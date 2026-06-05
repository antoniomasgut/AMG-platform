package com.amg.digitalitzacio.metaads.api.dto;

import java.time.Instant;
import java.util.UUID;

public record MetaAdsConfigResponse(
    UUID tenantId,
    String adAccountId,
    boolean hasAccessToken,
    boolean enabled,
    Instant lastSyncAt,
    Instant updatedAt
) {}
