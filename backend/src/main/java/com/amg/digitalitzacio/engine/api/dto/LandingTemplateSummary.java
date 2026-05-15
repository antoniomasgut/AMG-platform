package com.amg.digitalitzacio.engine.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LandingTemplateSummary(
    UUID id,
    String name,
    String slug,
    String description,
    int sectionCount,
    boolean isActive,
    Instant createdAt
) {}
