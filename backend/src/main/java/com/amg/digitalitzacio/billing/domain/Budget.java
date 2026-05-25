package com.amg.digitalitzacio.billing.domain;

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
@Table(name = "budgets", indexes = {
    @Index(name = "idx_budget_tenant_status", columnList = "tenantId,status"),
    @Index(name = "idx_budget_tenant_number", columnList = "tenantId,budgetNumber")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Budget {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID tenantId;
    @Column(length = 20) private String budgetNumber;
    private UUID profileId;
    @Builder.Default private Integer version = 1;
    @Enumerated(EnumType.STRING) @Builder.Default @Column(nullable = false) private BudgetStatus status = BudgetStatus.DRAFT;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal subtotal;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal discountTotal;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal total;
    private LocalDate validUntil;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(columnDefinition = "TEXT") private String clientNotes;
    @Column(columnDefinition = "TEXT") private String recommendation;
    @Column(columnDefinition = "TEXT") private String recommendedPhaseIds;
    @Column(length = 64) private String acceptanceToken;
    private Instant sentAt;
    private Instant acceptedAt;
    private Instant rejectedAt;
    @Column(length = 255) private String rejectedReason;
    @CreatedDate @Column(updatable = false) private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
