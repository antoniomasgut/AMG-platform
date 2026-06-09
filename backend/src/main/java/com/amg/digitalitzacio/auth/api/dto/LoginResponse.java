package com.amg.digitalitzacio.auth.api.dto;

import com.amg.digitalitzacio.shared.security.Role;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        UserInfo user
) {
    public record UserInfo(UUID id, String email, String name, String position, Role role, TenantInfo tenant) {}

    public record TenantInfo(UUID id, String name) {}
}
