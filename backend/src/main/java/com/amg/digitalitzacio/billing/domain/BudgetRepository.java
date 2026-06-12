package com.amg.digitalitzacio.billing.domain;

import com.amg.digitalitzacio.billing.domain.Budget;
import com.amg.digitalitzacio.billing.domain.BudgetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    Page<Budget> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<Budget> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, BudgetStatus status, Pageable pageable);
    Page<Budget> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Budget> findByStatusOrderByCreatedAtDesc(BudgetStatus status, Pageable pageable);
    Optional<Budget> findByAcceptanceToken(String acceptanceToken);
    Optional<Budget> findFirstByLeadIdOrderByCreatedAtDesc(UUID leadId);
    long countByTenantIdAndStatus(UUID tenantId, BudgetStatus status);
    long countByTenantId(UUID tenantId);
    void deleteByTenantId(UUID tenantId);
}
