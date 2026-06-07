import { apiFetch } from './api';

export interface SectorTemplateResponse {
  id: string;
  sector: string;
  type: string;
  title: string;
  body: string;
  sortOrder: number;
}

export interface SectorTemplateUpdateRequest {
  title?: string;
  body?: string;
}

export async function listSectorTemplates(params?: { sector?: string; type?: string }): Promise<SectorTemplateResponse[]> {
  const q = new URLSearchParams();
  if (params?.sector) q.set('sector', params.sector);
  if (params?.type) q.set('type', params.type);
  const qs = q.toString();
  return apiFetch(`/sector-templates${qs ? `?${qs}` : ''}`);
}

export async function getSectorTemplate(id: string): Promise<SectorTemplateResponse> {
  return apiFetch(`/sector-templates/${id}`);
}

export async function updateSectorTemplate(id: string, data: SectorTemplateUpdateRequest): Promise<SectorTemplateResponse> {
  return apiFetch(`/sector-templates/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}
