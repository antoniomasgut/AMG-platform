package com.amg.digitalitzacio.engine.api.dto;

import java.time.Instant;
import java.util.UUID;

public record LandingSummary(
    UUID id,
    String title,
    String slug,
    String status,
    String publicUrl,
    Boolean domainVerified,
    Boolean managedDomain,
    String domainStatus,
    Instant createdAt
) {}
