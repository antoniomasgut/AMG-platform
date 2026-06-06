package com.amg.digitalitzacio.google.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "oauth_states", indexes = {
    @Index(name = "idx_oauth_states_token", columnList = "state_token")
})
@Getter @Setter @NoArgsConstructor
public class OAuthState {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "state_token", nullable = false, unique = true, length = 64)
    private String stateToken;

    @Column(name = "redirect_uri", nullable = false, length = 500)
    private String redirectUri;

    @Column(name = "requested_scopes", nullable = false, columnDefinition = "TEXT")
    private String requestedScopes;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
