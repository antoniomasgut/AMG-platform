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

    // Preu de cada fase individual. El mensual total = suma de priceF{n} per cada fase contractada.
    @Column(name = "price_f1", columnDefinition = "numeric(10,2) DEFAULT 0")
    private BigDecimal priceF1;

    @Column(name = "price_f2", columnDefinition = "numeric(10,2) DEFAULT 0")
    private BigDecimal priceF2;

    @Column(name = "price_f3", columnDefinition = "numeric(10,2) DEFAULT 0")
    private BigDecimal priceF3;

    @Column(name = "price_f4", columnDefinition = "numeric(10,2) DEFAULT 0")
    private BigDecimal priceF4;

    @Column(name = "price_f5", columnDefinition = "numeric(10,2) DEFAULT 0")
    private BigDecimal priceF5;

    public BigDecimal totalMonthly(java.util.Collection<String> phases) {
        if (phases == null || phases.isEmpty()) return BigDecimal.ZERO;
        var sorted = phases.stream()
                .filter(p -> p != null && p.matches("F[1-5]"))
                .sorted()
                .toList();
        BigDecimal total = BigDecimal.ZERO;
        java.math.BigDecimal[] tierPrices = { priceF1, priceF2, priceF3, priceF4, priceF5 };
        for (int i = 0; i < sorted.size() && i < tierPrices.length; i++) {
            if (tierPrices[i] != null) total = total.add(tierPrices[i]);
        }
        return total;
    }

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
