package com.amg.digitalitzacio.metaads.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AdResponse(
    UUID id,
    UUID adSetId,
    String metaAdId,
    String name,
    String status,
    UUID creativeId,
    String headline,
    String body,
    String callToAction,
    String linkUrl,
    String metaImageHash,
    String metaError,
    Instant createdAt
) {}
