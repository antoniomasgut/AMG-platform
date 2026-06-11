import { apiFetch } from './api';
import type { PageResponse } from './userService';

export interface TenantResponse {
  id: string;
  name: string;
  slug: string;
  email: string | null;
  phone: string | null;
  address: string | null;
  city: string | null;
  sector: string | null;
  businessSize: string | null;
  contractedPhases: string[] | null;
  isActive: boolean;
  isFree: boolean;
  createdAt: string;
}

export interface CreateTenantRequest {
  name: string;
  slug: string;
  email?: string;
  phone?: string;
  address?: string;
  city?: string;
  sector?: string;
  businessSize?: string;
  contractedPhases?: string[];
  preferredChannel?: string;
  isFree?: boolean;
}

export interface UpdateTenantRequest {
  name?: string;
  slug?: string;
  email?: string;
  phone?: string;
  address?: string;
  city?: string;
  sector?: string;
  businessSize?: string;
  contractedPhases?: string[];
  preferredChannel?: string;
  isFree?: boolean;
}

export interface ListTenantsParams {
  page?: number;
  size?: number;
  search?: string;
}

export async function listTenants(params: ListTenantsParams = {}): Promise<PageResponse<TenantResponse>> {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set('page', String(params.page));
  if (params.size !== undefined) searchParams.set('size', String(params.size));
  if (params.search) searchParams.set('search', params.search);
  const qs = searchParams.toString();
  return apiFetch(`/tenants${qs ? `?${qs}` : ''}`);
}

export async function getTenant(id: string): Promise<TenantResponse> {
  return apiFetch(`/tenants/${id}`);
}

export async function createTenant(data: CreateTenantRequest): Promise<TenantResponse> {
  return apiFetch('/tenants', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function updateTenant(id: string, data: UpdateTenantRequest): Promise<TenantResponse> {
  return apiFetch(`/tenants/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}
