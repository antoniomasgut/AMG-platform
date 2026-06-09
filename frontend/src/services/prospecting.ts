import { apiFetch } from './api';

export interface Campaign {
  id: string;
  name: string;
  sector: string;
  location: string;
  source: string;
  status: string;
  totalFound: number;
  totalExported: number;
  createdAt: string;
  scheduledNextRun?: string | null;
  repeatIntervalDays?: number | null;
}

export interface ProspectSignal {
  code: string;
  label: string;
  pitch: string;
  tone: 'warning' | 'danger' | 'opportunity' | 'info' | 'neutral';
  points: number;
}

export interface Prospect {
  id: string;
  campaignId: string;
  name: string;
  description?: string;
  sector?: string;
  address?: string;
  city?: string;
  postalCode?: string;
  phone?: string;
  email?: string;
  website?: string;
  instagram?: string;
  googleRating?: number;
  googleReviews?: number;
  googlePlaceId?: string;
  hasWebsite?: boolean;
  hasWhatsapp?: boolean;
  status: string;
  source?: string;
  notes?: string;
  leadId?: string;
  createdAt?: string;
  score?: number | null;
  reviews?: string[];
  signals?: ProspectSignal[];
}

export type ProspectSource = 'GOOGLE_MAPS' | 'INSTAGRAM' | 'PAGINAS_AMARILLAS' | 'MANUAL';

export interface CreateCampaignRequest {
  name: string;
  sector: string;
  location: string;
  source: ProspectSource;
  searchParams?: string;
  notes?: string;
}

export interface CampaignsPage {
  content: Campaign[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const getCampaigns = (params?: { sector?: string; location?: string; status?: string; page?: number; size?: number }) => {
  const q = new URLSearchParams();
  if (params?.sector)   q.set('sector', params.sector);
  if (params?.location) q.set('location', params.location);
  if (params?.status)   q.set('status', params.status);
  if (params?.page != null) q.set('page', String(params.page));
  if (params?.size != null) q.set('size', String(params.size));
  return apiFetch<CampaignsPage>(`/prospecting/campaigns${q.toString() ? `?${q}` : ''}`);
};

export const getCampaign = (id: string) =>
  apiFetch<Campaign>(`/prospecting/campaigns/${id}`);

export const createCampaign = (data: CreateCampaignRequest) =>
  apiFetch<Campaign>('/prospecting/campaigns', { method: 'POST', body: JSON.stringify(data) });

export const deleteCampaign = (id: string) =>
  apiFetch<void>(`/prospecting/campaigns/${id}`, { method: 'DELETE' });

export const runCampaign = (id: string) =>
  apiFetch<Campaign>(`/prospecting/campaigns/${id}/run`, { method: 'POST' });

export const getCampaignProspects = (id: string) =>
  apiFetch<Prospect[]>(`/prospecting/campaigns/${id}/prospects`);

export const exportProspect = (id: string) =>
  apiFetch<void>(`/prospecting/prospects/${id}/export`, { method: 'POST' });

export const cloneCampaign = (id: string) =>
  apiFetch<Campaign>(`/prospecting/campaigns/${id}/clone`, { method: 'POST' });

export const enrichAllProspects = (id: string) =>
  apiFetch<{ enriched: number }>(`/prospecting/campaigns/${id}/enrich-all`, { method: 'POST' });

export const enrichProspect = (id: string) =>
  apiFetch<Prospect>(`/prospecting/prospects/${id}/enrich`, { method: 'POST' });

export const exportContactableProspects = (id: string) =>
  apiFetch<{ exported: number; skipped: number }>(`/prospecting/campaigns/${id}/export-contactable`, { method: 'POST' });

export interface UpdateProspectPayload {
  status?: string;
  notes?: string;
  name?: string;
  phone?: string;
  email?: string;
  website?: string;
  address?: string;
  city?: string;
}

export const updateProspect = (id: string, payload: UpdateProspectPayload) =>
  apiFetch<Prospect>(`/prospecting/prospects/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });

export const exportQualifiedProspects = (id: string) =>
  apiFetch<{ exported: number; skipped: number }>(`/prospecting/campaigns/${id}/export-qualified`, { method: 'POST' });

export const generateOutreach = (id: string, channel: 'email' | 'whatsapp') =>
  apiFetch<{ message: string }>(`/prospecting/prospects/${id}/generate-outreach?channel=${channel}`, { method: 'POST' });

export const scoreProspects = (id: string) =>
  apiFetch<Prospect[]>(`/prospecting/campaigns/${id}/score`, { method: 'POST' });

export const qualifyTop = (id: string, topN = 10) =>
  apiFetch<{ qualified: number }>(`/prospecting/campaigns/${id}/qualify-top?topN=${topN}`, { method: 'POST' });

export const qualifyByMinScore = (id: string, minScore: number) =>
  apiFetch<{ qualified: number }>(`/prospecting/campaigns/${id}/qualify-min-score?minScore=${minScore}`, { method: 'POST' });

export const scheduleCampaign = (id: string, nextRun: string, repeatDays: number) =>
  apiFetch<Campaign>(`/prospecting/campaigns/${id}/schedule?nextRun=${encodeURIComponent(nextRun)}&repeatDays=${repeatDays}`, { method: 'POST' });

export const unscheduleCampaign = (id: string) =>
  apiFetch<Campaign>(`/prospecting/campaigns/${id}/unschedule`, { method: 'POST' });
