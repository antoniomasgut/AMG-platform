package com.amg.digitalitzacio.auth.api.dto;

import com.amg.digitalitzacio.shared.security.Role;

import java.util.UUID;

public record UpdateUserRequest(
        String email,
        String name,
        String position,
        Role role,
        UUID tenantId,
        Boolean isActive
) {}
