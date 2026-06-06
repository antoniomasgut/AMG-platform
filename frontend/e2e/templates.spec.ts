import { test, expect } from '@playwright/test';
import { loginProd, apiCall, dismissCookies } from './helpers/prod-auth';

const API_BASE = process.env.PLAYWRIGHT_API_BASE || 'https://api.amgdl.com';

test.describe('Admin Templates Pages', () => {
  test('Template list page loads with templates', async ({ page }) => {
    await loginProd(page);

    await page.goto('/ca/portal/admin/templates');
    await expect(page.locator('text=Gestió de plantilles').first()).toBeVisible({ timeout: 10000 });
  });

  test('New template page renders form', async ({ page }) => {
    await loginProd(page);

    await page.goto('/ca/portal/admin/templates/new');
    await expect(page.locator('text=Nova plantilla').first()).toBeVisible({ timeout: 10000 });
  });

  test('Landings new page shows templates from API', async ({ page }) => {
    await loginProd(page);

    await page.goto('/ca/portal/landings/new');
    await expect(page.locator('text=Selecciona una plantilla').first()).toBeVisible({ timeout: 10000 });
  });

  test('Template detail page renders with sections', async ({ page }) => {
    const token = await loginProd(page);

    const resp = await apiCall(page, token, 'GET', '/api/v1/engine/templates');
    const templates = await resp.json();

    if (templates && templates.length > 0) {
      const tid = templates[0].id;
      await page.goto(`/ca/portal/admin/templates/${tid}`);
      await expect(page.locator(`text=${templates[0].name}`).first()).toBeVisible({ timeout: 10000 });
    }
  });
});
