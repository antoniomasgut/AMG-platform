package com.amg.digitalitzacio.prospecting.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProspectRepository extends JpaRepository<Prospect, UUID> {
    List<Prospect> findByCampaignId(UUID campaignId);
    List<Prospect> findByCampaignIdAndStatus(UUID campaignId, ProspectStatus status);
    Optional<Prospect> findByGooglePlaceId(String googlePlaceId);
    boolean existsByGooglePlaceId(String googlePlaceId);
    boolean existsByPhone(String phone);
    long countByCampaignId(UUID campaignId);
    long countByCampaignIdAndStatus(UUID campaignId, ProspectStatus status);
}
