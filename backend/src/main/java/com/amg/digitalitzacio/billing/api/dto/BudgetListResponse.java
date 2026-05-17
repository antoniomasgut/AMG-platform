package com.amg.digitalitzacio.billing.api.dto;

import java.util.List;

public record BudgetListResponse(List<BudgetResponse> budgets, long totalCount) {}
