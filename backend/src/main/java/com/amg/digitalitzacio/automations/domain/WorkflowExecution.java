package com.amg.digitalitzacio.automations.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_executions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantWorkflowId;

    @Enumerated(EnumType.STRING)
    private TriggerType triggerType;

    @Column(length = 100)
    private String sourceId;

    @Column(columnDefinition = "TEXT")
    private String requestPayload;

    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    @Column(length = 50)
    private String n8nExecutionId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Column(length = 500)
    private String errorMessage;

    @CreatedDate
    @Column(updatable = false)
    private Instant executedAt;

    private Instant completedAt;
}
