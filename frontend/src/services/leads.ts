import { apiFetch } from './api';

export interface Lead {
  id: string;
  name: string;
  email: string | null;
  phone: string | null;
  source: string;
  stage: string;
  notes?: string | null;
  tags?: string | null;
  lostReason?: string | null;
  estimatedValue?: number | null;
  hasWhatsapp?: boolean | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LeadStats {
  total: number;
  byStage: Record<string, number>;
  conversionRate: number;
}

export interface Activity {
  id: string;
  type: string;
  description: string;
  dueDate?: string | null;
  completedAt?: string | null;
  createdAt: string;
}

export interface CreateLeadRequest {
  name: string;
  email?: string;
  phone?: string;
  source: string;
  notes?: string;
  estimatedValue?: number;
}

export interface CreateActivityRequest {
  type: string;
  description: string;
  dueDate?: string;
}

export const getLeads = () =>
  apiFetch<{ content: Lead[] }>('/leads?size=200').then(r => r.content);

export const getLead = (id: string) =>
  apiFetch<Lead>(`/leads/${id}`);

export const getLeadStats = () =>
  apiFetch<LeadStats>('/leads/stats');

export const createLead = (data: CreateLeadRequest) =>
  apiFetch<Lead>('/leads', { method: 'POST', body: JSON.stringify(data) });

export const updateLead = (id: string, data: Partial<CreateLeadRequest>) =>
  apiFetch<Lead>(`/leads/${id}`, { method: 'PUT', body: JSON.stringify(data) });

export const deleteLead = (id: string) =>
  apiFetch<void>(`/leads/${id}`, { method: 'DELETE' });

export const changeStage = (id: string, stage: string, lostReason?: string) =>
  apiFetch<Lead>(`/leads/${id}/stage`, { method: 'PATCH', body: JSON.stringify({ stage, lostReason }) });

export interface OutreachRequest {
  leadIds: string[];
  subject: string;
  body: string;
  demoUrl: string;
  language: 'ca' | 'es';
}

export const sendOutreach = (data: OutreachRequest) =>
  apiFetch<{ sent: number }>('/leads/outreach', { method: 'POST', body: JSON.stringify(data) });

export const setWhatsapp = (id: string, value: boolean) =>
  apiFetch<Lead>(`/leads/${id}/whatsapp?value=${value}`, { method: 'PATCH' });

export const getActivities = (leadId: string) =>
  apiFetch<{ content: Activity[] }>(`/leads/${leadId}/activities?size=100`).then(r => r.content);

export const createActivity = (leadId: string, data: CreateActivityRequest) =>
  apiFetch<Activity>(`/leads/${leadId}/activities`, { method: 'POST', body: JSON.stringify(data) });
