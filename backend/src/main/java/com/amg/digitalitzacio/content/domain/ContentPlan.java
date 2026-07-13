package com.amg.digitalitzacio.content.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Pla de contingut mensual d'un tenant (Spec 58 §3.1).
 * UNIQUE(tenant_id, period): un pla per tenant i mes.
 */
@Entity
@Table(name = "content_plans",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "period"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Mes del pla en format YYYY-MM. */
    @Column(nullable = false, length = 7)
    private String period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ContentPlanStatus status = ContentPlanStatus.DRAFT;

    /** Idioma de les publicacions (ca/es/en/de); hereta el default del tenant en crear-se. */
    @Column(name = "content_language", length = 5)
    @Builder.Default
    private String contentLanguage = "ca";

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
