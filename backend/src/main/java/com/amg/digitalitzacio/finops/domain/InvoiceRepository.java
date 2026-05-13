package com.amg.digitalitzacio.finops.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Page<Invoice> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Invoice> findByTenantIdAndStatus(UUID tenantId, InvoiceStatus status, Pageable pageable);
    Optional<Invoice> findByBudgetId(UUID budgetId);
    Optional<Invoice> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Invoice> findByHoldedInvoiceId(String holdedInvoiceId);
    long countByTenantIdAndStatus(UUID tenantId, InvoiceStatus status);
}
