package com.amg.digitalitzacio.metaads.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdSetRepository extends JpaRepository<AdSet, UUID> {
    List<AdSet> findByCampaignIdOrderByCreatedAtAsc(UUID campaignId);
    Optional<AdSet> findByIdAndTenantId(UUID id, UUID tenantId);
    List<AdSet> findByTenantId(UUID tenantId);
}
