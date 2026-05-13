package com.amg.digitalitzacio.ops.api.dto;

import java.time.Instant;

public record OpsDashboardResponse(CurrentStatus currentStatus, int openIncidents, int todayIncidents,
                                    long avgResponseTimeMs, Instant lastBackup,
                                    double uptimePercentage) {
    public record CurrentStatus(int services, int up, int degraded, int down) {}
}
