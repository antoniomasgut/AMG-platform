import { Page } from '@playwright/test';

export const API_BASE = 'http://localhost:8080';

export const USERS = {
  superAdmin: { email: 'superadmin@test.com', password: 'admin123' },
  admin:      { email: 'admin@test.com',       password: 'admin123' },
  client:     { email: 'client@test.com',      password: 'admin123' },
};

export async function loginViaAPI(page: Page, user = USERS.superAdmin): Promise<string> {
  const resp = await page.request.post(`${API_BASE}/api/v1/auth/login`, {
    headers: { 'Content-Type': 'application/json' },
    data: user,
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

export async function dismissCookies(page: Page) {
  const btn = page.locator('button:has-text("Acceptar")').first();
  if (await btn.isVisible({ timeout: 800 }).catch(() => false)) {
    await btn.click();
    await page.waitForTimeout(200);
  }
}

export async function apiGet(page: Page, token: string, path: string) {
  return page.request.get(`${API_BASE}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export async function apiPost(page: Page, token: string, path: string, data: object) {
  return page.request.post(`${API_BASE}${path}`, {
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    data,
  });
}
