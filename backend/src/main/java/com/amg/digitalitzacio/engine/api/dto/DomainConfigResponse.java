package com.amg.digitalitzacio.engine.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DomainConfigResponse(
    String domain,
    Boolean managed,
    String domainStatus,
    String dnsInstructions,
    String registrar,
    LocalDate renewalDate,
    BigDecimal renewalPrice
) {}
