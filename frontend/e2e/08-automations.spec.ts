import { test, expect } from '@playwright/test';
import { loginViaAPI, dismissCookies, apiGet, apiPost } from './helpers/auth';

const TS = Date.now();

test.describe('Automations — n8n (Mòdul 10)', () => {
  test('pàgina automations carrega', async ({ page }) => {
    await loginViaAPI(page);
    await page.goto('/ca/portal/automations');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1000);

    const body = await page.locator('body').textContent();
    expect(body?.length).toBeGreaterThan(100);
  });

  test('API llista automations', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const resp = await apiGet(page, token, `/api/v1/automations/tenants/${tenants[0].id}/workflows`);
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data.content ?? data).toBeTruthy();
  });

  test('crear automation via API', async ({ page }) => {
    const token = await loginViaAPI(page);

    // Primer crear un template
    const templateKeys = ['form-to-whatsapp', 'welcome-email', 'lead-followup'];
    const templatesResp = await apiGet(page, token, '/api/v1/automations/templates');
    let templates = await templatesResp.json();
    if (!templates || templates.length === 0) return;

    const templateKey = templates[0].key;
    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const resp = await apiPost(page, token, `/api/v1/automations/tenants/${tenants[0].id}/workflows`, {
      templateKey,
      config: { note: `E2E Test ${TS}` },
    });
    expect([200, 201]).toContain(resp.status());
    const auto = await resp.json();
    expect(auto.id).toBeTruthy();
    expect(auto.templateKey).toBe(templateKey);
  });

  test('llistar execucions d\'una automation', async ({ page }) => {
    const token = await loginViaAPI(page);

    // Primer crear un template i assignar-lo per tenir un workflow
    const templatesResp = await apiGet(page, token, '/api/v1/automations/templates');
    const templates = await templatesResp.json();
    if (!templates || templates.length === 0) return;

    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const createResp = await apiPost(page, token, `/api/v1/automations/tenants/${tenants[0].id}/workflows`, {
      templateKey: templates[0].key,
      config: { note: 'Exec test' },
    });
    expect([200, 201]).toContain(createResp.status());
    const created = await createResp.json();

    const execResp = await apiGet(page, token, `/api/v1/automations/workflows/${created.id}/executions`);
    expect([200, 404]).toContain(execResp.status());
  });
});
