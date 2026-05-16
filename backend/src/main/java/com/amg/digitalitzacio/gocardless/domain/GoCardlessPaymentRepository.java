package com.amg.digitalitzacio.gocardless.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GoCardlessPaymentRepository extends JpaRepository<GoCardlessPayment, UUID> {
    Page<GoCardlessPayment> findByTenantId(UUID tenantId, Pageable pageable);
    Optional<GoCardlessPayment> findByGcPaymentId(String gcPaymentId);
    Optional<GoCardlessPayment> findByMonthlyInvoiceId(UUID monthlyInvoiceId);
}
