package com.amg.digitalitzacio.shared.security;

import java.util.UUID;

public record UserPrincipal(
        UUID id,
        String email,
        String role,
        UUID tenantId
) {}
