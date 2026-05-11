package com.amg.digitalitzacio.auth.api.dto;

public record RefreshResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType
) {}
