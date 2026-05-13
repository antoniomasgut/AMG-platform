package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.billing.api.dto.*;

import java.util.List;
import java.util.UUID;

public interface BillingService {
    BudgetResponse createBudget(UUID tenantId, CreateBudgetRequest request);
    List<BudgetResponse> listBudgets(UUID tenantId, String status, int page, int size);
    BudgetResponse getBudget(UUID budgetId, boolean includeInternalNotes);
    BudgetResponse updateBudget(UUID budgetId, UpdateBudgetRequest request);
    void cancelBudget(UUID budgetId);
    BudgetSendResponse sendBudget(UUID budgetId);
    AcceptRejectResponse acceptBudget(String token);
    AcceptRejectResponse rejectBudget(String token, String reason);
    DashboardResponse getDashboard(UUID tenantId);
}
