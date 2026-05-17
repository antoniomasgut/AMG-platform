import { test, expect } from '@playwright/test';

const LEGAL_ROUTES = [
  { path: 'avis-legal' },
  { path: 'politica-privacitat' },
  { path: 'termes-servei' },
];

test.describe('Legal pages', () => {
  for (const { path } of LEGAL_ROUTES) {
    test(`/${path} renders with h1 in ca`, async ({ page }) => {
      await page.goto(`/ca/legal/${path}`);
      await page.waitForLoadState('networkidle');
      await expect(page.locator('h1')).toBeVisible({ timeout: 10000 });
    });
  }

  test('all legal pages render in all locales', async ({ page }) => {
    const locales = ['ca', 'es', 'en', 'de'] as const;
    for (const locale of locales) {
      for (const { path } of LEGAL_ROUTES) {
        await page.goto(`/${locale}/legal/${path}`);
        await page.waitForLoadState('networkidle');
        await expect(page.locator('h1')).toBeVisible({ timeout: 10000 });
      }
    }
  });
});
