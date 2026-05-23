import { Page } from '@playwright/test';

export const API_BASE = 'https://api.amgdl.com';

export const PROD_USER = {
  email: 'antoniomasgut@gmail.com',
  password: 'Desidia_10A2026',
};

export const TENANT_ID = '05003ce9-b624-4430-9988-55eec1e9cc93';

export async function loginProd(page: Page): Promise<string> {
  const resp = await page.request.post(`${API_BASE}/api/v1/auth/login`, {
    headers: { 'Content-Type': 'application/json' },
    data: PROD_USER,
  });
  const data = await resp.json();
  await page.goto('/ca/login');
  await page.waitForLoadState('networkidle');
  await page.evaluate(({ token, refreshToken, u }) => {
    sessionStorage.setItem('access_token', token);
    sessionStorage.setItem('refresh_token', refreshToken);
    sessionStorage.setItem('user', JSON.stringify(u));
  }, { token: data.accessToken, refreshToken: data.refreshToken, u: data.user });
  return data.accessToken;
}

export async function apiCall(page: Page, token: string, method: string, path: string, body?: unknown) {
  return page.request.fetch(`${API_BASE}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    data: body ? JSON.stringify(body) : undefined,
  });
}

export async function dismissCookies(page: Page) {
  const btn = page.locator('button:has-text("Acceptar")').first();
  if (await btn.isVisible({ timeout: 800 }).catch(() => false)) {
    await btn.click();
  }
}
