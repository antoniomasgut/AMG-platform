package com.amg.digitalitzacio.prospecting.api.dto;

import com.amg.digitalitzacio.prospecting.domain.ProspectStatus;

public record UpdateProspectRequest(
    ProspectStatus status,
    String notes,
    String name,
    String phone,
    String email,
    String website,
    String address,
    String city
) {}
