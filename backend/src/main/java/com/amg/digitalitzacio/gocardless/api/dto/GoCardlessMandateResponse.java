package com.amg.digitalitzacio.gocardless.api.dto;

import java.time.Instant;
import java.util.UUID;

public record GoCardlessMandateResponse(
        UUID id,
        UUID tenantId,
        String gcMandateId,
        String status,
        String accountHolderName,
        String bankName,
        String lastFourDigits,
        Instant createdAt
) {}
