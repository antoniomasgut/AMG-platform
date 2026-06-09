import { apiFetch } from './api';

export interface MetaAdsGlobalSummary { configured: number; enabled: number; }
export const getMetaAdsGlobalSummary = () =>
  apiFetch<MetaAdsGlobalSummary>('/meta-ads/global/summary');

export interface MetaAdsConfig {
  tenantId: string;
  adAccountId: string | null;
  hasAccessToken: boolean;
  enabled: boolean;
  lastSyncAt: string | null;
  updatedAt: string | null;
}

export interface CampaignRow {
  campaignId: string;
  campaignName: string;
  spend: number;
  impressions: number;
  clicks: number;
  leads: number;
  cpl: number | null;
}

export interface CampaignStats {
  campaigns: CampaignRow[];
  totalSpend: number;
  totalLeadsFromAds: number;
  avgCpl: number | null;
  period: string;
}

export const getMetaAdsConfig = (tenantId: string) =>
  apiFetch<MetaAdsConfig>(`/meta-ads/tenants/${tenantId}/config`);

export const saveMetaAdsConfig = (tenantId: string, data: { adAccountId: string; accessToken?: string; enabled: boolean }) =>
  apiFetch<MetaAdsConfig>(`/meta-ads/tenants/${tenantId}/config`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });

export const getMetaAdsStats = (tenantId: string) =>
  apiFetch<CampaignStats>(`/meta-ads/tenants/${tenantId}/stats`);

export const syncMetaAds = (tenantId: string) =>
  apiFetch<string>(`/meta-ads/tenants/${tenantId}/sync`, { method: 'POST' });

// ---- Module 36 — Campaign Management ----

export interface GeoLocation {
  key: string;
  name: string;
  radius?: number;
  distanceUnit?: string;
}

export interface Interest {
  id: string;
  name: string;
}

export interface AdResponse {
  id: string;
  adSetId: string;
  metaAdId: string | null;
  name: string;
  status: string;
  creativeId: string | null;
  headline: string | null;
  body: string | null;
  callToAction: string | null;
  linkUrl: string | null;
  metaImageHash: string | null;
  metaError: string | null;
  createdAt: string;
}

export interface AdSetResponse {
  id: string;
  campaignId: string;
  metaAdSetId: string | null;
  name: string;
  status: string;
  dailyBudget: number | null;
  optimizationGoal: string | null;
  ageMin: number | null;
  ageMax: number | null;
  genders: string | null;
  publisherPlatforms: string | null;
  metaError: string | null;
  ads: AdResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface Campaign {
  id: string;
  metaCampaignId: string | null;
  name: string;
  objective: string;
  status: string;
  dailyBudget: number | null;
  lifetimeBudget: number | null;
  startTime: string | null;
  stopTime: string | null;
  metaError: string | null;
  adSets: AdSetResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface PublishProgress {
  status: string;
  steps: string[];
  errorMessage: string | null;
}

export interface TargetingItem {
  id: string;
  name: string;
  audienceSize: number | null;
}

export interface ImageUpload {
  hash: string;
  url: string;
}

export const listCampaigns = (tenantId: string) =>
  apiFetch<Campaign[]>(`/meta-ads/tenants/${tenantId}/campaigns`);

export const getCampaign = (tenantId: string, campaignId: string) =>
  apiFetch<Campaign>(`/meta-ads/tenants/${tenantId}/campaigns/${campaignId}`);

export const createCampaign = (tenantId: string, data: {
  name: string; objective: string;
  dailyBudget?: number; lifetimeBudget?: number;
  startTime?: string; stopTime?: string;
}) =>
  apiFetch<Campaign>(`/meta-ads/tenants/${tenantId}/campaigns`, {
    method: 'POST', body: JSON.stringify(data),
  });

export const updateCampaign = (tenantId: string, campaignId: string, data: Partial<{
  name: string; objective: string;
  dailyBudget: number; lifetimeBudget: number;
  startTime: string; stopTime: string;
}>) =>
  apiFetch<Campaign>(`/meta-ads/tenants/${tenantId}/campaigns/${campaignId}`, {
    method: 'PATCH', body: JSON.stringify(data),
  });

export const deleteCampaign = (tenantId: string, campaignId: string) =>
  apiFetch<void>(`/meta-ads/tenants/${tenantId}/campaigns/${campaignId}`, { method: 'DELETE' });

export const duplicateCampaign = (tenantId: string, campaignId: string) =>
  apiFetch<Campaign>(`/meta-ads/tenants/${tenantId}/campaigns/${campaignId}/duplicate`, { method: 'POST' });

export const publishCampaign = (tenantId: string, campaignId: string) =>
  apiFetch<PublishProgress>(`/meta-ads/tenants/${tenantId}/campaigns/${campaignId}/publish`, { method: 'POST' });

export const pauseCampaign = (tenantId: string, campaignId: string) =>
  apiFetch<Campaign>(`/meta-ads/tenants/${tenantId}/campaigns/${campaignId}/pause`, { method: 'POST' });

export const resumeCampaign = (tenantId: string, campaignId: string) =>
  apiFetch<Campaign>(`/meta-ads/tenants/${tenantId}/campaigns/${campaignId}/resume`, { method: 'POST' });

export const archiveCampaign = (tenantId: string, campaignId: string) =>
  apiFetch<Campaign>(`/meta-ads/tenants/${tenantId}/campaigns/${campaignId}/archive`, { method: 'POST' });

export const createAdSet = (tenantId: string, campaignId: string, data: {
  name: string; dailyBudget?: number; optimizationGoal?: string; billingEvent?: string;
  ageMin?: number; ageMax?: number; genders?: string;
  geoLocations?: GeoLocation[]; interests?: Interest[];
  publisherPlatforms?: string; startTime?: string; stopTime?: string;
}) =>
  apiFetch<AdSetResponse>(`/meta-ads/tenants/${tenantId}/campaigns/${campaignId}/adsets`, {
    method: 'POST', body: JSON.stringify(data),
  });

export const updateAdSet = (tenantId: string, adSetId: string, data: Partial<{
  name: string; dailyBudget: number; ageMin: number; ageMax: number;
  genders: string; geoLocations: GeoLocation[]; interests: Interest[];
  publisherPlatforms: string;
}>) =>
  apiFetch<AdSetResponse>(`/meta-ads/tenants/${tenantId}/adsets/${adSetId}`, {
    method: 'PATCH', body: JSON.stringify(data),
  });

export const deleteAdSet = (tenantId: string, adSetId: string) =>
  apiFetch<void>(`/meta-ads/tenants/${tenantId}/adsets/${adSetId}`, { method: 'DELETE' });

export const createAd = (tenantId: string, adSetId: string, data: {
  name: string; headline?: string; body?: string; description?: string;
  callToAction?: string; linkUrl?: string; metaImageHash?: string; imageAssetId?: string;
}) =>
  apiFetch<AdResponse>(`/meta-ads/tenants/${tenantId}/adsets/${adSetId}/ads`, {
    method: 'POST', body: JSON.stringify(data),
  });

export const deleteAd = (tenantId: string, adId: string) =>
  apiFetch<void>(`/meta-ads/tenants/${tenantId}/ads/${adId}`, { method: 'DELETE' });

export const searchInterests = (tenantId: string, q: string) =>
  apiFetch<TargetingItem[]>(`/meta-ads/tenants/${tenantId}/targeting/interests?q=${encodeURIComponent(q)}`);

export const searchLocations = (tenantId: string, q: string) =>
  apiFetch<TargetingItem[]>(`/meta-ads/tenants/${tenantId}/targeting/locations?q=${encodeURIComponent(q)}`);

export interface GenerateImageRequest {
  prompt: string;
  format: 'FEED' | 'SQUARE' | 'STORY' | 'BANNER';
  style: 'REALISTIC' | 'ILLUSTRATED' | 'MINIMAL' | 'CINEMATIC';
}

export const generateImage = (tenantId: string, data: GenerateImageRequest) =>
  apiFetch<ImageUpload>(`/meta-ads/tenants/${tenantId}/creatives/generate-image`, {
    method: 'POST',
    body: JSON.stringify(data),
  });

export const uploadImage = async (tenantId: string, file: File): Promise<ImageUpload> => {
  const form = new FormData();
  form.append('file', file);
  const token = typeof window !== 'undefined' ? sessionStorage.getItem('access_token') : null;
  const res = await fetch(`/api/v1/meta-ads/tenants/${tenantId}/creatives/upload-image`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: form,
  });
  if (!res.ok) throw new Error(await res.text());
  return res.json();
};
