package com.amg.digitalitzacio.engine.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "landings", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "slug"}))
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Landing {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "service_id", nullable = false) private UUID serviceId;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, length = 100) private String slug;
    @Column(length = 300) private String metaDescription;
    @Column(length = 500) private String ogImageUrl;
    @Column(name = "template_id", nullable = true) private UUID templateId;
    @Enumerated(EnumType.STRING) @Builder.Default @Column(nullable = false) private LandingStatus status = LandingStatus.DRAFT;
    @Column(name = "published_version_id", nullable = true) private UUID publishedVersionId;
    @Column(unique = true, nullable = true, length = 200) private String customDomain;
    @Builder.Default @Column(name = "domain_verified") private Boolean domainVerified = false;
    @Builder.Default @Column(name = "managed_domain") private Boolean managedDomain = false;
    @Enumerated(EnumType.STRING) @Builder.Default @Column(name = "domain_status") private DomainStatus domainStatus = DomainStatus.NOT_CONFIGURED;
    @Column(length = 100) private String domainRegistrar;
    @Column(name = "domain_renewal_date") private LocalDate domainRenewalDate;
    @Column(name = "domain_renewal_price", precision = 8, scale = 2) private BigDecimal domainRenewalPrice;
    @Column(length = 150) private String domainOwnerName;
    @Column(length = 200) private String domainOwnerEmail;
    @Column(length = 30) private String domainOwnerPhone;
    @Enumerated(EnumType.STRING) @Builder.Default @Column(name = "landing_type", nullable = false, length = 10) private LandingType landingType = LandingType.MICRO;
    @Builder.Default @Column(name = "is_active", nullable = false) private Boolean isActive = true;
    @Builder.Default @Column(name = "view_count", nullable = false) private Long viewCount = 0L;
    @CreatedDate @Column(updatable = false) private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
