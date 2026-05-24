package com.amg.digitalitzacio.vault.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantProfileRepository extends JpaRepository<TenantProfile, UUID> {
    Optional<TenantProfile> findByTenantIdAndProfileId(UUID tenantId, UUID profileId);
    List<TenantProfile> findByTenantId(UUID tenantId);
    void deleteByTenantId(UUID tenantId);
}
