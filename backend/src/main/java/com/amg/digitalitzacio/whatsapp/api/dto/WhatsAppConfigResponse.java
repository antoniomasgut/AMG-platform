package com.amg.digitalitzacio.whatsapp.api.dto;

import java.time.Instant;
import java.util.UUID;

public record WhatsAppConfigResponse(
        UUID id,
        UUID tenantId,
        String wabaId,
        String phoneNumberId,
        String displayPhoneNumber,
        String businessName,
        String status,
        Boolean webhookRegistered,
        Instant connectedAt,
        Instant createdAt
) {}
