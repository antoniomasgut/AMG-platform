package com.amg.digitalitzacio.engine.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LandingResponse(
    UUID id,
    String title,
    String slug,
    String status,
    String landingType,
    String publicUrl,
    String customDomain,
    Boolean domainVerified,
    Boolean managedDomain,
    String domainStatus,
    String domainRegistrar,
    String domainOwnerName,
    String domainOwnerEmail,
    String domainOwnerPhone,
    String metaDescription,
    String ogImageUrl,
    List<VersionResponse> versions,
    String previewToken,
    Instant createdAt,
    Instant updatedAt
) {}
