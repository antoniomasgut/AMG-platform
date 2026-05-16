import { test, expect } from '@playwright/test';
import { API_BASE, loginViaAPI } from './helpers/auth';

// Comprova que el control d'accés per rol funciona correctament

test.describe('RBAC — Control d\'accés per rol (Mòdul 01)', () => {
  test('SUPER_ADMIN pot accedir a rutes admin', async ({ page }) => {
    const token = await loginViaAPI(page);

    const endpoints = [
      '/api/v1/tenants',
      '/api/v1/users',
      '/api/v1/ops/backups',
      '/api/v1/infraops/status',
      '/api/v1/finops/dashboard/global',
    ];

    for (const ep of endpoints) {
      const resp = await page.request.get(`${API_BASE}${ep}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      expect(resp.status(), `SUPER_ADMIN ha de poder accedir a ${ep}`).not.toBe(403);
      expect(resp.status(), `${ep} no hauria de retornar 401`).not.toBe(401);
    }
  });

  test('sense token → tots els endpoints retornen 401', async ({ page }) => {
    const protectedEndpoints = [
      '/api/v1/tenants',
      '/api/v1/users',
      '/api/v1/ops/backups',
      '/api/v1/infraops/status',
      '/api/v1/billing/budgets',
      '/api/v1/finops/dashboard/global',
      '/api/v1/automations',
    ];

    for (const ep of protectedEndpoints) {
      const resp = await page.request.get(`${API_BASE}${ep}`);
      expect(resp.status(), `${ep} sense token ha de ser 401`).toBe(401);
    }
  });

  test('InfraOps és exclusiu de SUPER_ADMIN (ADMIN no pot)', async ({ page }) => {
    // Si existeix un token d'ADMIN (rol limitat), no hauria de poder accedir a infraops
    // Primer intentem login com ADMIN (pot no existir)
    const adminResp = await page.request.post(`${API_BASE}/api/v1/auth/login`, {
      headers: { 'Content-Type': 'application/json' },
      data: { email: 'admin@test.com', password: 'admin123' },
    });

    if (adminResp.status() !== 200) return; // skip si l'usuari no existeix

    const adminData = await adminResp.json();
    if (adminData.user?.role === 'SUPER_ADMIN') return; // skip si és superadmin

    const adminToken = adminData.accessToken;
    const resp = await page.request.get(`${API_BASE}/api/v1/infraops/status`, {
      headers: { Authorization: `Bearer ${adminToken}` },
    });
    expect(resp.status()).toBe(403);
  });

  test('webhook GoCardless és accessible sense token (endpoint públic)', async ({ page }) => {
    const resp = await page.request.post(`${API_BASE}/api/v1/gocardless/webhook`, {
      headers: { 'Content-Type': 'application/json' },
      data: { events: [] },
    });
    // Ha de ser 200 (webhook públic) o 400 (validació), no 401
    expect(resp.status()).not.toBe(401);
    expect([200, 400]).toContain(resp.status());
  });

  test('endpoint complete mandate de GoCardless és accessible sense token (callback)', async ({ page }) => {
    // El complete és GET públic (callback de GoCardless)
    const resp = await page.request.get(
      `${API_BASE}/api/v1/gocardless/tenants/00000000-0000-0000-0000-000000000001/mandate/complete?redirect_flow_id=RE_TEST`
    );
    // Ha de ser 404 (tenant no existeix) o 400, no 401
    expect(resp.status()).not.toBe(401);
  });

  test('ops health és accessible per a usuaris autenticats', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await page.request.get(`${API_BASE}/api/v1/ops/health`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(resp.status()).toBe(200);
  });
});
