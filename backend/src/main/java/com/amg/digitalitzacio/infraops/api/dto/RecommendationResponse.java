package com.amg.digitalitzacio.infraops.api.dto;

import java.time.Instant;
import java.util.UUID;

public record RecommendationResponse(
        UUID id,
        String type,
        String severity,
        String message,
        Double triggerValue,
        Double threshold,
        Instant sentAt,
        Instant resolvedAt,
        boolean isActive,
        Instant createdAt
) {}
