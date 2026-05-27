package com.amg.digitalitzacio.vault.domain;

import com.amg.digitalitzacio.auth.domain.BusinessSector;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_activated_phases",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "sector", "phase_number"}))
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantActivatedPhase {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private BusinessSector sector;
    @Column(name = "phase_number", nullable = false) private int phaseNumber;
    @Column(name = "phase_name", length = 200) private String phaseName;
    @Column(name = "setup_price_paid", precision = 10, scale = 2) private BigDecimal setupPricePaid;
    @Column(name = "monthly_price", precision = 10, scale = 2) private BigDecimal monthlyPrice;
    @Column(name = "budget_id") private UUID budgetId;
    @Builder.Default @Column(length = 30) private String status = "ACTIVE";
    @CreatedDate @Column(updatable = false) private Instant activatedAt;
}
