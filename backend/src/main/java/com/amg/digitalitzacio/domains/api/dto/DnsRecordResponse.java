package com.amg.digitalitzacio.domains.api.dto;

import java.util.UUID;

// Resposta amb les dades d'un registre DNS
public record DnsRecordResponse(
        UUID id,
        String type,
        String name,
        String value,
        int ttl,
        Integer priority
) {}
