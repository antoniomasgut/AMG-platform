package com.amg.digitalitzacio.social.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SocialPostRepository extends JpaRepository<SocialPost, UUID> {

    List<SocialPost> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    @Query("SELECT p FROM SocialPost p WHERE p.status = 'SCHEDULED' AND p.scheduledAt <= :now")
    List<SocialPost> findDueScheduled(Instant now);

    List<SocialPost> findByTenantIdAndNetworkOrderByCreatedAtDesc(UUID tenantId, String network);
}
