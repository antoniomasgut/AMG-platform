package com.amg.digitalitzacio.backup.api.dto;

import java.time.Instant;
import java.util.UUID;

public record BackupTaskResponse(
        UUID id,
        String type,
        String status,
        String scope,
        UUID tenantId,
        UUID requestedBy,
        Integer totalTenants,
        Integer completedTenants,
        Integer failedTenants,
        Long archiveSize,
        Instant startedAt,
        Instant completedAt,
        Instant retentionUntil
) {}
