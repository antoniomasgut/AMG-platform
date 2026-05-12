import { test, expect } from '@playwright/test';

const LOCALES = ['ca', 'es', 'en', 'de'] as const;

test.describe('Smoke — landing page', () => {
  for (const locale of LOCALES) {
    test(`loads and shows content in ${locale}`, async ({ page }) => {
      await page.goto(`/${locale}`);

      // Wait for page to be fully rendered
      await page.waitForLoadState('networkidle');

      // Hero section is visible
      await expect(page.locator('h1').first()).toBeVisible({ timeout: 10000 });

      // Header nav exists
      await expect(page.locator('header').or(page.getByRole('banner'))).toBeVisible();

      // Pricing section has pricing tiers
      await expect(page.getByText(/29|49|99/).first()).toBeVisible({ timeout: 10000 });

      // Footer
      await expect(page.locator('footer')).toBeVisible();
    });
  }
});

test.describe('Smoke — locale redirect', () => {
  test('root / redirects to /ca', async ({ page }) => {
    await page.goto('/');
    await page.waitForURL(/\/ca/);
    expect(page.url()).toContain('/ca');
  });

  test('root / redirects to a locale prefix', async ({ page }) => {
    await page.goto('/');
    await page.waitForURL(/\/(ca|es|en|de)/);
  });
});
