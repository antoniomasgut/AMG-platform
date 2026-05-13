package com.amg.digitalitzacio.engine.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DomainConfigRequest(
    String domain,
    Boolean managed,
    String registrar,
    LocalDate renewalDate,
    BigDecimal renewalPrice,
    String ownerName,
    String ownerEmail,
    String ownerPhone
) {}
