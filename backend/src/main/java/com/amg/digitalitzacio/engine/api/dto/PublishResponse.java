package com.amg.digitalitzacio.engine.api.dto;

import java.time.Instant;

public record PublishResponse(
    Integer versionNumber,
    String status,
    String publicUrl,
    Instant publishedAt
) {}
