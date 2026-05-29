import { apiFetch } from './api';

export type TemplateType = 'WHATSAPP' | 'EMAIL' | 'CALL_SCRIPT';

export interface MessageTemplate {
  id: string;
  name: string;
  type: TemplateType;
  subject: string | null;
  body: string;
  createdAt: string;
  updatedAt: string;
}

export interface TemplateRequest {
  name: string;
  type: TemplateType;
  subject?: string;
  body: string;
}

export const listTemplates = () =>
  apiFetch<MessageTemplate[]>('/leads/templates');

export const createTemplate = (req: TemplateRequest) =>
  apiFetch<MessageTemplate>('/leads/templates', {
    method: 'POST',
    body: JSON.stringify(req),
  });

export const updateTemplate = (id: string, req: TemplateRequest) =>
  apiFetch<MessageTemplate>(`/leads/templates/${id}`, {
    method: 'PUT',
    body: JSON.stringify(req),
  });

export const deleteTemplate = (id: string) =>
  apiFetch<void>(`/leads/templates/${id}`, { method: 'DELETE' });
