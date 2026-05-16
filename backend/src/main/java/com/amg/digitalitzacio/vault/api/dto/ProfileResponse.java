package com.amg.digitalitzacio.vault.api.dto;

import com.amg.digitalitzacio.vault.domain.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(
    UUID id, String name, String slug, String description, Boolean isActive,
    List<ServiceResponse> directServices,
    List<PhaseResponse> phases,
    Instant createdAt, Instant updatedAt
) {
    public record PhaseResponse(UUID id, String name, String description, Integer sortOrder, List<ServiceResponse> services) {}
    public record ServiceResponse(UUID id, String name, String slug, String description, ServiceType type, Boolean isAddon,
                                  BigDecimal cost, BigDecimal salePrice, BigDecimal monthlyPrice, Integer sortOrder, List<CredentialFieldResponse> fields) {}
    public record CredentialFieldResponse(UUID id, String key, String label, FieldType fieldType, Boolean isRequired,
                                          String placeholder, String validationRegex, Integer sortOrder) {}
}
