package com.amg.digitalitzacio.agents.api.dto;

import java.util.UUID;

public record SectorTemplateResponse(
        UUID id,
        String sector,
        String type,
        String title,
        String body,
        int sortOrder
) {}
