package com.amg.digitalitzacio.prospecting.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProspectCampaignRepository extends JpaRepository<ProspectCampaign, UUID> {
    List<ProspectCampaign> findBySectorAndLocation(String sector, String location);
    List<ProspectCampaign> findByStatus(CampaignStatus status);
    @Query("SELECT c FROM ProspectCampaign c WHERE c.status = 'SCHEDULED' AND c.scheduledNextRun IS NOT NULL AND c.scheduledNextRun <= :now")
    List<ProspectCampaign> findScheduledDue(@Param("now") Instant now);
}
