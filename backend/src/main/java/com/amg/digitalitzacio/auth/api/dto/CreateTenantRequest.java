package com.amg.digitalitzacio.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 60) String slug,
        @Size(max = 150) String email,
        @Size(max = 20) String phone,
        @Size(max = 255) String address,
        @Size(max = 20) String nif,
        @Size(max = 20) String contactPhone,
        String preferredChannel,
        String sector,
        String businessSize,
        java.util.List<String> contractedPhases,
        String agentSystemPrompt,
        Boolean isFree
) {}
