package com.amg.digitalitzacio.gocardless.api.dto;

import java.util.UUID;

public record GoCardlessConfigRequest(
        UUID tenantId,
        String apiKeyRef,
        String environment,
        String creditorId,
        String webhookSecret
) {}
