# Millores Frontend — Portal AMG Digitalització

> Generat per `/frontend-audit` · 14 maig 2026 · Nota inicial: **42/100** → Nota actual: **78/100**

Marca cada millora amb `[x]` quan estigui feta.

---

## 🔴 Ràpides — < 2h cadascuna

- [x] **Favicon SVG** — Creat `/public/favicon.svg` amb la marca AMG.
- [x] **`autocomplete` al login** — Afegit `autocomplete="email"` i `autocomplete="current-password"`.
- [x] **`<img>` raw a BlockRenderer** — Afegit `loading="lazy"` i `decoding="async"`.
- [x] **Tel: links** — Afegit `<a href="tel:+34971000000">` al footer del login. ⚠️ Substituir pel número real.
- [x] **`preconnect` a Google Fonts** — Afegits `<link rel="preconnect">` al layout.
- [x] **`og-image.jpg` (1200×630px)** — Generat dinàmicament via `src/app/opengraph-image.tsx` amb `next/og`. Disseny AMG complet: grid fosc, headline gran, stats bar, accent taronja. Next.js l'injecta automàticament als tags OG i Twitter.
- [x] **`favicon.ico` + `apple-touch-icon.png`** — Generats dinàmicament: `src/app/icon.tsx` (32×32 PNG) i `src/app/apple-icon.tsx` (180×180 PNG). Hexàgon AMG taronja sobre fons fosc. Servits automàticament per Next.js App Router.

---

## 🟡 Mitjà termini — 1-2 dies cadascuna

- [x] **`prefers-reduced-motion`** — Afegit bloc `@media (prefers-reduced-motion: reduce)` a `globals.css`.
- [x] **Contrast de color** — `#64748b` → `#8896aa` en tots els textos secundaris. Passa WCAG AA (4.51:1).
- [x] **Refactor `portal/page.tsx`** — Extrets `<PortalSidebar>`, `<ServiceCards>`, `<InvoicesTable>` i `<OnboardingGuide>`.
- [x] **Reduir `any`** — De 56 a 0 usos explícits. `IconProps` type a `icons.tsx`, `catch (err: unknown)` arreu.
- [x] **Onboarding per a nous clients** — `<OnboardingGuide>`: detecta estat buit, 3 passos amb progrés, skip via `localStorage`.
- [x] **Verificar labels als formularis** — `aria-label` al botó mostrar/ocultar contrasenya. Inputs amb `<label>`.
- [x] **Mobile: grid de factures** — Grid simplificat `[1fr_72px_24px]` a mòbil, complet a `sm+`.
- [x] **Migrar 253 valors Tailwind inline a tokens** — 442 substitucions en 24 fitxers via script Python. `text-[10px]`→`text-label`, `text-[#FF9A3C]`→`text-accent-light`, `tracking-[0.2em]`→`tracking-widest`, etc. TypeScript compila net (`tsc --noEmit` sense errors).
- [x] **Links legals al portal autenticat** — Footer mínim afegit a `portal/page.tsx`: links a Avís Legal, Privacitat, Cookies, Suport i copyright dinàmic.
- [x] **Focus visible consistent** — `:focus-visible { outline: 2px solid #FF6B00; }` afegit a `globals.css`. Suprimeix outline en click (`focus:not(:focus-visible)`), mantenint accessibilitat per teclat.

---

## 🔵 Gran esforç — > 1 setmana

- [x] **Landing pública multi-idioma (Mòdul 13)** — next-intl v4 instal·lat. Routing `/[locale]/` (ca/es/en/de) amb middleware. Landing completa: Header, Hero, Problem, Services, HowItWorks, Pricing, CTA, Footer. Missatges en 4 idiomes. generateMetadata amb hreflang i OG per locale. JSON-LD Organization a `[locale]/layout.tsx`. CookieConsentBanner amb localStorage. Sitemap dinàmic (`app/sitemap.ts`) amb alternates hreflang. Build compila net.
- [ ] **Lighthouse audit en producció** — Un cop desplegat a Hetzner/Coolify, executar Lighthouse per mesurar Core Web Vitals reals (LCP, CLS, INP).
- [x] **Revisar Server Components** — Auditoria completada: tots els `'use client'` son legítims (hooks, handlers, context). Landing sections (Hero, Problem, Services, HowItWorks, Pricing, CTA, Footer) ja son Server Components correctament.
- [x] **Pàgines legals — omplir NIF i adreça** — NIF: 182237442B · Titular: Antonio Mas Gutiérrez · Domicili: Carrer Joan Capó, núm. 7, 1r esq., 07200 Felanitx. Actualitzats els 4 fitxers legals.
- [x] **Content-Security-Policy (CSP)** — Implementat amb nonces per request al middleware. `script-src 'nonce-xxx' 'strict-dynamic'`, `style-src nonce+fonts.googleapis.com`, `font-src fonts.gstatic.com`, `frame-ancestors 'none'`, `upgrade-insecure-requests`. JSON-LD rep el nonce via `headers()` al Server Component.
- [x] **Banner de cookies (RGPD)** — `CookieConsentBanner` implementat a `[locale]/layout.tsx`. Acceptar/rebutjar opcionals, persistència a `localStorage('cookie_consent')`.
- [x] **Structured data (JSON-LD)** — Organization schema injectat a `[locale]/layout.tsx` amb `dangerouslySetInnerHTML`.

---

## ✅ Ja fet

- [x] Design System complet — Space Grotesk (body), Orbitron (display), Share Tech Mono. Paleta de tokens completa (accent, border, ink, semantic). Escala tipogràfica Major Third (label→7xl). CSS custom properties.
- [x] Headers de seguretat HTTP (X-Frame-Options, HSTS, X-Content-Type-Options, Referrer-Policy, Permissions-Policy) — `next.config.js`
- [x] DOMPurify per sanititzar `dangerouslySetInnerHTML` — `BlockRenderer.tsx`
- [x] Metadata Open Graph + Twitter Cards completes — `layout.tsx`
- [x] Skip navigation link per a navegació per teclat — `layout.tsx`
- [x] Landmarks ARIA (`aria-label` a aside, nav, main, botó mòbil) — `portal/page.tsx`
- [x] Pàgina Avís Legal (LSSI compliant) — `legal/avis-legal/page.tsx`
- [x] Política de Privacitat (RGPD: finalitats, base legal, drets) — `legal/privacitat/page.tsx`
- [x] Política de Cookies (taula de cookies, instruccions navegadors) — `legal/cookies/page.tsx`
- [x] Links del login corregits (Termes, Privacitat, Suport amb href reals) — `login/page.tsx`
- [x] Pàgina 404 personalitzada — `not-found.tsx`
- [x] `robots.txt` (bloqueja `/portal/`, `/api/`) — `public/robots.txt`
- [x] `sitemap.xml` amb hreflang per als 4 idiomes — `public/sitemap.xml`
- [x] `.env.example` per a nous developers — `.env.example`
