package com.amg.digitalitzacio.domains.api.dto;

import java.time.Instant;

// Resultat de la verificació DNS d'un domini
public record DnsVerifyResponse(
        boolean verified,
        String resolvedIp,
        String expectedIp,
        Instant checkedAt
) {}
