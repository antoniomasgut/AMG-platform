package com.amg.digitalitzacio.vault.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantServiceAddonRepository extends JpaRepository<TenantServiceAddon, UUID> {
    Optional<TenantServiceAddon> findByTenantIdAndServiceId(UUID tenantId, UUID serviceId);
    List<TenantServiceAddon> findByTenantId(UUID tenantId);
}
