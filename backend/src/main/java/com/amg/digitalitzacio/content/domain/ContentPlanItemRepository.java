package com.amg.digitalitzacio.content.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentPlanItemRepository extends JpaRepository<ContentPlanItem, UUID> {

    List<ContentPlanItem> findByPlanIdOrderByWeekNumberAsc(UUID planId);

    List<ContentPlanItem> findByTenantIdAndStatus(UUID tenantId, ContentItemStatus status);

    List<ContentPlanItem> findByStatus(ContentItemStatus status);

    // Recordatoris: items amb foto demanada, sense foto i sense recordatori enviat
    List<ContentPlanItem> findByStatusAndMediaUrlIsNullAndReminderSentAtIsNull(ContentItemStatus status);

    void deleteByPlanId(UUID planId);
}
