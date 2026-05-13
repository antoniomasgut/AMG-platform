package com.amg.digitalitzacio.automations.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_workflows", indexes = {
    @Index(name = "idx_tenant_status", columnList = "tenantId,status"),
    @Index(name = "idx_tenant_template_active", columnList = "tenantId,templateId,isActive")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantWorkflow {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID templateId;

    @Column(length = 50)
    private String n8nWorkflowId;

    @Column(length = 500)
    private String n8nWebhookUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TenantWorkflowStatus status = TenantWorkflowStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String config;

    private Instant lastRunAt;

    @Enumerated(EnumType.STRING)
    private ExecutionStatus lastRunStatus;

    @Column(length = 500)
    private String errorMessage;

    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
