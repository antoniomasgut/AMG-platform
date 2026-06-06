package com.amg.digitalitzacio.google.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "google_connections", indexes = {
    @Index(name = "idx_google_connections_tenant", columnList = "tenant_id", unique = true),
    @Index(name = "idx_google_connections_active", columnList = "is_active")
})
@Getter @Setter @NoArgsConstructor
public class GoogleConnection {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "google_account_email", nullable = false)
    private String googleAccountEmail;

    @Column(name = "google_user_id", nullable = false)
    private String googleUserId;

    @Column(name = "encrypted_access_token", nullable = false, columnDefinition = "TEXT")
    private String encryptedAccessToken;

    @Column(name = "encrypted_refresh_token", nullable = false, columnDefinition = "TEXT")
    private String encryptedRefreshToken;

    @Column(name = "token_expires_at", nullable = false)
    private Instant tokenExpiresAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
