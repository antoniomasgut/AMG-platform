import { test, expect } from '@playwright/test';
import { loginViaAPI, dismissCookies, apiGet, apiPost } from './helpers/auth';

test.describe('Ops & Health (Mòdul 11)', () => {
  test('pàgina ops carrega', async ({ page }) => {
    await loginViaAPI(page);
    await page.goto('/ca/portal/ops');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1000);

    const body = await page.locator('body').textContent();
    expect(body?.length).toBeGreaterThan(100);
  });

  test('API health retorna status UP', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/ops/health');
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data.status).toBe('UP');
    expect(data.version).toBeTruthy();
    expect(data.uptime).toBeTruthy();
  });

  test('API all-health retorna serveis', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/ops/health/all');
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data.services).toBeTruthy();
    expect(Array.isArray(data.services)).toBeTruthy();
    expect(data.services.length).toBeGreaterThan(0);
  });

  test('API incidents llista', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/ops/incidents');
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(Array.isArray(data)).toBeTruthy();
  });

  test('API backup manual MANUAL_FULL crea backup record', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiPost(page, token, '/api/v1/ops/backups', {
      type: 'MANUAL_FULL',
    });
    expect([200, 201, 202]).toContain(resp.status());
    const data = await resp.json();
    expect(data.id).toBeTruthy();
    expect(data.type).toBe('MANUAL_FULL');
    expect(['PENDING', 'IN_PROGRESS', 'COMPLETED']).toContain(data.status);
  });

  test('API llista backups', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/ops/backups');
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data.content ?? data).toBeTruthy();
  });

  test('InfraOps status retorna mètriques', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/infraops/status');
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data.cpu).toBeDefined();
    expect(data.ram).toBeDefined();
    expect(data.disk).toBeDefined();
    expect(data.database).toBeDefined();
    expect(data.overallStatus).toBeDefined();
  });

  test('InfraOps collect força recol·lecció', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiPost(page, token, '/api/v1/infraops/collect', {});
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data.collectedAt).toBeTruthy();
  });

  test('InfraOps recommendations retorna llista', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/infraops/recommendations');
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(Array.isArray(data)).toBeTruthy();
  });

  test('InfraOps metrics retorna historial', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/infraops/metrics?hours=1');
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data.content).toBeDefined();
  });
});
