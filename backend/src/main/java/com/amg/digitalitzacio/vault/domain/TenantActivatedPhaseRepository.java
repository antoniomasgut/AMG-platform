package com.amg.digitalitzacio.vault.domain;

import com.amg.digitalitzacio.auth.domain.BusinessSector;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantActivatedPhaseRepository extends JpaRepository<TenantActivatedPhase, UUID> {
    List<TenantActivatedPhase> findByTenantIdOrderBySectorAscPhaseNumberAsc(UUID tenantId);
    Optional<TenantActivatedPhase> findByTenantIdAndSectorAndPhaseNumber(UUID tenantId, BusinessSector sector, int phaseNumber);
    void deleteByTenantId(UUID tenantId);
}
