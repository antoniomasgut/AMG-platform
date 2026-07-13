package com.amg.digitalitzacio.content.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Una publicació planificada dins un pla (Spec 58 §3.2).
 * UNIQUE(plan_id, week_number).
 */
@Entity
@Table(name = "content_plan_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "week_number"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    /** Desnormalitzat per a queries d'aïllament i de scheduler. */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentPillar pillar;

    @Column(name = "brief_text", columnDefinition = "TEXT")
    private String briefText;

    @Column(name = "example_text", columnDefinition = "TEXT")
    private String exampleText;

    /** CSV: INSTAGRAM,FACEBOOK,GOOGLE_BUSINESS. */
    @Column(length = 100)
    private String networks;

    /** Opcional; sobreescriu l'idioma del pla per a aquesta publicació. */
    @Column(name = "content_language", length = 5)
    private String contentLanguage;

    @Column(name = "photo_deadline")
    private LocalDate photoDeadline;

    @Column(name = "target_publish_date")
    private LocalDate targetPublishDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ContentItemStatus status = ContentItemStatus.PLANNED;

    @Column(name = "media_url", columnDefinition = "TEXT")
    private String mediaUrl;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Column(name = "brief_sent_at")
    private Instant briefSentAt;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

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
