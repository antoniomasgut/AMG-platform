package com.amg.digitalitzacio.comm.api.dto;

public record CommTemplateRequest(
        String sector,
        String channel,
        String action,
        String language,
        String subject,
        String body,
        Boolean isActive,
        int sortOrder
) {}
