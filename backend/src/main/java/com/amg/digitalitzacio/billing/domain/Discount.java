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
@Table(name = "billing_discounts")
@EntityListeners(AuditingEntityListener.class)
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType type;

    @Column(name = "discount_value", precision = 10, scale = 2, nullable = false)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppliesTo appliesTo;

    @Column
    private UUID referenceId;

    @Column(length = 255)
    private String label;

    @Column(nullable = false)
    private Boolean isActive;

    @Column
    private LocalDate validFrom;

    @Column
    private LocalDate validUntil;

    @Column
    private Integer maxApplications;

    @Column(nullable = false)
    private Integer appliedCount;

    @Column(nullable = false)
    private UUID createdBy;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
