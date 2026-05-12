import { apiFetch } from './api';

export interface UserResponse {
  id: string;
  email: string;
  name: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'CLIENT';
  tenant: { id: string; name: string } | null;
  isActive: boolean;
  isBlocked: boolean;
  lastLoginAt: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  name: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'CLIENT';
  tenantId?: string;
}

export interface UpdateUserRequest {
  email?: string;
  name?: string;
  role?: 'SUPER_ADMIN' | 'ADMIN' | 'CLIENT';
  tenantId?: string;
  isActive?: boolean;
}

export interface ListUsersParams {
  page?: number;
  size?: number;
  role?: string;
  tenantId?: string;
  search?: string;
}

export async function listUsers(params: ListUsersParams = {}): Promise<PageResponse<UserResponse>> {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set('page', String(params.page));
  if (params.size !== undefined) searchParams.set('size', String(params.size));
  if (params.role) searchParams.set('role', params.role);
  if (params.tenantId) searchParams.set('tenantId', params.tenantId);
  if (params.search) searchParams.set('search', params.search);
  const qs = searchParams.toString();
  return apiFetch(`/users${qs ? `?${qs}` : ''}`);
}

export async function getUser(id: string): Promise<UserResponse> {
  return apiFetch(`/users/${id}`);
}

export async function createUser(data: CreateUserRequest): Promise<UserResponse> {
  return apiFetch('/users', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export async function updateUser(id: string, data: UpdateUserRequest): Promise<UserResponse> {
  return apiFetch(`/users/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function deleteUser(id: string): Promise<void> {
  await apiFetch(`/users/${id}`, { method: 'DELETE' });
}

export async function unlockUser(id: string): Promise<{ message: string }> {
  return apiFetch(`/users/${id}/unlock`, { method: 'POST' });
}
