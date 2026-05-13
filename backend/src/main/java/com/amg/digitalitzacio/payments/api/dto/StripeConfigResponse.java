package com.amg.digitalitzacio.payments.api.dto;

import java.time.Instant;
import java.util.UUID;

public record StripeConfigResponse(
        UUID id,
        UUID tenantId,
        boolean isActive,
        Instant createdAt
) {}
