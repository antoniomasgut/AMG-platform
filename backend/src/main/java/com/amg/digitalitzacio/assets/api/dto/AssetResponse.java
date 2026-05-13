package com.amg.digitalitzacio.assets.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AssetResponse(
        UUID id,
        String originalName,
        String mimeType,
        long size,
        Integer width,
        Integer height,
        String url,
        String thumbnailUrl,
        Instant createdAt
) {}
