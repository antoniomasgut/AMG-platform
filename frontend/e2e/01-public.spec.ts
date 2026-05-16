import { test, expect } from '@playwright/test';

// Pàgines públiques — sense autenticació

test.describe('PUB-LOCALE — LocaleSwitcher', () => {
  test('PUB-LOCALE-01: visible en mòbil i canvia d\'idioma', async ({ page }) => {
    // Test en viewport mòbil
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/ca');
    await page.waitForLoadState('networkidle');

    // L'enllaç d'idioma CAT ha de ser visible (no hidden sm:inline)
    const caLink = page.locator('a[href*="/ca"]', { hasText: 'CAT' }).first();
    await expect(caLink).toBeVisible();

    // L'enllaç ES ha d'estar visible i funcionar
    const esLink = page.locator('a[href*="/es"]', { hasText: 'ES' }).first();
    await expect(esLink).toBeVisible();
    await esLink.click();

    // Ha de canviar la URL a /es/
    await expect(page).toHaveURL(/\/es/);
  });
});

test.describe('Pàgina principal (landing pública)', () => {
  test('carrega i mostra el hero amb CTA', async ({ page }) => {
    await page.goto('/ca');
    await expect(page).toHaveTitle(/AMG/i);
    await expect(page.locator('h1').first()).toBeVisible();
  });

  test('té la secció de serveis', async ({ page }) => {
    await page.goto('/ca');
    await page.waitForLoadState('networkidle');
    // Comprova que hi ha contingut significatiu (no pàgina en blanc)
    const body = await page.locator('main, body').first().textContent();
    expect(body?.length).toBeGreaterThan(100);
  });
});

test.describe('Pàgines legals', () => {
  test('/ca/legal/privacitat carrega', async ({ page }) => {
    await page.goto('/ca/legal/privacitat');
    await page.waitForLoadState('networkidle');
    const body = await page.locator('body').textContent();
    expect(body?.length).toBeGreaterThan(200);
  });

  test('/ca/legal/avis-legal carrega', async ({ page }) => {
    await page.goto('/ca/legal/avis-legal');
    await page.waitForLoadState('networkidle');
    const body = await page.locator('body').textContent();
    expect(body?.length).toBeGreaterThan(200);
  });

  test('/ca/legal/termes-servei carrega', async ({ page }) => {
    await page.goto('/ca/legal/termes-servei');
    await page.waitForLoadState('networkidle');
    const body = await page.locator('body').textContent();
    expect(body?.length).toBeGreaterThan(200);
  });

  test('/ca/legal/cookies carrega', async ({ page }) => {
    await page.goto('/ca/legal/cookies');
    await page.waitForLoadState('networkidle');
    const body = await page.locator('body').textContent();
    expect(body?.length).toBeGreaterThan(200);
  });
});

test.describe('PUB-LAND — Millores landing (testimonials + CTA demo)', () => {
  test('PUB-LAND-01: secció de testimonis es renderitza', async ({ page }) => {
    await page.goto('/ca');
    await page.waitForLoadState('networkidle');

    const testimonials = page.locator('#testimonials');
    await expect(testimonials).toBeVisible();

    // Ha de contenir testimonis amb noms
    await expect(testimonials.locator('h2')).toBeVisible();
    const cards = testimonials.locator('.amg-card');
    await expect(cards.first()).toBeVisible();
  });

  test('PUB-LAND-02: botó de demo existeix i té href correcte', async ({ page }) => {
    await page.goto('/ca');
    await page.waitForLoadState('networkidle');

    const demoBtn = page.locator('a[href*="mailto:amg"]');
    await expect(demoBtn).toBeVisible();
    await expect(demoBtn).toContainText('DEMO');
  });
});

test.describe('Pàgines auth públiques', () => {
  test('login mostra formulari d\'email i password', async ({ page }) => {
    await page.goto('/ca/login');
    // La pàgina és 'use client' — esperar que React hidrati
    await page.waitForSelector('input[type="email"]', { timeout: 15000 });
    await expect(page.locator('input[type="email"]').first()).toBeVisible();
    await expect(page.locator('input[type="password"]').first()).toBeVisible();
  });

  test('forgot-password mostra formulari', async ({ page }) => {
    await page.goto('/ca/forgot-password');
    await page.waitForSelector('input[type="email"]', { timeout: 15000 });
    await expect(page.locator('input[type="email"]').first()).toBeVisible();
  });

  test('portal sense auth redirigeix al login', async ({ page }) => {
    await page.goto('/ca/portal');
    // Esperar que la redirecció al login es completi
    await page.waitForURL(/\/(ca|es|en|de)\/login/, { timeout: 15000 });
    expect(page.url()).toContain('/login');
  });
});
