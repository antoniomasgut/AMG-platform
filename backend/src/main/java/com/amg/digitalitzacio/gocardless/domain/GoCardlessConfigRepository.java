package com.amg.digitalitzacio.gocardless.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GoCardlessConfigRepository extends JpaRepository<GoCardlessConfig, UUID> {
    Optional<GoCardlessConfig> findByTenantId(UUID tenantId);
}
