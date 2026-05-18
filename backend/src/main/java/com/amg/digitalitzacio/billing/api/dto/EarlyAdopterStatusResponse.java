package com.amg.digitalitzacio.billing.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EarlyAdopterStatusResponse(
    UUID id,
    Integer maxSlots,
    Integer usedSlots,
    Integer availableSlots,
    BigDecimal setupDiscountPct,
    BigDecimal monthlyDiscountPct,
    Integer commitmentMonths,
    Boolean active,
    Instant updatedAt
) {}
