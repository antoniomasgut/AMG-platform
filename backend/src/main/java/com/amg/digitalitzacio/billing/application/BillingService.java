package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.billing.api.dto.*;
import com.amg.digitalitzacio.shared.security.UserPrincipal;

import java.util.UUID;

public interface BillingService {

    BudgetResponse createBudget(UUID tenantId, CreateBudgetRequest request, UUID createdBy);

    BudgetListResponse listBudgets(UUID tenantId);

    BudgetListResponse listBudgetsByTenant(UUID tenantId);

    BudgetResponse getBudget(UUID tenantId, UUID id);

    BudgetResponse updateBudget(UUID id, UpdateBudgetRequest request);

    void cancelBudget(UUID id);

    BudgetResponse sendBudget(UUID id);

    BudgetResponse acceptBudget(String token);

    BudgetResponse rejectBudget(String token, String reason);

    DashboardResponse getDashboard(UUID tenantId);
}
