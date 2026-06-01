package com.amg.digitalitzacio.agents.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantAIConfigRepository extends JpaRepository<TenantAIConfig, UUID> {
    java.util.Optional<TenantAIConfig> findByTenantId(UUID tenantId);
}
