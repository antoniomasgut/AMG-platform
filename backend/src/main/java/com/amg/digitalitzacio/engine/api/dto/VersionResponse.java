package com.amg.digitalitzacio.engine.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record VersionResponse(
    UUID id,
    Integer versionNumber,
    String status,
    Map<String, Object> content,
    Map<String, Object> styles,
    Instant publishedAt,
    Instant createdAt
) {}
