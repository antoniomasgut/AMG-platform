package com.amg.digitalitzacio.leads.api.dto;

import com.amg.digitalitzacio.leads.domain.ActivityType;

import java.time.Instant;
import java.util.UUID;

public record ActivityResponse(
        UUID id,
        ActivityType type,
        String description,
        UserRef user,
        Instant dueDate,
        Instant completedAt,
        Instant createdAt
) {
    public record UserRef(UUID id, String name) {}
}
