package com.amg.digitalitzacio.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "sector_phases", indexes = {
    @Index(name = "idx_sector_phase_sector", columnList = "sector,phaseNumber"),
    @Index(name = "idx_sector_phase_unique", columnList = "sector,phaseNumber", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SectorPhase {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private BusinessSector sector;

    @Column(nullable = false)
    private Integer phaseNumber; // 1-7 segons el sector

    @Column(length = 100, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PhaseDepType dependencyType;

    // Fases requerides prèviament, comma-separated ("1,2")
    @Column(length = 50)
    private String requiredPhases;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal setupPrice;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal monthlyPrice;
}
