package com.amg.digitalitzacio.domains.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

// Tarifes per extensió de domini (TLD): cost d'OpenProvider i preu de venda al client
@Entity
@Table(name = "tld_pricing")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TldPricing {

    // El TLD és la clau primària (p.ex. "cat", "es", "com")
    @Id
    @Column(length = 20)
    private String tld;

    // Costos interns (OpenProvider)
    @Column(precision = 10, scale = 2)
    private BigDecimal costRegister;

    @Column(precision = 10, scale = 2)
    private BigDecimal costRenew;

    // Preus de venda al client
    @Column(precision = 10, scale = 2)
    private BigDecimal saleRegister;

    @Column(precision = 10, scale = 2)
    private BigDecimal saleRenew;

    // Indica si el TLD està disponible per a nous registres
    @Builder.Default
    private boolean isActive = true;

    @LastModifiedDate
    private Instant updatedAt;
}
