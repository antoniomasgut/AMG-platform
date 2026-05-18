package com.amg.digitalitzacio.billing.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "referral_codes", indexes = {
    @Index(columnList = "owner_tenant_id"),
    @Index(columnList = "code", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReferralCode {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false) private UUID ownerTenantId;
    @Column(nullable = false, length = 20, unique = true) private String code;
    private UUID usedByTenantId;
    private Instant usedAt;
    @Builder.Default private Integer referrerCreditMonths = 1;
    @Builder.Default private Boolean referredSetupFree = true;
    @Builder.Default private Boolean creditApplied = false;
    @CreatedDate @Column(updatable = false) private Instant createdAt;
}
