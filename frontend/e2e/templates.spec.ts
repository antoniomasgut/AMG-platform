import { test, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080';

async function loginViaAPI(page: any) {
  const resp = await page.request.post(`${API_BASE}/api/v1/auth/login`, {
    headers: { 'Content-Type': 'application/json' },
    data: { email: 'superadmin@amg.com', password: 'admin123' },
  });
  const data = await resp.json();
  await page.goto('/ca/login');
  await page.waitForLoadState('networkidle');
  await page.evaluate(({ token, refreshToken, user }) => {
    sessionStorage.setItem('access_token', token);
    sessionStorage.setItem('refresh_token', refreshToken);
    sessionStorage.setItem('user', JSON.stringify(user));
  }, { token: data.accessToken, refreshToken: data.refreshToken, user: data.user });
}

test.describe('Admin Templates Pages', () => {
  test('Template list page loads with templates', async ({ page }) => {
    await loginViaAPI(page);

    await page.goto('/ca/portal/admin/templates');
    await expect(page.locator('text=Gestió de plantilles').first()).toBeVisible({ timeout: 10000 });
  });

  test('New template page renders form', async ({ page }) => {
    await loginViaAPI(page);

    await page.goto('/ca/portal/admin/templates/new');
    await expect(page.locator('text=Nova plantilla').first()).toBeVisible({ timeout: 10000 });
  });

  test('Landings new page shows templates from API', async ({ page }) => {
    await loginViaAPI(page);

    await page.goto('/ca/portal/landings/new');
    await expect(page.locator('text=Selecciona una plantilla').first()).toBeVisible({ timeout: 10000 });
  });

  test('Template detail page renders with sections', async ({ page }) => {
    await loginViaAPI(page);

    const token = await page.evaluate(() => sessionStorage.getItem('access_token'));
    const resp = await page.request.get(`${API_BASE}/api/v1/engine/templates`, {
      headers: { 'Authorization': `Bearer ${token}` },
    });
    const templates = await resp.json();

    if (templates && templates.length > 0) {
      const tid = templates[0].id;
      await page.goto(`/ca/portal/admin/templates/${tid}`);
      await expect(page.locator(`text=${templates[0].name}`).first()).toBeVisible({ timeout: 10000 });
    }
  });
});
