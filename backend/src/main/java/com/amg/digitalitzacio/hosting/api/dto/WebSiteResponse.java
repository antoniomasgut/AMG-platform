package com.amg.digitalitzacio.hosting.api.dto;

import com.amg.digitalitzacio.hosting.domain.WebsiteStatus;
import com.amg.digitalitzacio.hosting.domain.WebsiteType;

import java.time.Instant;
import java.util.UUID;

public record WebSiteResponse(
        UUID id,
        UUID tenantId,
        WebsiteType type,
        WebsiteStatus status,
        String domain,
        String containerName,
        Long storageBytes,
        String clientNotes,
        String reviewNotes,
        Instant reviewedAt,
        Instant deployedAt,
        Instant createdAt
) {}
