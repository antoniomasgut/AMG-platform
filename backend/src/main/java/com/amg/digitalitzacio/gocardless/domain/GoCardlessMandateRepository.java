package com.amg.digitalitzacio.gocardless.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GoCardlessMandateRepository extends JpaRepository<GoCardlessMandate, UUID> {
    Optional<GoCardlessMandate> findByTenantId(UUID tenantId);
    Optional<GoCardlessMandate> findByTenantIdAndStatus(UUID tenantId, GoCardlessMandateStatus status);
    Optional<GoCardlessMandate> findByGcRedirectFlowId(String gcRedirectFlowId);
}
