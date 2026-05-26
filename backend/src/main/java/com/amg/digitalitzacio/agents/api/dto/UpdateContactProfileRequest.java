package com.amg.digitalitzacio.agents.api.dto;

public record UpdateContactProfileRequest(
    String displayName,
    String phone,
    String email
) {}
