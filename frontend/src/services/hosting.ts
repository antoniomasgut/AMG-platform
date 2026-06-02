'use client';

import { apiFetch } from './api';

export type WebsiteStatus = 'PENDING_REVIEW' | 'APPROVED' | 'DEPLOYING' | 'ACTIVE' | 'SUSPENDED' | 'REJECTED';
export type WebsiteType = 'STATIC' | 'CONTAINER';

export interface WebSiteResponse {
  id: string;
  tenantId: string;
  type: WebsiteType;
  status: WebsiteStatus;
  domain: string | null;
  containerName: string | null;
  storageBytes: number | null;
  clientNotes: string | null;
  reviewNotes: string | null;
  reviewedAt: string | null;
  deployedAt: string | null;
  createdAt: string;
}

export async function listSites(tenantId: string): Promise<WebSiteResponse[]> {
  return apiFetch<WebSiteResponse[]>(`/hosting/tenants/${tenantId}/sites`);
}

export async function requestStaticSite(tenantId: string, file: File, domain: string): Promise<WebSiteResponse> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('domain', domain);
  return apiFetch<WebSiteResponse>(`/hosting/tenants/${tenantId}/sites/static`, {
    method: 'POST',
    body: formData,
    headers: {},
  });
}

export async function updateStaticSite(tenantId: string, siteId: string, file: File): Promise<WebSiteResponse> {
  const formData = new FormData();
  formData.append('file', file);
  return apiFetch<WebSiteResponse>(`/hosting/tenants/${tenantId}/sites/${siteId}/static`, {
    method: 'PUT',
    body: formData,
    headers: {},
  });
}

export async function exportSite(tenantId: string, siteId: string): Promise<void> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
  const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? '';
  const res = await fetch(`${apiUrl}/api/v1/hosting/tenants/${tenantId}/sites/${siteId}/export`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) throw new Error('Error descarregant el ZIP');
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `web-${siteId}.zip`;
  a.click();
  URL.revokeObjectURL(url);
}

export async function requestContainerSite(
  tenantId: string,
  compose: File,
  envExample: File | undefined,
  domain: string,
  description?: string,
): Promise<WebSiteResponse> {
  const formData = new FormData();
  formData.append('compose', compose);
  if (envExample) formData.append('envExample', envExample);
  formData.append('domain', domain);
  if (description) formData.append('description', description);
  return apiFetch<WebSiteResponse>(`/hosting/tenants/${tenantId}/sites/container`, {
    method: 'POST',
    body: formData,
    headers: {},
  });
}

export async function listAllAdminSites(): Promise<WebSiteResponse[]> {
  return apiFetch<WebSiteResponse[]>('/hosting/admin/sites');
}

export async function listPendingSites(): Promise<WebSiteResponse[]> {
  return apiFetch<WebSiteResponse[]>('/hosting/admin/sites/pending');
}

export async function approveSite(siteId: string, notes?: string): Promise<WebSiteResponse> {
  return apiFetch<WebSiteResponse>(`/hosting/admin/sites/${siteId}/approve`, {
    method: 'POST',
    body: JSON.stringify({ notes }),
  });
}

export async function rejectSite(siteId: string, notes: string): Promise<WebSiteResponse> {
  return apiFetch<WebSiteResponse>(`/hosting/admin/sites/${siteId}/reject`, {
    method: 'POST',
    body: JSON.stringify({ notes }),
  });
}
