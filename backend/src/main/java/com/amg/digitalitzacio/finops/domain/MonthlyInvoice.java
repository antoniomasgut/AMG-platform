package com.amg.digitalitzacio.finops.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "monthly_invoices", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "period"}))
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MonthlyInvoice {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(nullable = false, length = 7) private String period;  // ex: "2026-05"
    @Column(unique = true, length = 50) private String holdedInvoiceId;
    @Column(length = 20) private String invoiceNumber;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Builder.Default @Column(nullable = false) private InvoiceStatus status = InvoiceStatus.PENDING;
    private LocalDate sepaCollectionDate;
    @Builder.Default @Column(nullable = false) private Boolean sepaCollected = false;
    @Column(length = 500) private String invoicePdfUrl;
    @CreatedDate @Column(updatable = false) private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
