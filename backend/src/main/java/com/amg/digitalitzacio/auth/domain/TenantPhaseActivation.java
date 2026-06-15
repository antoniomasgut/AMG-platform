package com.amg.digitalitzacio.auth.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_phase_activations")
@Data
public class TenantPhaseActivation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 10)
    private String phase;

    @Column(nullable = false, length = 50)
    private String source;

    private UUID budgetId;

    private String activatedBy;

    @Column(nullable = false)
    private Instant activatedAt;

    private Instant deactivatedAt;

    private String deactivatedBy;
}
