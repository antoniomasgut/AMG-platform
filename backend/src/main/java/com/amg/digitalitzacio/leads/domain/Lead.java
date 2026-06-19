package com.amg.digitalitzacio.leads.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "leads", indexes = {
    @Index(name = "idx_leads_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_leads_tenant_stage", columnList = "tenant_id, stage")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Lead {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    private LeadSource source;

    @Enumerated(EnumType.STRING)
    private PipelineStage stage;

    private UUID assignedTo;

    @Column(precision = 12, scale = 2)
    private BigDecimal estimatedValue;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = 500)
    private String tags;

    @Column(name = "utm_source", length = 100)
    private String utmSource;

    @Column(name = "utm_medium", length = 100)
    private String utmMedium;

    @Column(name = "utm_campaign", length = 150)
    private String utmCampaign;

    @Column(name = "meta_lead_id", length = 50)
    private String metaLeadId;

    @Column(length = 255)
    private String lostReason;

    private Instant convertedAt;

    private UUID convertedTenantId;

    private Instant lastContactAt;

    private Instant lastServiceAt;

    private Boolean hasWhatsapp;

    @Column(columnDefinition = "TEXT") private String interviewNotes;
    @Column(name = "web_need", length = 30) private String webNeed;
    @Column(name = "interview_sector", length = 50) private String interviewSector;
    @Column(name = "interview_business_size", length = 30) private String interviewBusinessSize;

    @Column(name = "pipeline_stage", length = 30)
    private String pipelineStage;

    @Column(name = "sla_deadline")
    private Instant slaDeadline;

    @Column(name = "sla_alerted")
    private Boolean slaAlerted = false;

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (stage == null) stage = PipelineStage.NEW;
        if (isActive == null) isActive = true;
    }
}
