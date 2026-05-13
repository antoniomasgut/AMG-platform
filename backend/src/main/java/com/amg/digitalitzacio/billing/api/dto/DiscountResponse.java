package com.amg.digitalitzacio.billing.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DiscountResponse(UUID id, UUID tenantId, String type, BigDecimal value, String appliesTo,
                                UUID referenceId, String label, Boolean isActive,
                                LocalDate validFrom, LocalDate validUntil,
                                Integer maxApplications, Integer appliedCount, Instant createdAt) {}
