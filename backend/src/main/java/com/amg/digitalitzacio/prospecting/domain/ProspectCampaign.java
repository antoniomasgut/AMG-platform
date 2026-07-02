package com.amg.digitalitzacio.prospecting.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prospect_campaigns")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProspectCampaign {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String sector;

    @Column(nullable = false, length = 100)
    private String location;

    @Enumerated(EnumType.STRING)
    private ProspectSource source;

    @Enumerated(EnumType.STRING)
    private CampaignStatus status;

    @Builder.Default
    private Integer totalFound = 0;

    @Builder.Default
    private Integer totalExported = 0;

    @Column(columnDefinition = "TEXT")
    private String searchParams;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private UUID createdBy;

    private Instant scheduledNextRun;

    private Integer repeatIntervalDays;

    @Builder.Default
    private Integer autoDemoThreshold = 61;

    @Column(length = 20)
    private String autoContactChannel;

    @Builder.Default
    private Integer totalDemosGenerated = 0;

    @Builder.Default
    private Integer totalDiscarded = 0;

    // Enviament automàtic de pitch per email al generar l'informe IA
    @Builder.Default
    private Boolean autoSendEmail = false;

    // Seqüència automàtica de follow-up
    @Builder.Default
    private Boolean autoSequenceEnabled = false;

    @Builder.Default
    private Integer followupDays1 = 3;

    @Builder.Default
    private Integer followupDays2 = 7;

    @Builder.Default
    private Integer followupDays3 = 14;

    @CreatedDate @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
