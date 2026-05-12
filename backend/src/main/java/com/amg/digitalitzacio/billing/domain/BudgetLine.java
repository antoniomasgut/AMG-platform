package com.amg.digitalitzacio.billing.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "billing_budget_lines")
@EntityListeners(AuditingEntityListener.class)
public class BudgetLine {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID budgetId;

    @Column(nullable = false)
    private UUID phaseId;

    @Column(nullable = false)
    private UUID serviceId;

    @Column(length = 100, nullable = false)
    private String serviceName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal total;

    private Integer sortOrder;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;
}
