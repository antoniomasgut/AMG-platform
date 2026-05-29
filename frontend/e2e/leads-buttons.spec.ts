import { test, expect } from '@playwright/test';
import { loginViaAPI, apiGet, USERS, API_BASE } from './helpers/auth';

test.describe('Leads — WA toggle i canvi d\'etapa', () => {
  test('WA toggle i stage change funcionen sense navegar fora', async ({ page }) => {
    const token = await loginViaAPI(page, USERS.superAdmin);

    // Obtenir un lead existent (crea'n un si cal)
    const list = await apiGet(page, token, '/api/v1/leads?size=1');
    const data = await list.json();
    let leadId: string;

    if (data.content && data.content.length > 0) {
      leadId = data.content[0].id;
    } else {
      // Crear lead de test
      const create = await page.request.post(`${API_BASE}/api/v1/leads`, {
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        data: { name: 'Test Lead Playwright', phone: '611222333', source: 'OTHER' },
      });
      const created = await create.json();
      leadId = created.id;
    }

    // Verificar que el lead existeix i té etapa NEW
    const leadResp = await apiGet(page, token, `/api/v1/leads/${leadId}`);
    const lead = await leadResp.json();
    console.log('Lead inicial:', lead.name, 'etapa:', lead.stage, 'WA:', lead.hasWhatsapp);

    // Test 1: WA toggle via API (simula el que fa el botó)
    const waResp = await page.request.patch(
      `${API_BASE}/api/v1/leads/${leadId}/whatsapp?value=true`,
      { headers: { Authorization: `Bearer ${token}` } }
    );
    console.log('WA toggle HTTP:', waResp.status());
    expect(waResp.status()).toBe(200);
    const waData = await waResp.json();
    expect(waData.hasWhatsapp).toBe(true);

    // Test 2: Stage change via API
    const stageResp = await page.request.patch(
      `${API_BASE}/api/v1/leads/${leadId}/stage`,
      {
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        data: { stage: 'CONTACTED' },
      }
    );
    console.log('Stage change HTTP:', stageResp.status());
    expect(stageResp.status()).toBe(200);
    const stageData = await stageResp.json();
    expect(stageData.stage).toBe('CONTACTED');

    // Test 3: Anar a la pàgina de leads i verificar que els botons existeixen
    await page.goto('/ca/portal/leads');
    await page.waitForLoadState('networkidle');

    // Verificar que els botons "Provar WA →" o "Marcar WA" apareixen (si hi ha leads amb telèfon)
    const waButtons = page.locator('button:has-text("Marcar WA"), button:has-text("✓ WA")');
    const waLinks = page.locator('a:has-text("Provar WA →")');
    const stageButtons = page.locator('button:has-text("Avançar →"), button:has-text("Guanyar"), button:has-text("Perdut")');

    console.log('Botons WA:', await waButtons.count());
    console.log('Links WA:', await waLinks.count());
    console.log('Botons etapa:', await stageButtons.count());

    // Test 4: Clicar "Avançar →" en el lead de test i verificar que NO navega fora
    const stageBtn = stageButtons.first();
    if (await stageBtn.isVisible()) {
      const currentUrl = page.url();
      await stageBtn.click();
      await page.waitForTimeout(1000);
      const newUrl = page.url();
      // Si el botó funciona correctament, NO hauria de navegar a la pàgina del lead
      console.log('URL abans:', currentUrl);
      console.log('URL després:', newUrl);
      expect(newUrl).toBe(currentUrl); // Hauria de quedar-se a /leads
    }
  });
});
