package com.amg.digitalitzacio.domains.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Resposta completa d'un domini gestionat amb els seus registres DNS
public record ManagedDomainResponse(
        UUID id,
        UUID tenantId,
        String domainName,
        String tld,
        String status,
        String scenario,
        BigDecimal purchasePrice,
        BigDecimal salePrice,
        Instant registeredAt,
        Instant expiresAt,
        boolean autoRenew,
        boolean dnsConfigured,
        boolean dnsVerified,
        Instant dnsVerifiedAt,
        String provider,
        List<DnsRecordResponse> dnsRecords
) {}
