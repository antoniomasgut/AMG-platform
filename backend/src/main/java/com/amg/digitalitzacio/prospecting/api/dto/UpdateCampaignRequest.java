package com.amg.digitalitzacio.prospecting.api.dto;

import com.amg.digitalitzacio.prospecting.domain.ProspectSource;

import java.time.Instant;

public record UpdateCampaignRequest(
        String name, String sector, String location,
        ProspectSource source, String searchParams, String notes,
        Boolean scheduled, Instant scheduledNextRun, Integer repeatIntervalDays,
        Boolean autoSendEmail
) {}
