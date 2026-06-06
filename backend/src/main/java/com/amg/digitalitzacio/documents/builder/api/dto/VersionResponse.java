package com.amg.digitalitzacio.documents.builder.api.dto;

import java.time.Instant;
import java.util.UUID;

public record VersionResponse(
    UUID id,
    UUID templateId,
    Integer version,
    String layout,
    String dataBindings,
    String styles,
    String notes,
    Instant createdAt
) {}
