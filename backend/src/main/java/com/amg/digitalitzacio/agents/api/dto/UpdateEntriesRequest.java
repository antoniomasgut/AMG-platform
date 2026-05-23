package com.amg.digitalitzacio.agents.api.dto;

import java.util.List;

public record UpdateEntriesRequest(
    List<KnowledgeEntryRequest> entries
) {}
