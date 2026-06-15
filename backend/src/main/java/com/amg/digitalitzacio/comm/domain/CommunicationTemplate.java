package com.amg.digitalitzacio.comm.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "communication_templates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommunicationTemplate {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** NULL = plantilla global del sistema; UUID = override per tenant */
    @Column
    private UUID tenantId;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String sector = "ALL";

    /** ca | es | en | de */
    @Column(nullable = false, length = 5)
    @Builder.Default
    private String language = "ca";

    /** EMAIL | WHATSAPP */
    @Column(nullable = false, length = 20)
    private String channel;

    /** DEMO_SEND | BUDGET_ACCEPTED | APPOINTMENT_CONFIRM | APPOINTMENT_REMINDER | APPOINTMENT_CANCEL */
    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 200)
    private String subject;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate  void onUpdate() { updatedAt = Instant.now(); }
}
