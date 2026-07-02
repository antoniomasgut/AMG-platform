package com.amg.digitalitzacio.engine.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LandingTemplateResponse(
    UUID id,
    String name,
    String slug,
    String description,
    boolean isActive,
    String defaultStyles,
    List<LandingTemplateSectionView> sections,
    Instant createdAt,
    Instant updatedAt
) {}
