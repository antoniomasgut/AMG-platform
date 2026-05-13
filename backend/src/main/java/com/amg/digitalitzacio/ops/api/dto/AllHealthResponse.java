package com.amg.digitalitzacio.ops.api.dto;

import java.time.Instant;
import java.util.List;

public record AllHealthResponse(List<ServiceHealthDto> services, HealthSummary summary, Instant timestamp) {
    public record HealthSummary(int total, int up, int down, int degraded) {}
}
