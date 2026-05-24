package com.amg.digitalitzacio.finops.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonthlyInvoiceRepository extends JpaRepository<MonthlyInvoice, UUID> {
    Optional<MonthlyInvoice> findByTenantIdAndPeriod(UUID tenantId, String period);
    List<MonthlyInvoice> findByPeriodAndSepaCollectedFalse(String period);
    Page<MonthlyInvoice> findByTenantId(UUID tenantId, Pageable pageable);
    Page<MonthlyInvoice> findByPeriod(String period, Pageable pageable);
    List<MonthlyInvoice> findByPeriod(String period);
    long countByTenantId(UUID tenantId);
}
