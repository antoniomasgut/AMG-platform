import { test, expect } from '@playwright/test';

test.describe('Login page', () => {
  test('renders email form in ca', async ({ page }) => {
    await page.goto('/ca/login');
    await page.waitForLoadState('networkidle');

    // Email input visible
    await expect(page.getByPlaceholder('tu@empresa.com')).toBeVisible({ timeout: 10000 });

    // Send magic link button
    await expect(page.getByRole('button', { name: 'ENVIAR MAGIC LINK' })).toBeVisible();
  });

  test('shows password step when clicking password button', async ({ page }) => {
    await page.goto('/ca/login');
    await page.waitForLoadState('networkidle');

    // Must type an email first (the button validates email is not empty)
    await page.getByPlaceholder('tu@empresa.com').fill('test@amg.cat');

    // Click "use password" button
    await page.getByRole('button', { name: 'Usar contrasenya' }).click();

    // Password input should appear
    await expect(page.getByPlaceholder('••••••••')).toBeVisible({ timeout: 5000 });
  });

  test('renders in all locales', async ({ page }) => {
    for (const locale of ['ca', 'es', 'en', 'de'] as const) {
      await page.goto(`/${locale}/login`);
      await page.waitForLoadState('networkidle');
      await expect(page.locator('body')).not.toContainText('404');
    }
  });
});

test.describe('Forgot password', () => {
  test('renders email form', async ({ page }) => {
    await page.goto('/ca/forgot-password');
    await page.waitForLoadState('networkidle');
    await expect(page.getByPlaceholder('tu@empresa.com')).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('button', { name: /enllaç|enviar|send/i })).toBeVisible();
  });
});

test.describe('Reset password', () => {
  test('shows invalid link when no token', async ({ page }) => {
    await page.goto('/ca/reset-password');
    await page.waitForLoadState('networkidle');
    await expect(page.getByText(/ENLLAÇ INVÀLID/i).first()).toBeVisible({ timeout: 10000 });
  });
});
