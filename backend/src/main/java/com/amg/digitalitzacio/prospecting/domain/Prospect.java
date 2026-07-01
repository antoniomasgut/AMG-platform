package com.amg.digitalitzacio.prospecting.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prospects", indexes = {
    @Index(name = "idx_prospects_campaign_id", columnList = "campaign_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Prospect {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID campaignId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String sector;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 10)
    private String postalCode;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 300)
    private String website;

    @Column(length = 100)
    private String instagram;

    @Column(precision = 2, scale = 1)
    private BigDecimal googleRating;

    private Integer googleReviews;

    @Column(length = 100, unique = true)
    private String googlePlaceId;

    @Builder.Default
    private Boolean hasWebsite = false;

    @Builder.Default
    private Boolean hasInstagram = false;

    @Builder.Default
    private Boolean hasWhatsapp = false;

    // Web analysis
    private Boolean hasSsl;
    private Boolean isResponsive;
    private Boolean hasAnalytics;
    private Boolean hasPixel;
    private Boolean hasGtm;
    private Boolean hasChatWidget;
    private Boolean hasBookingSystem;
    private Boolean hasContactForm;
    private Boolean hasFaq;
    private Boolean hasClearCta;
    @Column(length = 500)
    private String techStack;
    private Integer webLoadMs;
    @Column(length = 50)
    private String cmsDetected;

    // Social networks
    @Column(length = 300)
    private String facebookUrl;
    @Column(length = 300)
    private String linkedinUrl;
    @Column(length = 300)
    private String tiktokUrl;
    @Column(length = 300)
    private String youtubeUrl;
    private Boolean hasFacebook;
    private Boolean hasLinkedin;
    private Boolean hasTiktok;

    // Scoring & classification
    @Column(columnDefinition = "TEXT")
    private String scoreBreakdown;
    @Column(length = 20)
    private String prospectTier;

    // AI analysis
    @Column(columnDefinition = "TEXT")
    private String aiReport;
    @Column(columnDefinition = "TEXT")
    private String aiPitch;
    @Column(length = 500)
    private String demoUrl;
    @Column(columnDefinition = "TEXT")
    private String widgetCode;

    private Instant webAnalyzedAt;
    private Instant aiAnalyzedAt;

    @Enumerated(EnumType.STRING)
    private ProspectStatus status;

    @Enumerated(EnumType.STRING)
    private ProspectSource source;

    @Column(length = 100)
    private String externalId;

    private UUID leadId;

    @Column(length = 500)
    private String notes;

    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String reviewsJson;

    // Tracking d'obertures (pixel email + demo)
    private Instant pitchSentAt;
    private Instant pitchOpenedAt;
    private Instant demoOpenedAt;

    @Builder.Default
    private Integer followupCount = 0;
    private Instant lastFollowupAt;

    @CreatedDate @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
