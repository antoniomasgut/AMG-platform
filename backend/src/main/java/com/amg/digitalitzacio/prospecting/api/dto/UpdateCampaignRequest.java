package com.amg.digitalitzacio.prospecting.api.dto;

import com.amg.digitalitzacio.prospecting.domain.ProspectSource;

public record UpdateCampaignRequest(
        String name, String sector, String location,
        ProspectSource source, String searchParams, String notes
) {}
