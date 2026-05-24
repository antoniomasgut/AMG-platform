package com.amg.digitalitzacio.vault.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantServiceRepository extends JpaRepository<TenantService, UUID> {
    List<TenantService> findByTenantId(UUID tenantId);
    List<TenantService> findByTenantIdAndPhaseId(UUID tenantId, UUID phaseId);
    Optional<TenantService> findByTenantIdAndServiceId(UUID tenantId, UUID serviceId);
    List<TenantService> findByServiceId(UUID serviceId);
    List<TenantService> findByOutdatedTrue();
    long countByTenantIdAndIsEnabled(UUID tenantId, Boolean isEnabled);
    void deleteByTenantId(UUID tenantId);
}
