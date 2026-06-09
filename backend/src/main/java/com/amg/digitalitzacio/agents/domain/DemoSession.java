package com.amg.digitalitzacio.agents.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "demo_sessions")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DemoSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "token", nullable = false, unique = true)
    private UUID token;

    @Column(name = "prospect_email", nullable = false)
    private String prospectEmail;

    @Column(name = "company_name", length = 150)
    private String companyName;

    @Column(name = "agent_context", columnDefinition = "TEXT")
    private String agentContext;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "block_reason", length = 255)
    private String blockReason;

    @Column(name = "sector", length = 50)
    private String sector;

    @Column(name = "landing_slug", length = 100)
    private String landingSlug;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** ca | es | en | de — idioma de la landing i del xat */
    @Column(name = "locale", length = 5, nullable = false)
    @Builder.Default
    private String locale = "ca";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
