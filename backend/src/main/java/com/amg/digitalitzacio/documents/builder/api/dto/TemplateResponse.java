package com.amg.digitalitzacio.documents.builder.api.dto;

import com.amg.digitalitzacio.documents.builder.domain.DocumentType;

import java.time.Instant;
import java.util.UUID;

public record TemplateResponse(
    UUID id,
    UUID tenantId,
    String name,
    DocumentType documentType,
    Integer version,
    Boolean active,
    String layout,
    String dataBindings,
    String styles,
    Instant createdAt,
    Instant updatedAt
) {}
