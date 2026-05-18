package com.amg.digitalitzacio.billing.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "early_adopter_programs")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EarlyAdopterProgram {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private Integer maxSlots;
    @Builder.Default @Column(nullable = false) private Integer usedSlots = 0;
    @Builder.Default @Column(nullable = false, precision = 5, scale = 2) private BigDecimal setupDiscountPct = BigDecimal.valueOf(100);
    @Builder.Default @Column(nullable = false, precision = 5, scale = 2) private BigDecimal monthlyDiscountPct = BigDecimal.valueOf(30);
    @Builder.Default @Column(nullable = false) private Integer commitmentMonths = 6;
    @Builder.Default @Column(nullable = false) private Boolean active = true;
    @LastModifiedDate private Instant updatedAt;
}
