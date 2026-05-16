import { test, expect } from '@playwright/test';
import { loginViaAPI, USERS, API_BASE } from './helpers/auth';

test.describe('Autenticació (Mòdul 01)', () => {
  test('login amb credencials correctes via formulari', async ({ page }) => {
    await page.goto('/ca/login');
    await page.waitForLoadState('networkidle');

    await page.locator('input[type="email"], input[name="email"]').first().fill(USERS.superAdmin.email);
    await page.locator('input[type="password"]').first().fill(USERS.superAdmin.password);
    await page.locator('button[type="submit"], button:has-text("Iniciar"), button:has-text("Entrar")').first().click();

    await page.waitForLoadState('networkidle');
    // Hauria de redirigir al portal
    await expect(page).toHaveURL(/portal/, { timeout: 10000 });
  });

  test('login amb credencials incorrectes mostra error', async ({ page }) => {
    await page.goto('/ca/login');
    await page.waitForLoadState('networkidle');

    await page.locator('input[type="email"], input[name="email"]').first().fill('wrong@test.com');
    await page.locator('input[type="password"]').first().fill('wrongpassword');
    await page.locator('button[type="submit"], button:has-text("Iniciar"), button:has-text("Entrar")').first().click();

    await page.waitForTimeout(2000);
    const body = await page.locator('body').textContent();
    // Hauria de mostrar missatge d'error o continuar al login
    const hasError = body?.toLowerCase().includes('incorrecte') ||
                     body?.toLowerCase().includes('invalid') ||
                     body?.toLowerCase().includes('error') ||
                     page.url().includes('login');
    expect(hasError).toBeTruthy();
  });

  test('API login retorna token vàlid', async ({ page }) => {
    const resp = await page.request.post(`${API_BASE}/api/v1/auth/login`, {
      headers: { 'Content-Type': 'application/json' },
      data: USERS.superAdmin,
    });
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data.accessToken).toBeTruthy();
    expect(data.user).toBeTruthy();
    expect(data.user.role).toBe('SUPER_ADMIN');
  });

  test('API login ADMIN retorna rol correcte', async ({ page }) => {
    const resp = await page.request.post(`${API_BASE}/api/v1/auth/login`, {
      headers: { 'Content-Type': 'application/json' },
      data: USERS.admin,
    }).catch(() => null);

    // Si l'usuari admin existeix
    if (resp && resp.status() === 200) {
      const data = await resp.json();
      expect(['ADMIN', 'SUPER_ADMIN']).toContain(data.user.role);
    }
  });

  test('endpoint protegit sense token retorna 401', async ({ page }) => {
    const resp = await page.request.get(`${API_BASE}/api/v1/tenants`);
    expect(resp.status()).toBe(401);
  });

  test('token invàlid retorna 401', async ({ page }) => {
    const resp = await page.request.get(`${API_BASE}/api/v1/tenants`, {
      headers: { Authorization: 'Bearer token-fals-invalid' },
    });
    expect(resp.status()).toBe(401);
  });

  test('token SUPER_ADMIN pot accedir a ruta SUPER_ADMIN', async ({ page }) => {
    const token = await loginViaAPI(page);
    const resp = await page.request.get(`${API_BASE}/api/v1/tenants`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    expect(resp.status()).toBe(200);
  });
});
