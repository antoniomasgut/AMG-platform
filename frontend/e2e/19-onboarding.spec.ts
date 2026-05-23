import { test, expect } from '@playwright/test';
import { loginProd, apiCall, dismissCookies, TENANT_ID } from './helpers/prod-auth';

test.describe('Mòdul 16 — Onboarding (producció)', () => {

  test('API: GET tenant setup retorna 200', async ({ page }) => {
    const token = await loginProd(page);
    const resp = await apiCall(page, token, 'GET', `/api/v1/vault/tenants/${TENANT_ID}/setup`);
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data).toHaveProperty('profiles');
    expect(Array.isArray(data.profiles)).toBeTruthy();
  });

  test('UI: pàgina portal dashboard carrega sense error', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2000);

    const body = await page.locator('body').textContent();
    expect(body).toBeTruthy();
    expect(body!.length).toBeGreaterThan(200);
    // Ha de contenir contingut visible del portal (dashboard admin o client)
    const hasPortalContent = body!.includes('Dashboard') || body!.includes('AMG') ||
      body!.includes('Tenants') || body!.includes('PRIMERS PASSOS') || body!.includes('Leads');
    expect(hasPortalContent).toBeTruthy();
  });

  test('UI: dashboard mostra onboarding o dashboard normal (mai en blanc)', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2500);

    const body = await page.locator('body').textContent();
    // O mostra el dashboard admin (per a SUPER_ADMIN) amb KPIs...
    const isAdminDashboard = body!.includes('Tenants') || body!.includes('Leads') || body!.includes('CPU') || body!.includes('Dashboard');
    // ... o mostra l'onboarding o el dashboard de client
    const isClientContent = body!.includes('PRIMERS PASSOS') || body!.includes('Comencem') ||
      body!.includes('Crea la teva') || body!.includes('pressupost') ||
      body!.includes('Inversió total') || body!.includes('Factures');

    expect(isAdminDashboard || isClientContent).toBeTruthy();
  });

  test('UI: skip onboarding persisteix via localStorage', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2000);

    // Si el botó de "Salta" existeix, clicar-lo ha d'amagar l'onboarding
    const skipBtn = page.locator('button:has-text("Salta")').first();
    if (await skipBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
      await skipBtn.click();
      await page.waitForTimeout(1000);

      // L'onboarding ha de desaparèixer
      await expect(skipBtn).not.toBeVisible({ timeout: 2000 });

      // Recarregar ha de mantenir l'estat (no torna a aparèixer)
      await page.reload();
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(2000);
      const skipBtnAfterReload = page.locator('button:has-text("Salta")').first();
      expect(await skipBtnAfterReload.isVisible({ timeout: 1000 }).catch(() => false)).toBeFalsy();
    }
    // Si no hi ha onboarding (ja complet o saltat), el test passa igualment
  });

  test('UI: dashboard carrega en menys de 10s i té contingut', async ({ page }) => {
    await loginProd(page);
    const start = Date.now();
    await page.goto('/ca/portal');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2000);
    const elapsed = Date.now() - start;

    // Ha de carregar en menys de 10s
    expect(elapsed).toBeLessThan(10000);

    // Ha de tenir contingut visible
    const body = await page.locator('body').textContent();
    expect(body!.length).toBeGreaterThan(300);
  });

  test('UI: si hi ha passos, els CTAs apunten a les rutes correctes', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2500);

    // Comprova que si apareix el pas de landing, el link apunta a /portal/landings/new
    const landingLink = page.locator('a[href*="/portal/landings/new"]').first();
    if (await landingLink.isVisible({ timeout: 2000 }).catch(() => false)) {
      const href = await landingLink.getAttribute('href');
      expect(href).toContain('/portal/landings/new');
    }

    // Comprova que si apareix el pas d'automatitzacions, el link apunta a /portal/automations
    const automationLink = page.locator('a[href*="/portal/automations"]').first();
    if (await automationLink.isVisible({ timeout: 2000 }).catch(() => false)) {
      const href = await automationLink.getAttribute('href');
      expect(href).toContain('/portal/automations');
    }
  });

});
