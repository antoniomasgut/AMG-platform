package com.amg.digitalitzacio.backup.api.dto;

import java.time.Instant;

public record BackupDashboardResponse(
        long totalBackups,
        Instant lastBackup,
        String lastBackupStatus,
        long scheduledCount,
        long manualFullCount,
        long manualTenantCount,
        long storageUsedBytes,
        Instant nextScheduledBackup,
        int retentionDays
) {}
