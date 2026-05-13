package com.amg.digitalitzacio.prospecting.api.dto;

import java.util.UUID;

public record CampaignRunResponse(UUID campaignId, String status, String estimatedDuration) {}
