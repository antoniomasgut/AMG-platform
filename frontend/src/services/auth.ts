import { apiFetch, setTokens, clearTokens } from './api';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginUser {
  id: string;
  email: string;
  name: string;
  position: string | null;
  role: string;
  tenantId: string | null;
  tenantName: string | null;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: LoginUser;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface RefreshResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  password: string;
}

export async function login(data: LoginRequest): Promise<LoginResponse> {
  const res = await apiFetch<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(data),
    skipAuth: true,
  });
  // Normalize: backend returns { user: { tenant: {id, name} | null } } but frontend expects tenantId/tenantName
  if (res.user && 'tenant' in res.user) {
    const tenant = (res.user as Record<string, unknown>).tenant;
    if (tenant && typeof tenant === 'object') {
      (res.user as Record<string, unknown>).tenantId = (tenant as Record<string, unknown>).id as string;
      (res.user as Record<string, unknown>).tenantName = (tenant as Record<string, unknown>).name as string;
    } else {
      (res.user as Record<string, unknown>).tenantId = typeof tenant === 'string' ? tenant : null;
      (res.user as Record<string, unknown>).tenantName = null;
    }
  }
  setTokens(res.accessToken, res.refreshToken);
  return res;
}

export async function refresh(refreshToken: string): Promise<RefreshResponse> {
  const res = await apiFetch<RefreshResponse>('/auth/refresh', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
    skipAuth: true,
  });
  return res;
}

export async function logout(): Promise<void> {
  try {
    const stored = sessionStorage.getItem('refresh_token');
    if (stored) {
      const tokenId = stored.split(':')[0];
      await apiFetch('/auth/logout', {
        method: 'POST',
        body: JSON.stringify({ tokenId }),
      });
    }
  } finally {
    clearTokens();
  }
}

export async function forgotPassword(data: ForgotPasswordRequest): Promise<void> {
  await apiFetch('/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify(data),
    skipAuth: true,
  });
}

export async function resetPassword(data: ResetPasswordRequest): Promise<void> {
  await apiFetch('/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify(data),
    skipAuth: true,
  });
}

export function getCurrentUser(): LoginResponse['user'] | null {
  if (typeof window === 'undefined') return null;
  const stored = sessionStorage.getItem('user');
  if (!stored) return null;
  const user: LoginResponse['user'] = JSON.parse(stored);
  // Normalize: backend stores as 'tenant' (string | {id, name} | null), frontend expects 'tenantId'/'tenantName'
  if ('tenant' in user && !('tenantId' in user)) {
    const tenantVal = (user as Record<string, unknown>).tenant;
    if (tenantVal && typeof tenantVal === 'object') {
      (user as Record<string, unknown>).tenantId = (tenantVal as Record<string, unknown>).id as string;
      (user as Record<string, unknown>).tenantName = (tenantVal as Record<string, unknown>).name as string;
    } else {
      (user as Record<string, unknown>).tenantId = typeof tenantVal === 'string' ? tenantVal : null;
      (user as Record<string, unknown>).tenantName = null;
    }
  }
  return user;
}
