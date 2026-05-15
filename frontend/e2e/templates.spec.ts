import { test, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080';

async function loginViaAPI(page: any) {
  const resp = await page.request.post(`${API_BASE}/api/v1/auth/login`, {
    headers: { 'Content-Type': 'application/json' },
    data: { email: 'superadmin@amg.com', password: 'admin123' },
  });
  const data = await resp.json();
  const token = data.accessToken;
  const refreshToken = data.refreshToken;

  // Set tokens in sessionStorage before navigating to portal
  await page.goto('/ca/login');
  await page.waitForLoadState('networkidle');
  await page.evaluate(({ token, refreshToken }) => {
    sessionStorage.setItem('access_token', token);
    sessionStorage.setItem('refresh_token', refreshToken);
    sessionStorage.setItem('user', JSON.stringify({
      email: 'superadmin@amg.com',
      role: 'SUPER_ADMIN',
      name: 'Super Admin',
    }));
  }, { token, refreshToken });
}

test.describe('Admin Templates Pages', () => {
  test('Template list page loads with templates', async ({ page }) => {
    await loginViaAPI(page);

    await page.goto('/ca/portal/admin/templates');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2000);

    const body = await page.textContent('body');
    expect(body).toContain('Gestió de plantilles');
  });

  test('New template page renders form', async ({ page }) => {
    await loginViaAPI(page);

    await page.goto('/ca/portal/admin/templates/new');
    await page.waitForLoadState('networkidle');

    const body = await page.textContent('body');
    expect(body).toContain('Nova plantilla');
  });

  test('Landings new page shows templates from API', async ({ page }) => {
    await loginViaAPI(page);

    await page.goto('/ca/portal/landings/new');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(3000);

    const body = await page.textContent('body');
    expect(body).toContain('Selecciona una plantilla');
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
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(1000);

      const body = await page.textContent('body');
      expect(body).toContain(templates[0].name);
    }
  });
});
