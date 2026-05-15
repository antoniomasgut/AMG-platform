package com.amg.digitalitzacio.engine.api.dto;

import java.util.Map;
import java.util.UUID;

public record CreateLandingFromTemplateRequest(
    String title,
    String slug,
    String metaDescription,
    UUID serviceId,
    UUID templateId,
    Map<String, Map<String, Object>> filledSections
) {}
