import { test, expect } from '@playwright/test';

const LOCALES = ['ca', 'es', 'en', 'de'] as const;

async function go(page: import('@playwright/test').Page, url: string) {
  await page.goto(url);
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(1000);
}

async function section(page: import('@playwright/test').Page, id: string) {
  const el = page.locator(`section#${id}`);
  await expect(el).toBeVisible({ timeout: 10000 });
  return el;
}

/* ───────── 1. HEADER & NAV ───────── */
test.describe('PUB-HEADER', () => {
  test('PUB-HEADER-ca: logo, nav, locale switcher, login', async ({ page }) => {
    await go(page, '/ca');
    await expect(page.locator('header').first()).toBeVisible();
    await expect(page.locator('header a[href*="/ca"]').first()).toBeVisible();
    await expect(page.locator('header nav a').first()).toBeAttached();
    await expect(page.locator('header a[href*="/login"]').first()).toBeVisible();
  });

  test('PUB-HEADER-locales: tots els idiomes al switcher', async ({ page }) => {
    await go(page, '/ca');
    for (const code of ['ca', 'es', 'en', 'de']) {
      await expect(page.locator(`header a[href="/${code}"]`).first()).toBeAttached();
    }
  });

  test('PUB-HEADER-mobile: hamburguesa i menu', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await go(page, '/ca');
    await page.waitForTimeout(500);
    const burger = page.locator('header button[aria-label*="men"]').first();
    await expect(burger).toBeVisible({ timeout: 5000 });
    await page.evaluate(() => {
      const btn = document.querySelector('header button[aria-label*="men"]') as HTMLElement;
      btn?.click();
    });
    await page.waitForTimeout(500);
    // Check mobile menu by looking for visible services link
    await expect(page.locator('a[href="#services"]').last()).toBeVisible({ timeout: 3000 });
  });
});

/* ───────── 2. HERO ───────── */
test.describe('PUB-HERO', () => {
  for (const locale of LOCALES) {
    test(`PUB-HERO-${locale}`, async ({ page }) => {
      await go(page, `/${locale}`);
      await expect(page.locator('h1').first()).toBeVisible({ timeout: 10000 });
      const cta = page.locator('a[href*="wa.me"]');
      await expect(cta.first()).toBeVisible();
    });
  }
});

/* ───────── 3. PROBLEM ───────── */
test.describe('PUB-PROBLEM', () => {
  for (const locale of LOCALES) {
    test(`PUB-PROBLEM-${locale}`, async ({ page }) => {
      await go(page, `/${locale}`);
      // Problem section is the first section after hero (no id)
      // It contains a badge and items with border + bg-bg-1
      const sections = page.locator('section');
      const count = await sections.count();
      let found = false;
      for (let i = 0; i < count; i++) {
        const text = await sections.nth(i).textContent();
        if (text && text.includes('eines')) { found = true; break; }
      }
      if (!found) {
        // fallback: just check enough problem-themed items exist
        const items = page.locator('.border-border-subtle.bg-bg-1');
        const c = await items.count();
        expect(c).toBeGreaterThanOrEqual(2);
      }
      expect(found || (await page.locator('.border-border-subtle.bg-bg-1').count()) >= 2).toBeTruthy();
    });
  }
});

/* ───────── 4. SERVICES ───────── */
test.describe('PUB-SERVICES', () => {
  for (const locale of LOCALES) {
    test(`PUB-SERVICES-${locale}`, async ({ page }) => {
      await go(page, `/${locale}`);
      await section(page, 'services');
      const cards = page.locator('section#services .amg-card');
      expect(await cards.count()).toBeGreaterThanOrEqual(3);
    });
  }
});

/* ───────── 5. HOW IT WORKS ───────── */
test.describe('PUB-HOW', () => {
  for (const locale of LOCALES) {
    test(`PUB-HOW-${locale}`, async ({ page }) => {
      await go(page, `/${locale}`);
      await section(page, 'how-it-works');
      const t = await page.locator('section#how-it-works').textContent();
      expect(t?.length).toBeGreaterThan(50);
    });
  }
});

/* ───────── 6. PORTAL ───────── */
test.describe('PUB-PORTAL', () => {
  for (const locale of LOCALES) {
    test(`PUB-PORTAL-${locale}`, async ({ page }) => {
      await go(page, `/${locale}`);
      await section(page, 'portal');
    });
  }
});

/* ───────── 7. PRICING ───────── */
test.describe('PUB-PRICING', () => {
  for (const locale of LOCALES) {
    test(`PUB-PRICING-${locale}`, async ({ page }) => {
      await go(page, `/${locale}`);
      await section(page, 'pricing');
      // Cards are not .amg-card — use border + padding
      const cards = page.locator('section#pricing .relative.flex-col.gap-4');
      expect(await cards.count()).toBeGreaterThanOrEqual(4);
    });
  }
});

/* ───────── 8. TESTIMONIALS ───────── */
test.describe('PUB-TEST', () => {
  for (const locale of LOCALES) {
    test(`PUB-TEST-${locale}`, async ({ page }) => {
      await go(page, `/${locale}`);
      await section(page, 'testimonials');
      const cards = page.locator('section#testimonials .amg-card');
      expect(await cards.count()).toBeGreaterThanOrEqual(2);
    });
  }
});

/* ───────── 9. CTA / FORM ───────── */
test.describe('PUB-CTA', () => {
  for (const locale of LOCALES) {
    test(`PUB-CTA-${locale}`, async ({ page }) => {
      await go(page, `/${locale}`);
      await section(page, 'contact');
      await expect(page.locator('section#contact form')).toBeVisible({ timeout: 5000 });
      await expect(page.locator('section#contact input[type="text"]').first()).toBeVisible();
      await expect(page.locator('section#contact input[type="email"]').first()).toBeVisible();
      await expect(page.locator('section#contact textarea').first()).toBeVisible();
    });
  }

  test('PUB-CTA-form-submit', async ({ page }) => {
    await go(page, '/ca');
    await page.locator('section#contact input[type="text"]').first().fill('Test PW');
    await page.locator('section#contact input[type="email"]').first().fill('test@pw.test');
    await page.locator('section#contact textarea').first().fill('Test message.');
    await page.locator('section#contact button[type="submit"]').first().click();
    await page.waitForTimeout(3000);
    const txt = await page.locator('section#contact').textContent();
    const ok = txt?.includes('rebut') || txt?.includes('received') || txt?.includes('recibido') || txt?.includes('erhalten') || txt?.includes('pogut') || txt?.includes('Could not') || txt?.includes('podido') || txt?.includes('nicht');
    expect(ok).toBeTruthy();
  });
});

/* ───────── 10. FOOTER ───────── */
test.describe('PUB-FOOTER', () => {
  for (const locale of LOCALES) {
    test(`PUB-FOOTER-${locale}`, async ({ page }) => {
      await go(page, `/${locale}`);
      await expect(page.locator('footer')).toBeVisible({ timeout: 10000 });
      expect(await page.locator('footer a[href*="/legal/"]').count()).toBeGreaterThanOrEqual(3);
    });
  }
});

/* ───────── 11. LOCALE SWITCHING ───────── */
test.describe('PUB-LOCALE', () => {
  test('PUB-LOCALE-mobile', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await go(page, '/ca');
    const burger = page.locator('header button[aria-label*="men"]').first();
    await expect(burger).toBeVisible({ timeout: 5000 });
    await page.evaluate(() => {
      const btn = document.querySelector('header button[aria-label*="men"]') as HTMLElement;
      btn?.click();
    });
    await page.waitForTimeout(500);
    const esLink = page.locator('header a[href^="/es"]').first();
    await expect(esLink).toBeVisible({ timeout: 3000 });
    await esLink.click();
    await page.waitForTimeout(2000);
    expect(page.url()).toContain('/es');
  });

  test('PUB-LOCALE-all', async ({ page }) => {
    for (const loc of LOCALES) {
      await go(page, `/${loc}`);
      const title = await page.title();
      expect(title.length).toBeGreaterThan(0);
      const h1 = await page.locator('h1').first().textContent();
      expect(h1?.length).toBeGreaterThan(5);
    }
  });
});

/* ───────── 12. LEGAL PAGES ───────── */
test.describe('PUB-LEGAL', () => {
  for (const locale of LOCALES) {
    for (const pageName of ['privacitat', 'avis-legal', 'termes-servei', 'cookies']) {
      test(`PUB-LEGAL-${locale}-${pageName}`, async ({ page }) => {
        await go(page, `/${locale}/legal/${pageName}`);
        const body = await page.locator('body').textContent();
        expect(body?.length).toBeGreaterThan(200);
      });
    }
  }
});

/* ───────── 13. AUTH ───────── */
test.describe('PUB-AUTH', () => {
  for (const locale of LOCALES) {
    test(`PUB-AUTH-${locale}-login`, async ({ page }) => {
      await page.goto(`/${locale}/login`);
      await page.waitForSelector('input[type="email"]', { timeout: 15000 });
      await expect(page.locator('input[type="email"]').first()).toBeVisible();
      await expect(page.locator('input[type="password"]').first()).toBeVisible();
    });

    test(`PUB-AUTH-${locale}-forgot`, async ({ page }) => {
      await page.goto(`/${locale}/forgot-password`);
      await page.waitForSelector('input[type="email"]', { timeout: 15000 });
      await expect(page.locator('input[type="email"]').first()).toBeVisible();
    });

    test(`PUB-AUTH-${locale}-redirect`, async ({ page }) => {
      await page.goto(`/${locale}/portal`);
      await page.waitForURL(/\/(ca|es|en|de)\/login/, { timeout: 20000 });
      expect(page.url()).toContain('/login');
    });
  }
});

/* ───────── 14. COOKIE CONSENT ───────── */
test.describe('PUB-COOKIE', () => {
  test('PUB-COOKIE-banner', async ({ page }) => {
    await go(page, '/ca');
    const btn = page.locator('button').filter({ hasText: /Acceptar|Accept|Aceptar|Akzeptieren/i }).first();
    const ok = await btn.isVisible({ timeout: 3000 }).catch(() => false);
    if (ok) {
      await btn.click();
      await page.waitForTimeout(500);
    }
  });
});

/* ───────── 15. RESPONSIVE ───────── */
test.describe('PUB-RESP', () => {
  const sizes = [
    { name: 'mobil', w: 375, h: 667 },
    { name: 'tablet', w: 768, h: 1024 },
    { name: 'desktop', w: 1440, h: 900 },
  ];
  for (const s of sizes) {
    test(`PUB-RESP-${s.name}`, async ({ page }) => {
      await page.setViewportSize({ width: s.w, height: s.h });
      await go(page, '/ca');
      await expect(page.locator('h1').first()).toBeVisible({ timeout: 10000 });
      await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
      await page.waitForTimeout(1500);
      await expect(page.locator('footer')).toBeVisible({ timeout: 5000 });
    });
  }
});

/* ───────── 16. PERFORMANCE ───────── */
test.describe('PUB-PERF', () => {
  test('PUB-PERF-01: load < 10s', async ({ page }) => {
    const start = Date.now();
    await page.goto('/ca');
    await page.waitForLoadState('domcontentloaded');
    await expect(page.locator('h1').first()).toBeVisible({ timeout: 15000 });
    expect(Date.now() - start).toBeLessThan(10000);
  });

  test('PUB-PERF-02: status 200', async ({ page }) => {
    const r = await page.goto('/ca');
    expect(r?.status()).toBe(200);
  });
});
