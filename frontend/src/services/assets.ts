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

export async function listAssets(tenantId: string): Promise<AssetResponse[]> {
  return apiFetch<AssetResponse[]>(`/assets/tenant/${tenantId}`);
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
