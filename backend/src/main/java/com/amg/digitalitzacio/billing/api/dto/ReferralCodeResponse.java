package com.amg.digitalitzacio.billing.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ReferralCodeResponse(
    UUID id,
    UUID ownerTenantId,
    String code,
    UUID usedByTenantId,
    Instant usedAt,
    Boolean creditApplied,
    Integer referrerCreditMonths,
    Instant createdAt
) {}
