package com.amg.digitalitzacio.metaads.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CampaignSpendRepository extends JpaRepository<CampaignSpend, UUID> {

    boolean existsByTenantIdAndCampaignIdAndSpendDate(UUID tenantId, String campaignId, LocalDate spendDate);

    @Query("""
        SELECT c.campaignId, c.campaignName, SUM(c.spend), SUM(c.impressions), SUM(c.clicks)
        FROM CampaignSpend c
        WHERE c.tenantId = :tenantId AND c.spendDate >= :from
        GROUP BY c.campaignId, c.campaignName
        ORDER BY SUM(c.spend) DESC
        """)
    List<Object[]> sumByCampaign(UUID tenantId, LocalDate from);
}
