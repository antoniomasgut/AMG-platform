package com.amg.digitalitzacio.vault.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TenantActivatedPhaseAgentRepository extends JpaRepository<TenantActivatedPhaseAgent, UUID> {
    List<TenantActivatedPhaseAgent> findByPhaseId(UUID phaseId);
    void deleteByPhaseId(UUID phaseId);
    void deleteByTenantId(UUID tenantId);
}
