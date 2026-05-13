package com.amg.digitalitzacio.automations.api.dto;

import java.util.Map;

public record WebhookRequest(
    String workflowId,
    String executionId,
    String status,
    Map<String, Object> output,
    String error,
    String executionStart,
    String executionEnd
) {}
