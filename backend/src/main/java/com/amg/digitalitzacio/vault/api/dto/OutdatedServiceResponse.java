package com.amg.digitalitzacio.vault.api.dto;

import java.time.Instant;
import java.util.UUID;

public record OutdatedServiceResponse(
        UUID tenantServiceId,
        UUID tenantId,
        UUID serviceId,
        String serviceName,
        String serviceSlug,
        int catalogVersion,
        int lockedVersion,
        Instant outdatedAt
) {}
