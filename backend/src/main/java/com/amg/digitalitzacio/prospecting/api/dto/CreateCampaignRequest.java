package com.amg.digitalitzacio.prospecting.api.dto;

import com.amg.digitalitzacio.prospecting.domain.ProspectSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateCampaignRequest(
        @NotBlank String name,
        @NotBlank String sector,
        @NotBlank String location,
        @NotNull ProspectSource source,
        String searchParams,
        String notes,
        Boolean scheduled,
        Instant scheduledNextRun,
        Integer repeatIntervalDays,
        Boolean autoSendEmail
) {}
