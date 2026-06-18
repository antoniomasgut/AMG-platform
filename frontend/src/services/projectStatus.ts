import { apiFetch } from './api';

export interface ProjectStatus {
  tenantName: string | null;
  sector: string | null;
  configReceived: boolean;
  configReceivedAt: string | null;
  paymentConfirmed: boolean;
  paymentConfirmedAt: string | null;
  setupCompleted: boolean;
  setupCompletedAt: string | null;
  implementing: boolean;
  active: boolean;
  portalUrl: string;
}

export async function getProjectStatus(token: string): Promise<ProjectStatus> {
  return apiFetch<ProjectStatus>(`/billing/status/${token}`);
}
