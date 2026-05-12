import { test, expect } from '@playwright/test';

test.describe('Locale & SEO', () => {
  test('hreflang alternates in head', async ({ page }) => {
    await page.goto('/ca');
    await page.waitForLoadState('networkidle');

    const hreflangCount = await page.evaluate(() => {
      return document.querySelectorAll('link[hreflang]').length;
    });
    expect(hreflangCount).toBeGreaterThanOrEqual(4);
  });

  test('canonical link present', async ({ page }) => {
    await page.goto('/ca');
    await page.waitForLoadState('networkidle');

    const canonical = await page.evaluate(() => {
      const link = document.querySelector('link[rel="canonical"]');
      return link ? link.getAttribute('href') : null;
    });
    expect(canonical).toBeTruthy();
  });

  test('locale switcher navigates between locales', async ({ page }) => {
    await page.goto('/ca');
    await page.waitForLoadState('networkidle');

    // LocaleSwitcher renders in header and footer with title="EN", title="ES", etc.
    const enBtn = page.locator('button[title="EN"]').first();
    await expect(enBtn).toBeVisible({ timeout: 5000 });
    await enBtn.click();
    await page.waitForURL(/\/en/);
    expect(page.url()).toContain('/en');
  });
});
