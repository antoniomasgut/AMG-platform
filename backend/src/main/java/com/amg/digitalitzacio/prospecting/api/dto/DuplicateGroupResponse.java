package com.amg.digitalitzacio.prospecting.api.dto;

import java.util.List;
import java.util.UUID;

public record DuplicateGroupResponse(
        String matchType,    // PHONE | PLACE_ID
        String matchValue,   // el valor compartit (telèfon o placeId)
        List<ProspectSummary> prospects
) {
    public record ProspectSummary(UUID id, UUID campaignId, String name, String phone, String email, String googlePlaceId, String status) {}
}
