package com.amg.digitalitzacio.finops.api.dto;

import java.time.Instant;
import java.util.UUID;

public record HoldedConfigResponse(
        UUID id,
        UUID tenantId,
        String holdedCompanyId,
        String holdedContactId,
        boolean isSynced,
        Instant lastSyncAt,
        boolean isActive,
        Instant createdAt
) {}
