package com.amg.digitalitzacio.auth.api.dto;

import com.amg.digitalitzacio.shared.security.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        Role role,
        TenantRef tenant,
        boolean isActive,
        boolean isBlocked,
        Instant lastLoginAt,
        Instant createdAt
) {
    public record TenantRef(UUID id, String name) {}
}
