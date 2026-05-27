package com.amg.digitalitzacio.telegram.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantTelegramConfigRepository extends JpaRepository<TenantTelegramConfig, UUID> {
    Optional<TenantTelegramConfig> findByTenantId(UUID tenantId);
    long countByTenantId(UUID tenantId);
    void deleteByTenantId(UUID tenantId);
}
