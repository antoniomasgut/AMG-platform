package com.amg.digitalitzacio.payments.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stripe_configs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "api_key_ref", nullable = false, length = 100)
    private String apiKeyRef;

    @Column(length = 100)
    private String webhookSecret;

    @Builder.Default
    private Boolean isActive = true;

    // Mètode de pagament guardat per a càrrecs automàtics
    @Column(length = 100)
    private String stripeCustomerId;

    @Column(length = 100)
    private String stripePaymentMethodId;

    @Column(length = 10)
    private String pmLastFour;

    @Column(length = 30)
    private String pmBrand;

    private Integer pmExpMonth;
    private Integer pmExpYear;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
