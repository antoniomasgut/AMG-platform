import { test, expect } from '@playwright/test';
import { loginProd, apiCall, dismissCookies, TENANT_ID } from './helpers/prod-auth';

test.describe('Mòdul 24 — Agent Activation Flows (producció)', () => {

  test('API: GET channels retorna 200 amb estructura correcta', async ({ page }) => {
    const token = await loginProd(page);
    const resp = await apiCall(page, token, 'GET', `/api/v1/agents/conversational/${TENANT_ID}/channels`);
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data).toHaveProperty('tenantId');
    expect(data).toHaveProperty('agentMode');
    expect(data).toHaveProperty('isActive');
    expect(data).toHaveProperty('telegramLinked');
    expect(['AUTO', 'HYBRID', 'MANUAL']).toContain(data.agentMode);
    expect(typeof data.isActive).toBe('boolean');
  });

  test('API: GET activation-instructions retorna 200', async ({ page }) => {
    const token = await loginProd(page);
    const resp = await apiCall(page, token, 'GET', `/api/v1/agents/conversational/${TENANT_ID}/activation-instructions`);
    expect(resp.status()).toBe(200);
    const data = await resp.json();
    expect(data).toHaveProperty('telegram');
    expect(data).toHaveProperty('whatsapp');
    expect(typeof data.telegram.configured).toBe('boolean');
    expect(typeof data.whatsapp.configured).toBe('boolean');
  });

  test('UI: pàgina agents carrega sense error', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal/agents');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2000);

    const body = await page.locator('body').textContent();
    expect(body).toBeTruthy();
    expect(body!.length).toBeGreaterThan(200);
    const hasAgentsContent = body!.includes('Agent') || body!.includes('Bot') || body!.includes('ACTIU') || body!.includes('ATURAT') || body!.includes('PENDENT');
    expect(hasAgentsContent).toBeTruthy();
  });

  test('UI: tab Agent mostra estat del bot', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal/agents');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2000);

    // Ha de mostrar l'estat del bot
    const body = await page.locator('body').textContent();
    const hasStatus = body!.includes('ACTIU') || body!.includes('ATURAT') || body!.includes('PENDENT CONFIGURAR');
    expect(hasStatus).toBeTruthy();
  });

  test('UI: tab Agent mostra canals (Telegram, WhatsApp, Email)', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal/agents');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2000);

    const body = await page.locator('body').textContent();
    expect(body).toContain('Telegram');
    expect(body).toContain('WhatsApp');
    expect(body).toContain('Email');
  });

  test('UI: botó activar o aturar bot visible', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal/agents');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2000);

    // Ha de mostrar un dels dos botons d'acció
    const activateBtn = page.locator('button:has-text("ACTIVAR BOT")').first();
    const deactivateBtn = page.locator('button:has-text("ATURAR BOT")').first();

    const hasActivate = await activateBtn.isVisible({ timeout: 3000 }).catch(() => false);
    const hasDeactivate = await deactivateBtn.isVisible({ timeout: 3000 }).catch(() => false);

    expect(hasActivate || hasDeactivate).toBeTruthy();
  });

  test('UI: selector de mode AUTO/HYBRID/MANUAL visible', async ({ page }) => {
    await loginProd(page);
    await page.goto('/ca/portal/agents');
    await page.waitForLoadState('networkidle');
    await dismissCookies(page);
    await page.waitForTimeout(2000);

    const body = await page.locator('body').textContent();
    expect(body).toContain('AUTO');
    expect(body).toContain('HYBRID');
    expect(body).toContain('MANUAL');
  });

});
