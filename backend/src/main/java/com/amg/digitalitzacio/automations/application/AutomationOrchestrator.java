package com.amg.digitalitzacio.automations.application;

import com.amg.digitalitzacio.automations.api.dto.*;
import com.amg.digitalitzacio.automations.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AutomationOrchestrator implements AutomationService {

    private final WorkflowTemplateRepository workflowTemplateRepository;
    private final TenantWorkflowRepository tenantWorkflowRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TemplateResponse> listTemplates() {
        return workflowTemplateRepository.findByIsActiveTrue().stream()
                .map(t -> new TemplateResponse(
                        t.getKey(), t.getName(), t.getDescription(),
                        t.getCategory().name(), t.getActivationType().name()))
                .toList();
    }

    @Override
    @Transactional
    public WorkflowResponse assignWorkflow(UUID tenantId, AssignWorkflowRequest request) {
        var template = workflowTemplateRepository.findByKey(request.templateKey())
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + request.templateKey()));

        var workflow = TenantWorkflow.builder()
                .tenantId(tenantId)
                .templateId(template.getId())
                .config(request.config() != null ? request.config().toString() : null)
                .status(TenantWorkflowStatus.PENDING)
                .build();

        if (template.getActivationType() == WorkflowActivationType.AUTOMATIC) {
            workflow.setStatus(TenantWorkflowStatus.DEPLOYED);
            workflow.setN8nWorkflowId(UUID.randomUUID().toString());
        }

        workflow = tenantWorkflowRepository.save(workflow);
        return toWorkflowResponse(workflow, template);
    }

    @Override
    @Transactional
    public WorkflowResponse deployWorkflow(UUID workflowId) {
        var workflow = findActiveWorkflow(workflowId);
        workflow.setStatus(TenantWorkflowStatus.DEPLOYED);
        workflow.setN8nWorkflowId(UUID.randomUUID().toString());
        workflow = tenantWorkflowRepository.save(workflow);
        return toWorkflowResponse(workflow, findTemplate(workflow.getTemplateId()));
    }

    @Override
    @Transactional
    public WorkflowResponse activateWorkflow(UUID workflowId) {
        var workflow = findActiveWorkflow(workflowId);
        workflow.setStatus(TenantWorkflowStatus.ACTIVE);
        workflow = tenantWorkflowRepository.save(workflow);
        return toWorkflowResponse(workflow, findTemplate(workflow.getTemplateId()));
    }

    @Override
    @Transactional
    public WorkflowResponse deactivateWorkflow(UUID workflowId) {
        var workflow = findActiveWorkflow(workflowId);
        workflow.setStatus(TenantWorkflowStatus.DISABLED);
        workflow = tenantWorkflowRepository.save(workflow);
        return toWorkflowResponse(workflow, findTemplate(workflow.getTemplateId()));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowListResponse listWorkflows(UUID tenantId, String status, int page, int size) {
        var pageable = PageRequest.of(page, size);
        Page<TenantWorkflow> result;
        if (status != null && !status.isBlank()) {
            result = tenantWorkflowRepository.findByTenantIdAndStatus(
                    tenantId, TenantWorkflowStatus.valueOf(status.toUpperCase()), pageable);
        } else {
            result = tenantWorkflowRepository.findByTenantId(tenantId, pageable);
        }
        var workflows = result.getContent().stream()
                .map(w -> toWorkflowResponse(w, findTemplate(w.getTemplateId())))
                .toList();
        return new WorkflowListResponse(workflows, result.getNumber(), result.getTotalPages(), result.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowExecutionResponse> listExecutions(UUID workflowId, String status, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var executions = workflowExecutionRepository.findByTenantWorkflowId(workflowId, pageable);
        return executions.map(e -> new WorkflowExecutionResponse(
                e.getId(),
                e.getTriggerType() != null ? e.getTriggerType().name() : null,
                e.getSourceId(),
                e.getStatus().name(),
                e.getErrorMessage(),
                e.getExecutedAt(),
                e.getCompletedAt()));
    }

    @Override
    @Transactional
    public void deleteWorkflow(UUID workflowId) {
        var workflow = findActiveWorkflow(workflowId);
        workflow.setIsActive(false);
        workflow.setStatus(TenantWorkflowStatus.DISABLED);
        tenantWorkflowRepository.save(workflow);
    }

    @Override
    @Transactional
    public WebhookResponse processWebhook(WebhookRequest request) {
        if (request.executionId() != null) {
            var existing = workflowExecutionRepository.findByN8nExecutionId(request.executionId());
            if (existing.isPresent()) {
                var execution = existing.get();
                execution.setStatus(mapExecutionStatus(request.status()));
                execution.setResponsePayload(request.output() != null ? request.output().toString() : null);
                execution.setErrorMessage(request.error());
                execution.setCompletedAt(Instant.now());
                workflowExecutionRepository.save(execution);
            }
        }
        return new WebhookResponse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public HealthResponse health() {
        var activeWorkflows = (int) tenantWorkflowRepository.countByTenantIdAndStatus(
                null, TenantWorkflowStatus.ACTIVE);
        return new HealthResponse(true, "1.80.0", Math.max(0, activeWorkflows), 0);
    }

    private TenantWorkflow findActiveWorkflow(UUID id) {
        return tenantWorkflowRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + id));
    }

    private WorkflowTemplate findTemplate(UUID templateId) {
        return workflowTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));
    }

    private WorkflowResponse toWorkflowResponse(TenantWorkflow w, WorkflowTemplate t) {
        return new WorkflowResponse(
                w.getId(),
                t.getName(),
                t.getKey(),
                w.getStatus().name(),
                w.getN8nWebhookUrl(),
                w.getN8nWorkflowId(),
                w.getLastRunAt(),
                w.getLastRunStatus() != null ? w.getLastRunStatus().name() : null,
                w.getErrorMessage(),
                w.getCreatedAt());
    }

    private ExecutionStatus mapExecutionStatus(String status) {
        if (status == null) return ExecutionStatus.PENDING;
        return switch (status.toLowerCase()) {
            case "success" -> ExecutionStatus.SUCCESS;
            case "error", "failed" -> ExecutionStatus.FAILED;
            case "timeout" -> ExecutionStatus.TIMEOUT;
            default -> ExecutionStatus.PENDING;
        };
    }
}
