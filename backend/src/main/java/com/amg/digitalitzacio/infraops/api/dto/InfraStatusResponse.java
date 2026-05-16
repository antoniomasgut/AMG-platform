package com.amg.digitalitzacio.infraops.api.dto;

import java.time.Instant;

public record InfraStatusResponse(
        Instant collectedAt,
        CpuStatus cpu,
        RamStatus ram,
        DiskStatus disk,
        DbStatus database,
        TenantStatus tenants,
        String overallStatus,
        int activeRecommendations
) {
    public record CpuStatus(double percent, String status) {}
    public record RamStatus(long usedMb, long totalMb, double percent, String status) {}
    public record DiskStatus(long usedGb, long totalGb, double percent, String status) {}
    public record DbStatus(int activeConnections, int maxConnections, double percent, String status) {}
    public record TenantStatus(int active, String recommendation) {}
}
