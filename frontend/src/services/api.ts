const API_BASE = '/api';

interface ApiOptions extends RequestInit {
  skipAuth?: boolean;
}

function getTokens(): { accessToken: string | null; refreshToken: string | null } {
  if (typeof window === 'undefined') return { accessToken: null, refreshToken: null };
  return {
    accessToken: sessionStorage.getItem('access_token'),
    refreshToken: sessionStorage.getItem('refresh_token'),
  };
}

function setTokens(access: string, refresh: string) {
  sessionStorage.setItem('access_token', access);
  sessionStorage.setItem('refresh_token', refresh);
}

function clearTokens() {
  sessionStorage.removeItem('access_token');
  sessionStorage.removeItem('refresh_token');
}

let isRefreshing = false;
let refreshQueue: Array<{ resolve: (t: string) => void; reject: (e: unknown) => void }> = [];

async function processRefresh(): Promise<string> {
  const { refreshToken } = getTokens();
  if (!refreshToken) throw new Error('No refresh token');

  const res = await fetch(`${API_BASE}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });

  if (!res.ok) {
    clearTokens();
    throw new Error('Refresh failed');
  }

  const data = await res.json();
  setTokens(data.accessToken, data.refreshToken);
  return data.accessToken;
}

async function refreshAccessToken(): Promise<string> {
  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      refreshQueue.push({ resolve, reject });
    });
  }

  isRefreshing = true;
  try {
    const token = await processRefresh();
    refreshQueue.forEach((q) => q.resolve(token));
    return token;
  } catch (e) {
    refreshQueue.forEach((q) => q.reject(e));
    clearTokens();
    throw e;
  } finally {
    isRefreshing = false;
    refreshQueue = [];
  }
}

function buildUrl(path: string): string {
  return `${API_BASE}${path}`;
}

export async function apiFetch<T = unknown>(
  path: string,
  options: ApiOptions = {}
): Promise<T> {
  const { accessToken } = getTokens();
  const headers: Record<string, string> = {
    ...(options.headers as Record<string, string>),
  };

  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = headers['Content-Type'] || 'application/json';
  }

  if (!options.skipAuth && accessToken) {
    headers['Authorization'] = `Bearer ${accessToken}`;
  }

  let res = await fetch(buildUrl(path), { ...options, headers });

  // Attempt token refresh on 401
  if (res.status === 401 && !options.skipAuth) {
    try {
      const newToken = await refreshAccessToken();
      headers['Authorization'] = `Bearer ${newToken}`;
      res = await fetch(buildUrl(path), { ...options, headers });
    } catch {
      if (typeof window !== 'undefined') {
        window.location.href = '/login';
      }
      throw new Error('Session expired');
    }
  }

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    const error = new Error(body.message || `Request failed: ${res.status}`) as Error & {
      status: number;
      body: unknown;
    };
    error.status = res.status;
    error.body = body;
    throw error;
  }

  return res.json();
}

export { setTokens, clearTokens, getTokens };
