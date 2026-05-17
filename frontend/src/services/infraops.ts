import { apiFetch } from './api';

export interface InfraStatus {
  cpuPercent: number;
  ramPercent: number;
  diskPercent: number;
  status: string;
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
  const params = hours ? `?hours=${hours}` : '';
  return apiFetch<InfraMetric[]>(`/infraops/metrics${params}`);
};

export const getRecommendations = () =>
  apiFetch<Recommendation[]>('/infraops/recommendations');
