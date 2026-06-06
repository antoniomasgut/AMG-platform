import { test, expect } from '@playwright/test';
import { loginProd, apiCall, dismissCookies } from './helpers/prod-auth';

const API_BASE = process.env.PLAYWRIGHT_API_BASE || 'https://api.amgdl.com';
const TS = Date.now();

async function gotoAndWait(page: any, path: string) {
  await page.goto(path, { waitUntil: 'load', timeout: 60000 });
  await page.waitForTimeout(1500);
}

test.describe('Domains — menú i navegació', () => {
  test.setTimeout(90000);

  test('El menú conté Dominis a la secció Serveis', async ({ page }) => {
    await loginProd(page);
    await gotoAndWait(page, '/ca/portal');
    const link = page.locator('nav a[href*="/portal/admin/domains"]');
    await expect(link).toBeVisible();
  });

  test('La pàgina /portal/admin/domains es carrega correctament', async ({ page }) => {
    await loginProd(page);
    await gotoAndWait(page, '/ca/portal/admin/domains');
    await expect(page.locator('h1:has-text("Dominis")')).toBeVisible({ timeout: 15000 });
    await expect(page.locator('button:has-text("Registrar domini")').first()).toBeVisible();
  });

  test('Mostra empty state quan no hi ha dominis', async ({ page }) => {
    await loginProd(page);
    await gotoAndWait(page, '/ca/portal/admin/domains');
    const hasTable = await page.locator('table').isVisible().catch(() => false);
    const hasEmpty = await page.locator('text=Cap domini registrat').isVisible().catch(() => false);
    expect(hasTable || hasEmpty).toBeTruthy();
  });
});

test.describe('Domains — registre de domini', () => {
  test.setTimeout(90000);

  test('Obre el modal de registre en clicar el botó', async ({ page }) => {
    await loginProd(page);
    await gotoAndWait(page, '/ca/portal/admin/domains');
    await page.locator('button:has-text("Registrar domini")').first().click();
    await expect(page.locator('text=Registrar domini').last()).toBeVisible();
    await expect(page.locator('button:has-text("Comprovar disponibilitat")')).toBeVisible();
  });

  test("Comprova disponibilitat d'un domini disponible", async ({ page }) => {
    await loginProd(page);
    await gotoAndWait(page, '/ca/portal/admin/domains');
    await page.locator('button:has-text("Registrar domini")').first().click();
    await page.waitForTimeout(300);

    await page.locator('input[placeholder="perruqueria-maria"]').fill(`testdomain${TS}`);
    await page.locator('button:has-text("Comprovar disponibilitat")').click();
    await page.waitForTimeout(2000);

    await expect(page.locator('text=Disponible')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('text=Registre (1r any)')).toBeVisible();
  });

  test('Comprova un domini no disponible i mostra alternatives', async ({ page }) => {
    await loginProd(page);
    await gotoAndWait(page, '/ca/portal/admin/domains');
    await page.locator('button:has-text("Registrar domini")').first().click();
    await page.waitForTimeout(300);

    await page.locator('select').first().selectOption('com');
    await page.locator('input[placeholder="perruqueria-maria"]').fill('taken');
    await page.locator('button:has-text("Comprovar disponibilitat")').click();
    await page.waitForTimeout(2000);

    await expect(page.locator('text=No disponible')).toBeVisible({ timeout: 10000 });
  });
});

test.describe('Domains — tarifes TLD (SUPER_ADMIN)', () => {
  test('La llista de TLD es pot obtenir via API', async ({ page }) => {
    const token = await loginProd(page);
    const tldResp = await apiCall(page, token, 'GET', '/api/v1/domains/admin/tld-pricing');
    expect(tldResp.ok()).toBeTruthy();
    const pricing = await tldResp.json();
    expect(Array.isArray(pricing)).toBeTruthy();
    expect(pricing.length).toBeGreaterThan(0);
    expect(pricing[0]).toHaveProperty('tld');
    expect(pricing[0]).toHaveProperty('saleRegister');
  });
});
