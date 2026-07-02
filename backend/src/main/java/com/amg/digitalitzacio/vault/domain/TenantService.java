package com.amg.digitalitzacio.vault.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_services", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "service_id"}))
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantService {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "service_id", nullable = false) private UUID serviceId;
    @Column(name = "phase_id", nullable = true) private UUID phaseId;
    @Enumerated(EnumType.STRING) @Builder.Default @Column(nullable = false) private ServiceStatus status = ServiceStatus.PENDING;
    @Column(nullable = false, precision = 10, scale = 2) @Builder.Default private BigDecimal setupPriceLocked = BigDecimal.ZERO;
    @Column(nullable = false, precision = 10, scale = 2) @Builder.Default private BigDecimal monthlyPriceLocked = BigDecimal.ZERO;
    @Builder.Default @Column(nullable = false) private Integer catalogVersionLocked = 1;
    @Builder.Default @Column(nullable = false) private Boolean outdated = false;
    private Instant outdatedAt;
    @Builder.Default @Column(nullable = false, name = "is_enabled") private Boolean isEnabled = true;
    private Instant activatedAt;
    private Instant statusChangedAt;
    @CreatedDate @Column(updatable = false) private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
