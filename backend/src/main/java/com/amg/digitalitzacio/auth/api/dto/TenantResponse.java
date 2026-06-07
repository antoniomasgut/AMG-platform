package com.amg.digitalitzacio.auth.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        String slug,
        String email,
        String phone,
        String address,
        String city,
        String nif,
        String contactPhone,
        String preferredChannel,
        String sector,
        String businessSize,
        java.util.List<String> contractedPhases,
        java.util.List<String> activePhases,
        String agentSystemPrompt,
        boolean isActive,
        boolean isFree,
        LocalDate billingStartDate,
        Instant implementationDeliveredAt,
        Instant onboardingCompletedAt,
        Instant createdAt
) {}
