import { apiFetch } from './api';

export interface UserResponse {
  id: string; email: string; name: string | null;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'CLIENT';
  tenantId: string | null; tenantName: string | null;
  isActive: boolean; isBlocked: boolean;
  lastLoginAt: string | null; createdAt: string;
}

export interface TenantResponse {
  id: string; name: string; slug: string;
  email: string | null; phone: string | null;
  address: string | null; isActive: boolean; createdAt: string;
}

export interface PageResponse<T> {
  content: T[]; totalPages: number; totalElements: number;
  number: number; size: number;
}

export interface CreateUserRequest {
  email: string; password: string; name: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'CLIENT'; tenantId?: string;
}

export interface UpdateUserRequest {
  email?: string; name?: string;
  role?: 'SUPER_ADMIN' | 'ADMIN' | 'CLIENT';
  tenantId?: string; isActive?: boolean;
}

export interface CreateTenantRequest {
  name: string; slug: string; email?: string; phone?: string; address?: string;
}

export interface UpdateTenantRequest {
  name?: string; email?: string; phone?: string; address?: string; isActive?: boolean;
}

export const listUsers = (params: { page?: number; size?: number; role?: string; tenantId?: string; search?: string } = {}) => {
  const q = new URLSearchParams({
    page: String(params.page ?? 0),
    size: String(params.size ?? 20),
  });
  if (params.role) q.set('role', params.role);
  if (params.tenantId) q.set('tenantId', params.tenantId);
  if (params.search) q.set('search', params.search);
  return apiFetch<PageResponse<UserResponse>>(`/users?${q}`);
};

export const getUser = (id: string) =>
  apiFetch<UserResponse>(`/users/${id}`);

export const createUser = (data: CreateUserRequest) =>
  apiFetch<UserResponse>('/users', { method: 'POST', body: JSON.stringify(data) });

export const updateUser = (id: string, data: UpdateUserRequest) =>
  apiFetch<UserResponse>(`/users/${id}`, { method: 'PUT', body: JSON.stringify(data) });

export const deleteUser = (id: string) =>
  apiFetch<void>(`/users/${id}`, { method: 'DELETE' });

export const unlockUser = (id: string) =>
  apiFetch<UserResponse>(`/users/${id}/unlock`, { method: 'POST' });

export const listTenants = (params: { page?: number; size?: number; search?: string } = {}) => {
  const q = new URLSearchParams({
    page: String(params.page ?? 0),
    size: String(params.size ?? 20),
  });
  if (params.search) q.set('search', params.search);
  return apiFetch<PageResponse<TenantResponse>>(`/tenants?${q}`);
};

export const getTenant = (id: string) =>
  apiFetch<TenantResponse>(`/tenants/${id}`);

export const createTenant = (data: CreateTenantRequest) =>
  apiFetch<TenantResponse>('/tenants', { method: 'POST', body: JSON.stringify(data) });

export const updateTenant = (id: string, data: UpdateTenantRequest) =>
  apiFetch<TenantResponse>(`/tenants/${id}`, { method: 'PUT', body: JSON.stringify(data) });

// --- Vault API (tenant services) ---

export interface CatalogService {
  id: string; name: string; slug: string;
  description: string; type: string; isAddon: boolean;
  cost: number; salePrice: number;
}

export interface TenantSetup {
  profiles: Array<{
    profile: { id: string; name: string; slug: string };
    phases: Array<{
      phase: { id: string; name: string; sortOrder: number };
      approvalStatus: string;
      services: Array<{
        service: { id: string; name: string; slug: string; type: string };
        status: string;
      }>;
    }>;
  }>;
  addons: Array<{
    service: { id: string; name: string };
    approvalRequired: boolean;
    approvalStatus: string;
  }>;
}

export interface AssignProfileResponse {
  profileId: string;
  phases: Array<{ phaseId: string; name: string; sortOrder: number; approvalStatus: string; totalServices: number; totalPrice: number }>;
  totalPrice: number;
}

export const listCatalogServices = () =>
  apiFetch<CatalogService[]>('/vault/services');

export const getTenantSetup = (tenantId: string) =>
  apiFetch<TenantSetup>(`/vault/tenants/${tenantId}/setup`);

export const assignProfileToTenant = (tenantId: string, profileId: string) =>
  apiFetch<AssignProfileResponse>(`/vault/tenants/${tenantId}/profiles/${profileId}`, { method: 'POST' });

export const removeProfileFromTenant = (tenantId: string, profileId: string) =>
  apiFetch<void>(`/vault/tenants/${tenantId}/profiles/${profileId}`, { method: 'DELETE' });

// --- Vault Catalog Management (profiles, phases, services) ---

export interface CatalogProfileResponse {
  id: string; name: string; slug: string;
  description: string; isActive: boolean;
  directServices: CatalogServiceDetail[];
  phases: CatalogPhaseResponse[];
  createdAt: string; updatedAt: string;
}

export interface CatalogPhaseResponse {
  id: string; name: string; description: string;
  sortOrder: number; services: CatalogServiceDetail[];
}

export interface CatalogServiceDetail {
  id: string; name: string; slug: string; description: string;
  type: string; isAddon: boolean;
  cost: number; salePrice: number; sortOrder: number;
}

export interface CreateCatalogProfileRequest {
  name: string; slug: string; description?: string;
}

export interface UpdateCatalogProfileRequest {
  name?: string; slug?: string; description?: string;
}

export interface CreateCatalogPhaseRequest {
  name: string; description?: string; sortOrder?: number;
}

export interface UpdateCatalogPhaseRequest {
  name?: string; description?: string; sortOrder?: number;
}

export interface CreateCatalogServiceRequest {
  name: string; slug: string; description?: string;
  type: string; cost: number; salePrice: number; sortOrder?: number;
}

// --- Vault catalog API functions ---

export const listProfiles = () =>
  apiFetch<CatalogProfileResponse[]>('/vault/profiles');

export const getProfile = (id: string) =>
  apiFetch<CatalogProfileResponse>(`/vault/profiles/${id}`);

export const createProfile = (data: CreateCatalogProfileRequest) =>
  apiFetch<CatalogProfileResponse>('/vault/profiles', { method: 'POST', body: JSON.stringify(data) });

export const updateProfile = (id: string, data: UpdateCatalogProfileRequest) =>
  apiFetch<CatalogProfileResponse>(`/vault/profiles/${id}`, { method: 'PUT', body: JSON.stringify(data) });

export const deleteProfile = (id: string) =>
  apiFetch<void>(`/vault/profiles/${id}`, { method: 'DELETE' });

export const addPhaseToProfile = (profileId: string, data: CreateCatalogPhaseRequest) =>
  apiFetch<CatalogProfileResponse>(`/vault/profiles/${profileId}/phases`, { method: 'POST', body: JSON.stringify(data) });

export const updatePhase = (profileId: string, phaseId: string, data: UpdateCatalogPhaseRequest) =>
  apiFetch<CatalogProfileResponse>(`/vault/profiles/${profileId}/phases/${phaseId}`, { method: 'PUT', body: JSON.stringify(data) });

export const deletePhase = (profileId: string, phaseId: string) =>
  apiFetch<CatalogProfileResponse>(`/vault/profiles/${profileId}/phases/${phaseId}`, { method: 'DELETE' });

export const addServiceToPhase = (phaseId: string, data: CreateCatalogServiceRequest) =>
  apiFetch<CatalogProfileResponse>(`/vault/phases/${phaseId}/services`, { method: 'POST', body: JSON.stringify(data) });

export const addServiceToProfile = (profileId: string, data: CreateCatalogServiceRequest) =>
  apiFetch<CatalogProfileResponse>(`/vault/profiles/${profileId}/services`, { method: 'POST', body: JSON.stringify(data) });

export const updateService = (serviceId: string, data: Partial<CreateCatalogServiceRequest>) =>
  apiFetch<CatalogProfileResponse>(`/vault/services/${serviceId}`, { method: 'PUT', body: JSON.stringify(data) });

export const deleteService = (serviceId: string) =>
  apiFetch<void>(`/vault/services/${serviceId}`, { method: 'DELETE' });

export const createAddonService = (data: CreateCatalogServiceRequest) =>
  apiFetch<CatalogProfileResponse>('/vault/services', { method: 'POST', body: JSON.stringify(data) });
