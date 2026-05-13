package com.amg.digitalitzacio.payments.api.dto;

import java.util.UUID;

public record StripeConfigRequest(
        UUID tenantId,
        String apiKeyRef,
        String webhookSecret
) {}
