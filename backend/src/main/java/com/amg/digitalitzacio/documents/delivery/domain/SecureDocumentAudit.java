package com.amg.digitalitzacio.documents.delivery.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "secure_document_audit",
    indexes = {
        @Index(name = "idx_sda_token", columnList = "token_id, occurred_at DESC")
    })
@Getter @Setter
public class SecureDocumentAudit {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable = false)
    private UUID tokenId;

    @Column(nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditEventType eventType;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @PrePersist
    void prePersist() { occurredAt = Instant.now(); }
}
