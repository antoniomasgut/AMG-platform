package com.amg.digitalitzacio.vault.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vault_tenant_profiles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenantId", "profileId"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID profileId;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant completedAt;
}
