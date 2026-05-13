package com.amg.digitalitzacio.billing.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiscountRepository extends JpaRepository<Discount, UUID> {
    List<Discount> findByTenantId(UUID tenantId);
    List<Discount> findByTenantIdAndIsActive(UUID tenantId, Boolean isActive);
    List<Discount> findByIsActiveTrueAndAppliesToAndReferenceId(String appliesTo, UUID referenceId);
}
