package com.amg.digitalitzacio.finops.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sepa_mandates")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SepaMandate {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true) private UUID tenantId;
    @Column(nullable = false, unique = true, length = 35) private String mandateId;
    @Column(nullable = false, length = 34) private String iban;
    @Column(length = 11) private String bic;
    @Column(nullable = false, length = 100) private String accountHolderName;
    @Column(nullable = false) private LocalDate signedAt;
    @Builder.Default @Column(nullable = false) private Boolean isActive = true;
    private Instant revokedAt;
    @CreatedDate @Column(updatable = false) private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
}
