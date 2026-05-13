package com.amg.digitalitzacio.automations.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_templates")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "template_key", unique = true, nullable = false, length = 50)
    private String key;

    @Column(length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    private WorkflowCategory category;

    @Enumerated(EnumType.STRING)
    private WorkflowActivationType activationType;

    @Column(columnDefinition = "TEXT")
    private String n8nWorkflowJson;

    @Column(columnDefinition = "TEXT")
    private String setupGuide;

    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;
}
