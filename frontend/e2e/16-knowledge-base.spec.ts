import { test, expect } from '@playwright/test';
import { loginProd, apiCall, dismissCookies, TENANT_ID } from './helpers/prod-auth';

test.describe('Mòdul 26 — Knowledge Base', () => {

  test('API: login correcte amb credencials de producció', async ({ page }) => {
    const token = await loginProd(page);
    expect(token).toBeTruthy();
    expect(token.startsWith('eyJ')).toBeTruthy();
  });

  test('API: GET knowledge base retorna 200', async ({ page }) => {
    const token = await loginProd(page);
    const resp = await apiCall(page, token, 'GET', `/api/v1/agents/knowledge/${TENANT_ID}`);
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data.tenantId).toBe(TENANT_ID);
    expect(typeof data.entriesByCategory).toBe('object');
    expect(Array.isArray(data.documents)).toBeTruthy();
  });

  test('API: PUT entries categoria BEHAVIOR', async ({ page }) => {
    const token = await loginProd(page);
    const resp = await apiCall(page, token, 'PUT',
      `/api/v1/agents/knowledge/${TENANT_ID}/entries/BEHAVIOR`, {
        entries: [
          { key: 'tone', content: 'Respon sempre en català, de forma professional i amable.', sortOrder: 0 },
          { key: 'greeting', content: 'Comença sempre saludant pel nom si el coneixes.', sortOrder: 1 },
        ],
      });
    expect(resp.status()).toBe(204);
  });

  test('API: PUT entries categoria BUSINESS_INFO', async ({ page }) => {
    const token = await loginProd(page);
    const resp = await apiCall(page, token, 'PUT',
      `/api/v1/agents/knowledge/${TENANT_ID}/entries/BUSINESS_INFO`, {
        entries: [
          { key: 'name', content: 'AMG Digitalització — Agència digital a Mallorca', sortOrder: 0 },
          { key: 'phone', content: 'Telèfon: 654 048 164', sortOrder: 1 },
        ],
      });
    expect(resp.status()).toBe(204);
  });

  test('API: GET knowledge block preview retorna text no buit', async ({ page }) => {
    const token = await loginProd(page);
    const resp = await apiCall(page, token, 'GET', `/api/v1/agents/knowledge/${TENANT_ID}/preview`);
    expect(resp.status()).toBe(200);
    const text = await resp.text();
    expect(text.length).toBeGreaterThan(50);
    expect(text).toContain('COMPORTAMENT');
  });

  test('UI: pàgina /portal/agents carrega amb tab Coneixement', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal/agents');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1500);

    const body = await page.locator('body').textContent();
    expect(body).toContain('Coneixement');
  });

  test('UI: tab Coneixement mostra categories', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal/agents');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);

    await page.locator('button:has-text("Coneixement")').click();
    await page.waitForTimeout(2000);

    const body = await page.locator('body').textContent();
    expect(body).toContain('Comportament');
    expect(body).toContain('Horaris');
    expect(body).toContain('Serveis');
  });

  test('UI: tab Coneixement mostra entrades desades', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal/agents');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);

    await page.locator('button:has-text("Coneixement")').click();
    await page.waitForTimeout(2000);

    const body = await page.locator('body').textContent();
    expect(body).toContain('professional i amable');
    expect(body).toContain('AMG Digitalització');
  });

  test('API: afegir i eliminar document', async ({ page }) => {
    const token = await loginProd(page);

    // Afegir document
    const addResp = await apiCall(page, token, 'POST',
      `/api/v1/agents/knowledge/${TENANT_ID}/documents`, {
        filename: 'test-e2e.txt',
        content: 'Contingut de prova per a tests e2e.',
      });
    expect(addResp.status()).toBe(200);
    const doc = await addResp.json();
    expect(doc.id).toBeTruthy();
    expect(doc.filename).toBe('test-e2e.txt');

    // Eliminar document
    const delResp = await apiCall(page, token, 'DELETE',
      `/api/v1/agents/knowledge/${TENANT_ID}/documents/${doc.id}`);
    expect(delResp.status()).toBe(204);
  });
});
