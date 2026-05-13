package com.amg.digitalitzacio.engine.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateDomainStatusRequest(
    String domainStatus,
    String registrar,
    LocalDate renewalDate,
    BigDecimal renewalPrice
) {}
