package com.amg.digitalitzacio.vault.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantPhaseRepository extends JpaRepository<TenantPhase, UUID> {
    List<TenantPhase> findByTenantId(UUID tenantId);
    List<TenantPhase> findByTenantIdAndProfileId(UUID tenantId, UUID profileId);
    Optional<TenantPhase> findByTenantIdAndPhaseId(UUID tenantId, UUID phaseId);
}
