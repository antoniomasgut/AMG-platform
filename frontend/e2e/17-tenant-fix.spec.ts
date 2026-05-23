import { test, expect } from '@playwright/test';
import { loginProd, apiCall, dismissCookies } from './helpers/prod-auth';

const TS = Date.now();

test.describe('Fix — Llistat i creació de tenants (ORDER BY created_at)', () => {

  test('API: llistat tenants retorna 200 sense error createdAt', async ({ page }) => {
    const token = await loginProd(page);
    const resp = await apiCall(page, token, 'GET', '/api/v1/tenants?page=0&size=10');
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(Array.isArray(data.content)).toBeTruthy();
    expect(data.content.length).toBeGreaterThan(0);
  });

  test('API: crear tenant nou funciona correctament', async ({ page }) => {
    const token = await loginProd(page);
    const slug = `e2e-${TS}`;

    const resp = await apiCall(page, token, 'POST', '/api/v1/tenants', {
      name: `E2E Test ${TS}`,
      slug,
      email: `${slug}@test.com`,
    });
    expect(resp.status()).toBe(201);
    const tenant = await resp.json();
    expect(tenant.id).toBeTruthy();
    expect(tenant.slug).toBe(slug);
  });

  test('UI: pàgina admin tenants carrega sense error', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal/admin/tenants');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1500);

    const body = await page.locator('body').textContent();
    expect(body).toContain('AMG');
    // No ha de contenir l'error de columna SQL
    expect(body).not.toContain('createdAt does not exist');
    expect(body).not.toContain('column t.createdat');
    // Ha de mostrar la taula de tenants
    expect(body).toContain('Tenant');
  });
});
