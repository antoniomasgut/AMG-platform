package com.amg.digitalitzacio.google.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GoogleConnectionRepository extends JpaRepository<GoogleConnection, UUID> {
    Optional<GoogleConnection> findByTenantIdAndActiveTrue(UUID tenantId);
    Optional<GoogleConnection> findByTenantId(UUID tenantId);
    boolean existsByTenantIdAndActiveTrue(UUID tenantId);
}
