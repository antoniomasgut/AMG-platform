'use client';

import { apiFetch } from './api';

export interface GoogleStatus {
  connected: boolean;
  email: string;
  driveEnabled: boolean;
  gmailEnabled: boolean;
  calendarEnabled: boolean;
  sheetsEnabled: boolean;
  driveFolderId?: string | null;
}

export interface AuthUrlResponse {
  authUrl: string;
  stateToken: string;
}

export interface ModuleConfigRequest {
  driveEnabled: boolean;
  gmailEnabled: boolean;
  calendarEnabled: boolean;
  sheetsEnabled: boolean;
  driveFolderId?: string | null;
}

export interface DriveFileItem {
  id: string;
  name: string;
  mimeType: string;
  size: number;
  createdTime: string;
  webViewLink: string;
}

export interface CalendarEventItem {
  id: string;
  summary: string;
  description?: string;
  start: string;
  end: string;
  htmlLink: string;
}

export async function getGoogleStatus(tenantId: string): Promise<GoogleStatus> {
  return apiFetch<GoogleStatus>(`/tenants/${tenantId}/google`);
}

export async function getAuthUrl(tenantId: string, modules: string[], redirectUri: string): Promise<AuthUrlResponse> {
  return apiFetch<AuthUrlResponse>(`/tenants/${tenantId}/google/auth-url`, {
    method: 'POST',
    body: JSON.stringify({ modules, redirectUri }),
  });
}

export async function updateModules(tenantId: string, config: ModuleConfigRequest): Promise<void> {
  return apiFetch<void>(`/tenants/${tenantId}/google/modules`, {
    method: 'PUT',
    body: JSON.stringify(config),
  });
}

export async function testConnection(tenantId: string): Promise<string> {
  return apiFetch<string>(`/tenants/${tenantId}/google/test`, { method: 'POST' });
}

export async function disconnectTenant(tenantId: string): Promise<void> {
  return apiFetch<void>(`/tenants/${tenantId}/google/disconnect`, { method: 'DELETE' });
}

export async function sendMail(tenantId: string, to: string, subject: string, body: string): Promise<string> {
  return apiFetch<string>(`/tenants/${tenantId}/google/send`, {
    method: 'POST',
    body: JSON.stringify({ to, subject, body }),
  });
}

export async function getDriveFiles(tenantId: string, folderId?: string): Promise<DriveFileItem[]> {
  const qs = folderId ? `?folderId=${folderId}` : '';
  return apiFetch<DriveFileItem[]>(`/tenants/${tenantId}/google/drive${qs}`);
}

export async function getCalendarEvents(tenantId: string): Promise<CalendarEventItem[]> {
  return apiFetch<CalendarEventItem[]>(`/tenants/${tenantId}/google/calendar/events`);
}

// ── Drive AMG admin ───────────────────────────────────────────

export async function shareDriveAMGFolder(email: string, role: 'reader' | 'writer'): Promise<{ message: string }> {
  return apiFetch<{ message: string }>('/admin/drive/share', {
    method: 'POST',
    body: JSON.stringify({ email, role }),
  });
}

export async function getDriveAMGStatus(): Promise<{ saConfigured: boolean }> {
  return apiFetch<{ saConfigured: boolean }>('/admin/drive/status');
}
