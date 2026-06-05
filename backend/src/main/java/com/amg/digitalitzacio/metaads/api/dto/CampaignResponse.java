package com.amg.digitalitzacio.metaads.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CampaignResponse(
    UUID id,
    String metaCampaignId,
    String name,
    String objective,
    String status,
    BigDecimal dailyBudget,
    BigDecimal lifetimeBudget,
    Instant startTime,
    Instant stopTime,
    String metaError,
    List<AdSetResponse> adSets,
    Instant createdAt,
    Instant updatedAt
) {}
