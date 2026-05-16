package com.amg.digitalitzacio.backup.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RestoreResponse(
        UUID id,
        UUID tenantId,
        String status,
        List<String> sections,
        Instant createdAt
) {}
