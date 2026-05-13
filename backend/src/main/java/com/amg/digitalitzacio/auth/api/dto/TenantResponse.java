package com.amg.digitalitzacio.auth.api.dto;

import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        String slug,
        String email,
        String phone,
        String address,
        String nif,
        String contactPhone,
        String preferredChannel,
        boolean isActive,
        Instant createdAt
) {}
