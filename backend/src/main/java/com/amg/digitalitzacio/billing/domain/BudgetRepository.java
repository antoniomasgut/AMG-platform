package com.amg.digitalitzacio.billing.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
    List<Budget> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<Budget> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<Budget> findByAcceptanceToken(UUID token);
    long countByBudgetNumberStartingWith(String prefix);
    List<Budget> findByStatus(BudgetStatus status);
}
