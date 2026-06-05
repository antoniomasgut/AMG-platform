package com.amg.digitalitzacio.metaads.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ad_campaigns")
@Getter
@Setter
public class AdCampaign {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(length = 50)
    private String metaCampaignId;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CampaignObjective objective;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(precision = 10, scale = 2)
    private BigDecimal dailyBudget;

    @Column(precision = 10, scale = 2)
    private BigDecimal lifetimeBudget;

    private Instant startTime;
    private Instant stopTime;

    @Column(columnDefinition = "TEXT")
    private String metaError;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }
}
