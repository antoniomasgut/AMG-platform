import { apiFetch } from './api';

export type ContentPillar = 'NOVELTY' | 'COMBINE' | 'SHOP' | 'SOCIAL_PROOF';
export type ContentItemStatus =
  | 'PLANNED' | 'PHOTO_REQUESTED' | 'PHOTO_RECEIVED'
  | 'AWAITING_APPROVAL' | 'PUBLISHED' | 'FAILED' | 'SKIPPED';
export type ContentLanguage = 'ca' | 'es' | 'en' | 'de';

export interface ContentPlanItem {
  id: string;
  weekNumber: number;
  pillar: ContentPillar;
  briefText: string | null;
  exampleText: string | null;
  networks: string | null;
  contentLanguage: string | null;
  photoDeadline: string | null;
  targetPublishDate: string | null;
  status: ContentItemStatus;
  mediaUrl: string | null;
  caption: string | null;
  error: string | null;
}

export interface ContentPlan {
  id: string;
  tenantId: string;
  period: string;
  status: 'DRAFT' | 'ACTIVE' | 'DONE';
  contentLanguage: string | null;
  notes: string | null;
  items: ContentPlanItem[];
}

export interface CreatePlanRequest {
  period: string;
  contentLanguage?: string;
  generate?: boolean;
  notes?: string;
}

const BASE = '/api/v1/content-plans';

export function listPlans(tenantId: string): Promise<ContentPlan[]> {
  return apiFetch(`${BASE}/tenants/${tenantId}`);
}

export function createPlan(tenantId: string, body: CreatePlanRequest): Promise<ContentPlan> {
  return apiFetch(`${BASE}/tenants/${tenantId}`, { method: 'POST', body: JSON.stringify(body) });
}

export function getPlan(planId: string): Promise<ContentPlan> {
  return apiFetch(`${BASE}/${planId}`);
}

export function generatePlan(planId: string): Promise<ContentPlan> {
  return apiFetch(`${BASE}/${planId}/generate`, { method: 'POST' });
}

export function activatePlan(planId: string): Promise<ContentPlan> {
  return apiFetch(`${BASE}/${planId}/activate`, { method: 'POST' });
}

export function deletePlan(planId: string): Promise<void> {
  return apiFetch(`${BASE}/${planId}`, { method: 'DELETE' });
}

export function updateItem(itemId: string, body: Partial<ContentPlanItem>): Promise<ContentPlan> {
  return apiFetch(`${BASE}/items/${itemId}`, { method: 'PUT', body: JSON.stringify(body) });
}

export function getPendingItems(tenantId: string): Promise<ContentPlanItem[]> {
  return apiFetch(`${BASE}/tenants/${tenantId}/pending`);
}

export function uploadItemPhoto(itemId: string, file: File): Promise<ContentPlan> {
  const form = new FormData();
  form.append('file', file);
  return apiFetch(`${BASE}/items/${itemId}/photo`, { method: 'POST', body: form });
}

export function getDefaultLanguage(tenantId: string): Promise<{ language: string }> {
  return apiFetch(`${BASE}/tenants/${tenantId}/default-language`);
}

export function setDefaultLanguage(tenantId: string, language: string): Promise<{ language: string }> {
  return apiFetch(`${BASE}/tenants/${tenantId}/default-language`, {
    method: 'PUT',
    body: JSON.stringify({ language }),
  });
}
