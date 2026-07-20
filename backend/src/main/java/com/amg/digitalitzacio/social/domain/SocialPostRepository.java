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

    // Mòdul 55 feature 2: posts publicats en un rang (per al resum setmanal i sync de mètriques)
    @Query("SELECT p FROM SocialPost p WHERE p.tenantId = :tenantId AND p.status = 'PUBLISHED' "
            + "AND p.publishedAt >= :since ORDER BY p.publishedAt DESC")
    List<SocialPost> findPublishedSince(UUID tenantId, Instant since);

    // P23: posts encallats en PUBLISHING (crash del servidor enmig d'una publicació)
    // Si scheduledAt <= (ara - 5 min) i segueix en PUBLISHING → definitivamente encallat
    @Query("SELECT p FROM SocialPost p WHERE p.status = 'PUBLISHING' AND p.scheduledAt <= :before")
    List<SocialPost> findStuckPublishing(Instant before);

    // P34: pròxims posts programats per tenant (per al /posts de Telegram)
    @Query("SELECT p FROM SocialPost p WHERE p.tenantId = :tenantId AND p.status = 'SCHEDULED' "
            + "AND p.scheduledAt > :after ORDER BY p.scheduledAt ASC")
    List<SocialPost> findUpcomingScheduled(UUID tenantId, Instant after);
}
