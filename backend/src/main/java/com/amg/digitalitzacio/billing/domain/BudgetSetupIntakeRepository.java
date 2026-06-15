package com.amg.digitalitzacio.billing.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BudgetSetupIntakeRepository extends JpaRepository<BudgetSetupIntake, UUID> {

    Optional<BudgetSetupIntake> findByToken(String token);

    Optional<BudgetSetupIntake> findByBudgetId(UUID budgetId);
}
