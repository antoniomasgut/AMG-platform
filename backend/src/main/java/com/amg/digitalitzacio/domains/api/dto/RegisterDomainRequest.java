package com.amg.digitalitzacio.domains.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Petició de registre d'un nou domini per a un tenent
public record RegisterDomainRequest(
        @NotNull UUID tenantId,
        @NotBlank String domainName,
        UUID landingId,
        boolean autoRenew,
        String registrantName,
        String registrantEmail,
        String registrantPhone,
        String registrantNif
) {}
