package com.amg.digitalitzacio.documents.delivery.api.dto;

import java.time.Instant;

public record DocumentViewResponse(
    String fileName,
    String description,
    String documentType,
    String htmlContent,
    boolean canAccept,
    boolean accepted,
    Instant acceptedAt,
    String signerName
) {}
