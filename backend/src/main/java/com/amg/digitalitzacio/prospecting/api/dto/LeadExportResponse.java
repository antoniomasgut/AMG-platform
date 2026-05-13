package com.amg.digitalitzacio.prospecting.api.dto;

import java.util.UUID;

public record LeadExportResponse(UUID prospectId, UUID leadId, String leadUrl, String status) {}
