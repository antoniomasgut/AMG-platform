import { test, expect } from '@playwright/test';
import { loginProd, apiCall, dismissCookies, API_BASE } from './helpers/prod-auth';

const TIMESTAMP = Date.now();
const TEST_TENANT_SLUG = `e2e-del-${TIMESTAMP}`;

test.describe('Eliminar tenant — SUPER_ADMIN', () => {

  let tenantId: string;
  let token: string;

  test.beforeAll(async ({ browser }) => {
    const page = await browser.newPage();
    token = await loginProd(page);

    // Crea tenant de prova
    const resp = await apiCall(page, token, 'POST', '/api/v1/tenants', {
      name: `E2E Delete ${TIMESTAMP}`,
      slug: TEST_TENANT_SLUG,
      email: `e2e-del-${TIMESTAMP}@test.com`,
    });
    expect(resp.status(), `Crear tenant: ${await resp.text()}`).toBe(201);
    const data = await resp.json();
    tenantId = data.id;
    await page.close();
  });

  test.afterAll(async ({ browser }) => {
    // Neteja si el test va fallar i no va eliminar el tenant
    if (!tenantId) return;
    const page = await browser.newPage();
    try {
      const t = await loginProd(page);
      await apiCall(page, t, 'DELETE', `/api/v1/tenants/${tenantId}`);
    } catch { /* ignore */ }
    await page.close();
  });

  test('API: delete-check retorna canDelete=true per tenant nou', async ({ page }) => {
    token = await loginProd(page);
    const resp = await apiCall(page, token, 'GET', `/api/v1/tenants/${tenantId}/delete-check`);
    expect(resp.status(), `delete-check status: ${await resp.text()}`).toBe(200);
    const data = await resp.json();
    expect(data.canDelete).toBe(true);
  });

  test('API: DELETE /tenants/{id} retorna 204 i elimina el tenant', async ({ page }) => {
    token = await loginProd(page);

    const del = await apiCall(page, token, 'DELETE', `/api/v1/tenants/${tenantId}`);
    expect(del.status(), `DELETE response: ${await del.text()}`).toBe(204);

    // Verifica que ja no existeix
    const get = await apiCall(page, token, 'GET', `/api/v1/tenants/${tenantId}`);
    expect(get.status()).toBe(404);

    tenantId = ''; // ja eliminat, afterAll no ha d'intentar-ho
  });

  test('UI: modal eliminar tenant — apareix motius i confirma correctament', async ({ page }) => {
    // Crea un nou tenant per la prova UI
    token = await loginProd(page);
    const resp = await apiCall(page, token, 'POST', '/api/v1/tenants', {
      name: `E2E Delete UI ${TIMESTAMP}`,
      slug: `e2e-del-ui-${TIMESTAMP}`,
      email: `e2e-del-ui-${TIMESTAMP}@test.com`,
    });
    expect(resp.status()).toBe(201);
    const { id: uiTenantId } = await resp.json();

    await page.goto(`/ca/portal/admin/tenants/${uiTenantId}`);
    await dismissCookies(page);
    await page.waitForLoadState('networkidle');

    // Intercepta la resposta DELETE per veure l'status code real
    const deleteResponse = page.waitForResponse(
      r => r.url().includes(`/api/v1/tenants/${uiTenantId}`) && r.request().method() === 'DELETE'
    );

    // Clica "Eliminar"
    await page.getByRole('button', { name: 'Eliminar' }).click();

    // Espera que aparegui el modal
    await expect(page.getByText('Eliminar tenant')).toBeVisible({ timeout: 5000 });

    // Clica "Confirmar i eliminar"
    await page.getByRole('button', { name: /Confirmar i eliminar/i }).click();

    // Recull la resposta DELETE
    const delResp = await deleteResponse;
    expect(delResp.status(), `DELETE retornà ${delResp.status()}`).toBe(204);

    // Ha de redirigir a la llista de tenants
    await expect(page).toHaveURL(/\/portal\/admin\/tenants$/, { timeout: 8000 });

    // Cleanup
    tenantId = '';
  });

});
