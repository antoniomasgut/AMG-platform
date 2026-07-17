package com.amg.digitalitzacio.agents.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FollowupLogRepository extends JpaRepository<FollowupLog, UUID> {
    List<FollowupLog> findByTenantIdOrderBySentAtDesc(UUID tenantId);

    boolean existsByTenantIdAndTypeAndEntityId(UUID tenantId, String type, UUID entityId);

    List<FollowupLog> findByTypeAndSentAtBetween(String type, Instant from, Instant to);

    void deleteByTenantId(UUID tenantId);
}
