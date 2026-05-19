import { apiFetch } from './api';

export interface ConfigStatus {
  key: string;
  label: string;
  description: string;
  category: string;
  secret: boolean;
  configured: boolean;
  source: 'ENV' | 'DB' | 'MISSING';
}

export const getSystemConfig = () =>
  apiFetch<ConfigStatus[]>('/admin/system-config');

export const setSystemConfig = (key: string, value: string) =>
  apiFetch<{ status: string; key: string }>(`/admin/system-config/${key}`, {
    method: 'PUT',
    body: JSON.stringify({ value }),
  });

export const deleteSystemConfig = (key: string) =>
  apiFetch<void>(`/admin/system-config/${key}`, { method: 'DELETE' });
