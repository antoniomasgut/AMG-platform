package com.amg.digitalitzacio.vault.api.dto;

import java.time.Instant;
import java.util.UUID;

public record RequestInfoResponse(
        UUID requestId,
        String channel,
        String status,
        String message,
        Instant expiresAt
) {}
