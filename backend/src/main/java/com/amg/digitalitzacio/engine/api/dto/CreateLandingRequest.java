package com.amg.digitalitzacio.engine.api.dto;

import java.util.UUID;

public record CreateLandingRequest(
    String title,
    String slug,
    String metaDescription,
    String landingType,
    UUID serviceId,
    UUID templateId
) {}
