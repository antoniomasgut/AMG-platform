package com.amg.digitalitzacio.platform.api.dto;

public record PlatformRecommendation(
        String service,
        String title,
        String description,
        String impact,
        String category   // CLIENT_CONNECTIVITY | UPGRADE_OFFER | COST_OPTIMIZATION
) {}
