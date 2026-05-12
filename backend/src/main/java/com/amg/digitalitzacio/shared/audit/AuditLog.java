package com.amg.digitalitzacio.shared.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Instant timestamp;

    private UUID userId;

    @Column(length = 150)
    private String email;

    @Column(length = 45)
    private String ip;

    @Column(length = 10, nullable = false)
    private String method;

    @Column(length = 255, nullable = false)
    private String path;

    @Column(nullable = false)
    private int status;

    private long durationMs;
}
