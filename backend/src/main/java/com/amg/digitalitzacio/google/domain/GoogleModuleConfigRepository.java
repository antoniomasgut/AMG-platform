package com.amg.digitalitzacio.google.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GoogleModuleConfigRepository extends JpaRepository<GoogleModuleConfig, UUID> {
    Optional<GoogleModuleConfig> findByTenantId(UUID tenantId);
}
