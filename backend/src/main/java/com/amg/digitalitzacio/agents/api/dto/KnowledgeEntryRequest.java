package com.amg.digitalitzacio.agents.api.dto;

public record KnowledgeEntryRequest(
    String key,
    String content,
    Integer sortOrder
) {}
