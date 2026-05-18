package com.amg.digitalitzacio.agents.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations", indexes = {
    @Index(columnList = "tenant_id,customer_identifier,channel"),
    @Index(columnList = "tenant_id,pending_approval")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Conversation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_identifier", length = 100, nullable = false)
    private String customerIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private ConversationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ConversationRole role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder.Default
    @Column(name = "pending_approval", nullable = false)
    private Boolean pendingApproval = false;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
