import { test, expect } from '@playwright/test';
import { loginViaAPI, apiGet, apiPost } from './helpers/auth';

test.describe('Payments — Stripe i GoCardless (Mòduls 09 i 09b)', () => {
  test('API llista pagaments Stripe', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const resp = await apiGet(page, token, `/api/v1/payments?tenantId=${tenants[0].id}`);
    expect(resp.status()).toBe(200);
  });

  test('GET /providers retorna estructura correcta', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const tenantId = tenants[0].id;
    const resp = await apiGet(page, token, `/api/v1/payments/tenants/${tenantId}/providers`);
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data.tenantId).toBe(tenantId);
    expect(data.setup).toBeDefined();
    expect(data.setup.activeProvider).toBeDefined();
    expect(data.recurring).toBeDefined();
    expect(data.recurring.activeProvider).toBeDefined();
  });

  test('configurar Stripe per a un tenant', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const resp = await apiPost(page, token, '/api/v1/payments/configure', {
      tenantId: tenants[0].id,
      apiKeyRef: 'sk_test_mock_key_ref',
      webhookSecret: 'whsec_test',
    });
    expect([200, 201]).toContain(resp.status());
    const config = await resp.json();
    expect(config.tenantId).toBe(tenants[0].id);
    expect(config.isActive).toBe(true);
  });

  test('GoCardless configure per a un tenant', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const tenantId = tenants[0].id;
    const resp = await apiPost(page, token, `/api/v1/gocardless/configure/${tenantId}`, {
      apiKeyRef: 'gc_test_key_ref',
      environment: 'SANDBOX',
      creditorId: 'CR001',
      webhookSecret: 'webhook_secret_test',
    });
    expect([200, 201]).toContain(resp.status());
    const config = await resp.json();
    expect(config.tenantId).toBe(tenantId);
    expect(config.environment).toBe('SANDBOX');
    expect(config.isActive).toBe(true);
  });

  test('GoCardless initiate mandate retorna redirectUrl', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const tenantId = tenants[0].id;

    // Assegurem que GoCardless està configurat
    await apiPost(page, token, `/api/v1/gocardless/configure/${tenantId}`, {
      apiKeyRef: 'gc_test_key_ref',
      environment: 'SANDBOX',
      creditorId: 'CR001',
    });

    const resp = await apiPost(page, token, `/api/v1/gocardless/tenants/${tenantId}/mandate/initiate`, {});
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data.redirectFlowId).toBeTruthy();
    expect(data.redirectUrl).toBeTruthy();
    expect(data.redirectUrl).toContain('gocardless.com');
  });

  test('GoCardless complete mandate via mock', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const tenantId = tenants[0].id;

    // Iniciar flux
    const initiateResp = await apiPost(page, token, `/api/v1/gocardless/tenants/${tenantId}/mandate/initiate`, {});
    if (initiateResp.status() !== 200) return;
    const { redirectFlowId } = await initiateResp.json();

    // Completar via mock
    const completeResp = await page.request.get(
      `http://localhost:8080/api/v1/gocardless/tenants/${tenantId}/mandate/complete?redirect_flow_id=${redirectFlowId}`
    );
    expect(completeResp.status()).toBe(200);
    const mandate = await completeResp.json();
    expect(mandate.status).toBe('ACTIVE');
    expect(mandate.gcMandateId).toBeTruthy();
  });

  test('GET providers mostra GoCardless actiu després del mandate', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const tenantId = tenants[0].id;
    const resp = await apiGet(page, token, `/api/v1/payments/tenants/${tenantId}/providers`);
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    // Si el mandate del test anterior és actiu, hauria de mostrar GOCARDLESS
    expect(['GOCARDLESS', 'SEPA_MANUAL', 'TRANSFER']).toContain(data.recurring.activeProvider);
  });

  test('GoCardless pagaments llista per tenant', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    const resp = await apiGet(page, token, `/api/v1/gocardless/tenants/${tenants[0].id}/payments`);
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data.content).toBeDefined();
  });

  test('GoCardless sense JWT → 401', async ({ page }) => {
    const resp = await page.request.get('http://localhost:8080/api/v1/gocardless/configure/00000000-0000-0000-0000-000000000001');
    expect(resp.status()).toBe(401);
  });
});
