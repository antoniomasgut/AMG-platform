package com.amg.digitalitzacio.billing.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BudgetListResponse {
    private List<BudgetResponse.BudgetSummary> budgets;
    private long totalCount;
}
