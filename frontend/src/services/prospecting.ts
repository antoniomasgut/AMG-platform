import { apiFetch } from './api';

export interface Campaign {
  id: string;
  name: string;
  sector: string;
  city: string;
  status: string;
  maxResults: number;
  prospectsFound: number;
  createdAt: string;
}

export interface Prospect {
  id: string;
  businessName: string;
  phone?: string;
  email?: string;
  address?: string;
  status: string;
}

export interface CreateCampaignRequest {
  name: string;
  sector: string;
  city: string;
  maxResults?: number;
}

export const getCampaigns = () =>
  apiFetch<Campaign[]>('/prospecting/campaigns');

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
