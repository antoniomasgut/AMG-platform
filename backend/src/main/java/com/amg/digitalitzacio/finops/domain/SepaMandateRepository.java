package com.amg.digitalitzacio.finops.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SepaMandateRepository extends JpaRepository<SepaMandate, UUID> {
    Optional<SepaMandate> findByTenantIdAndIsActiveTrue(UUID tenantId);
    Optional<SepaMandate> findByTenantId(UUID tenantId);
    List<SepaMandate> findAllByIsActiveTrue();
}
