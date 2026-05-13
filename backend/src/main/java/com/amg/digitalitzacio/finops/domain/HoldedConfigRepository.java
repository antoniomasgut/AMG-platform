package com.amg.digitalitzacio.finops.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HoldedConfigRepository extends JpaRepository<HoldedConfig, UUID> {
    Optional<HoldedConfig> findByTenantId(UUID tenantId);
}
