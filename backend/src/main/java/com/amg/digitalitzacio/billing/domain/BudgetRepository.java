package com.amg.digitalitzacio.billing.domain;

import com.amg.digitalitzacio.billing.domain.Budget;
import com.amg.digitalitzacio.billing.domain.BudgetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    Page<Budget> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Budget> findByTenantIdAndStatus(UUID tenantId, BudgetStatus status, Pageable pageable);
    Optional<Budget> findByAcceptanceToken(String acceptanceToken);
    long countByTenantIdAndStatus(UUID tenantId, BudgetStatus status);
}
