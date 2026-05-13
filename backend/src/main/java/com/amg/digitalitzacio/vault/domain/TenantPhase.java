package com.amg.digitalitzacio.vault.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_phases", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "phase_id"}))
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantPhase {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "profile_id", nullable = false) private UUID profileId;
    @Column(name = "phase_id", nullable = false) private UUID phaseId;
    @Enumerated(EnumType.STRING) @Builder.Default @Column(nullable = false) private ApprovalStatus approvalStatus = ApprovalStatus.PENDING_APPROVAL;
    private Instant approvedAt;
    @Column(length = 100) private String invoiceId;
    @Column(precision = 10, scale = 2) private BigDecimal invoiceAmount;
    @Enumerated(EnumType.STRING) private InvoiceStatus invoiceStatus;
    @Enumerated(EnumType.STRING) private PaymentStatus paymentStatus;
    private Instant paidAt;
    @Enumerated(EnumType.STRING) @Builder.Default @Column(nullable = false) private ImplementationStatus implementationStatus = ImplementationStatus.NOT_STARTED;
    private Instant completedAt;
    @CreatedDate @Column(updatable = false) private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
