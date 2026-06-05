package com.amg.digitalitzacio.auth.api.dto;

public record UpdateTenantRequest(
        String name,
        String slug,
        String email,
        String phone,
        String address,
        String nif,
        String contactPhone,
        String preferredChannel,
        String sector,
        String businessSize,
        java.util.List<String> contractedPhases,
        java.util.List<String> activePhases,
        String agentSystemPrompt,
        Boolean isFree,
        Boolean isActive
) {}
