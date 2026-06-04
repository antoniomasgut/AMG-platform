package com.amg.digitalitzacio.agents.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "absence_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AbsenceRecord {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "absence_date", nullable = false)
    private LocalDate absenceDate;

    @Column(name = "triggered_by")
    private Long triggeredBy;

    @Column(name = "affected_count", nullable = false)
    private int affectedCount;

    @Column(name = "notified_count", nullable = false)
    private int notifiedCount;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
