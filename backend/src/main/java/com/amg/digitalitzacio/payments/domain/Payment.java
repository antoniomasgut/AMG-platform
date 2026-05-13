package com.amg.digitalitzacio.payments.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false, unique = true)
    private UUID budgetId;

    @Column(unique = true)
    private UUID invoiceId;

    @Column(unique = true, length = 100)
    private String stripeSessionId;

    @Column(unique = true, length = 100)
    private String stripePaymentIntentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "EUR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(length = 500)
    private String checkoutUrl;

    private Instant paidAt;

    private Instant refundedAt;

    @Column(length = 500)
    private String errorMessage;

    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
