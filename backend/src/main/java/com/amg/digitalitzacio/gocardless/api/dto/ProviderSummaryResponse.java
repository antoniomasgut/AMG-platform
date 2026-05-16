package com.amg.digitalitzacio.gocardless.api.dto;

import java.util.UUID;

public record ProviderSummaryResponse(
        UUID tenantId,
        SetupProviders setup,
        RecurringProviders recurring
) {
    public record SetupProviders(
            String activeProvider,
            boolean stripeConfigured,
            boolean stripeActive
    ) {}

    public record RecurringProviders(
            String activeProvider,
            boolean sepaMandateActive,
            boolean goCardlessMandateActive,
            String goCardlessMandateStatus
    ) {}
}
