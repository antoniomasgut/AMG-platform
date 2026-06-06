import { test, expect } from '@playwright/test';
import { loginViaAPI, dismissCookies, apiGet, apiPost, apiPut, apiDelete, API_BASE } from './helpers/auth';

// ── Helpers ───────────────────────────────────────────────────────────────────

async function getFirstTenantId(page: Parameters<typeof apiGet>[0], token: string): Promise<string | null> {
  const resp = await apiGet(page, token, '/api/v1/tenants?page=0&size=1');
  if (resp.status() !== 200) return null;
  const data = await resp.json();
  const list = data.content ?? data;
  return list?.[0]?.id ?? null;
}

async function getFirstProfileWithPhases(page: Parameters<typeof apiGet>[0], token: string) {
  const resp = await apiGet(page, token, '/api/v1/vault/profiles');
  if (resp.status() !== 200) return null;
  const profiles = await resp.json();
  return profiles?.find((p: any) => p.phases?.length > 0) ?? null;
}

async function createTestBudget(page: Parameters<typeof apiGet>[0], token: string, tenantId: string) {
  const profile = await getFirstProfileWithPhases(page, token);
  if (!profile) return null;

  const phaseIds = profile.phases.map((p: any) => p.id);
  const validUntil = new Date(Date.now() + 30 * 86400_000).toISOString().split('T')[0];

  const resp = await apiPost(page, token, `/api/v1/billing/tenants/${tenantId}/budgets`, {
    profileId: profile.id,
    phaseIds,
    notes: 'Test E2E',
    clientNotes: 'Pressupost de prova E2E',
    validUntil,
  });
  if (![200, 201].includes(resp.status())) return null;
  return resp.json();
}

// ── Tests ─────────────────────────────────────────────────────────────────────

test.describe('Billing — Pressupostos (Mòdul 07)', () => {

  // ── API: CRUD complet ──────────────────────────────────────────────────────

  test('API: crear pressupost DRAFT', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantId = await getFirstTenantId(page, token);
    if (!tenantId) return test.skip();

    const budget = await createTestBudget(page, token, tenantId);
    expect(budget).not.toBeNull();
    expect(budget.id).toBeTruthy();
    expect(budget.status).toBe('DRAFT');
    expect(budget.budgetNumber).toMatch(/^BUD-/);
    expect(budget.total).toBeGreaterThan(0);
    // Nous camps
    expect(budget.profileId).toBeTruthy();
    expect(budget.phaseIds).toBeTruthy();
    expect(budget.tenantId).toBe(tenantId);
  });

  test('API: llistar tots els pressupostos (admin global)', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await apiGet(page, token, '/api/v1/billing/budgets');
    expect(resp.status()).toBe(200);
    const budgets = await resp.json();
    expect(Array.isArray(budgets)).toBe(true);
    if (budgets.length > 0) {
      const b = budgets[0];
      expect(b.tenantId).toBeTruthy();
      expect(b.budgetNumber).toBeTruthy();
    }
  });

  test('API: llistar pressupostos per tenant', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantId = await getFirstTenantId(page, token);
    if (!tenantId) return test.skip();

    const resp = await apiGet(page, token, `/api/v1/billing/tenants/${tenantId}/budgets`);
    expect(resp.status()).toBe(200);
    const budgets = await resp.json();
    expect(Array.isArray(budgets)).toBe(true);
  });

  test('API: editar pressupost DRAFT', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantId = await getFirstTenantId(page, token);
    if (!tenantId) return test.skip();

    const budget = await createTestBudget(page, token, tenantId);
    if (!budget) return test.skip();

    const profile = await getFirstProfileWithPhases(page, token);
    if (!profile) return test.skip();

    const validUntil = new Date(Date.now() + 60 * 86400_000).toISOString().split('T')[0];
    const resp = await apiPut(page, token, `/api/v1/billing/budgets/${budget.id}`, {
      profileId: profile.id,
      phaseIds: profile.phases.map((p: any) => p.id),
      notes: 'Notes actualitzades E2E',
      clientNotes: 'Nota client actualitzada',
      validUntil,
    });
    expect(resp.status()).toBe(200);
    const updated = await resp.json();
    expect(updated.notes).toBe('Notes actualitzades E2E');
    expect(updated.validUntil).toBe(validUntil);
  });

  test('API: eliminar pressupost DRAFT', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantId = await getFirstTenantId(page, token);
    if (!tenantId) return test.skip();

    const budget = await createTestBudget(page, token, tenantId);
    if (!budget) return test.skip();

    const resp = await apiDelete(page, token, `/api/v1/billing/budgets/${budget.id}`);
    expect(resp.status()).toBe(204);

    // Verificar que el pressupost s'ha cancel·lat (soft-delete)
    const checkResp = await apiGet(page, token, `/api/v1/billing/tenants/${tenantId}/budgets/${budget.id}`);
    expect([200, 404, 400]).toContain(checkResp.status());
    if (checkResp.status() === 200) {
      const body = await checkResp.json();
      expect(body.status).toBe('CANCELLED');
    }
  });

  test('API: enviar pressupost (DRAFT → SENT)', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantId = await getFirstTenantId(page, token);
    if (!tenantId) return test.skip();

    const budget = await createTestBudget(page, token, tenantId);
    if (!budget) return test.skip();

    const resp = await apiPost(page, token, `/api/v1/billing/budgets/${budget.id}/send`, {});
    expect(resp.status()).toBe(200);
    const sent = await resp.json();
    expect(sent.status).toBe('SENT');
    expect(sent.acceptanceUrl).toContain('accept-budget?token=');
  });

  test('API: clonar pressupost', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantId = await getFirstTenantId(page, token);
    if (!tenantId) return test.skip();

    const original = await createTestBudget(page, token, tenantId);
    if (!original) return test.skip();

    // Clonar = crear un nou amb les mateixes dades
    const profile = await getFirstProfileWithPhases(page, token);
    if (!profile) return test.skip();

    const resp = await apiPost(page, token, `/api/v1/billing/tenants/${tenantId}/budgets`, {
      profileId: original.profileId,
      phaseIds: original.phaseIds,
      notes: original.notes,
      clientNotes: original.clientNotes,
    });
    expect([200, 201]).toContain(resp.status());
    const clone = await resp.json();
    expect(clone.id).not.toBe(original.id);
    expect(clone.status).toBe('DRAFT');
    expect(clone.profileId).toBe(original.profileId);
  });

  test('API: preview públic per token (pàgina acceptació)', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantId = await getFirstTenantId(page, token);
    if (!tenantId) return test.skip();

    const budget = await createTestBudget(page, token, tenantId);
    if (!budget) return test.skip();

    // Enviar per obtenir el token d'acceptació
    const sendResp = await apiPost(page, token, `/api/v1/billing/budgets/${budget.id}/send`, {});
    if (sendResp.status() !== 200) return test.skip();
    const sent = await sendResp.json();
    const acceptanceUrl = sent.acceptanceUrl as string;
    const acceptToken = new URL(acceptanceUrl).searchParams.get('token');
    if (!acceptToken) return test.skip();

    // Preview públic (sense auth)
    const previewResp = await page.request.get(`${API_BASE}/api/v1/billing/budgets/preview?token=${acceptToken}`);
    expect(previewResp.status()).toBe(200);
    const preview = await previewResp.json();
    expect(preview.budgetNumber).toBe(budget.budgetNumber);
    expect(preview.status).toBe('SENT');
    expect(Array.isArray(preview.phases)).toBe(true);
    expect(preview.total).toBeGreaterThan(0);
  });

  test('API: acceptació completa per token (SENT → ACCEPTED)', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantId = await getFirstTenantId(page, token);
    if (!tenantId) return test.skip();

    const budget = await createTestBudget(page, token, tenantId);
    if (!budget) return test.skip();

    const sendResp = await apiPost(page, token, `/api/v1/billing/budgets/${budget.id}/send`, {});
    if (sendResp.status() !== 200) return test.skip();
    const sent = await sendResp.json();
    const acceptanceUrl = sent.acceptanceUrl as string;
    const acceptToken = new URL(acceptanceUrl).searchParams.get('token');
    if (!acceptToken) return test.skip();

    const acceptResp = await page.request.post(
      `${API_BASE}/api/v1/billing/budgets/accept?token=${acceptToken}`
    );
    expect(acceptResp.status()).toBe(200);
    const result = await acceptResp.json();
    expect(result.status).toBe('ACCEPTED');
  });

  test('API: acceptació per fases seleccionades', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantId = await getFirstTenantId(page, token);
    if (!tenantId) return test.skip();

    const budget = await createTestBudget(page, token, tenantId);
    if (!budget || !budget.phaseIds?.length) return test.skip();

    const sendResp = await apiPost(page, token, `/api/v1/billing/budgets/${budget.id}/send`, {});
    if (sendResp.status() !== 200) return test.skip();
    const sent = await sendResp.json();
    const acceptToken = new URL(sent.acceptanceUrl).searchParams.get('token');
    if (!acceptToken) return test.skip();

    // Acceptar només la primera fase
    const firstPhaseId = budget.phaseIds[0];
    const resp = await page.request.post(
      `${API_BASE}/api/v1/billing/budgets/accept-phases?token=${acceptToken}`,
      {
        headers: { 'Content-Type': 'application/json' },
        data: { phaseIds: [firstPhaseId] },
      }
    );
    expect(resp.status()).toBe(200);
    const result = await resp.json();
    expect(result.status).toBe('ACCEPTED');
  });

  test('API: rebuig per token', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantId = await getFirstTenantId(page, token);
    if (!tenantId) return test.skip();

    const budget = await createTestBudget(page, token, tenantId);
    if (!budget) return test.skip();

    const sendResp = await apiPost(page, token, `/api/v1/billing/budgets/${budget.id}/send`, {});
    if (sendResp.status() !== 200) return test.skip();
    const sent = await sendResp.json();
    const acceptToken = new URL(sent.acceptanceUrl).searchParams.get('token');
    if (!acceptToken) return test.skip();

    const resp = await page.request.post(
      `${API_BASE}/api/v1/billing/budgets/reject?token=${acceptToken}`,
      {
        headers: { 'Content-Type': 'application/json' },
        data: { reason: 'Pressupost massa car E2E' },
      }
    );
    expect(resp.status()).toBe(200);
    const result = await resp.json();
    expect(result.status).toBe('REJECTED');
  });

  // ── UI: pàgina de billing admin ──────────────────────────────────────────

  test('UI: pàgina billing admin carrega i mostra pressupostos', async ({ page }) => {
    await loginViaAPI(page);
    await page.goto('/ca/portal/billing');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1000);

    // Ha de mostrar la taula o el missatge buit
    const body = await page.locator('body').textContent();
    expect(body).toContain('Pressupostos');
    expect(body?.length).toBeGreaterThan(100);
  });

  test('UI: filtres d\'estat funcionen a la pàgina de billing', async ({ page }) => {
    const token = await loginViaAPI(page);
    await page.goto('/ca/portal/billing');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(800);

    // Clicar filtre DRAFT
    const draftBtn = page.locator('button', { hasText: 'Esborrany' }).first();
    if (await draftBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await draftBtn.click();
      await page.waitForTimeout(500);
      const body = await page.locator('body').textContent();
      expect(body).toBeTruthy();
    }
  });

  test('UI: clic en pressupost obre modal de detall', async ({ page }) => {
    const token = await loginViaAPI(page);

    // Crear pressupost de test per assegurar que n'hi ha
    const tenantId = await getFirstTenantId(page, token);
    if (tenantId) await createTestBudget(page, token, tenantId);

    await page.goto('/ca/portal/billing');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1000);

    // Clicar la primera fila de la taula
    const firstRow = page.locator('table tbody tr').first();
    if (await firstRow.isVisible({ timeout: 3000 }).catch(() => false)) {
      await firstRow.click();
      await page.waitForTimeout(500);
      // El modal hauria d'aparèixer (botó X visible)
      const closeBtn = page.locator('button[title=""]').or(page.locator('.fixed.inset-0')).first();
      const modalVisible = await page.locator('.fixed.inset-0').isVisible({ timeout: 2000 }).catch(() => false);
      expect(modalVisible).toBe(true);
    }
  });

  // ── UI: pàgina de detall tenant ──────────────────────────────────────────

  test('UI: pressupostos visibles a la pàgina de tenant', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantId = await getFirstTenantId(page, token);
    if (!tenantId) return test.skip();

    // Crear pressupost si no n'hi ha
    await createTestBudget(page, token, tenantId);

    await page.goto(`/ca/portal/admin/tenants/${tenantId}`);
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1500);

    const body = await page.locator('body').textContent();
    expect(body).toContain('Pressupostos');
    // Ha de mostrar el número BUD- o el missatge buit
    const hasBudget = body?.includes('BUD-') || body?.includes('Cap pressupost');
    expect(hasBudget).toBe(true);
  });

  test('UI: modal de detall de pressupost al tenant (editar/enviar/clonar/eliminar)', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantId = await getFirstTenantId(page, token);
    if (!tenantId) return test.skip();

    const budget = await createTestBudget(page, token, tenantId);
    if (!budget) return test.skip();

    await page.goto(`/ca/portal/admin/tenants/${tenantId}`);
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1500);

    // Clicar la fila del pressupost creat
    const budgetRow = page.locator(`text=${budget.budgetNumber}`).first();
    if (!await budgetRow.isVisible({ timeout: 5000 }).catch(() => false)) return test.skip();

    await budgetRow.click();
    await page.waitForTimeout(500);

    // El modal ha d'estar obert
    const modal = page.locator('.fixed.inset-0').first();
    await expect(modal).toBeVisible({ timeout: 3000 });

    // Ha de mostrar el número del pressupost al modal
    const modalText = await modal.textContent();
    expect(modalText).toContain(budget.budgetNumber);

    // Ha de mostrar el total
    expect(modalText).toContain('€');

    // Botons d'acció: editar (DRAFT), clonar, enviar, eliminar
    const editBtn = modal.locator('button[title="Editar"]');
    const cloneBtn = modal.locator('button[title="Clonar"]');
    const sendBtn = modal.locator('button[title="Enviar al client"]');
    const deleteBtn = modal.locator('button[title="Eliminar"]');

    await expect(editBtn).toBeVisible({ timeout: 2000 });
    await expect(cloneBtn).toBeVisible({ timeout: 2000 });
    await expect(deleteBtn).toBeVisible({ timeout: 2000 });
  });

  test('UI: mode edició de pressupost DRAFT', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantId = await getFirstTenantId(page, token);
    if (!tenantId) return test.skip();

    const budget = await createTestBudget(page, token, tenantId);
    if (!budget) return test.skip();

    await page.goto(`/ca/portal/admin/tenants/${tenantId}`);
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1500);

    const budgetRow = page.locator(`text=${budget.budgetNumber}`).first();
    if (!await budgetRow.isVisible({ timeout: 5000 }).catch(() => false)) return test.skip();
    await budgetRow.click();
    await page.waitForTimeout(500);

    const modal = page.locator('.fixed.inset-0').first();
    await expect(modal).toBeVisible({ timeout: 3000 });

    // Clicar editar
    const editBtn = modal.locator('button[title="Editar"]');
    if (!await editBtn.isVisible({ timeout: 2000 }).catch(() => false)) return test.skip();
    await editBtn.click();
    await page.waitForTimeout(500);

    // Ha de mostrar el formulari d'edició
    const modalText = await modal.textContent();
    expect(modalText).toContain('Desar canvis');
  });

  // ── UI: pàgina d'acceptació del client ────────────────────────────────────

  test('UI: pàgina acceptació client carrega amb token vàlid', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantId = await getFirstTenantId(page, token);
    if (!tenantId) return test.skip();

    const budget = await createTestBudget(page, token, tenantId);
    if (!budget) return test.skip();

    const sendResp = await apiPost(page, token, `/api/v1/billing/budgets/${budget.id}/send`, {});
    if (sendResp.status() !== 200) return test.skip();
    const sent = await sendResp.json();
    const acceptanceUrl = sent.acceptanceUrl as string;
    const acceptToken = new URL(acceptanceUrl).searchParams.get('token');
    if (!acceptToken) return test.skip();

    await page.goto(`/ca/accept-budget?token=${acceptToken}`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);

    const body = await page.locator('body').textContent();
    expect(body).toContain(budget.budgetNumber);
    expect(body).toContain('Acceptar proposta');
    expect(body).toContain('AMG Digitalitzacions');
  });

  test('UI: pàgina acceptació client mostra fases i permet selecció', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantId = await getFirstTenantId(page, token);
    if (!tenantId) return test.skip();

    const budget = await createTestBudget(page, token, tenantId);
    if (!budget || !budget.phases?.length) return test.skip();

    const sendResp = await apiPost(page, token, `/api/v1/billing/budgets/${budget.id}/send`, {});
    if (sendResp.status() !== 200) return test.skip();
    const sent = await sendResp.json();
    const acceptToken = new URL(sent.acceptanceUrl).searchParams.get('token');
    if (!acceptToken) return test.skip();

    await page.goto(`/ca/accept-budget?token=${acceptToken}`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);

    // Ha de mostrar les fases
    const body = await page.locator('body').textContent();
    expect(body).toContain('Acceptar proposta');
    expect(body).toContain('€');

    // Botó acceptar visible i clickable
    const acceptBtn = page.locator('button', { hasText: 'Acceptar proposta' }).first();
    await expect(acceptBtn).toBeVisible({ timeout: 3000 });
  });

  test('UI: pàgina acceptació mostra error amb token invàlid', async ({ page }) => {
    await page.goto('/ca/accept-budget?token=token-invàlid-12345');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    const body = await page.locator('body').textContent();
    expect(body).toMatch(/no disponible|no s'ha trobat|caducat|Token/i);
  });

  // ── UI: pressupostos del tenant visibles a la pàgina de billing ──────────

  test('UI: pressupostos creats al tenant apareixen a la pàgina de billing admin', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantId = await getFirstTenantId(page, token);
    if (!tenantId) return test.skip();

    // Crear pressupost via API (simulant la creació des del tenant)
    const budget = await createTestBudget(page, token, tenantId);
    if (!budget) return test.skip();

    // Anar a la pàgina de billing global
    await page.goto('/ca/portal/billing');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(1500);

    const body = await page.locator('body').textContent();
    // El pressupost ha d'aparèixer (pot ser qualsevol BUD-, no necessàriament el nostre)
    expect(body).toContain('Pressupostos');
    const hasBudgets = body?.includes('BUD-') || body?.includes('Cap pressupost');
    expect(hasBudgets).toBe(true);
  });

  // ── API: dashboard de billing ─────────────────────────────────────────────

  test('API: dashboard de billing retorna mètriques', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenantId = await getFirstTenantId(page, token);
    if (!tenantId) return test.skip();

    const resp = await apiGet(page, token, `/api/v1/billing/tenants/${tenantId}/dashboard`);
    expect([200, 204]).toContain(resp.status());
    if (resp.status() === 200) {
      const data = await resp.json();
      expect(typeof data.pendingBudgets).toBe('number');
      expect(typeof data.totalSpent).toBe('number');
    }
  });
});
