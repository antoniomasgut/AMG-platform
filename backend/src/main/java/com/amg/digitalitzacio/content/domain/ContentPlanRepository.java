package com.amg.digitalitzacio.content.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentPlanRepository extends JpaRepository<ContentPlan, UUID> {

    List<ContentPlan> findByTenantIdOrderByPeriodDesc(UUID tenantId);

    Optional<ContentPlan> findByTenantIdAndPeriod(UUID tenantId, String period);

    List<ContentPlan> findByTenantIdAndStatus(UUID tenantId, ContentPlanStatus status);

    List<ContentPlan> findByStatus(ContentPlanStatus status);
}
