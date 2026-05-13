package com.amg.digitalitzacio.billing.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "discounts")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Discount {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    private UUID tenantId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private DiscountType type;
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2) private BigDecimal value;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private DiscountAppliesTo appliesTo;
    private UUID referenceId;
    @Column(length = 100) private String label;
    @Builder.Default @Column(nullable = false) private Boolean isActive = true;
    private LocalDate validFrom;
    private LocalDate validUntil;
    private Integer maxApplications;
    @Builder.Default private Integer appliedCount = 0;
    @Column(nullable = false) private UUID createdBy;
    @CreatedDate @Column(updatable = false) private Instant createdAt;
}
