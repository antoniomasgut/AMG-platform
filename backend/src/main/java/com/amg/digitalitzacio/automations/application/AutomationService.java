package com.amg.digitalitzacio.automations.application;

import com.amg.digitalitzacio.automations.api.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AutomationService {
    List<TemplateResponse> listTemplates();
    WorkflowResponse assignWorkflow(UUID tenantId, AssignWorkflowRequest request);
    WorkflowResponse deployWorkflow(UUID workflowId);
    WorkflowResponse activateWorkflow(UUID workflowId);
    WorkflowResponse deactivateWorkflow(UUID workflowId);
    WorkflowListResponse listWorkflows(UUID tenantId, String status, int page, int size);
    Page<WorkflowExecutionResponse> listExecutions(UUID workflowId, String status, int page, int size);
    void deleteWorkflow(UUID workflowId);
    WebhookResponse processWebhook(WebhookRequest request);
    HealthResponse health();
}
