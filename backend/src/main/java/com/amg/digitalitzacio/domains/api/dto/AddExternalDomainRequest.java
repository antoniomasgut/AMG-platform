package com.amg.digitalitzacio.domains.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Petició per a afegir un domini extern al sistema (Escenaris A i D)
public record AddExternalDomainRequest(
        @NotNull UUID tenantId,
        @NotBlank String domainName,   // ex: "restaurantmaria.com" o "carta.restaurantmaria.com"
        UUID landingId,
        @NotBlank String scenario      // "EXTERNAL_OWN" o "EXTERNAL_SUBDOMAIN"
) {}
