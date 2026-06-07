package com.amg.digitalitzacio.prospecting.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ProspectRepository extends JpaRepository<Prospect, UUID> {
    List<Prospect> findByCampaignId(UUID campaignId);
    List<Prospect> findByCampaignIdAndStatus(UUID campaignId, ProspectStatus status);
    Optional<Prospect> findByGooglePlaceId(String googlePlaceId);
    boolean existsByGooglePlaceId(String googlePlaceId);
    boolean existsByPhone(String phone);
    long countByCampaignId(UUID campaignId);
    long countByCampaignIdAndStatus(UUID campaignId, ProspectStatus status);

    @Query("SELECT p.googlePlaceId FROM Prospect p WHERE p.campaignId = :campaignId AND p.googlePlaceId IN :ids")
    Set<String> findExistingPlaceIds(@Param("campaignId") UUID campaignId, @Param("ids") Collection<String> ids);

    @Query("SELECT p.phone FROM Prospect p WHERE p.campaignId = :campaignId AND p.phone IN :phones")
    Set<String> findExistingPhones(@Param("campaignId") UUID campaignId, @Param("phones") Collection<String> phones);
}
