import { test, expect } from '@playwright/test';
import { loginViaAPI, dismissCookies, apiGet, apiPost } from './helpers/auth';

const TS = Date.now();

test.describe('Billing — Pressupostos (Mòdul 07)', () => {
  test('pàgina billing carrega', async ({ page }) => {
    await loginViaAPI(page);
    await page.goto('/ca/portal/billing');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1000);

    const body = await page.locator('body').textContent();
    expect(body?.length).toBeGreaterThan(100);
  });

  test('API llista pressupostos', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const resp = await apiGet(page, token, `/api/v1/billing/tenants/${tenants[0].id}/budgets`);
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data.content ?? data).toBeTruthy();
  });

  test('crear pressupost via API', async ({ page }) => {
    const token = await loginViaAPI(page);

    // Necessita un profile + phases del Vault per crear el pressupost
    const profilesResp = await apiGet(page, token, '/api/v1/vault/profiles');
    const profiles = await profilesResp.json();
    if (!profiles || profiles.length === 0) return;

    const profile = profiles[0];
    const phaseIds = (profile.phases ?? []).map((p: any) => p.id);
    if (phaseIds.length === 0) return;

    const validUntil = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const resp = await apiPost(page, token, `/api/v1/billing/tenants/${tenants[0].id}/budgets`, {
      profileId: profile.id,
      phaseIds,
      addonIds: [],
      notes: 'Pressupost E2E',
      clientNotes: 'Test E2E',
      discountIds: [],
      validUntil,
    });
    expect([200, 201]).toContain(resp.status());
    const budget = await resp.json();
    expect(budget.id).toBeTruthy();
  });

  test('API dashboard de billing retorna mètriques', async ({ page }) => {
    const token = await loginViaAPI(page);

    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const resp = await apiGet(page, token, `/api/v1/billing/tenants/${tenants[0].id}/dashboard`);
    expect([200, 204]).toContain(resp.status());
  });

  test('els serveis del catàleg tenen preu setup i mensual', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/vault/services');
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    const services = data.content ?? data;
    if (services && services.length > 0) {
      const svc = services[0];
      expect(svc.salePrice).toBeDefined();
      expect(svc.monthlyPrice).toBeDefined();
    }
  });
});
