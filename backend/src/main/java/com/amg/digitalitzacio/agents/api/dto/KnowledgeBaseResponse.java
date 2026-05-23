package com.amg.digitalitzacio.agents.api.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record KnowledgeBaseResponse(
    UUID id,
    UUID tenantId,
    boolean isActive,
    int version,
    Map<String, List<KnowledgeEntryResponse>> entriesByCategory,
    List<KnowledgeDocumentResponse> documents
) {}
