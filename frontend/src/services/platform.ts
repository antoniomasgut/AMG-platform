import { apiFetch } from './api';

export interface PlatformRecommendation {
  service: string;
  title: string;
  description: string;
  impact: 'HIGH' | 'MEDIUM' | 'LOW';
  category: 'CLIENT_CONNECTIVITY' | 'UPGRADE_OFFER' | 'COST_OPTIMIZATION';
}

/** Retorna el tenantId de la plataforma AMG (configurat com a PLATFORM_TENANT_ID) */
export const getPlatformSelf = () =>
  apiFetch<{ tenantId: string }>('/platform/self');

/** Problemes de connectivitat i qualitat que afecten directament el client */
export const getClientRecommendations = (tenantId: string) =>
  apiFetch<PlatformRecommendation[]>(`/platform/recommendations/${tenantId}`);

/** Ofertes d'upgrade i optimitzacions de cost — només per a admins */
export const getAdminInsights = (tenantId: string) =>
  apiFetch<PlatformRecommendation[]>(`/platform/insights/${tenantId}`);
