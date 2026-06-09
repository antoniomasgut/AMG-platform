'use client';

import { apiFetch } from './api';

export interface AssetResponse {
  id: string;
  originalName: string;
  mimeType: string;
  size: number;
  width: number | null;
  height: number | null;
  url: string;
  thumbnailUrl: string | null;
  createdAt: string;
}

export interface AssetStatsResponse {
  usedBytes: number;
  quotaBytes: number;
  fileCount: number;
}

export type AssetCategory = 'all' | 'images' | 'documents' | 'other';

export function getAssetCategory(mimeType: string): AssetCategory {
  if (mimeType.startsWith('image/')) return 'images';
  if (
    mimeType === 'application/pdf' ||
    mimeType === 'application/msword' ||
    mimeType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' ||
    mimeType === 'application/vnd.ms-excel' ||
    mimeType === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  ) return 'documents';
  return 'other';
}

export async function listAssets(tenantId: string): Promise<AssetResponse[]> {
  return apiFetch<AssetResponse[]>(`/assets/tenant/${tenantId}`);
}

export async function getAssetStats(tenantId: string): Promise<AssetStatsResponse> {
  return apiFetch<AssetStatsResponse>(`/assets/tenant/${tenantId}/stats`);
}

export async function uploadAsset(tenantId: string, file: File): Promise<AssetResponse> {
  const formData = new FormData();
  formData.append('file', file);
  return apiFetch<AssetResponse>('/assets/upload', {
    method: 'POST',
    body: formData,
    headers: {},
    skipAuth: false,
  });
}

export async function deleteAsset(assetId: string): Promise<void> {
  return apiFetch<void>(`/assets/${assetId}`, { method: 'DELETE' });
}
