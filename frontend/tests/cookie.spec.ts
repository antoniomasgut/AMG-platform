import { test, expect } from '@playwright/test';

test.describe('Cookie consent banner', () => {
  test.beforeEach(async ({ page }) => {
    // Clear localStorage before each test
    await page.goto('/ca');
    await page.evaluate(() => localStorage.removeItem('amg_cookies_consent'));
    await page.reload();
    await page.waitForLoadState('networkidle');
  });

  test('appears on first visit (no stored consent)', async ({ page }) => {
    await expect(page.getByText('Aquest web utilitza cookies').first()).toBeVisible({ timeout: 10000 });
  });

  test('disappears after saving consent via localStorage', async ({ page }) => {
    await expect(page.getByText('Aquest web utilitza cookies').first()).toBeVisible();

    // Set consent in localStorage
    await page.evaluate(() => localStorage.setItem('amg_cookies_consent', 'all'));

    // Reload — banner should not appear since consent is saved
    await page.reload();
    await page.waitForLoadState('networkidle');
    await expect(page.getByText('Aquest web utilitza cookies').first()).not.toBeVisible();
  });

  test('clicking accept button saves consent', async ({ page }) => {
    await expect(page.getByText('Aquest web utilitza cookies').first()).toBeVisible();

    // Click all "Acceptar totes" buttons (React StrictMode double-renders in dev)
    await page.evaluate(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      for (const btn of buttons) {
        if (btn.textContent?.includes('Acceptar totes')) {
          btn.click();
        }
      }
    });
    await page.waitForTimeout(50);

    // Banner should hide immediately
    await expect(page.getByText('Aquest web utilitza cookies').first()).not.toBeVisible();

    // Reload — banner should still be gone
    await page.reload();
    await page.waitForLoadState('networkidle');
    await expect(page.getByText('Aquest web utilitza cookies').first()).not.toBeVisible();
  });
});
