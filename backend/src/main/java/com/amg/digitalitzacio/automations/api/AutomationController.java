package com.amg.digitalitzacio.automations.api;

import com.amg.digitalitzacio.automations.api.dto.*;
import com.amg.digitalitzacio.automations.application.AutomationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/automations")
@RequiredArgsConstructor
public class AutomationController {

    private final AutomationService automationService;

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.OK)
    public WebhookResponse webhook(@RequestBody WebhookRequest request) {
        return automationService.processWebhook(request);
    }

    @GetMapping("/webhook/health")
    @ResponseStatus(HttpStatus.OK)
    public WebhookHealthResponse webhookHealth() {
        return new WebhookHealthResponse("ok", java.time.Instant.now().toString());
    }

    @GetMapping("/templates")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public List<TemplateResponse> listTemplates() {
        return automationService.listTemplates();
    }

    @GetMapping("/tenants/{tenantId}/workflows")
    @PreAuthorize("isAuthenticated()")
    public WorkflowListResponse listWorkflows(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return automationService.listWorkflows(tenantId, status, page, size);
    }

    @PostMapping("/tenants/{tenantId}/workflows")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowResponse assignWorkflow(
            @PathVariable UUID tenantId,
            @RequestBody AssignWorkflowRequest request) {
        return automationService.assignWorkflow(tenantId, request);
    }

    @PostMapping("/workflows/{workflowId}/deploy")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public WorkflowResponse deployWorkflow(@PathVariable UUID workflowId) {
        return automationService.deployWorkflow(workflowId);
    }

    @PostMapping("/workflows/{workflowId}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public WorkflowResponse activateWorkflow(@PathVariable UUID workflowId) {
        return automationService.activateWorkflow(workflowId);
    }

    @PostMapping("/workflows/{workflowId}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public WorkflowResponse deactivateWorkflow(@PathVariable UUID workflowId) {
        return automationService.deactivateWorkflow(workflowId);
    }

    @DeleteMapping("/workflows/{workflowId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkflow(@PathVariable UUID workflowId) {
        automationService.deleteWorkflow(workflowId);
    }

    @GetMapping("/workflows/{workflowId}/executions")
    @PreAuthorize("isAuthenticated()")
    public Page<WorkflowExecutionResponse> listExecutions(
            @PathVariable UUID workflowId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return automationService.listExecutions(workflowId, status, page, size);
    }

    @GetMapping("/health")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public HealthResponse health() {
        return automationService.health();
    }

    public record WebhookHealthResponse(String status, String timestamp) {}
}
