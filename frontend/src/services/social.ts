import { apiFetch } from './api';

export interface SocialPost {
  id: string;
  tenantId: string;
  network: 'INSTAGRAM' | 'FACEBOOK' | 'GOOGLE_BUSINESS';
  postType: string;
  caption: string | null;
  mediaUrl: string | null;
  externalPostId: string | null;
  externalPostUrl: string | null;
  status: 'DRAFT' | 'SCHEDULED' | 'PUBLISHED' | 'FAILED' | 'CANCELLED';
  scheduledAt: string | null;
  publishedAt: string | null;
  errorMessage: string | null;
  createdAt: string;
}

export interface MetaStatus {
  connected: boolean;
  facebookPageId?: string;
  instagramAccountId?: string;
  pagesManagedPosts?: boolean;
  igContentPublish?: boolean;
  tokenValid?: boolean;
  tokenExpiresAt?: string;
}

export interface MetaConfigRequest {
  facebookPageId?: string;
  instagramAccountId?: string;
  pageAccessToken?: string;
  tokenExpiresAt?: string | null;
  pagesManagedPosts?: boolean;
  igContentPublish?: boolean;
}

export async function listSocialPosts(tenantId: string): Promise<SocialPost[]> {
  return apiFetch(`/api/v1/social/tenants/${tenantId}/posts`);
}

export async function listSocialPostsByNetwork(tenantId: string, network: string): Promise<SocialPost[]> {
  return apiFetch(`/api/v1/social/tenants/${tenantId}/posts/${network}`);
}

export async function cancelSocialPost(tenantId: string, postId: string): Promise<void> {
  return apiFetch(`/api/v1/social/tenants/${tenantId}/posts/${postId}`, { method: 'DELETE' });
}

export interface SocialFeatures {
  commentsToTelegram: boolean;
  weeklyAnalytics: boolean;
  aiSuggestions: boolean;
  autoPostReviews: boolean;
  dmsToInbox: boolean;
}

export interface SocialFeaturesStatus extends SocialFeatures {
  enabled: boolean;
}

export async function getSocialFeatures(tenantId: string): Promise<SocialFeaturesStatus> {
  return apiFetch(`/api/v1/social/tenants/${tenantId}/features`);
}

export async function updateSocialFeatures(tenantId: string, features: SocialFeatures): Promise<void> {
  return apiFetch(`/api/v1/social/tenants/${tenantId}/features`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(features),
  });
}

export async function getMetaStatus(tenantId: string): Promise<MetaStatus> {
  return apiFetch(`/api/v1/social/tenants/${tenantId}/meta/status`);
}

export async function saveMetaConfig(tenantId: string, config: MetaConfigRequest): Promise<void> {
  return apiFetch(`/api/v1/social/tenants/${tenantId}/meta/config`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(config),
  });
}

export const NETWORK_LABELS: Record<string, string> = {
  INSTAGRAM: 'Instagram',
  FACEBOOK:  'Facebook',
  GOOGLE_BUSINESS: 'Google Business',
};

export const NETWORK_ICONS: Record<string, string> = {
  INSTAGRAM:       '📷',
  FACEBOOK:        '👥',
  GOOGLE_BUSINESS: '🗺',
};

export const STATUS_LABELS: Record<string, string> = {
  DRAFT:      'Esborrany',
  SCHEDULED:  'Programat',
  PUBLISHED:  'Publicat',
  FAILED:     'Error',
  CANCELLED:  'Cancel·lat',
};

export const STATUS_COLORS: Record<string, string> = {
  DRAFT:      'bg-gray-100 text-gray-700',
  SCHEDULED:  'bg-blue-100 text-blue-700',
  PUBLISHED:  'bg-green-100 text-green-700',
  FAILED:     'bg-red-100 text-red-700',
  CANCELLED:  'bg-gray-100 text-gray-500',
};
