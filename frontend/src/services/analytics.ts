import { apiFetch } from './api';

export interface AnalyticsResponse {
  totalLeads: number;
  newLeads7d: number;
  newLeads30d: number;
  wonLeads: number;
  conversionRate: number;
  leadsByStage: Record<string, number>;
  leadsBySource: Record<string, number>;
  totalConversations: number;
  newConversations7d: number;
  pendingApproval: number;
  conversationsByChannel: Record<string, number>;
  totalBudgets: number;
  sentBudgets: number;
  acceptedBudgets: number;
  budgetConversionRate: number;
  avgResponseMinutes: number;
  dailyReport: string;
  weeklyReport: string;
}

export function getAnalytics(tenantId: string): Promise<AnalyticsResponse> {
  return apiFetch(`/api/v1/analytics/tenants/${tenantId}`);
}

export interface ParsedDocumentResponse {
  fileName: string;
  rawText: string;
  extractedName: string | null;
  extractedEmail: string | null;
  extractedPhone: string | null;
  extractedNotes: string | null;
  source: string | null;
  leadCreated: boolean;
  leadId: string | null;
}

export function parseDocument(
  tenantId: string,
  file: File,
  autoCreateLead: boolean
): Promise<ParsedDocumentResponse> {
  const form = new FormData();
  form.append('file', file);
  return apiFetch(`/api/v1/documents/tenants/${tenantId}/parse?autoCreateLead=${autoCreateLead}`, {
    method: 'POST',
    body: form,
  });
}

export interface VisitRecordResponse {
  id: string;
  contactIdentifier: string;
  contactName: string | null;
  visitDate: string;
  treatmentType: string | null;
  notes: string | null;
  nextVisitDue: string | null;
  createdAt: string;
}

export interface VisitRecordRequest {
  contactIdentifier: string;
  contactName?: string;
  visitDate?: string;
  treatmentType?: string;
  notes?: string;
  nextVisitDue?: string;
}

export function listVisits(tenantId: string): Promise<VisitRecordResponse[]> {
  return apiFetch(`/api/v1/visits/tenants/${tenantId}`);
}

export function listVisitsByContact(tenantId: string, identifier: string): Promise<VisitRecordResponse[]> {
  return apiFetch(`/api/v1/visits/tenants/${tenantId}/contacts/${encodeURIComponent(identifier)}`);
}

export function createVisit(tenantId: string, req: VisitRecordRequest): Promise<VisitRecordResponse> {
  return apiFetch(`/api/v1/visits/tenants/${tenantId}`, {
    method: 'POST',
    body: JSON.stringify(req),
  });
}

export function updateVisit(tenantId: string, id: string, req: VisitRecordRequest): Promise<VisitRecordResponse> {
  return apiFetch(`/api/v1/visits/tenants/${tenantId}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(req),
  });
}

export function deleteVisit(tenantId: string, id: string): Promise<void> {
  return apiFetch(`/api/v1/visits/tenants/${tenantId}/${id}`, { method: 'DELETE' });
}
