package com.amg.digitalitzacio.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TenantPhaseActivationRepository extends JpaRepository<TenantPhaseActivation, UUID> {

    List<TenantPhaseActivation> findByTenantIdOrderByActivatedAtDesc(UUID tenantId);

    boolean existsByTenantIdAndPhaseAndDeactivatedAtIsNull(UUID tenantId, String phase);

    List<TenantPhaseActivation> findByActivatedAtBetweenAndDeactivatedAtIsNull(Instant from, Instant to);

    List<TenantPhaseActivation> findByDeactivatedAtIsNullAndActivatedAtBefore(Instant before);

    void deleteByTenantId(UUID tenantId);
}
