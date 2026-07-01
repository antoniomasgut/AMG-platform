package com.amg.digitalitzacio.prospecting.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProspectCampaignRepository extends JpaRepository<ProspectCampaign, UUID> {
    List<ProspectCampaign> findBySectorAndLocation(String sector, String location);
    List<ProspectCampaign> findByStatus(CampaignStatus status);

    @Query("SELECT c FROM ProspectCampaign c WHERE " +
           "(:sector IS NULL OR c.sector = :sector) AND " +
           "(:location IS NULL OR c.location = :location) AND " +
           "(:status IS NULL OR c.status = :status)")
    Page<ProspectCampaign> findFiltered(
            @Param("sector") String sector,
            @Param("location") String location,
            @Param("status") CampaignStatus status,
            Pageable pageable);
    @Query("SELECT c FROM ProspectCampaign c WHERE c.status = 'SCHEDULED' AND c.scheduledNextRun IS NOT NULL AND c.scheduledNextRun <= :now")
    List<ProspectCampaign> findScheduledDue(@Param("now") Instant now);

    List<ProspectCampaign> findByAutoSequenceEnabled(Boolean autoSequenceEnabled);
}
