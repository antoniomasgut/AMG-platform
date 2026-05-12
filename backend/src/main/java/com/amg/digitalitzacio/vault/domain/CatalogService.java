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
@Table(name = "vault_services")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CatalogService {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID phaseId;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 60, unique = true, nullable = false)
    private String slug;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType type;

    @Column(nullable = false)
    private Boolean isAddon;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal cost;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal salePrice;

    private Integer sortOrder;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
