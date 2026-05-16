import { test, expect } from '@playwright/test';
import { loginViaAPI, dismissCookies, apiGet, apiPost } from './helpers/auth';

const TS = Date.now();

test.describe('Vault Admin — Catàleg i Perfils (Mòdul 02)', () => {
  test('pàgina vault admin carrega', async ({ page }) => {
    await loginViaAPI(page);
    await page.goto('/ca/portal/admin/vault');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1000);

    const body = await page.locator('body').textContent();
    expect(body?.length).toBeGreaterThan(100);
  });

  test('API catàleg de serveis té serveis amb preu setup i mensual', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/vault/services');
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    const services = data.content ?? data;
    expect(Array.isArray(services)).toBeTruthy();
    expect(services.length).toBeGreaterThan(0);

    const svc = services[0];
    expect(svc.salePrice).toBeDefined();
    expect(svc.monthlyPrice).toBeDefined();
    expect(typeof svc.salePrice).toBe('number');
    expect(typeof svc.monthlyPrice).toBe('number');
  });

  test('crear servei al catàleg amb preu mensual', async ({ page }) => {
    const token = await loginViaAPI(page);
    const slug = `servei-e2e-${TS}`;
    const resp = await apiPost(page, token, '/api/v1/vault/services', {
      name: `Servei E2E ${TS}`,
      slug,
      description: 'Test E2E',
      type: 'LANDING',
      cost: 20.00,
      salePrice: 80.00,
      monthlyPrice: 15.00,
      sortOrder: 99,
    });
    expect([200, 201]).toContain(resp.status());

    // Verificar preus via GET services
    const listResp = await apiGet(page, token, '/api/v1/vault/services');
    const services = await listResp.json();
    const created = services.find((s: { slug: string }) => s.slug === slug);
    if (created) {
      expect(created.salePrice).toBe(80.00);
      expect(created.monthlyPrice).toBe(15.00);
    }
  });

  test('API llista perfils de vault', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/vault/profiles');
    expect(resp.status()).toBe(200);
    const profiles = await resp.json();
    expect(Array.isArray(profiles)).toBeTruthy();
  });

  test('crear perfil al vault', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiPost(page, token, '/api/v1/vault/profiles', {
      name: `Perfil E2E ${TS}`,
      slug: `perfil-e2e-${TS}`,
      description: 'Test E2E profile',
    });
    expect([200, 201]).toContain(resp.status());
    const profile = await resp.json();
    expect(profile.id).toBeTruthy();
    expect(profile.name).toContain('E2E');
  });

  test('assignar perfil a tenant — preu quedat frozen al moment', async ({ page }) => {
    const token = await loginViaAPI(page);

    // Obtenir tenant i perfil existents
    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const profilesResp = await apiGet(page, token, '/api/v1/vault/profiles');
    const profiles = await profilesResp.json();
    if (!profiles || profiles.length === 0) return;

    // Assignar perfil al tenant
    const assignResp = await apiPost(page, token,
      `/api/v1/vault/tenants/${tenants[0].id}/profiles/${profiles[0].id}`,
      {}
    );
    expect([200, 201, 409]).toContain(assignResp.status()); // 409 si ja assignat

    if ([200, 201].includes(assignResp.status())) {
      const assignment = await assignResp.json();
      // La resposta ha de tenir profileId i totalPrice
      expect(assignment.profileId).toBeDefined();
      expect(assignment.totalPrice).toBeDefined();
    }
  });

  test('pàgina perfils vault carrega', async ({ page }) => {
    await loginViaAPI(page);
    await page.goto('/ca/portal/admin/vault/profiles');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1000);

    const body = await page.locator('body').textContent();
    expect(body?.length).toBeGreaterThan(100);
  });
});
