package com.amg.digitalitzacio.billing.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "budget_lines")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetLine {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID budgetId;
    private UUID phaseId;
    @Column(nullable = false) private UUID serviceId;
    @Column(length = 100) private String serviceName;
    @Builder.Default private Integer quantity = 1;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal total;
    private Integer sortOrder;
}
