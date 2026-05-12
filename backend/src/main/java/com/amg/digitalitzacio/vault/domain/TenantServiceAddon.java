package com.amg.digitalitzacio.vault.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vault_tenant_service_addons", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenantId", "serviceId"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TenantServiceAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID serviceId;

    @Column(nullable = false)
    private UUID addedBy;

    @Column(nullable = false)
    private Boolean approvalRequired;

    @Enumerated(EnumType.STRING)
    private AddonApprovalStatus approvalStatus;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;
}
