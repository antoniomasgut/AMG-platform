import { apiFetch } from './api';

export interface ServiceHealthDto {
  serviceName: string; status: 'UP' | 'DOWN' | 'DEGRADED';
  responseTimeMs: number | null; errorMessage: string | null; checkedAt: string;
}

export interface HealthResponse {
  status: 'UP' | 'DOWN' | 'DEGRADED';
  services: ServiceHealthDto[];
  checkedAt: string;
}

export interface AllHealthResponse extends HealthResponse {
  tenantSites: ServiceHealthDto[];
}

export interface IncidentResponse {
  id: string; serviceName: string; severity: 'CRITICAL' | 'WARNING' | 'INFO';
  status: 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED';
  title: string; description: string | null;
  startedAt: string; resolvedAt: string | null; durationSeconds: number | null;
}

export interface CurrentStatus { services: number; up: number; degraded: number; down: number; }
export interface OpsDashboard {
  currentStatus: CurrentStatus; openIncidents: number;
  todayIncidents: number; avgResponseTimeMs: number;
  lastBackup: string | null; uptimePercentage: number;
}

export interface BackupResponse {
  id: string; type: string; status: string;
  description: string | null; startedAt: string;
  completedAt: string | null; filePath: string | null;
  sizeBytes: number | null;
}

export interface LogEntry {
  timestamp: string; level: string; message: string; service: string;
}

export const getOpsHealth = () =>
  apiFetch<HealthResponse>('/ops/health');

export const getAllHealth = () =>
  apiFetch<AllHealthResponse>('/ops/health/all');

export const getOpsDashboard = () =>
  apiFetch<OpsDashboard>('/ops/dashboard');

export const listIncidents = (status?: string, serviceName?: string) => {
  const params = new URLSearchParams();
  if (status) params.set('status', status);
  if (serviceName) params.set('serviceName', serviceName);
  return apiFetch<IncidentResponse[]>(`/ops/incidents?${params}`);
};

export const acknowledgeIncident = (id: string) =>
  apiFetch<IncidentResponse>(`/ops/incidents/${id}/acknowledge`, { method: 'PUT' });

export const resolveIncident = (id: string) =>
  apiFetch<IncidentResponse>(`/ops/incidents/${id}/resolve`, { method: 'PUT' });

export const startBackup = (type: string, description?: string) =>
  apiFetch<BackupResponse>('/ops/backups', {
    method: 'POST',
    body: JSON.stringify({ type, description }),
  });

export const listBackups = (type?: string) => {
  const params = new URLSearchParams();
  if (type) params.set('type', type);
  return apiFetch<BackupResponse[]>(`/ops/backups?${params}`);
};

export const getLogs = (serviceName: string, limit = 100) =>
  apiFetch<LogEntry[]>(`/ops/logs/${serviceName}?limit=${limit}`);
