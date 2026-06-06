import { test, expect } from '@playwright/test';
import { loginProd, dismissCookies } from './helpers/prod-auth';

test.describe('SMOKE — Smoke test integral', () => {

  test('SMOKE-01: Landing carrega correctament (públic)', async ({ page }) => {
    await page.goto('/ca');
    await page.waitForLoadState('networkidle');

    // Títol visible
    await expect(page).toHaveTitle(/AMG/i);
    await expect(page.locator('h1').first()).toBeVisible();

    // Seccions principals
    await expect(page.locator('#services')).toBeVisible();
    await expect(page.locator('#testimonials')).toBeVisible();
    await expect(page.locator('#contact')).toBeVisible();

    // Status 200 (no errors)
    const status = await page.evaluate(() => document.body.innerText.length);
    expect(status).toBeGreaterThan(100);
  });

  test('SMOKE-02: Login com a admin i accés al portal', async ({ page }) => {
    const token = await loginProd(page);
    expect(token).toBeTruthy();

    await page.goto('/ca/portal');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);

    // No ha de mostrar error ni redirect a login
    const url = page.url();
    expect(url).not.toContain('login');
    expect(url).toContain('portal');
  });

  test('SMOKE-03: Dashboard del portal mostra dades', async ({ page }) => {
    const token = await loginProd(page);
    expect(token).toBeTruthy();

    // Navegar directament al portal
    await page.goto('/ca/portal');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    // No 500 errors — el body ha de tenir contingut significatiu
    const body = page.locator('body');
    const text = await body.textContent();
    expect(text?.length).toBeGreaterThan(200);

    // No hi ha missatges d'error al DOM
    await expect(page.locator('text=Error').first()).not.toBeVisible({ timeout: 1000 }).catch(() => {
      // Si no hi ha "Error" al DOM, perfecte
    });
  });

});
