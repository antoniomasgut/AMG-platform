package com.amg.digitalitzacio.domains.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

// Petició per a assignar un subdomini de amgdl.com a un tenent (Escenari C)
public record AssignAmgSubdomainRequest(
        @NotNull UUID tenantId,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]{2,50}$") String subdomain,  // ex: "canpedro"
        UUID landingId
) {}
