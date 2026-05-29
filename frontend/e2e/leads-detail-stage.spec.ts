import { test, expect } from '@playwright/test';
import { loginViaAPI, API_BASE, USERS } from './helpers/auth';

test('Stage change debug', async ({ page }) => {
  const token = await loginViaAPI(page, USERS.superAdmin);

  const list = await page.request.get(`${API_BASE}/api/v1/leads?size=1`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const data = await list.json();
  const lead = data.content[0];
  console.log('Lead:', lead.id, lead.name, 'stage:', lead.stage);

  await page.goto(`/ca/portal/leads/${lead.id}`);
  await page.waitForLoadState('networkidle');

  const storedToken = await page.evaluate(() => sessionStorage.getItem('access_token'));
  console.log('Token sessionStorage:', storedToken ? storedToken.substring(0, 30) + '...' : 'NUL!');

  // Executa el PATCH directament des del browser context
  const result = await page.evaluate(async ({ leadId, tok }) => {
    try {
      const res = await fetch(`/api/v1/leads/${leadId}/stage`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${tok}`,
        },
        body: JSON.stringify({ stage: 'NEW' }),
      });
      const body = await res.text().catch(() => '');
      return { status: res.status, body, ok: res.ok };
    } catch (e: unknown) {
      return { error: String(e) };
    }
  }, { leadId: lead.id, tok: storedToken });

  console.log('Fetch result:', JSON.stringify(result));
});
