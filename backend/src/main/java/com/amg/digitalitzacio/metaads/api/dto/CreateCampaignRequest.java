package com.amg.digitalitzacio.metaads.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateCampaignRequest(
    String name,
    String objective,
    BigDecimal dailyBudget,
    BigDecimal lifetimeBudget,
    Instant startTime,
    Instant stopTime
) {}
