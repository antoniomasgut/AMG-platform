package com.amg.digitalitzacio.shared.storage.api.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ProviderConfigResponse(
    UUID tenantId,
    String providerKey,
    Map<String, Object> config,
    boolean active,
    Instant updatedAt
) {}
