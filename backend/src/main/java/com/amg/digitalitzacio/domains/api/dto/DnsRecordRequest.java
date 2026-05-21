package com.amg.digitalitzacio.domains.api.dto;

import jakarta.validation.constraints.NotBlank;

// Petició de creació o actualització d'un registre DNS
public record DnsRecordRequest(
        @NotBlank String type,
        @NotBlank String name,
        @NotBlank String value,
        int ttl,
        Integer priority
) {}
