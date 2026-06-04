package com.amg.digitalitzacio.agents.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ScheduledAgentTaskRepository extends JpaRepository<ScheduledAgentTask, UUID> {
    List<ScheduledAgentTask> findByStatusAndScheduledAtBeforeOrderByScheduledAtAsc(
            ScheduledTaskStatus status, Instant before);

    List<ScheduledAgentTask> findByTenantIdAndAgentSlugAndStatusAndScheduledAtBetween(
            UUID tenantId, String agentSlug, ScheduledTaskStatus status,
            Instant from, Instant to);
}
