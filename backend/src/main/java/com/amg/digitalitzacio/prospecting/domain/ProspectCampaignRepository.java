package com.amg.digitalitzacio.prospecting.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProspectCampaignRepository extends JpaRepository<ProspectCampaign, UUID> {
    List<ProspectCampaign> findBySectorAndLocation(String sector, String location);
    List<ProspectCampaign> findByStatus(CampaignStatus status);
}
