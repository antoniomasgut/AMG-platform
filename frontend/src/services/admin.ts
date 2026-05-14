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
