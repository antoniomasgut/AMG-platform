package com.amg.digitalitzacio.domains.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Entitat principal per a dominis gestionats per AMG (via revenedor OpenProvider)
@Entity
@Table(name = "managed_domains")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Tenent propietari del domini
    @Column(nullable = false)
    private UUID tenantId;

    // Landing associada (opcional)
    private UUID landingId;

    // Nom complet del domini (p.ex. "el-meu-negoci.cat")
    @Column(length = 253, unique = true, nullable = false)
    private String domainName;

    // TLD del domini (p.ex. "cat", "es", "com")
    @Column(length = 20, nullable = false)
    private String tld;

    // Estat actual del domini
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DomainStatus status = DomainStatus.PENDING_PURCHASE;

    // Proveïdor de registre (per defecte OpenProvider)
    @Column(length = 30)
    @Builder.Default
    private String provider = "OPEN_PROVIDER";

    // ID intern del domini al proveïdor
    @Column(length = 100)
    private String providerDomainId;

    // Dades del registrant
    @Column(length = 150)
    private String registrantName;

    @Column(length = 150)
    private String registrantEmail;

    @Column(length = 20)
    private String registrantPhone;

    @Column(length = 20)
    private String registrantNif;

    // Preus de compra i venda
    @Column(precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal salePrice;

    // Dates de registre i caducitat
    private Instant registeredAt;
    private Instant expiresAt;

    // Renovació automàtica activa per defecte
    @Builder.Default
    private boolean autoRenew = true;

    // Moment en què s'ha enviat notificació de renovació propera
    private Instant renewalNotifiedAt;

    // Indica si el DNS s'ha configurat al servidor
    @Builder.Default
    private boolean dnsConfigured = false;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
