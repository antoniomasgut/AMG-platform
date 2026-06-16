package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.billing.api.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface BillingService {
    BudgetResponse createBudget(UUID tenantId, CreateBudgetRequest request);
    BudgetResponse createLeadProposal(UUID leadId, CreateBudgetRequest request);
    Page<BudgetResponse> listBudgets(UUID tenantId, String status, int page, int size);
    Page<BudgetResponse> listAllBudgets(String status, UUID tenantId, int page, int size);
    BudgetResponse getBudget(UUID budgetId, boolean includeInternalNotes);
    BudgetResponse updateBudget(UUID budgetId, UpdateBudgetRequest request);
    void cancelBudget(UUID budgetId);
    BudgetSendResponse sendBudget(UUID budgetId);
    AcceptRejectResponse acceptBudget(String token);
    AcceptRejectResponse rejectBudget(String token, String reason);
    BudgetResponse previewBudget(String token);
    AcceptRejectResponse acceptBudgetPhases(String token, List<String> phaseKeys);
    DashboardResponse getDashboard(UUID tenantId);
    void forceActivatePhases(UUID budgetId);
}
