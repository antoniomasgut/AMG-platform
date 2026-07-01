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
  // Spec 12 v2
  hasSsl?: boolean;
  isResponsive?: boolean;
  hasAnalytics?: boolean;
  hasPixel?: boolean;
  hasGtm?: boolean;
  hasChatWidget?: boolean;
  hasBookingSystem?: boolean;
  hasContactForm?: boolean;
  hasFaq?: boolean;
  hasClearCta?: boolean;
  techStack?: string;
  webLoadMs?: number;
  cmsDetected?: string;
  facebookUrl?: string;
  linkedinUrl?: string;
  tiktokUrl?: string;
  youtubeUrl?: string;
  hasFacebook?: boolean;
  hasLinkedin?: boolean;
  hasTiktok?: boolean;
  prospectTier?: 'DISCARD' | 'REVIEW' | 'DEMO' | 'PRIORITY';
  scoreBreakdown?: string;
  aiReport?: string;
  aiPitch?: string;
  demoUrl?: string;
  widgetCode?: string;
  webAnalyzedAt?: string;
  aiAnalyzedAt?: string;
  // v2.1 tracking
  pitchSentAt?: string;
  pitchOpenedAt?: string;
  demoOpenedAt?: string;
  followupCount?: number;
  lastFollowupAt?: string;
}

export interface ProspectingDashboard {
  totalCampaigns: number;
  totalProspects: number;
  byTier: Record<string, number>;
  byStatus: Record<string, number>;
  avgScore: number;
  topCampaigns: Array<{ id: string; name: string; total: number; priority: number; demo: number }>;
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

// ── Spec 12 v2 ────────────────────────────────────────────────────────────────
export const analyzeWebProspect = (id: string) =>
  apiFetch<Prospect>(`/prospecting/prospects/${id}/analyze-web`, { method: 'POST' });

export const analyzeAllWebCampaign = (id: string) =>
  apiFetch<{ analyzed: number }>(`/prospecting/campaigns/${id}/analyze-all`, { method: 'POST' });

export const generateAiReport = (id: string) =>
  apiFetch<Prospect>(`/prospecting/prospects/${id}/ai-report`, { method: 'POST' });

export const getWidgetCode = (id: string) =>
  apiFetch<{ code: string }>(`/prospecting/prospects/${id}/widget-code`);

export const detectChannels = (id: string) =>
  apiFetch<Record<string, string>>(`/prospecting/prospects/${id}/channels`);

export const getCampaignDashboard = (id: string) =>
  apiFetch<Record<string, unknown>>(`/prospecting/campaigns/${id}/dashboard`);

export const getGlobalDashboard = () =>
  apiFetch<Record<string, unknown>>(`/prospecting/dashboard`);

// ── Spec 12 v2.1 ──────────────────────────────────────────────────────────────
export const generatePitch = (id: string) =>
  apiFetch<Prospect>(`/prospecting/prospects/${id}/generate-pitch`, { method: 'POST' });

export const generateDemo = (id: string) =>
  apiFetch<{ demoUrl: string }>(`/prospecting/prospects/${id}/generate-demo`, { method: 'POST' });

export const importCsv = (campaignId: string, file: File) => {
  const form = new FormData();
  form.append('file', file);
  return apiFetch<{ imported: number; skipped: number }>(
    `/prospecting/campaigns/${campaignId}/import-csv`,
    { method: 'POST', body: form, headers: {} }
  );
};

export interface DuplicateGroup {
  matchType: 'PHONE' | 'PLACE_ID';
  prospects: Array<{ id: string; name: string; campaignId: string; phone?: string; googlePlaceId?: string }>;
}

export const getDuplicates = () =>
  apiFetch<DuplicateGroup[]>('/prospecting/duplicates');
