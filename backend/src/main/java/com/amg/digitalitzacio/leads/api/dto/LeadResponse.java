package com.amg.digitalitzacio.leads.api.dto;

import com.amg.digitalitzacio.leads.domain.LeadSource;
import com.amg.digitalitzacio.leads.domain.PipelineStage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        String name,
        String email,
        String phone,
        LeadSource source,
        PipelineStage stage,
        UserRef assignedTo,
        BigDecimal estimatedValue,
        String notes,
        String tags,
        String lostReason,
        Instant convertedAt,
        Instant lastContactAt,
        Instant lastServiceAt,
        Boolean hasWhatsapp,
        String utmSource,
        String utmMedium,
        String utmCampaign,
        String metaLeadId,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public record UserRef(UUID id, String name) {}
}
