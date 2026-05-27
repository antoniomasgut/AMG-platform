package com.amg.digitalitzacio.telegram.api.dto;

import java.time.Instant;
import java.util.UUID;

public record TelegramConfigResponse(
        UUID id,
        UUID tenantId,
        String botUsername,
        String status,
        Boolean webhookRegistered,
        Instant connectedAt,
        Instant createdAt
) {}
