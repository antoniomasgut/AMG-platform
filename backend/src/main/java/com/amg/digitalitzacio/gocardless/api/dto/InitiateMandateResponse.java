package com.amg.digitalitzacio.gocardless.api.dto;

public record InitiateMandateResponse(
        String redirectFlowId,
        String redirectUrl
) {}
