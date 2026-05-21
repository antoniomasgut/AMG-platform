import { test, expect } from '@playwright/test';

const API_BASE = 'http://localhost:8080';
const TS = Date.now();

// Next.js dev mode keeps HMR WebSocket open — never use 'networkidle'
async function loginViaAPI(page: any, email = 'superadmin@test.com', password = 'changeme123') {
  const resp = await page.request.post(`${API_BASE}/api/v1/auth/login`, {
    headers: { 'Content-Type': 'application/json' },
    data: { email, password },
  });
  const data = await resp.json();
  await page.goto('/ca/login', { waitUntil: 'load', timeout: 60000 });
  await page.evaluate(({ token, refreshToken, user }: any) => {
    sessionStorage.setItem('access_token', token);
    sessionStorage.setItem('refresh_token', refreshToken);
    sessionStorage.setItem('user', JSON.stringify(user));
  }, { token: data.accessToken, refreshToken: data.refreshToken, user: data.user });
  return data.accessToken;
}

async function gotoAndWait(page: any, path: string) {
  await page.goto(path, { waitUntil: 'load', timeout: 60000 });
  await page.waitForTimeout(1500);
}

async function createTenantViaAPI(token: string, page: any) {
  const resp = await page.request.post(`${API_BASE}/api/v1/tenants`, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    data: { name: `Test Tenant Domains ${TS}`, slug: `test-tenant-domains-${TS}` },
  });
  return await resp.json();
}

test.describe('Domains — menú i navegació', () => {
  test.setTimeout(90000);

  test('El menú conté Dominis a la secció Serveis', async ({ page }) => {
    await loginViaAPI(page);
    await gotoAndWait(page, '/ca/portal');
    await expect(page.locator('nav a[href*="/portal/admin/domains"]')).toBeVisible();
  });

  test('La pàgina /portal/admin/domains es carrega correctament', async ({ page }) => {
    await loginViaAPI(page);
    await gotoAndWait(page, '/ca/portal/admin/domains');
    await expect(page.locator('h1:has-text("Dominis")')).toBeVisible({ timeout: 15000 });
    await expect(page.locator('button:has-text("Registrar domini")').first()).toBeVisible();
  });

  test('Mostra empty state quan no hi ha dominis', async ({ page }) => {
    await loginViaAPI(page);
    await gotoAndWait(page, '/ca/portal/admin/domains');
    const hasTable = await page.locator('table').isVisible().catch(() => false);
    const hasEmpty = await page.locator('text=Cap domini registrat').isVisible().catch(() => false);
    expect(hasTable || hasEmpty).toBeTruthy();
  });
});

test.describe('Domains — registre de domini', () => {
  test.setTimeout(90000);

  test('Obre el modal de registre en clicar el botó', async ({ page }) => {
    await loginViaAPI(page);
    await gotoAndWait(page, '/ca/portal/admin/domains');
    await page.locator('button:has-text("Registrar domini")').first().click();
    await expect(page.locator('text=Registrar domini').last()).toBeVisible();
    await expect(page.locator('button:has-text("Comprovar disponibilitat")')).toBeVisible();
  });

  test("Comprova disponibilitat d'un domini disponible", async ({ page }) => {
    await loginViaAPI(page);
    await gotoAndWait(page, '/ca/portal/admin/domains');
    await page.locator('button:has-text("Registrar domini")').first().click();
    await page.waitForTimeout(300);

    await page.locator('input[placeholder="perruqueria-maria"]').fill(`testdomain${TS}`);
    await page.locator('button:has-text("Comprovar disponibilitat")').click();
    await page.waitForTimeout(2000);

    await expect(page.locator('text=Disponible')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('text=Registre (1r any)')).toBeVisible();
  });

  test('Comprova un domini no disponible i mostra alternatives', async ({ page }) => {
    await loginViaAPI(page);
    await gotoAndWait(page, '/ca/portal/admin/domains');
    await page.locator('button:has-text("Registrar domini")').first().click();
    await page.waitForTimeout(300);

    // Canviem a TLD .com i posem "taken" que el mock retorna com a no disponible
    await page.locator('select').first().selectOption('com');
    await page.locator('input[placeholder="perruqueria-maria"]').fill('taken');
    await page.locator('button:has-text("Comprovar disponibilitat")').click();
    await page.waitForTimeout(2000);

    await expect(page.locator('text=No disponible')).toBeVisible({ timeout: 10000 });
  });

  test('Registra un domini correctament amb tenant seleccionat', async ({ page }) => {
    const token = await loginViaAPI(page);
    const tenant = await createTenantViaAPI(token, page);

    await gotoAndWait(page, '/ca/portal/admin/domains');
    await page.locator('button:has-text("Registrar domini")').first().click();
    await page.waitForTimeout(300);

    const domainName = `autotest${TS}`;
    await page.locator('input[placeholder="perruqueria-maria"]').fill(domainName);
    await page.locator('button:has-text("Comprovar disponibilitat")').click();
    await page.waitForTimeout(2000);

    await expect(page.locator('text=Disponible')).toBeVisible({ timeout: 10000 });

    // El select de tenant té classe w-full (diferent del select TLD)
    await page.locator('select.w-full').selectOption(tenant.id);
    await page.locator(`button:has-text("Registrar ${domainName}")`).click();
    await page.waitForTimeout(2000);

    // El modal es tanca (el botó "Comprovar disponibilitat" desapareix) i el domini apareix a la taula
    await expect(page.locator('button:has-text("Comprovar disponibilitat")')).not.toBeVisible({ timeout: 8000 });
    await expect(page.locator(`text=${domainName}.cat`).first()).toBeVisible({ timeout: 10000 });
  });
});

test.describe('Domains — operacions sobre dominis existents', () => {
  test.setTimeout(90000);
  let token: string;
  let domainId: string;
  let domainName: string;

  test.beforeAll(async ({ browser }) => {
    const page = await browser.newPage();
    const resp = await page.request.post(`${API_BASE}/api/v1/auth/login`, {
      headers: { 'Content-Type': 'application/json' },
      data: { email: 'superadmin@test.com', password: 'changeme123' },
    });
    const data = await resp.json();
    token = data.accessToken;

    // Creem tenant
    const tenantResp = await page.request.post(`${API_BASE}/api/v1/tenants`, {
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
      data: { name: `Tenant Ops ${TS}`, slug: `tenant-ops-${TS}` },
    });
    const tenant = await tenantResp.json();

    // Registrem domini via API
    domainName = `opstest${TS}.cat`;
    const domResp = await page.request.post(`${API_BASE}/api/v1/domains/register`, {
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
      data: { domainName, tenantId: tenant.id, autoRenew: true },
    });
    const dom = await domResp.json();
    domainId = dom.id;
    await page.close();
  });

  test('Mostra el domini a la taula amb estat ACTIVE', async ({ page }) => {
    await loginViaAPI(page);
    await gotoAndWait(page, '/ca/portal/admin/domains');
    await expect(page.locator(`text=${domainName}`)).toBeVisible({ timeout: 10000 });
    await expect(page.locator('text=Actiu').first()).toBeVisible();
  });

  test('Configura DNS automàticament', async ({ page }) => {
    await loginViaAPI(page);
    await gotoAndWait(page, '/ca/portal/admin/domains');

    const row = page.locator(`tr:has-text("${domainName}")`);
    await row.locator('button:has-text("DNS auto")').click();
    await page.waitForTimeout(1500);

    await expect(page.locator('text=DNS configurat correctament')).toBeVisible({ timeout: 10000 });
  });
});

test.describe('Domains — tarifes TLD (SUPER_ADMIN)', () => {
  test('La llista de TLD es pot obtenir via API', async ({ page }) => {
    const resp = await page.request.post(`${API_BASE}/api/v1/auth/login`, {
      headers: { 'Content-Type': 'application/json' },
      data: { email: 'superadmin@test.com', password: 'changeme123' },
    });
    const { accessToken } = await resp.json();

    const tldResp = await page.request.get(`${API_BASE}/api/v1/domains/admin/tld-pricing`, {
      headers: { 'Authorization': `Bearer ${accessToken}` },
    });
    expect(tldResp.ok()).toBeTruthy();
    const pricing = await tldResp.json();
    expect(Array.isArray(pricing)).toBeTruthy();
    expect(pricing.length).toBeGreaterThan(0);
    expect(pricing[0]).toHaveProperty('tld');
    expect(pricing[0]).toHaveProperty('saleRegister');
  });
});
