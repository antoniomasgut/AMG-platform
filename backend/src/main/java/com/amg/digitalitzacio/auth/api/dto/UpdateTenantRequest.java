package com.amg.digitalitzacio.auth.api.dto;

public record UpdateTenantRequest(
        String name,
        String slug,
        String email,
        String phone,
        String address
) {}
