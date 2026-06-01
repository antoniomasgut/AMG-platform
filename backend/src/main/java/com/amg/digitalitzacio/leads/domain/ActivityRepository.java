package com.amg.digitalitzacio.leads.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    Page<Activity> findByLeadIdOrderByCreatedAtDesc(UUID leadId, Pageable pageable);

    List<Activity> findByLeadIdOrderByCreatedAtDesc(UUID leadId);

    void deleteByLeadId(UUID leadId);
}
