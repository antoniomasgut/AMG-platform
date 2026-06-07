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

    @CreatedDate @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
