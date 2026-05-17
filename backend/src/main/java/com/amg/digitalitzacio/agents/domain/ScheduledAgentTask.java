package com.amg.digitalitzacio.agents.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scheduled_agent_tasks")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduledAgentTask {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "agent_slug", nullable = false, length = 50)
    private String agentSlug;

    @Column(name = "task_type", nullable = false, length = 50)
    private String taskType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default @Column(nullable = false, length = 20)
    private ScheduledTaskStatus status = ScheduledTaskStatus.PENDING;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
