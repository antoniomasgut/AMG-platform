---
name: playwright-verify
description: "Verifica que una pàgina web generada funciona correctament usant Playwright. Comprova: càrrega, consola sense errors, elements visibles, animacions, formularis i responsivitat. S'activa amb: 'verifica la web', 'comprova que funciona', 'testa la pàgina', 'obre i verifica', 'playwright', 'check the page', 'verifica el html', 'testa el fitxer', 'comprova la landing'."
---

# Playwright Verify — Verificació automàtica de pàgines web

Executa verificacions automàtiques sobre fitxers HTML o URLs locals per confirmar que la pàgina generada funciona correctament.

**Eina disponible:** `npx playwright` (v1.60, Chromium instal·lat)

---

## Pas 1 — Identificar el target

Determina què cal verificar:

1. **Fitxer local** — si l'usuari acaba de generar un `.html`, usa la ruta absoluta: `file:///ruta/al/fitxer.html`
2. **Servidor local** — si hi ha un servidor corrent (Next.js, Vite, etc.), usa `http://localhost:PORT`
3. **URL externa** — si l'usuari proporciona una URL

Si no és clar, pregunta quin fitxer o URL cal verificar.

---

## Pas 2 — Crear l'script de verificació

Crea un fitxer temporal `/tmp/pw-verify.mjs` amb aquest contingut adaptat al target:

```javascript
import { chromium } from 'playwright';

const TARGET = process.argv[2] || 'file:///tmp/index.html';

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();

const errors = [];
const warnings = [];

// Capturar errors de consola
page.on('console', msg => {
  if (msg.type() === 'error') errors.push(`[CONSOLA ERROR] ${msg.text()}`);
  if (msg.type() === 'warning') warnings.push(`[CONSOLA WARN] ${msg.text()}`);
});

// Capturar errors de xarxa
page.on('requestfailed', req => {
  errors.push(`[XARXA ERROR] ${req.url()} — ${req.failure()?.errorText}`);
});

console.log(`\n🔍 Verificant: ${TARGET}\n`);

try {
  const response = await page.goto(TARGET, { waitUntil: 'networkidle', timeout: 15000 });

  // 1. Codi HTTP
  if (response && response.status() >= 400) {
    errors.push(`HTTP ${response.status()}`);
  }

  // 2. Títol
  const title = await page.title();
  console.log(`📄 Títol: ${title || '(buit)'}`);

  // 3. Esperar que carregui el contingut visible
  await page.waitForTimeout(1500);

  // 4. Comprovar elements clau
  const checks = [
    { selector: 'body', label: 'body' },
    { selector: 'h1, h2', label: 'heading principal' },
    { selector: 'nav, header', label: 'navegació/header' },
    { selector: 'img', label: 'imatges' },
    { selector: 'footer', label: 'footer' },
  ];

  console.log('\n📋 Elements detectats:');
  for (const { selector, label } of checks) {
    const count = await page.locator(selector).count();
    const icon = count > 0 ? '✅' : '⚠️ ';
    console.log(`  ${icon} ${label}: ${count} element(s)`);
  }

  // 5. Formularis
  const forms = await page.locator('form').count();
  if (forms > 0) {
    console.log(`\n📝 Formularis: ${forms}`);
    const inputs = await page.locator('input, textarea, select').count();
    console.log(`   Camps: ${inputs}`);
  }

  // 6. Links trencats
  const links = await page.locator('a[href]').all();
  const brokenLinks = [];
  for (const link of links.slice(0, 20)) {
    const href = await link.getAttribute('href');
    if (href && href.startsWith('#')) continue;
    if (href && (href.startsWith('http') || href.startsWith('file'))) {
      try {
        const res = await page.request.get(href, { timeout: 3000 }).catch(() => null);
        if (res && res.status() >= 400) brokenLinks.push(href);
      } catch {}
    }
  }
  if (brokenLinks.length > 0) {
    brokenLinks.forEach(l => errors.push(`[LINK TRENCAT] ${l}`));
  }

  // 7. Screenshot
  const screenshotPath = '/tmp/pw-screenshot.png';
  await page.screenshot({ path: screenshotPath, fullPage: true });
  console.log(`\n📸 Screenshot: ${screenshotPath}`);

  // 8. Viewport mòbil
  await page.setViewportSize({ width: 375, height: 812 });
  await page.waitForTimeout(500);
  const mobileScreenshot = '/tmp/pw-screenshot-mobile.png';
  await page.screenshot({ path: mobileScreenshot });
  console.log(`📱 Screenshot mòbil: ${mobileScreenshot}`);

} catch (err) {
  errors.push(`[CÀRREGA] ${err.message}`);
}

await browser.close();

// Resum final
console.log('\n' + '─'.repeat(50));
if (errors.length === 0) {
  console.log('✅ VERIFICACIÓ SUPERADA — cap error detectat');
} else {
  console.log(`❌ ERRORS (${errors.length}):`);
  errors.forEach(e => console.log(`   • ${e}`));
}
if (warnings.length > 0) {
  console.log(`\n⚠️  AVISOS (${warnings.length}):`);
  warnings.forEach(w => console.log(`   • ${w}`));
}
console.log('─'.repeat(50) + '\n');

process.exit(errors.length > 0 ? 1 : 0);
```

---

## Pas 3 — Executar la verificació

```bash
node /tmp/pw-verify.mjs "URL_O_RUTA"
```

Exemples:
- `node /tmp/pw-verify.mjs "file:///home/toma/projectes/portalAMGv3-claude/kit-web-scrolling/output/index.html"`
- `node /tmp/pw-verify.mjs "http://localhost:3000"`

Captura la sortida amb `Bash` i analitza els resultats.

---

## Pas 4 — Interpretar i reportar

Després d'executar, reporta:

### Si tot OK (`exit 0`):
```
✅ Pàgina verificada correctament
   • Títol: [títol]
   • Elements: heading, nav, footer detectats
   • Cap error de consola ni links trencats
   • Screenshots: /tmp/pw-screenshot.png (desktop) i /tmp/pw-screenshot-mobile.png (mòbil)
```

### Si hi ha errors (`exit 1`):
Llista els errors i proposa correccions concretes. Per a cada error:

| Error | Causa probable | Acció |
|-------|---------------|-------|
| `[CONSOLA ERROR] ReferenceError: X is not defined` | Variable JS no declarada | Revisa el JS del fitxer |
| `[XARXA ERROR] assets/foto.jpg` | Fitxer d'imatge no trobat | Comprova la ruta de l'asset |
| `[LINK TRENCAT] https://...` | URL externa trencada | Elimina o corregeix l'enllaç |
| `[CÀRREGA] Timeout` | La pàgina tarda massa | Revisa si hi ha recursos bloquejants |

Corregeix automàticament els errors que puguis (JS mal formatat, rutes d'assets incorrectes, tags HTML no tancats). Per als que no puguis corregir, explica com fer-ho.

---

## Pas 5 — Verificació post-correcció

Si has fet correccions, re-executa la verificació per confirmar que els errors han desaparegut. No donguis la pàgina per bona fins que `exit 0`.

---

## Checks especials per tipus de pàgina

### Web amb animacions scroll (kit-web-scrolling)
Afegeix al script:
```javascript
// Simular scroll complet
await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
await page.waitForTimeout(1000);
await page.evaluate(() => window.scrollTo(0, 0));
// Comprovar que no hi ha errors JS durant el scroll
```

### Next.js / React (frontend de la plataforma)
```javascript
// Comprovar hidratació
const hydrationErrors = errors.filter(e => e.includes('Hydration') || e.includes('hydrat'));
if (hydrationErrors.length > 0) {
  console.log('⚠️  Error d\'hidratació detectat — revisa useTranslations vs next-intl/client');
}
```

### Formularis
```javascript
// Comprovar que els formularis tenen action o handler
const formsWithoutAction = await page.locator('form:not([action]):not([onsubmit])').count();
```
