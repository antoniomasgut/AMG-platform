package com.amg.digitalitzacio.prospecting.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CampaignResponse(
        UUID id, String name, String sector, String location,
        String source, String status, Integer totalFound, Integer totalExported,
        String searchParams, String notes, UUID createdBy,
        Instant createdAt, Instant updatedAt,
        Instant scheduledNextRun, Integer repeatIntervalDays
) {}
