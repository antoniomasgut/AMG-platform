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
@Table(name = "leads")
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

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    private LeadSource source;

    @Enumerated(EnumType.STRING)
    private PipelineStage stage;

    private UUID assignedTo;

    @Column(precision = 12, scale = 2)
    private BigDecimal estimatedValue;

    @Lob
    private String notes;

    @Column(length = 500)
    private String tags;

    @Column(length = 255)
    private String lostReason;

    private Instant convertedAt;

    private Boolean hasWhatsapp;

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
