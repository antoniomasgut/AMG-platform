package com.amg.digitalitzacio.visits.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "visit_records", indexes = {
    @Index(columnList = "tenant_id,contact_identifier"),
    @Index(columnList = "tenant_id,visit_date")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VisitRecord {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    // Identificador del contacte (telèfon, email o wgt:sessionId)
    @Column(name = "contact_identifier", length = 100, nullable = false)
    private String contactIdentifier;

    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "treatment_type", length = 100)
    private String treatmentType;

    @Column(name = "notes", length = 1000)
    private String notes;

    // Data de la propera revisió recomanada
    @Column(name = "next_visit_due")
    private LocalDate nextVisitDue;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
