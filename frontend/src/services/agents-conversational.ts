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

export interface ModelInfo {
  id: string;
  label: string;
  provider: string;
  requiresApiKey: boolean;
}

export interface TenantAIConfig {
  tenantId: string;
  preferredModel: string;
  maxTokens: number;
  temperature: number;
}

export interface AIConfigRequest {
  preferredModel?: string;
  maxTokens?: number;
  temperature?: number;
}

export interface AIModelTestRequest {
  model: string;
  message: string;
  systemPrompt?: string;
}

export interface AIModelTestResponse {
  model: string;
  provider: string;
  response: string;
  error?: string;
}

export const getAvailableModels = () =>
  apiFetch<ModelInfo[]>('/agents/conversational/models');

export const getAIConfig = (tenantId: string) =>
  apiFetch<TenantAIConfig>(`/agents/conversational/${tenantId}/ai-config`);

export const updateAIConfig = (tenantId: string, config: AIConfigRequest) =>
  apiFetch<TenantAIConfig>(`/agents/conversational/${tenantId}/ai-config`, {
    method: 'PUT',
    body: JSON.stringify(config),
  });

export const testModel = (request: AIModelTestRequest) =>
  apiFetch<AIModelTestResponse>('/agents/conversational/test-model', {
    method: 'POST',
    body: JSON.stringify(request),
  });

// --- Omnichannel Inbox (Spec 25) ---

export interface ContactSummary {
  contactId: string;
  displayName: string;
  channels: { channel: string; identifier: string }[];
  lastMessage: string | null;
  lastMessageRole: 'USER' | 'ASSISTANT' | null;
  lastMessageAt: string | null;
  lastChannel: string | null;
  lastIdentifier: string | null;
  pendingCount: number;
}

export const listContacts = (tenantId: string) =>
  apiFetch<ContactSummary[]>(`/agents/contacts/${tenantId}`);

export const getContactThread = (tenantId: string, contactId: string) =>
  apiFetch<ConversationResponse[]>(`/agents/contacts/${tenantId}/${contactId}/thread`);

export const sendReply = (tenantId: string, contactId: string, text: string) =>
  apiFetch<void>(`/agents/contacts/${tenantId}/${contactId}/reply`, {
    method: 'POST',
    body: JSON.stringify({ text }),
  });

export const renameContact = (tenantId: string, contactId: string, displayName: string) =>
  apiFetch<void>(`/agents/contacts/${tenantId}/${contactId}/name`, {
    method: 'PATCH',
    body: JSON.stringify({ displayName }),
  });

// --- Channels & Activation (Spec 24) ---

export interface ChannelsConfig {
  tenantId: string;
  agentMode: 'AUTO' | 'HYBRID' | 'MANUAL';
  isActive: boolean;
  telegramLinked: boolean;
  telegramChatId: number | null;
  whatsappPhoneNumber: string | null;
  whatsappMetaPhoneNumberId: string | null;
}

export interface ChannelInstruction {
  active: boolean;
  configured: boolean;
  instructions: string | null;
  link: string | null;
}

export interface ActivationInstructions {
  telegram: ChannelInstruction;
  whatsapp: ChannelInstruction;
}

export const getChannels = (tenantId: string) =>
  apiFetch<ChannelsConfig>(`/agents/conversational/${tenantId}/channels`);

export const updateChannels = (tenantId: string, data: { agentMode?: string; whatsappPhoneNumber?: string; whatsappMetaPhoneNumberId?: string }) =>
  apiFetch<ChannelsConfig>(`/agents/conversational/${tenantId}/channels`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });

export const activateAgent = (tenantId: string) =>
  apiFetch<ChannelsConfig>(`/agents/conversational/${tenantId}/activate`, { method: 'POST' });

export const deactivateAgent = (tenantId: string) =>
  apiFetch<ChannelsConfig>(`/agents/conversational/${tenantId}/deactivate`, { method: 'POST' });

export const getActivationInstructions = (tenantId: string) =>
  apiFetch<ActivationInstructions>(`/agents/conversational/${tenantId}/activation-instructions`);
