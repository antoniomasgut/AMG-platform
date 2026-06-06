import { apiFetch } from './api';

export interface ConfigStatus {
  key: string;
  label: string;
  description: string;
  category: string;
  secret: boolean;
  type: 'secret' | 'string' | 'url' | 'number' | 'boolean' | 'json';
  configured: boolean;
  source: 'ENV' | 'DB' | 'MISSING' | 'DEFAULT';
}

export interface AuditEntry {
  id: string;
  configKey: string;
  action: string;
  previousValue: string | null;
  userId: string | null;
  userEmail: string | null;
  ip: string | null;
  changedAt: string;
}

export const getSystemConfig = () =>
  apiFetch<ConfigStatus[]>('/admin/system-config');

export const getSystemConfigDetail = (key: string) =>
  apiFetch<ConfigStatus>(`/admin/system-config/${key}`);

export const setSystemConfig = (key: string, value: string) =>
  apiFetch<{ status: string; key: string }>(`/admin/system-config/${key}`, {
    method: 'PUT',
    body: JSON.stringify({ value }),
  });

export const deleteSystemConfig = (key: string) =>
  apiFetch<void>(`/admin/system-config/${key}`, { method: 'DELETE' });

export const testSystemConfig = (key: string) =>
  apiFetch<{ ok: boolean; message: string }>(`/admin/system-config/${key}/test`, { method: 'POST' });

export const getAuditLog = (key: string) =>
  apiFetch<AuditEntry[]>(`/admin/system-config/${key}/audit`);

export const TESTABLE_KEYS = new Set([
  'BREVO_API_KEY',
  'ANTHROPIC_API_KEY',
  'TELEGRAM_BOT_TOKEN',
  'GOOGLE_PLACES_API_KEY',
]);
