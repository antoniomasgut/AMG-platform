package com.amg.digitalitzacio.content.api.dto;

import com.amg.digitalitzacio.content.domain.ContentPlan;
import com.amg.digitalitzacio.content.domain.ContentPlanItem;

import java.util.List;
import java.util.UUID;

public record ContentPlanResponse(
        UUID id,
        UUID tenantId,
        String period,
        String status,
        String contentLanguage,
        String notes,
        List<ContentPlanItemResponse> items
) {
    public static ContentPlanResponse from(ContentPlan p, List<ContentPlanItem> items) {
        return new ContentPlanResponse(
                p.getId(), p.getTenantId(), p.getPeriod(),
                p.getStatus() != null ? p.getStatus().name() : null,
                p.getContentLanguage(), p.getNotes(),
                items.stream().map(ContentPlanItemResponse::from).toList());
    }
}
