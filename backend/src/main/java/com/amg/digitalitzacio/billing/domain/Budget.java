package com.amg.digitalitzacio.billing.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "billing_budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(length = 20, nullable = false, unique = true)
    private String budgetNumber;

    @Column(nullable = true)
    private UUID profileId;

    @Column(nullable = false)
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BudgetStatus status;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal discountTotal;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal total;

    private LocalDate validUntil;

    @Column(length = 1000)
    private String notes;

    @Column(length = 1000)
    private String clientNotes;

    @Column(nullable = false)
    private UUID acceptanceToken;

    private Instant sentAt;

    private Instant acceptedAt;

    private Instant rejectedAt;

    @Column(length = 500)
    private String rejectedReason;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
