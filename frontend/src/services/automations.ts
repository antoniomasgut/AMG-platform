import { apiFetch } from './api';

export interface TemplateResponse {
  key: string; name: string; description: string;
  category: string; activationType: string;
}

export interface WorkflowResponse {
  id: string; templateName: string; templateKey: string;
  status: string; n8nWebhookUrl: string | null;
  n8nWorkflowId: string | null; lastRunAt: string | null;
  lastRunStatus: string | null; errorMessage: string | null;
  createdAt: string;
}

export interface WorkflowListResponse {
  content: WorkflowResponse[]; totalPages: number;
  totalElements: number; number: number;
}

export interface WorkflowExecutionResponse {
  id: string; workflowId: string; status: string;
  triggeredBy: string; payload: string | null;
  result: string | null; errorMessage: string | null;
  startedAt: string; completedAt: string | null;
}

export interface PageResponse<T> {
  content: T[]; totalPages: number; totalElements: number; number: number;
}

export const listTemplates = () =>
  apiFetch<TemplateResponse[]>('/automations/templates');

export const listWorkflows = (tenantId: string, status?: string, page = 0, size = 20) => {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) params.set('status', status);
  return apiFetch<WorkflowListResponse>(`/automations/tenants/${tenantId}/workflows?${params}`);
};

export const assignWorkflow = (tenantId: string, templateKey: string) =>
  apiFetch<WorkflowResponse>(`/automations/tenants/${tenantId}/workflows`, {
    method: 'POST',
    body: JSON.stringify({ templateKey }),
  });

export const deployWorkflow = (workflowId: string) =>
  apiFetch<WorkflowResponse>(`/automations/workflows/${workflowId}/deploy`, { method: 'POST' });

export const activateWorkflow = (workflowId: string) =>
  apiFetch<WorkflowResponse>(`/automations/workflows/${workflowId}/activate`, { method: 'POST' });

export const deactivateWorkflow = (workflowId: string) =>
  apiFetch<WorkflowResponse>(`/automations/workflows/${workflowId}/deactivate`, { method: 'POST' });

export const deleteWorkflow = (workflowId: string) =>
  apiFetch<void>(`/automations/workflows/${workflowId}`, { method: 'DELETE' });

export const listExecutions = (workflowId: string, page = 0, size = 20) =>
  apiFetch<PageResponse<WorkflowExecutionResponse>>(
    `/automations/workflows/${workflowId}/executions?page=${page}&size=${size}`
  );

export const getAutomationsHealth = () =>
  apiFetch<{ status: string; n8nConnected: boolean }>('/automations/health');
