import { apiFetch } from './api';

export interface ConversationResponse {
  id: number;
  customerIdentifier: string;
  channel: 'WHATSAPP' | 'TELEGRAM' | 'EMAIL';
  role: 'USER' | 'ASSISTANT';
  content: string;
  pendingApproval: boolean;
  createdAt: string;
}

export interface PendingResponseDto {
  id: number;
  customerIdentifier: string;
  channel: 'WHATSAPP' | 'TELEGRAM' | 'EMAIL';
  customerMessage: string;
  suggestedResponse: string;
  createdAt: string;
}

export interface AgentStatusResponse {
  agentMode: 'AUTO' | 'HYBRID' | 'MANUAL';
  telegramLinked: boolean;
  whatsappConfigured: boolean;
  emailConfigured: boolean;
}

export const getAgentStatus = (tenantId: string) =>
  apiFetch<AgentStatusResponse>(`/agents/conversational/${tenantId}/status`);

export const getPendingConversations = (tenantId: string) =>
  apiFetch<PendingResponseDto[]>(`/agents/conversational/${tenantId}/pending`);

export const getConversations = (tenantId: string, page: number = 0, size: number = 20) =>
  apiFetch<ConversationResponse[]>(
    `/agents/conversational/${tenantId}/conversations?page=${page}&size=${size}`
  );

export const approveResponse = (tenantId: string, id: number) =>
  apiFetch<void>(`/agents/conversational/${tenantId}/pending/${id}/approve`, {
    method: 'POST',
  });

export const editAndSend = (tenantId: string, id: number, content: string) =>
  apiFetch<void>(`/agents/conversational/${tenantId}/pending/${id}/edit`, {
    method: 'POST',
    body: JSON.stringify({ content }),
  });

export const discardResponse = (tenantId: string, id: number) =>
  apiFetch<void>(`/agents/conversational/${tenantId}/pending/${id}`, {
    method: 'DELETE',
  });

export const updateAgentMode = (tenantId: string, mode: 'AUTO' | 'HYBRID' | 'MANUAL') =>
  apiFetch<void>(`/agents/conversational/${tenantId}/mode`, {
    method: 'PUT',
    body: JSON.stringify({ mode }),
  });
