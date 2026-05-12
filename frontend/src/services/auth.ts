import { apiFetch, setTokens, clearTokens } from './api';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: {
    id: string;
    email: string;
    name: string;
    role: string;
    tenantId: string | null;
  };
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
  return stored ? JSON.parse(stored) : null;
}
