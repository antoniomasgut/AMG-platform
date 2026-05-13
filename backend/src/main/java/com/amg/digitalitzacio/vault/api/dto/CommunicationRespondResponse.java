package com.amg.digitalitzacio.vault.api.dto;

public record CommunicationRespondResponse(
        boolean processed,
        String action,
        String serviceStatus
) {}
