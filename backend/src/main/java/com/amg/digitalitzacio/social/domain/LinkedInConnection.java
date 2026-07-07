package com.amg.digitalitzacio.social.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Connexió LinkedIn d'un tenant (Mòdul 56 F4).
 * Actiu només per al tenant propietari AMG. PK = tenant_id (una connexió per tenant).
 */
@Entity
@Table(name = "linkedin_connections")
@Getter @Setter @NoArgsConstructor
public class LinkedInConnection {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    /** URN de la persona (p.ex. "urn:li:person:xxxx") per publicar com a autor */
    @Column(name = "person_urn", nullable = false, length = 120)
    private String personUrn;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "encrypted_access_token", nullable = false, columnDefinition = "TEXT")
    private String encryptedAccessToken;

    @Column(name = "token_expires_at")
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
