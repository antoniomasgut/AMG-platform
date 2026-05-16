import { test, expect } from '@playwright/test';
import { loginViaAPI, dismissCookies, apiGet, apiPost } from './helpers/auth';

test.describe('FinOps — Holded i Facturació (Mòdul 08)', () => {
  test('pàgina finops carrega', async ({ page }) => {
    await loginViaAPI(page);
    await page.goto('/ca/portal/finops');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1000);

    const body = await page.locator('body').textContent();
    expect(body?.length).toBeGreaterThan(100);
  });

  test('API dashboard FinOps global retorna dades', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/finops/dashboard/global');
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data.totalInvoices).toBeDefined();
  });

  test('API llista factures mensuals', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/finops/monthly-invoices');
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data.content ?? data).toBeTruthy();
  });

  test('SEPA mandate: registrar i obtenir', async ({ page }) => {
    const token = await loginViaAPI(page);

    // Obtenir un tenant existent
    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const tenantId = tenants[0].id;

    // Registrar mandat SEPA
    const mandateResp = await apiPost(page, token, `/api/v1/finops/tenants/${tenantId}/sepa-mandate`, {
      iban: 'ES7921000813610123456789',
      bic: 'CAIXESBBXXX',
      accountHolderName: 'Test Account Holder',
      signedAt: new Date().toISOString().split('T')[0],
    });
    expect([200, 201, 409]).toContain(mandateResp.status()); // 409 si ja existeix

    // Obtenir mandat actiu
    const getResp = await apiGet(page, token, `/api/v1/finops/tenants/${tenantId}/sepa-mandate`);
    expect([200, 404]).toContain(getResp.status());
    if (getResp.status() === 200) {
      const mandate = await getResp.json();
      expect(mandate.tenantId).toBe(tenantId);
      expect(mandate.iban).toBeTruthy();
    }
  });

  test('API holded config — tenant sense config retorna 404', async ({ page }) => {
    const token = await loginViaAPI(page);
    const fakeId = '00000000-0000-0000-0000-000000000000';
    const resp = await apiGet(page, token, `/api/v1/finops/configure/${fakeId}`);
    expect([404, 400]).toContain(resp.status());
  });

  test('configurar holded per a un tenant', async ({ page }) => {
    const token = await loginViaAPI(page);

    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const resp = await apiPost(page, token, '/api/v1/finops/configure', {
      tenantId: tenants[0].id,
      apiKeyRef: 'test-api-key-ref',
      holdedCompanyId: 'COMP001',
    });
    expect([200, 201]).toContain(resp.status());
    const config = await resp.json();
    expect(config.tenantId).toBe(tenants[0].id);
  });
});
