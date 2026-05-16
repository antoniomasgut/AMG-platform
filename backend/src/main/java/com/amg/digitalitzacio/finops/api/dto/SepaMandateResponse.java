package com.amg.digitalitzacio.finops.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SepaMandateResponse(UUID id, UUID tenantId, String mandateId, String iban,
                                   String accountHolderName, LocalDate signedAt,
                                   Boolean isActive, Instant createdAt) {}
