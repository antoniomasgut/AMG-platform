package com.amg.digitalitzacio.prospecting.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    // Deduplicació per campanya (CSV import)
    boolean existsByCampaignIdAndPhone(UUID campaignId, String phone);
    boolean existsByCampaignIdAndEmail(UUID campaignId, String email);

    // Deduplicació cross-campanya per googlePlaceId
    @Query("SELECT p FROM Prospect p WHERE p.googlePlaceId IS NOT NULL AND p.googlePlaceId IN " +
           "(SELECT p2.googlePlaceId FROM Prospect p2 WHERE p2.googlePlaceId IS NOT NULL GROUP BY p2.googlePlaceId HAVING COUNT(p2) > 1) " +
           "ORDER BY p.googlePlaceId, p.createdAt")
    List<Prospect> findDuplicatesByGooglePlaceId();

    // Deduplicació cross-campanya per telèfon
    @Query("SELECT p FROM Prospect p WHERE p.phone IS NOT NULL AND p.phone IN " +
           "(SELECT p2.phone FROM Prospect p2 WHERE p2.phone IS NOT NULL GROUP BY p2.phone HAVING COUNT(p2) > 1) " +
           "ORDER BY p.phone, p.createdAt")
    List<Prospect> findDuplicatesByPhone();

    // Prospects CONTACTED amb pitch enviat per al scheduler de follow-up
    @Query("SELECT p FROM Prospect p WHERE p.status = 'CONTACTED' AND p.pitchSentAt IS NOT NULL " +
           "AND (p.followupCount IS NULL OR p.followupCount < 3) " +
           "AND (p.lastFollowupAt IS NULL AND p.pitchSentAt < :cutoff " +
           "     OR p.lastFollowupAt IS NOT NULL AND p.lastFollowupAt < :cutoff)")
    List<Prospect> findContactedWithPendingFollowup(@Param("cutoff") Instant cutoff);
}
