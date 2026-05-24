package com.amg.digitalitzacio.whatsapp.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_waba_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppWabaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID tenantId;

    private String wabaId;

    @Column(unique = true)
    private String phoneNumberId;

    /** Token d'accés xifrat amb AES-256-GCM */
    @Column(length = 1000)
    private String accessTokenEncrypted;

    private String displayPhoneNumber;

    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WhatsAppConnectionStatus status = WhatsAppConnectionStatus.PENDING;

    @Builder.Default
    private Boolean webhookRegistered = false;

    private Instant connectedAt;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
