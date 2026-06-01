import { apiFetch } from './api';

export type TemplateType = 'WHATSAPP' | 'EMAIL' | 'CALL_SCRIPT';

export interface MessageTemplate {
  id: string;
  name: string;
  type: TemplateType;
  sector: string | null;
  subject: string | null;
  body: string;
  createdAt: string;
  updatedAt: string;
}

export interface TemplateRequest {
  name: string;
  type: TemplateType;
  sector?: string;
  subject?: string;
  body: string;
}

export const SECTORS = [
  { key: 'restaurant', label: 'Restaurants / Bars' },
  { key: 'comerc', label: 'Comerç local' },
  { key: 'serveis', label: 'Serveis professionals' },
  { key: 'turisme', label: 'Turisme / Allotjament' },
  { key: 'salut', label: 'Salut / Bellesa' },
  { key: 'construccio', label: 'Construcció / Reforma' },
  { key: 'automoció', label: 'Automoció' },
  { key: 'educacio', label: 'Educació / Formació' },
] as const;

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
