package com.amg.digitalitzacio.domains.api.dto;

import jakarta.validation.constraints.NotBlank;

// Petició de consulta de disponibilitat d'un domini
public record DomainCheckRequest(
        @NotBlank String domainName
) {}
