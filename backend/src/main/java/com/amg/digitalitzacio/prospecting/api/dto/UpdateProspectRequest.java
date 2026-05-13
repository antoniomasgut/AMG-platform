package com.amg.digitalitzacio.prospecting.api.dto;

import com.amg.digitalitzacio.prospecting.domain.ProspectStatus;

public record UpdateProspectRequest(ProspectStatus status, String notes) {}
