package com.amg.digitalitzacio.agents.api.dto;

import java.util.UUID;

public record KnowledgeEntryResponse(
    UUID id,
    String category,
    String key,
    String content,
    int sortOrder
) {}
