import { test, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080';

async function loginViaAPI(page: any) {
  const resp = await page.request.post(`${API_BASE}/api/v1/auth/login`, {
    headers: { 'Content-Type': 'application/json' },
    data: { email: 'superadmin@test.com', password: 'admin123' },
  });
  const data = await resp.json();
  await page.goto('/ca/login');
  await page.waitForLoadState('networkidle');
  await page.evaluate(({ token, refreshToken, user }) => {
    sessionStorage.setItem('access_token', token);
    sessionStorage.setItem('refresh_token', refreshToken);
    sessionStorage.setItem('user', JSON.stringify(user));
  }, { token: data.accessToken, refreshToken: data.refreshToken, user: data.user });
  return data.accessToken;
}

const TS = Date.now();

test('Complete vault flow: create profile, phases, services, assign', async ({ page }) => {
  const token = await loginViaAPI(page);

  // 1. Create a profile from the catalog
  await page.goto('/ca/portal/admin/vault');
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(1000);

  await page.locator('button:has-text("Nou perfil")').click();
  await page.locator('input[placeholder="Ex: Professionals amb cita"]').fill(`Vault E2E Profile ${TS}`);
  await page.locator('input[placeholder="professionals-cita"]').fill(`vault-e2e-${TS}`);
  await page.locator('textarea').fill('E2E test profile');
  await page.locator('button:has-text("Crear perfil")').click();
  await page.waitForTimeout(1000);

  // Dismiss cookie consent if present
  const cookieAccept = page.locator('button:has-text("Acceptar")').first();
  if (await cookieAccept.isVisible({ timeout: 500 }).catch(() => false)) {
    await cookieAccept.click();
    await page.waitForTimeout(300);
  }

  // 2. Open the specific profile detail
  await page.locator(`button:has-text("Vault E2E Profile ${TS}")`).click();
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(1000);

  // 3. Add a phase
  await page.locator('button:has-text("Nova fase")').click();
  await page.locator('input[placeholder="Ex: Configuració bàsica"]').fill('F1 Config');
  await page.locator('input[placeholder="1"]').fill('1');
  await page.locator('button:has-text("Afegir fase")').click();
  await page.waitForTimeout(1000);

  // 4. Add service to phase (create new service inline)
  await page.locator('button:has-text("Afegir servei")').last().click();
  await page.waitForTimeout(500);

  // Switch to "Nou servei" mode
  await page.locator('button:has-text("Nou servei")').last().click();
  await page.waitForTimeout(200);

  const svcName = `Landing E2E ${TS}`;
  await page.locator('input[placeholder="Ex: Landing Pro"]').fill(svcName);
  await page.locator('input[placeholder="landing-pro"]').fill(`landing-e2e-${TS}`);
  await page.locator('input[type="number"]').first().fill('30');
  await page.locator('input[type="number"]').nth(1).fill('80');
  await page.locator('button:has-text("Afegir servei")').last().click();
  await page.waitForTimeout(1000);

  // 5. Verify service appears in phase
  await expect(page.locator(`text=${svcName}`).first()).toBeVisible({ timeout: 3000 });
  await expect(page.locator('text=80.00 €').first()).toBeVisible({ timeout: 3000 });

  // 6. Create a tenant via API (faster and more reliable than UI creation for this step)
  const tenantSlug = `vt-tenant-${TS}`;
  const tenantResp = await page.request.post(`${API_BASE}/api/v1/tenants`, {
    headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
    data: { name: `Vault E2E Tenant ${TS}`, slug: tenantSlug, email: 'vt@test.com' },
  });
  const tenant = await tenantResp.json();
  const tenantId = tenant.id;
  expect(tenantId).toBeTruthy();

  // 7. Navigate directly to tenant detail page
  await page.goto(`/ca/portal/admin/tenants/${tenantId}`);
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(2000);

  // 8. Dismiss cookies if present
  const cookieBtn = page.locator('button:has-text("Acceptar")').first();
  if (await cookieBtn.isVisible({ timeout: 500 }).catch(() => false)) {
    await cookieBtn.click();
    await page.waitForTimeout(300);
  }

  // 9. Assign profile
  await expect(page.locator('button:has-text("Assignar perfil")')).toBeVisible({ timeout: 10000 });
  await page.locator('button:has-text("Assignar perfil")').click({ force: true });
  await page.waitForTimeout(500);

  // Click the profile in the modal via DOM API
  await page.evaluate((name) => {
    const buttons = document.querySelectorAll('.fixed.inset-0 button');
    for (const btn of buttons) {
      if (btn.textContent?.includes(name)) {
        (btn as HTMLButtonElement).click();
        break;
      }
    }
  }, `Vault E2E Profile ${TS}`);
  await page.waitForTimeout(300);

  // Click the modal's "Assignar perfil" confirm button
  await page.evaluate(() => {
    const overlay = document.querySelector('.fixed.inset-0');
    if (!overlay) return;
    const buttons = overlay.querySelectorAll('button');
    for (const btn of buttons) {
      if (btn.textContent?.trim() === 'Assignar perfil' && !btn.disabled) {
        (btn as HTMLButtonElement).click();
        return;
      }
    }
  });
  await page.waitForTimeout(2000);

  // 10. Verify phases and services assigned to tenant
  await expect(page.locator('text=F1 Config').first()).toBeVisible();
  await expect(page.locator(`text=${svcName}`).first()).toBeVisible();
});
