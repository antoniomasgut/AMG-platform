import { apiFetch } from './api';

export interface InfraStatusDetail {
  percent: number;
  status: string;
  usedMb?: number;
  totalMb?: number;
  usedGb?: number;
  totalGb?: number;
  activeConnections?: number;
  maxConnections?: number;
}

export interface InfraStatus {
  collectedAt: string;
  cpu: InfraStatusDetail;
  ram: InfraStatusDetail;
  disk: InfraStatusDetail;
  database: InfraStatusDetail;
  tenants: { active: number; recommendation: string | null };
  overallStatus: string;
  activeRecommendations: number;
}

export interface InfraMetric {
  timestamp: string;
  cpuPercent: number;
  ramPercent: number;
  diskPercent: number;
}

export interface Recommendation {
  id: string;
  type: string;
  severity: string;
  message: string;
  resolved: boolean;
}

export const getInfraStatus = () =>
  apiFetch<InfraStatus>('/infraops/status');

export const getInfraMetrics = (hours?: number) => {
  const params = hours ? `?hours=${hours}&size=200` : '?size=200';
  return apiFetch<{ content: InfraMetric[] }>(`/infraops/metrics${params}`).then(r => r.content);
};

export const getRecommendations = () =>
  apiFetch<Recommendation[]>('/infraops/recommendations');
