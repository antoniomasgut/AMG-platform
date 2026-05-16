package com.amg.digitalitzacio.infraops.api.dto;

import java.time.Instant;
import java.util.UUID;

public record MetricSnapshotResponse(
        UUID id,
        Instant collectedAt,
        double cpuPercent,
        double ramPercent,
        double diskPercent,
        long ramUsedMb,
        long ramTotalMb,
        long diskUsedGb,
        long diskTotalGb,
        int dbActiveConnections,
        int dbMaxConnections,
        int activeTenants
) {}
