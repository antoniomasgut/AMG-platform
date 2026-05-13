# Mòdul 13: i18n + Landing Professional + SEO + RGPD

> **Versió:** 1.0
> **Data:** 2026-05-12
> **Dependències:** Cap (mòdul base del frontend)

---

## 1. Objectius

- Implementar internacionalització (i18n) amb next-intl per a 4 idiomes: català, castellà, anglès i alemany
- Crear una landing professional multi-idioma per a la pàgina d'inici del portal
- Implementar SEO complert (sitemap, robots.txt, JSON-LD, OG tags, hreflang)
- Complir amb RGPD/LSSI: avís legal, política de privacitat, termes del servei, consentiment de cookies

---

## 2. Abast

### 2.1 Funcionalitats incloses

- Routing `/[locale]/` per a ca, es, en, de amb next-intl v4
- Landing professional amb seccions: Hero, Serveis, Preus, Com funciona, CTA, Header, Footer
- Pàgines d'autenticació multi-idioma (login, forgot-password, reset-password)
- Portal del client multi-idioma
- 3 pàgines legals: Avís Legal, Política de Privacitat, Termes del Servei
- CookieConsentBanner amb persistència a localStorage
- Sitemap XML dinàmic
- robots.txt
- JSON-LD dades estructurades (Organization, WebSite)
- OG tags (Open Graph) per a xarxes socials
- hreflang tags per a SEO multi-idioma
- Selector d'idioma (LocaleSwitcher)

### 2.2 Funcionalitats excloses

- Traducció automàtica (les traduccions són manuals)
- CMS per gestionar contingut de la landing
- A/B testing

---

## 3. Configuració tècnica

### 3.1 next-intl v4

```
next.config.js:
  - const withNextIntl = require('next-intl/plugin')()
  - module.exports = withNextIntl({ ... })

src/i18n/routing.ts:
  - locales: ['ca', 'es', 'en', 'de']
  - defaultLocale: 'ca'
  - localePrefix: 'always'

src/i18n/request.ts:
  - getRequestConfig: importa messages del locale

src/middleware.ts:
  - createNavigationMiddleware(routing)
```

### 3.2 Rutes

```
/                          → Redirigeix a /ca
/[locale]/                 → Landing professional
/[locale]/login            → Login
/[locale]/forgot-password  → Recuperar contrasenya
/[locale]/reset-password   → Restablir contrasenya
/[locale]/portal           → Dashboard del client
/[locale]/legal/avis-legal               → Avís Legal
/[locale]/legal/politica-privacitat      → Política de Privacitat
/[locale]/legal/termes-servei            → Termes del Servei
/sitemap.xml               → Sitemap dinàmic
/robots.txt                → Robots estàtic
```

### 3.3 Fitxers de traducció

```
messages/
├── ca.json     → Català
├── es.json     → Castellà
├── en.json     → Anglès
└── de.json     → Alemany
```

Seccions de cada fitxer:
- `landing`: Hero, Serveis, Preus, Com funciona, CTA, Footer
- `auth`: Login, forgot-password, reset-password
- `portal`: Dashboard, sidebar, serveis
- `legal`: Avís Legal, Privacitat, Termes
- `common`: Navegació, botons, error/success messages

---

## 4. Components del frontend

### 4.1 Landing

| Component | Descripció |
|-----------|-----------|
| `Header` | Barra de navegació superior amb logo, enllaços i LocaleSwitcher |
| `Hero` | Secció principal amb títol, subtítol, CTA i imatge de fons |
| `ServicesSection` | Secció de serveis (targetes amb icona, títol, descripció) |
| `ProblemSection` | Secció "Problema/Solució" que connecta amb el dolor del client |
| `PricingSection` | Secció de preus amb taula de plans |
| `HowItWorks` | Secció "Com funciona" amb 3 passos |
| `CTASection` | Secció de crida a l'acció final amb botó destacat |
| `Footer` | Footer amb enllaços legals, xarxes socials i copyright |
| `LocaleSwitcher` | Selector d'idioma (ca/es/en/de) |

### 4.2 Legals

| Component | Descripció |
|-----------|-----------|
| `CookieConsentBanner` | Banner de consentiment de cookies amb localStorage |
| `LegalLayout` | Layout compartit per a pàgines legals |

### 4.3 SEO

| Fitxer | Descripció |
|--------|-----------|
| `src/app/sitemap.ts` | Sitemap XML dinàmic amb URLs de tots els idiomes |
| `src/app/robots.ts` | robots.txt |
| `src/app/[locale]/layout.tsx` | generateMetadata amb OG tags + hreflang + JSON-LD |

---

## 5. SEO

### 5.1 Metadades (layout.tsx)

```typescript
export async function generateMetadata({ params: { locale } }: Props): Promise<Metadata> {
  return {
    title: { template: '%s | AMG', default: 'AMG · Enginyeria Digital' },
    description: '...',
    alternates: {
      canonical: `https://amg.cat/${locale}`,
      languages: {
        ca: '/ca', es: '/es', en: '/en', de: '/de',
      },
    },
    openGraph: {
      title: 'AMG · Enginyeria Digital',
      description: '...',
      url: `https://amg.cat/${locale}`,
      siteName: 'AMG Digitalització',
      locale: 'ca_ES',  // alterna segons locale
      type: 'website',
      images: [{ url: '/og-default.jpg', width: 1200, height: 630 }],
    },
    twitter: { card: 'summary_large_image', title: 'AMG · Enginyeria Digital', description: '...' },
  };
}
```

### 5.2 JSON-LD (layout.tsx)

```typescript
const jsonLd = {
  '@context': 'https://schema.org',
  '@type': 'Organization',
  name: 'AMG Digitalització',
  url: `https://amg.cat/${locale}`,
  contactPoint: { '@type': 'ContactPoint', telephone: '+34-XXX-XXX-XXX', contactType: 'customer service' },
};
```

---

## 6. RGPD / LSSI

### 6.1 Pàgines legals

| Pàgina | Contingut |
|--------|-----------|
| Avís Legal | Dades del responsable, propietat intel·lectual, condicions d'ús |
| Política de Privacitat | Dades recollides, finalitat, base legal, drets ARSO, conservació, destinataris |
| Termes del Servei | Condicions de contractació, preus, cancel·lació, responsabilitat |

### 6.2 Cookie Consent

- Banner visible a la primera visita amb: "Acceptar totes" i "Configurar"
- Guarda consentiment a localStorage (`cookie_consent`)
- Diferència entre cookies tècniques i analítiques
- El consentiment s'elimina si l'usuari esborra localStorage

---

## 7. Tests (e2e amb Playwright)

| # | Cas | Resultat |
|---|-----|---------|
| 1 | Landing carrega en català | 200, texts en català |
| 2 | Canvi d'idioma a anglès | URL canvia a /en, texts en anglès |
| 3 | Login renderitza correctament | Formulari visible, títol traduït |
| 4 | Avís Legal accessible | Contingut visible |
| 5 | Política Privacitat accessible | Contingut visible |
| 6 | Termes Servei accessible | Contingut visible |
| 7 | Cookie banner visible a primera visita | Banner es mostra |
| 8 | Cookie banner desapareix en acceptar | Banner amagat, consentiment a localStorage |
| 9 | Sitemap.xml accessible | 200, XML vàlid |
| 10 | robots.txt accessible | 200 |
| 11 | hreflang tags presents al HEAD | 4 tags (ca/es/en/de) |
| 12 | OG tags presents al HEAD | og:title, og:description, og:image |
| 13 | JSON-LD present al HEAD | Script type="application/ld+json" |
| 14 | Landing responsiva (375px, 768px, 1440px) | No overflow horitzontal |
| 15 | Navegació header funciona | Enllaços canvien de pàgina |
| 16 | Footer mostra enllaços legals | 3 enllaços (Avís, Privacitat, Termes) |
| 17 | Login incorrecte mostra error | Missatge d'error visible |
| 18 | Reset password mostra formulari | Input email + botó enviar |
| 19 | 404 per ruta inexistent | 404 |
| 20 | Portal redirect a login si no autenticat | Redirigeix a /{locale}/login |
| 21 | LocaleSwitcher accessible des de mòbil | Menú hamburguesa amb selector d'idioma |

---

## 8. Dependències npm

| Paquet | Versió | Ús |
|--------|--------|-----|
| next-intl | ^4.0.0 | Routing i traduccions |
| @playwright/test | ^1.50 | Tests e2e |

---

## 9. Dependències entre mòduls

| Mòdul | Dependència | Tipus |
|-------|-----------|-------|
| Mòdul 13 (i18n) | Cap (és el mòdul base del frontend) | — |
| Mòdul 14 (Admin Frontend) | Mòdul 13 (i18n) | Forta |
| Mòdul 05 (Factory) | Mòdul 13 (i18n) | Forta |
