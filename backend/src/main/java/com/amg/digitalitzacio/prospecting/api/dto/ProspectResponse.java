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
        // web analysis
        Boolean hasSsl, Boolean isResponsive, Boolean hasAnalytics, Boolean hasPixel,
        Boolean hasGtm, Boolean hasChatWidget, Boolean hasBookingSystem,
        Boolean hasContactForm, Boolean hasFaq, Boolean hasClearCta,
        String techStack, Integer webLoadMs, String cmsDetected,
        // social
        String facebookUrl, String linkedinUrl, String tiktokUrl, String youtubeUrl,
        Boolean hasFacebook, Boolean hasLinkedin, Boolean hasTiktok,
        // scoring
        Integer score, String prospectTier, String scoreBreakdown,
        // ai
        String aiReport, String aiPitch, String demoUrl,
        // meta
        String status, String source, String externalId, UUID leadId,
        String notes, Instant createdAt, Instant updatedAt,
        Instant webAnalyzedAt, Instant aiAnalyzedAt,
        List<String> reviews, List<ProspectSignal> signals
) {}
