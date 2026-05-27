package com.amg.digitalitzacio.agents.api.dto;

import java.util.List;

public record AgentHealthResponse(
        int score,
        String status,
        List<HealthCheck> checks,
        List<String> suggestions,
        List<String> recommendations
) {
    public record HealthCheck(
            String key,
            String label,
            String status,
            String detail
    ) {}
}
