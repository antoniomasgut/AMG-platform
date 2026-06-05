package com.amg.digitalitzacio.metaads.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdCampaignRepository extends JpaRepository<AdCampaign, UUID> {
    List<AdCampaign> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<AdCampaign> findByIdAndTenantId(UUID id, UUID tenantId);
    List<AdCampaign> findByStatusIn(List<CampaignStatus> statuses);
}
