package com.amg.digitalitzacio.shared.sysconfig.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "system_config_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfigAuditLog {

    @Id
    private UUID id;

    @Column(name = "config_key", nullable = false, length = 80)
    private String configKey;

    @Column(name = "action", nullable = false, length = 10)
    private String action;

    @Column(name = "previous_value", columnDefinition = "TEXT")
    private String previousValue;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_email", length = 150)
    private String userEmail;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (changedAt == null) changedAt = Instant.now();
    }
}
