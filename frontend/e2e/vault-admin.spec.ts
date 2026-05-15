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

const TS = Date.now();

test('Complete vault flow: create profile, phases, services, assign', async ({ page }) => {
  await loginViaAPI(page);

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

  // 2. Dismiss cookie consent if present
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

  // 6. Go to tenants page and create a tenant
  await page.goto('/ca/portal/admin/tenants');
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(1000);

  const tenantName = `Vault E2E Tenant ${TS}`;
  await page.locator('button:has-text("Nou tenant")').click();
  await page.locator('input[placeholder="Nom de l\'empresa"]').fill(tenantName);
  await page.locator('input[placeholder="nom-empresa"]').fill(`vt-tenant-${TS}`);
  await page.locator('input[placeholder="info@empresa.com"]').fill('vt@test.com');
  await page.locator('button:has-text("Crear tenant")').click();
  await page.waitForTimeout(1500);

  // 7. Open tenant detail — click Gestionar via DOM API to bypass React interception issues
  await page.evaluate((name) => {
    const rows = document.querySelectorAll('tr');
    for (const row of rows) {
      if (row.textContent?.includes(name)) {
        const btn = row.querySelector('button');
        if (btn) { btn.click(); break; }
      }
    }
  }, tenantName);
  await page.waitForURL(/\/portal\/admin\/tenants\//, { timeout: 5000 });
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(2000);

  // 8. Dismiss cookies and assign profile
  const cookieBtn = page.locator('button:has-text("Acceptar")').first();
  if (await cookieBtn.isVisible({ timeout: 500 }).catch(() => false)) {
    await cookieBtn.click();
    await page.waitForTimeout(300);
  }

  // Wait for "Assignar perfil" button
  await page.waitForTimeout(1000);
  await expect(page.locator('button:has-text("Assignar perfil")')).toBeVisible({ timeout: 5000 });
  await page.locator('button:has-text("Assignar perfil")').click({ force: true });
  await page.waitForTimeout(500);
  // Click the profile via DOM API since it may be outside the viewport in the modal
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
  // Click the modal's "Assignar perfil" button via DOM API
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

  // 9. Verify phases and services assigned to tenant
  await expect(page.locator('text=F1 Config').first()).toBeVisible();
  await expect(page.locator(`text=${svcName}`).first()).toBeVisible();
});
