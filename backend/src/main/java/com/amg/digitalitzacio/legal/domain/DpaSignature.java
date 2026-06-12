package com.amg.digitalitzacio.legal.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dpa_signatures")
@Getter @Setter @NoArgsConstructor @Builder
@AllArgsConstructor
public class DpaSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "document_version", nullable = false, length = 10)
    @Builder.Default
    private String documentVersion = "1.0";

    @Column(name = "signer_name", length = 150)
    private String signerName;

    @Column(name = "signer_position", length = 100)
    private String signerPosition;

    @Column(name = "signer_email", length = 150)
    private String signerEmail;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }

    public boolean isSigned()   { return signedAt != null; }
    public boolean isExpired()  { return Instant.now().isAfter(expiresAt); }
}
