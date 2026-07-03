package com.amg.digitalitzacio.billing.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateDiscountRequest(UUID tenantId, String type, BigDecimal value, String appliesTo,
                                     UUID referenceId, String label, LocalDate validFrom, LocalDate validUntil,
                                     Boolean appliesToSetup, Boolean appliesToMonthly,
                                     Boolean isLifetime, Integer maxApplications) {}
