package com.amg.digitalitzacio.telegram.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_telegram_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantTelegramConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID tenantId;

    /** Token del bot xifrat amb AES-256-GCM */
    @Column(length = 1000)
    private String botTokenEncrypted;

    private String botUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TelegramConnectionStatus status = TelegramConnectionStatus.PENDING;

    @Builder.Default
    private Boolean webhookRegistered = false;

    private Instant connectedAt;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
