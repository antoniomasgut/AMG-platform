package com.amg.digitalitzacio.content.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentPlanItemRepository extends JpaRepository<ContentPlanItem, UUID> {

    List<ContentPlanItem> findByPlanIdOrderByWeekNumberAsc(UUID planId);

    List<ContentPlanItem> findByTenantIdAndStatus(UUID tenantId, ContentItemStatus status);

    void deleteByPlanId(UUID planId);
}
