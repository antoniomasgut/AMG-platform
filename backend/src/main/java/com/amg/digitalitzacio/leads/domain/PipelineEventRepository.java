package com.amg.digitalitzacio.leads.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PipelineEventRepository extends JpaRepository<PipelineEvent, UUID> {
    List<PipelineEvent> findByLeadIdOrderByCreatedAtDesc(UUID leadId);
}
