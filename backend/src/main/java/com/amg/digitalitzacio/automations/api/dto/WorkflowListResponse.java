package com.amg.digitalitzacio.automations.api.dto;

import java.util.List;

public record WorkflowListResponse(List<WorkflowResponse> workflows, int page, int totalPages, long totalElements) {}
