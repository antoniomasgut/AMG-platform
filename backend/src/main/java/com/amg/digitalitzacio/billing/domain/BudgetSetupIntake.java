package com.amg.digitalitzacio.billing.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

// Fitxa de configuració vinculada a un pressupost acceptat
@Entity
@Table(name = "budget_setup_intakes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetSetupIntake {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID budgetId;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(length = 255)
    private String tenantName;

    @Column(length = 50)
    private String sector;

    // Números de fase separats per comes: "1,2,3"
    @Column(length = 50)
    private String phaseNumbers;

    @Column(length = 64, unique = true, nullable = false)
    private String token;

    // PENDING / IN_PROGRESS / COMPLETE
    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(columnDefinition = "TEXT")
    private String intakeData;

    private Instant completedAt;
    private Instant sentAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
