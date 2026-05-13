package com.amg.digitalitzacio.billing.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BudgetLineRepository extends JpaRepository<BudgetLine, UUID> {
    List<BudgetLine> findByBudgetIdOrderBySortOrder(UUID budgetId);
    void deleteByBudgetId(UUID budgetId);
}
