import { apiFetch } from './api';

export interface BackupTask {
  id: string;
  type: string;
  status: string;
  startedAt: string;
  completedAt?: string;
  sizeBytes?: number;
}

export interface BackupDashboard {
  totalBackups: number;
  lastBackup?: string;
  lastBackupStatus?: string;
  nextScheduledBackup?: string;
  scheduledCount: number;
  manualFullCount: number;
  storageUsedBytes?: number;
  retentionDays?: number;
}

export const triggerBackup = (type: string) =>
  apiFetch<BackupTask>('/ops/backups', { method: 'POST', body: JSON.stringify({ type }) });

export const listBackups = () =>
  apiFetch<{ content: BackupTask[] }>('/ops/backups').then(r => r.content);

export const getBackupDashboard = () =>
  apiFetch<BackupDashboard>('/ops/backups/dashboard');
