package com.amg.digitalitzacio.ops.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Incident {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String serviceName;

    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    private IncidentStatus status;

    @Column(length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    private Instant startedAt;

    private Instant resolvedAt;

    private Long durationSeconds;

    @Builder.Default
    private boolean alertSent = false;

    @Builder.Default
    private boolean alertRecovered = false;

    @CreatedDate @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
