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
  utmSource?: string | null;
  utmMedium?: string | null;
  utmCampaign?: string | null;
  metaLeadId?: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  interviewNotes?: string | null;
  webNeed?: string | null;
  interviewSector?: string | null;
  interviewBusinessSize?: string | null;
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
  utmSource?: string;
  utmMedium?: string;
  utmCampaign?: string;
  interviewNotes?: string;
  webNeed?: string;
  interviewSector?: string;
  interviewBusinessSize?: string;
}

export const WEB_NEED_OPTIONS = [
  { value: 'NONE', label: 'Sense web' },
  { value: 'PORTAL_STATIC', label: 'Web estàtica (portal AMG)' },
  { value: 'PORTAL_CONTAINER', label: 'Web containeritzada (portal AMG)' },
  { value: 'EXTERNAL', label: 'Web en proveïdor extern' },
] as const;

export interface CreateActivityRequest {
  type: string;
  description: string;
  dueDate?: string;
}

export const getLeads = () =>
  apiFetch<{ content: Lead[] }>('/leads?size=200&sort=createdAt,desc').then(r => r.content);

export const searchLeads = (query: string) =>
  apiFetch<{ content: Lead[] }>(`/leads?search=${encodeURIComponent(query)}&size=5`).then(r => r.content);

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

export const sendTemplate = (data: { templateId: string; leadIds: string[]; channel: string }) =>
  apiFetch<{ sent: number; failed: number }>('/leads/send-template', {
    method: 'POST',
    body: JSON.stringify(data),
  });

export const getActivities = (leadId: string) =>
  apiFetch<{ content: Activity[] }>(`/leads/${leadId}/activities?size=100`).then(r => r.content);

export const createActivity = (leadId: string, data: CreateActivityRequest) =>
  apiFetch<Activity>(`/leads/${leadId}/activities`, { method: 'POST', body: JSON.stringify(data) });

export const completeActivity = (leadId: string, activityId: string) =>
  apiFetch<Activity>(`/leads/${leadId}/activities/${activityId}/complete`, { method: 'PATCH' });

export interface UpdateActivityRequest {
  type: string;
  description: string;
  dueDate?: string;
}

export const updateActivity = (leadId: string, activityId: string, data: UpdateActivityRequest) =>
  apiFetch<Activity>(`/leads/${leadId}/activities/${activityId}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });

export const deleteActivity = (leadId: string, activityId: string) =>
  apiFetch<void>(`/leads/${leadId}/activities/${activityId}`, { method: 'DELETE' });

export interface ConvertLeadRequest {
  tenantName: string;
  billingEmail: string;
  billingNif: string;
  billingPhone?: string;
  billingAddress?: string;
  billingCity?: string;
  sector?: string;
  businessSize?: string;
  setupAmount?: number;
  monthlyAmount?: number;
  portalBaseUrl?: string;
}

export interface AnalyzeNotesRequest {
  notes: string;
}

export interface AnalyzeNotesResponse {
  painPoints: string[];
  recommendedSector: string;
  recommendedSize: string;
  setupAmount: number;
  monthlyAmount: number;
  recommendationPitch: string;
}

export const analyzeNotes = (leadId: string, data: AnalyzeNotesRequest) =>
  apiFetch<AnalyzeNotesResponse>(`/leads/${leadId}/analyze-notes`, { method: 'POST', body: JSON.stringify(data) });

export interface ConvertLeadResult {
  tenantId: string;
  tenantName: string;
  stripeCheckoutUrl: string | null;
  goCardlessRedirectUrl: string | null;
  holdedInvoiceId: string | null;
}

export const convertLead = (leadId: string, data: ConvertLeadRequest) =>
  apiFetch<ConvertLeadResult>(`/leads/${leadId}/convert`, { method: 'POST', body: JSON.stringify(data) });
