package com.amg.digitalitzacio.gocardless.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gocardless_mandates")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GoCardlessMandate {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID tenantId;

    @Column(length = 50)
    private String gcMandateId;

    @Column(length = 50)
    private String gcRedirectFlowId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GoCardlessMandateStatus status = GoCardlessMandateStatus.PENDING_SUBMISSION;

    @Column(length = 100)
    private String accountHolderName;

    @Column(length = 100)
    private String bankName;

    @Column(length = 4)
    private String lastFourDigits;

    @CreatedDate @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
