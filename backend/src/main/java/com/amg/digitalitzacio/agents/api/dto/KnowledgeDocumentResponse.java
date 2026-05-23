package com.amg.digitalitzacio.agents.api.dto;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeDocumentResponse(
    UUID id,
    String filename,
    Instant uploadedAt
) {}
