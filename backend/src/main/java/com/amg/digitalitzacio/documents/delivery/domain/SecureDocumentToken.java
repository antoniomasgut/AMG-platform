package com.amg.digitalitzacio.documents.delivery.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "secure_document_tokens",
    indexes = {
        @Index(name = "idx_sdt_tenant_created", columnList = "tenant_id, created_at DESC"),
        @Index(name = "idx_sdt_expires",        columnList = "expires_at")
    })
@Getter @Setter
public class SecureDocumentToken {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 500)
    private String storageFileId;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 100)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentSensitivity sensitivity;

    @Column(nullable = false, length = 255)
    private String recipientEmail;

    @Column(length = 30)
    private String recipientPhone;

    @Column(length = 255)
    private String recipientName;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column
    private Integer maxDownloads;

    @Column(nullable = false)
    private int downloadCount = 0;

    @Column
    private Instant firstDownloadedAt;

    @Column
    private Instant lastDownloadedAt;

    @Column
    private Instant emailSentAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column
    private Instant revokedAt;

    @Column
    private UUID createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentViewType documentType = DocumentViewType.GENERIC;

    @Column
    private UUID sourceEntityId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private SourceEntityType sourceEntityType;

    @Column
    private Instant acceptedAt;

    @Column(length = 255)
    private String signerName;

    @Column(length = 45)
    private String signerIp;

    // Mòdul 53: cobrament online opcional (Stripe del tenant)
    @Column(length = 20)
    private String paymentStatus;          // null | PENDING | PAID

    @Column(length = 120)
    private String paymentSessionId;

    @Column(precision = 10, scale = 2)
    private java.math.BigDecimal paymentAmount;

    @Column
    private Instant paymentPaidAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isDownloadsExhausted() {
        return maxDownloads != null && downloadCount >= maxDownloads;
    }

    public boolean isAccessible() {
        return !revoked && !isExpired() && !isDownloadsExhausted();
    }
}
