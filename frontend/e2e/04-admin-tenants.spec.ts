import { test, expect } from '@playwright/test';
import { loginViaAPI, dismissCookies, apiGet, apiPost, API_BASE } from './helpers/auth';

const TS = Date.now();

test.describe('Admin — Gestió de Tenants (Mòdul 01)', () => {
  test('llista de tenants carrega', async ({ page }) => {
    await loginViaAPI(page);
    await page.goto('/ca/portal/admin/tenants');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1000);

    const body = await page.locator('body').textContent();
    expect(body?.length).toBeGreaterThan(100);
  });

  test('API llista tenants retorna array', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/tenants');
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(Array.isArray(data.content ?? data)).toBeTruthy();
  });

  test('crear tenant via API i veure detall', async ({ page }) => {
    const token = await loginViaAPI(page);
    const slug = `e2e-tenant-${TS}`;

    const resp = await apiPost(page, token, '/api/v1/tenants', {
      name: `E2E Tenant ${TS}`,
      slug,
      email: `${slug}@test.com`,
    });
    expect(resp.status()).toBe(201);
    const tenant = await resp.json();
    expect(tenant.id).toBeTruthy();

    // Navega al detall del tenant
    await page.goto(`/ca/portal/admin/tenants/${tenant.id}`);
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1000);

    const body = await page.locator('body').textContent();
    expect(body).toContain(`E2E Tenant ${TS}`);
  });

  test('pàgina detall tenant mostra seccions', async ({ page }) => {
    const token = await loginViaAPI(page);

    // Obtenir primer tenant existent
    const resp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const data = await resp.json();
    const tenants = data.content ?? data;
    if (!tenants || tenants.length === 0) return; // skip si no hi ha tenants

    const tenantId = tenants[0].id;
    await page.goto(`/ca/portal/admin/tenants/${tenantId}`);
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1500);

    // Ha de mostrar informació del tenant
    const body = await page.locator('body').textContent();
    expect(body?.length).toBeGreaterThan(200);
  });

  test('pàgina admin/users carrega', async ({ page }) => {
    await loginViaAPI(page);
    await page.goto('/ca/portal/admin/users');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1000);

    const body = await page.locator('body').textContent();
    expect(body?.length).toBeGreaterThan(100);
  });

  test('API llista usuaris retorna dades', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/users');
    expect(resp.status()).toBe(200);
  });
});
