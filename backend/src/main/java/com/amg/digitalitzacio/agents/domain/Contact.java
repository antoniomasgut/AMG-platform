package com.amg.digitalitzacio.agents.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contacts", indexes = {
    @Index(name = "idx_contacts_tenant_id", columnList = "tenant_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "conversation_summary", columnDefinition = "TEXT")
    private String conversationSummary;

    @Builder.Default
    @Column(name = "total_message_count", nullable = false)
    private Integer totalMessageCount = 0;

    @Column(name = "summary_updated_at")
    private Instant summaryUpdatedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
