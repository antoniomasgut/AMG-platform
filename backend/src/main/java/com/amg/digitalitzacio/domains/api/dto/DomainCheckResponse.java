package com.amg.digitalitzacio.domains.api.dto;

import java.math.BigDecimal;
import java.util.List;

// Resposta de la consulta de disponibilitat d'un domini
public record DomainCheckResponse(
        String domainName,
        boolean available,
        BigDecimal saleRegister,
        BigDecimal saleRenew,
        List<String> alternatives
) {}
