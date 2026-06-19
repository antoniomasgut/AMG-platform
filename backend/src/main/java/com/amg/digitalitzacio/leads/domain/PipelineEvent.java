package com.amg.digitalitzacio.leads.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pipeline_events", indexes = {
    @Index(name = "idx_pipeline_events_lead", columnList = "lead_id"),
    @Index(name = "idx_pipeline_events_created", columnList = "created_at")
})
@Getter
@Setter
public class PipelineEvent {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "lead_id", nullable = false)
    private UUID leadId;

    @Column(name = "from_stage", length = 30)
    private String fromStage;

    @Column(name = "to_stage", nullable = false, length = 30)
    private String toStage;

    @Column(name = "triggered_by", nullable = false, length = 50)
    private String triggeredBy;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
