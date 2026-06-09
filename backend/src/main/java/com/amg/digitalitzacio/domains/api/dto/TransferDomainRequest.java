package com.amg.digitalitzacio.domains.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Petició per a transferir un domini existent cap a OpenProvider (Escenari E)
public record TransferDomainRequest(
        @NotNull UUID tenantId,
        @NotBlank String domainName,
        @NotBlank String eppCode,
        UUID landingId,
        boolean autoRenew,
        String registrantName,
        String registrantEmail,
        String registrantPhone,
        String registrantNif
) {}
