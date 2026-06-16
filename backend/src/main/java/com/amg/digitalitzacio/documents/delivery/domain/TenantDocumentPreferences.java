package com.amg.digitalitzacio.documents.delivery.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_document_preferences")
@Getter @Setter
public class TenantDocumentPreferences {

    @Id
    private UUID tenantId;

    /** Idioma per a les plantilles: ca | es | en | de */
    @Column(nullable = false, length = 5)
    private String language = "ca";

    /** Canal per a pressupostos i factures: WHATSAPP | EMAIL | BOTH */
    @Column(nullable = false, length = 10)
    private String standardDocChannel = "WHATSAPP";

    /** Canal per a documents sensibles (mèdics, legals): EMAIL | WHATSAPP | BOTH */
    @Column(nullable = false, length = 10)
    private String sensitiveDocChannel = "EMAIL";

    /** Envia invitació de reserva automàticament en acceptar un pressupost (F3 → F2) */
    @Column(nullable = false)
    private boolean autoBookingOnAccept = true;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
