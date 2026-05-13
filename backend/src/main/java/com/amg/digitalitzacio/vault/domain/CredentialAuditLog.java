package com.amg.digitalitzacio.vault.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credential_audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CredentialAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "credential_id", nullable = false) private UUID credentialId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AuditAction action;
    @Column(length = 100) private String maskedValue;
    @Column(nullable = false) private Instant createdAt;
}
