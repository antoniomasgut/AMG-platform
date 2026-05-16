package com.amg.digitalitzacio.gocardless.api.dto;

import java.time.Instant;
import java.util.UUID;

public record GoCardlessConfigResponse(
        UUID id,
        UUID tenantId,
        String environment,
        String creditorId,
        boolean isActive,
        Instant createdAt
) {}
