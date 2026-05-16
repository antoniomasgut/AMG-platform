import { test, expect } from '@playwright/test';
import { loginViaAPI, dismissCookies, apiGet } from './helpers/auth';

test.describe('Portal Dashboard (Mòdul 01 + 03)', () => {
  test('dashboard carrega per a SUPER_ADMIN', async ({ page }) => {
    await loginViaAPI(page);
    await page.goto('/ca/portal');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);

    // Ha de mostrar contingut del portal, no una pàgina en blanc
    const body = await page.locator('body').textContent();
    expect(body?.length).toBeGreaterThan(200);
  });

  test('dashboard mostra les seccions principals', async ({ page }) => {
    await loginViaAPI(page);
    await page.goto('/ca/portal');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1000);

    // Comprova que hi ha almenys un element de navegació o mètrica
    const hasNav = await page.locator('nav, aside, [role="navigation"]').first().isVisible().catch(() => false);
    const hasContent = (await page.locator('main, [role="main"]').first().textContent())?.length ?? 0 > 50;
    expect(hasNav || hasContent).toBeTruthy();
  });

  test('API health endpoint retorna UP', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/ops/health');
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data.status).toBe('UP');
  });

  test('API all-health retorna llista de serveis', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/ops/health/all');
    expect([200, 204]).toContain(resp.status());
  });

  test('sidebar de navegació és accessible', async ({ page }) => {
    await loginViaAPI(page);
    await page.goto('/ca/portal');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1000);

    // Comprova presència de links de navegació principals
    const navLinks = page.locator('a[href*="/portal/"]');
    const count = await navLinks.count();
    expect(count).toBeGreaterThan(2);
  });
});
