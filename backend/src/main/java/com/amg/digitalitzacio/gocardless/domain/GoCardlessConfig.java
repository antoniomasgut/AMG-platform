package com.amg.digitalitzacio.gocardless.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gocardless_configs")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GoCardlessConfig {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "api_key_ref", length = 100)
    private String apiKeyRef;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GoCardlessEnvironment environment = GoCardlessEnvironment.SANDBOX;

    @Column(length = 50)
    private String creditorId;

    @Column(length = 100)
    private String webhookSecret;

    @Builder.Default
    private Boolean isActive = false;

    @CreatedDate @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
