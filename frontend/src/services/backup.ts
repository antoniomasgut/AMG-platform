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
  lastBackupAt?: string;
  nextScheduledAt?: string;
  successRate: number;
}

export const triggerBackup = (type: string) =>
  apiFetch<BackupTask>('/ops/backups', { method: 'POST', body: JSON.stringify({ type }) });

export const listBackups = () =>
  apiFetch<BackupTask[]>('/ops/backups');

export const getBackupDashboard = () =>
  apiFetch<BackupDashboard>('/ops/backups/dashboard');
