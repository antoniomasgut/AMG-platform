package com.amg.digitalitzacio.gocardless.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record GoCardlessWebhookRequest(
        List<GoCardlessEvent> events
) {
    public record GoCardlessEvent(
            String id,
            @JsonProperty("resource_type") String resourceType,
            String action,
            Map<String, String> links
    ) {}
}
