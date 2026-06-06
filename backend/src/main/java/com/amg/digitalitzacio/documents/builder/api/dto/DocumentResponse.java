package com.amg.digitalitzacio.documents.builder.api.dto;

import com.amg.digitalitzacio.documents.builder.domain.DocumentStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
    UUID id,
    UUID tenantId,
    UUID templateId,
    Integer templateVersion,
    String number,
    DocumentStatus status,
    String htmlContent,
    String pdfUrl,
    Instant generatedAt,
    Instant createdAt
) {}
