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
@Table(name = "catalog_services")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CatalogService {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "phase_id", nullable = true) private UUID phaseId;
    @Column(name = "profile_id", nullable = true) private UUID profileId;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, length = 60, unique = true) private String slug;
    @Column(length = 255) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ServiceType type;
    @Builder.Default @Column(nullable = false) private Boolean isAddon = false;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal cost;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal salePrice;
    private Integer sortOrder;
    @CreatedDate @Column(updatable = false) private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
