import { test, expect } from '@playwright/test';
import { loginViaAPI, dismissCookies, apiGet } from './helpers/auth';

test.describe('Landings — Editor i Motor (Mòduls 04 i 05)', () => {
  test('pàgina /landings carrega', async ({ page }) => {
    await loginViaAPI(page);
    await page.goto('/ca/portal/landings');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1000);

    const body = await page.locator('body').textContent();
    expect(body?.length).toBeGreaterThan(100);
  });

  test('pàgina nova landing mostra selector de plantilles', async ({ page }) => {
    await loginViaAPI(page);
    await page.goto('/ca/portal/landings/new');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2000);

    await expect(page.locator('text=Selecciona una plantilla').first()).toBeVisible({ timeout: 10000 });
  });

  test('API llista plantilles retorna templates', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/engine/templates');
    expect(resp.status()).toBe(200);
    const templates = await resp.json();
    expect(Array.isArray(templates)).toBeTruthy();
    expect(templates.length).toBeGreaterThan(0);
  });

  test('API llista landings de l\'usuari', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const resp = await apiGet(page, token, `/api/v1/engine/tenants/${tenants[0].id}/landings`);
    expect(resp.status()).toBe(200);
  });

  test('pàgina admin/templates carrega amb llista', async ({ page }) => {
    await loginViaAPI(page);
    await page.goto('/ca/portal/admin/templates');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2000);

    await expect(page.locator('text=Gestió de plantilles').first()).toBeVisible({ timeout: 10000 });
  });

  test('pàgina nova plantilla admin mostra formulari', async ({ page }) => {
    await loginViaAPI(page);
    await page.goto('/ca/portal/admin/templates/new');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2000);

    await expect(page.locator('text=Nova plantilla').first()).toBeVisible({ timeout: 10000 });
  });

  test('detall plantilla existent carrega', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/engine/templates');
    const templates = await resp.json();
    if (!templates || templates.length === 0) return;

    await page.goto(`/ca/portal/admin/templates/${templates[0].id}`);
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2000);

    await expect(page.locator(`text=${templates[0].name}`).first()).toBeVisible({ timeout: 10000 });
  });

  test('landing pública renderitza sense autenticació', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const landingsResp = await apiGet(page, token, `/api/v1/engine/tenants/${tenants[0].id}/landings?page=0&size=1`);
    const data = await landingsResp.json();
    const landings = data.content ?? data;
    if (!landings || landings.length === 0) return;

    const slug = landings[0].slug;
    if (!slug) return;

    // Accés públic sense token
    const resp = await page.request.get(`http://localhost:8080/api/v1/engine/render/${slug}`);
    expect([200, 404]).toContain(resp.status()); // 404 si no té slug actiu
  });
});
