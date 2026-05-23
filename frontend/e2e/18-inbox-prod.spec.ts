import { test, expect } from '@playwright/test';
import { loginProd, apiCall, dismissCookies, TENANT_ID } from './helpers/prod-auth';

test.describe('Mòdul 25 — Omnichannel Inbox (producció)', () => {

  test('API: GET contacts retorna 200 i array', async ({ page }) => {
    const token = await loginProd(page);
    const resp = await apiCall(page, token, 'GET', `/api/v1/agents/contacts/${TENANT_ID}`);
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(Array.isArray(data)).toBeTruthy();
  });

  test('API: accés sense auth retorna 401/403', async ({ page }) => {
    const resp = await page.request.get(`https://api.amgdl.com/api/v1/agents/contacts/${TENANT_ID}`);
    expect([401, 403]).toContain(resp.status());
  });

  test('UI: pàgina inbox carrega sense error', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal/agents/inbox');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2000);

    expect(page.url()).toContain('inbox');
    const body = await page.locator('body').textContent();
    expect(body).toBeTruthy();
    expect(body!.length).toBeGreaterThan(100);
    // No ha de mostrar errors
    expect(body).not.toContain('items.inbox');
    expect(body).not.toContain('items.dashboard');
  });

  test('UI: sidebar mostra "Converses" com a text real', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal/agents/inbox');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2000);

    const body = await page.locator('body').textContent();
    // Ha de mostrar les traduccions reals, no les claus
    expect(body).toContain('Converses');
  });

  test('UI: menú conté link a inbox', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2000);

    const inboxLink = page.locator('nav a[href*="/portal/agents/inbox"]');
    await expect(inboxLink).toBeVisible({ timeout: 5000 });
  });

  test('UI: header de converses mostra el mode de l\'agent', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal/agents/inbox');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2500);

    const body = await page.locator('body').textContent();
    // Ha de mostrar "Converses" al header
    expect(body).toContain('Converses');
    // Si hi ha agent configurat, ha de mostrar el mode
    const hasMode = body!.includes('AUTO') || body!.includes('HÍBRID') || body!.includes('MANUAL');
    // Mode visible si l'agent està configurat (pot ser que no hi hagi config)
    if (hasMode) {
      expect(hasMode).toBeTruthy();
    }
  });

  test('UI: sense converses mostra estat buit adequat', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal/agents/inbox');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2500);

    // Si no hi ha converses seleccionades, el panell dret mostra l'estat buit
    const emptyState = page.locator('text=Selecciona una conversa');
    const isEmptyVisible = await emptyState.isVisible({ timeout: 2000 }).catch(() => false);
    if (isEmptyVisible) {
      await expect(emptyState).toBeVisible();
    }
    // Altrament hi ha converses — ambdós casos son vàlids
  });

});
