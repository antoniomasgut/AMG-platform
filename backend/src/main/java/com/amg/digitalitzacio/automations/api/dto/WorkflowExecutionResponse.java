package com.amg.digitalitzacio.automations.api.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkflowExecutionResponse(
    UUID id,
    String triggerType,
    String sourceId,
    String status,
    String errorMessage,
    Instant executedAt,
    Instant completedAt
) {}
