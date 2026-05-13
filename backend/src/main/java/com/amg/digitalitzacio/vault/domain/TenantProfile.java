package com.amg.digitalitzacio.vault.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_profiles", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "profile_id"}))
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantProfile {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "profile_id", nullable = false) private UUID profileId;
    @Enumerated(EnumType.STRING) @Builder.Default @Column(nullable = false) private PhaseStatus phaseStatus = PhaseStatus.CONFIGURING;
    @Builder.Default @Column(nullable = false) private Boolean isActive = true;
    private Instant startedAt;
    private Instant completedAt;
    @CreatedDate @Column(updatable = false) private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
