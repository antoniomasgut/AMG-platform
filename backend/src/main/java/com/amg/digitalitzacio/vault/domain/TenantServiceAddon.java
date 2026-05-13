package com.amg.digitalitzacio.vault.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_service_addons", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "service_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantServiceAddon {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "service_id", nullable = false) private UUID serviceId;
    @Column(name = "added_by", nullable = false) private UUID addedBy;
    @Builder.Default @Column(nullable = false) private Boolean approvalRequired = true;
    @Enumerated(EnumType.STRING) private ApprovalStatus approvalStatus;
    @Column(nullable = false) private Instant createdAt;
}
