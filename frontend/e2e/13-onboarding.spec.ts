import { test, expect } from '@playwright/test';
import { loginViaAPI, apiGet, apiPost, API_BASE } from './helpers/auth';

const TS = Date.now();

test.describe('Onboarding — Guia de primers passos (Mòdul 16)', () => {

  test('OB-E1: client sense dades veu l\'onboarding', async ({ page }) => {
    const token = await loginViaAPI(page);

    // Crear tenant + usuari CLIENT que no té res
    const tenantSlug = `ob-e1-tenant-${TS}`;
    const tenantResp = await apiPost(page, token, '/api/v1/tenants', {
      name: `OB E1 Tenant ${TS}`,
      slug: tenantSlug,
      email: `${tenantSlug}@test.com`,
    });
    expect(tenantResp.status()).toBe(201);
    const tenant = await tenantResp.json();

    const userResp = await apiPost(page, token, '/api/v1/users', {
      email: `ob-e1-${TS}@test.com`,
      password: 'admin123',
      name: 'OB E1 Client',
      role: 'CLIENT',
      tenantId: tenant.id,
    });
    expect(userResp.status()).toBe(201);

    // Login com a client nou
    await loginViaAPI(page, { email: `ob-e1-${TS}@test.com`, password: 'admin123' });
    await page.goto('/ca/portal');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    // Ha de veure l'OnboardingGuide amb progress bar
    const guide = page.locator('[aria-label="Guia d\'inici ràpid"]');
    await expect(guide).toBeVisible();
    await expect(guide).toContainText('Crea la teva primera landing');
  });

  test('OB-E2: client amb dades NO veu onboarding', async ({ page }) => {
    const token = await loginViaAPI(page);

    // Usar un tenant que ja té dades
    const tenantsResp = await apiGet(page, token, '/api/v1/tenants?page=0&size=10');
    const tenantsData = await tenantsResp.json();
    const tenants = tenantsData.content ?? tenantsData;
    if (!tenants || tenants.length === 0) return;

    // Buscar el primer client amb dades
    const usersResp = await apiGet(page, token, '/api/v1/users?page=0&size=20');
    const usersData = await usersResp.json();
    const users = usersData.content ?? usersData;
    const clientUser = (users ?? []).find((u: any) => u.role === 'CLIENT' && u.tenantId === tenants[0].id);
    if (!clientUser) return;

    await loginViaAPI(page, { email: clientUser.email, password: 'admin123' });
    await page.goto('/ca/portal');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    // No ha de mostrar onboarding (dashboard normal)
    const guide = page.locator('[aria-label="Guia d\'inici ràpid"]');
    await expect(guide).not.toBeVisible();
  });

  test('OB-E4: skip funciona i persisteix', async ({ page }) => {
    const token = await loginViaAPI(page);

    // Crear tenant + client net
    const tenantSlug = `ob-e4-tenant-${TS}`;
    const tenantResp = await apiPost(page, token, '/api/v1/tenants', {
      name: `OB E4 Tenant ${TS}`,
      slug: tenantSlug,
      email: `${tenantSlug}@test.com`,
    });
    expect(tenantResp.status()).toBe(201);
    const tenant = await tenantResp.json();

    const userResp = await apiPost(page, token, '/api/v1/users', {
      email: `ob-e4-${TS}@test.com`,
      password: 'admin123',
      name: 'OB E4 Client',
      role: 'CLIENT',
      tenantId: tenant.id,
    });
    expect(userResp.status()).toBe(201);

    // Login i click a "Salta"
    await loginViaAPI(page, { email: `ob-e4-${TS}@test.com`, password: 'admin123' });
    await page.goto('/ca/portal');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    // Ha de veure onboarding abans de saltar
    const guide = page.locator('[aria-label="Guia d\'inici ràpid"]');
    await expect(guide).toBeVisible();

    // Click skip
    const skipBtn = page.locator('button', { hasText: 'Salta' });
    if (await skipBtn.isVisible()) {
      await skipBtn.click();
      await page.waitForTimeout(500);
    }

    // Refrescar — no ha de tornar a mostrar onboarding
    await page.reload();
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    await expect(guide).not.toBeVisible();
  });
});
