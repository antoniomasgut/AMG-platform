package com.amg.digitalitzacio.metaads.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdSetResponse(
    UUID id,
    UUID campaignId,
    String metaAdSetId,
    String name,
    String status,
    BigDecimal dailyBudget,
    String optimizationGoal,
    Integer ageMin,
    Integer ageMax,
    String genders,
    String publisherPlatforms,
    String metaError,
    List<AdResponse> ads,
    Instant createdAt,
    Instant updatedAt
) {}
