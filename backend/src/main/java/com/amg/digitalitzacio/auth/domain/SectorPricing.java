package com.amg.digitalitzacio.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sector_pricing",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sector", "business_size"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectorPricing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BusinessSector sector;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_size", nullable = false, length = 20)
    private BusinessSize businessSize;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal setupPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyF1;

    @Column(name = "monthly_f1f2", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyF1f2;

    @Column(name = "monthly_f1f2f3", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyF1f2f3;

    @Column(name = "monthly_complete", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyComplete;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
