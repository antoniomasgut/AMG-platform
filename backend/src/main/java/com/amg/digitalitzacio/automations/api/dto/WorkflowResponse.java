package com.amg.digitalitzacio.automations.api.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkflowResponse(
    UUID id,
    String templateName,
    String templateKey,
    String status,
    String n8nWebhookUrl,
    String n8nWorkflowId,
    Instant lastRunAt,
    String lastRunStatus,
    String errorMessage,
    Instant createdAt
) {}
