package com.amg.digitalitzacio.payments.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Page<Payment> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Payment> findByTenantIdAndStatus(UUID tenantId, PaymentStatus status, Pageable pageable);
    Optional<Payment> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Payment> findByStripeSessionId(String stripeSessionId);
    long countByTenantIdAndStatus(UUID tenantId, PaymentStatus status);
    long countByStatus(PaymentStatus status);
}
