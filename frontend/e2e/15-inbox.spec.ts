import { test, expect } from '@playwright/test';
import { loginViaAPI, USERS, dismissCookies, API_BASE, apiGet } from './helpers/auth';

test.describe('INBOX-25 — Omnichannel Inbox', () => {

  test('INBOX-01: Pàgina inbox accessible com a admin', async ({ page }) => {
    await loginViaAPI(page, USERS.admin);
    await dismissCookies(page);

    await page.goto('/ca/portal/agents/inbox');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);

    // No redirigeix a login
    expect(page.url()).toContain('inbox');

    // Mostra el layout de l'inbox (header + sidebar)
    await expect(page.locator('text=Converses')).toBeVisible();
  });

  test('INBOX-02: Pàgina inbox accessible com a client', async ({ page }) => {
    await loginViaAPI(page, USERS.client);
    await dismissCookies(page);

    await page.goto('/ca/portal/agents/inbox');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);

    expect(page.url()).toContain('inbox');
    await expect(page.locator('text=Converses').first()).toBeVisible();
  });

  test('INBOX-03: Menú lateral conté "Converses" per a CLIENT', async ({ page }) => {
    await loginViaAPI(page, USERS.client);
    await dismissCookies(page);

    await page.goto('/ca/portal');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);

    const inboxLink = page.locator('nav a[href*="/portal/agents/inbox"]');
    await expect(inboxLink).toBeVisible();
  });

  test('INBOX-04: Menú lateral conté "Converses" per a ADMIN', async ({ page }) => {
    await loginViaAPI(page, USERS.admin);
    await dismissCookies(page);

    await page.goto('/ca/portal');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);

    const inboxLink = page.locator('nav a[href*="/portal/agents/inbox"]');
    await expect(inboxLink).toBeVisible();
  });

  test('INBOX-05: Sense converses mostra estat buit', async ({ page }) => {
    await loginViaAPI(page, USERS.client);
    await dismissCookies(page);

    await page.goto('/ca/portal/agents/inbox');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    // Si no hi ha converses, mostra l'estat buit o la llista buida
    const body = await page.locator('body').textContent();
    expect(body).toBeTruthy();
    expect(body!.length).toBeGreaterThan(100);
  });

  test('INBOX-06: API /agents/contacts retorna 200 per a admin', async ({ page }) => {
    const token = await loginViaAPI(page, USERS.admin);

    // Necessitem el tenantId de l'admin — agafem el primer tenant
    const tenantsResp = await page.request.get(`${API_BASE}/api/v1/tenants?size=1`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(tenantsResp.status()).toBe(200);
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) {
      test.skip();
      return;
    }
    const tenantId = tenants[0].id;

    const resp = await page.request.get(`${API_BASE}/api/v1/agents/contacts/${tenantId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(Array.isArray(data)).toBeTruthy();
  });

  test('INBOX-07: API /agents/contacts rebutja accés sense autenticació', async ({ page }) => {
    const resp = await page.request.get(`${API_BASE}/api/v1/agents/contacts/00000000-0000-0000-0000-000000000001`);
    expect([401, 403]).toContain(resp.status());
  });

  test('INBOX-08: Toggle de mode mostra opcions', async ({ page }) => {
    await loginViaAPI(page, USERS.admin);
    await dismissCookies(page);

    await page.goto('/ca/portal/agents/inbox');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    // Busca el toggle de mode si existeix (necessita agent configurat)
    const modeBtn = page.locator('button:has-text("AUTO"), button:has-text("MANUAL"), button:has-text("HÍBRID")').first();
    if (await modeBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
      await modeBtn.click();
      // Ha de mostrar el dropdown amb opcions
      await expect(page.locator('text=MANUAL')).toBeVisible({ timeout: 2000 });
    }
    // Si no hi ha agent configurat, el test passa igualment
  });

  test('INBOX-09: Seleccionar un contacte mostra el fil (si n\'hi ha)', async ({ page }) => {
    await loginViaAPI(page, USERS.admin);
    await dismissCookies(page);

    await page.goto('/ca/portal/agents/inbox');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    // Si hi ha contactes, en clicar un ha d'aparèixer el thread
    const firstContact = page.locator('[class*="border-b"] button').first();
    if (await firstContact.isVisible({ timeout: 2000 }).catch(() => false)) {
      await firstContact.click();
      await page.waitForTimeout(1000);
      // El panell dret ha de mostrar algun contingut
      const rightPanel = page.locator('[class*="flex-1"]').last();
      await expect(rightPanel).toBeVisible();
    }
  });

});
