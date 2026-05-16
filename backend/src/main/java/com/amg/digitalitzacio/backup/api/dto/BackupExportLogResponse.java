package com.amg.digitalitzacio.backup.api.dto;

import java.time.Instant;
import java.util.UUID;

public record BackupExportLogResponse(
        UUID id,
        UUID taskId,
        UUID tenantId,
        String tenantName,
        String status,
        Long fileSize,
        Integer sectionsCount,
        String errorMessage,
        Instant startedAt,
        Instant completedAt
) {}
