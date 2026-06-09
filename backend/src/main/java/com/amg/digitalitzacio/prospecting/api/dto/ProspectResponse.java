package com.amg.digitalitzacio.prospecting.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProspectResponse(
        UUID id, UUID campaignId, String name, String description,
        String sector, String address, String city, String postalCode,
        String phone, String email, String website, String instagram,
        BigDecimal googleRating, Integer googleReviews, String googlePlaceId,
        Boolean hasWebsite, Boolean hasInstagram, Boolean hasWhatsapp,
        String status, String source, String externalId, UUID leadId,
        String notes, Instant createdAt, Instant updatedAt, Integer score,
        List<String> reviews, List<ProspectSignal> signals
) {}
