package com.amg.digitalitzacio.ops.api.dto;

import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(UUID id, String serviceName, String severity, String status,
                                String title, String description, Instant startedAt,
                                Instant resolvedAt, Long durationSeconds) {}
