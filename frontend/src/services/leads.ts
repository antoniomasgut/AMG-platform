import { apiFetch } from './api';

export interface Lead {
  id: string;
  companyName: string;
  contactName: string;
  contactEmail: string;
  contactPhone?: string;
  source: string;
  stage: string;
  notes?: string;
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
  notes: string;
  dueDate?: string;
  completed: boolean;
  createdAt: string;
}

export interface CreateLeadRequest {
  companyName: string;
  contactName: string;
  contactEmail: string;
  contactPhone?: string;
  source: string;
  notes?: string;
}

export interface CreateActivityRequest {
  type: string;
  notes: string;
  dueDate?: string;
}

export const getLeads = () =>
  apiFetch<Lead[]>('/leads');

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

export const changeStage = (id: string, stage: string) =>
  apiFetch<Lead>(`/leads/${id}/stage`, { method: 'PATCH', body: JSON.stringify({ stage }) });

export const getActivities = (leadId: string) =>
  apiFetch<Activity[]>(`/leads/${leadId}/activities`);

export const createActivity = (leadId: string, data: CreateActivityRequest) =>
  apiFetch<Activity>(`/leads/${leadId}/activities`, { method: 'POST', body: JSON.stringify(data) });
