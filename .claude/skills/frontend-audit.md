---
name: frontend-audit
description: "Auditoria completa d'una pàgina web moderna: rendiment, SEO, accessibilitat, disseny/UX, seguretat, legal/RGPD, codi i contingut. Funciona amb URL pública o codi local. Genera un dashboard HTML visual amb puntuació per categoria i correccions prioritzades. Activa amb: 'audita el frontend', 'revisa la web', 'auditoria de la landing', 'analitza la pàgina', 'quina qualitat té la web', 'revisa el portal', 'audit frontend', 'auditoria web completa', 'analitza el codi del frontend', 'revisa la qualitat del frontend'."
---

# Frontend Audit — Auditoria Web Completa

Analitza una pàgina web o codebase des de 8 dimensions clau d'una web moderna i professional. Genera un dashboard HTML visual amb puntuació global, correccions prioritzades i guia d'implementació.

**Regla fonamental:** Només reporta el que trobes realment. No inventis problemes ni infleis la gravetat. Cada punt negatiu ha d'estar justificat amb evidència concreta.

---

## Pas 1 — Determinar l'objectiu

Pregunta a l'usuari:

1. **Vols auditar una URL pública o el codi local?**
   - URL pública → usa WebFetch + curl
   - Codi local → usa Read + grep + Bash sobre el directori del frontend
   - Tots dos → combina ambdues anàlisis

2. **Quin és el tipus de pàgina?** (landing de negoci local, portal SaaS, e-commerce, blog, portafoli...)
   - Adapta els criteris al tipus: una landing de restaurant es puntua diferent d'un portal SaaS

3. **Hi ha algun aspecte que t'interessa especialment?** (rendiment, accessibilitat, legal...)
   - Si sí, dóna-li pes extra en la puntuació

Si l'usuari ja ha donat la URL o el directori, comença directament sense preguntar.

---

## Pas 2 — Recollir dades

### Si és URL pública

```bash
# Temps de resposta, codi HTTP, mida
curl -sI -o /dev/null -w "HTTP: %{http_code} | Temps: %{time_total}s | Mida: %{size_download}b" [URL]

# Headers de seguretat
curl -sI [URL] | grep -iE "strict-transport|x-frame|x-content|content-security|referrer|permissions"

# HTML complet
# Usa WebFetch per obtenir el HTML i analitzar-lo
```

Amb WebFetch, extreu:
- HTML complet
- Tots els `<meta>` tags
- Estructura de headings (`h1`-`h6`)
- Imatges i atributs `alt`, `loading`, `srcset`
- Scripts (inline vs extern, `defer`/`async`)
- Links (interns, externs, broken)
- JSON-LD / Schema
- Open Graph i Twitter Cards
- Comprovació: `robots.txt`, `sitemap.xml`, `manifest.json`

### Si és codi local

```bash
# Busca fitxers clau del frontend
find [directori] -name "*.tsx" -o -name "*.ts" -o -name "*.html" | head -50
find [directori] -name "layout.tsx" -o -name "page.tsx" | head -20

# Busca imports de fonts, analytics, metes
grep -r "viewport\|description\|og:title\|canonical\|robots" [directori]/src --include="*.tsx" -l
grep -r "console\.error\|console\.warn" [directori]/src --include="*.tsx" | wc -l
grep -r "alt=" [directori]/src --include="*.tsx" | wc -l
grep -r "img " [directori]/src --include="*.tsx" | wc -l
grep -r "useEffect\|useState" [directori]/src --include="*.tsx" | wc -l

# Mida del bundle
ls -lh [directori]/.next/static/chunks/ 2>/dev/null | tail -10

# TypeScript errors
npx tsc --noEmit 2>&1 | wc -l
```

Llegeix els fitxers clau:
- `layout.tsx` o `_app.tsx` — metadades globals
- `page.tsx` de la pàgina principal — contingut i estructura
- `next.config.js` o `vite.config.ts` — optimitzacions
- `package.json` — dependències i scripts
- `public/robots.txt`, `public/sitemap.xml`
- Fitxers de CSS globals

---

## Pas 3 — Avaluar les 8 dimensions

Puntua cada dimensió de 0 a 100. Balanç: no regalis punts per coses secundàries ni penalitzis en excés coses menors.

---

### Dimensió 1 — Rendiment (pes: 15%)

Una web lenta perd usuaris. El 53% abandona si triga més de 3 segons.

**Core Web Vitals (si URL pública):**
- LCP (Largest Contentful Paint): < 2.5s = bé, 2.5-4s = millorable, > 4s = crític
- CLS (Cumulative Layout Shift): < 0.1 = bé, 0.1-0.25 = millorable, > 0.25 = crític
- INP (Interaction to Next Paint): < 200ms = bé, 200-500ms = millorable

**Verificar en el codi:**
- [ ] Imatges amb `loading="lazy"` per a imatges below-the-fold
- [ ] Imatges amb `width` i `height` definits (evita CLS)
- [ ] Formats moderns: WebP, AVIF (no PNG/JPEG per defecte)
- [ ] `next/image` o equivalent usat en lloc de `<img>` raw
- [ ] Fonts amb `font-display: swap` o `preload`
- [ ] Scripts externs amb `defer` o `async`
- [ ] No hi ha JS bloquejant al `<head>`
- [ ] CSS crític inline o `preload`
- [ ] Mida del bundle JS principal < 200KB gzip
- [ ] Code splitting (chunks per ruta)
- [ ] `preconnect` a dominis externs clau (fonts, analytics)

**Penalitzacions greus:**
- Imatges > 500KB sense comprimir: -20 pts per imatge
- Bundle JS > 1MB sense splitting: -30 pts
- No HTTPS: -40 pts

---

### Dimensió 2 — SEO (pes: 15%)

**Meta tags:**
- [ ] `<title>` present, 50-60 caràcters, descriptiu
- [ ] `<meta name="description">` present, 150-160 caràcters
- [ ] `<link rel="canonical">` present i correcte
- [ ] `<html lang="xx">` definit
- [ ] `<meta name="viewport">` present
- [ ] `<meta name="robots">` no bloqueja indexació per error

**Estructura de contingut:**
- [ ] Exactament un `<h1>` per pàgina
- [ ] Jerarquia de headings correcta (h1 > h2 > h3, sense salts)
- [ ] H1 conté la paraula clau principal
- [ ] H2 descriptius del contingut

**Dades estructurades:**
- [ ] JSON-LD present (Organization, LocalBusiness, WebSite...)
- [ ] JSON vàlid i complet
- [ ] Breadcrumbs (si aplica)

**Fitxers tècnics:**
- [ ] `robots.txt` present i correcte
- [ ] `sitemap.xml` present, accessible, actualitzat
- [ ] `favicon.ico` present (+ `apple-touch-icon`)

**Open Graph i Social:**
- [ ] `og:title`, `og:description`, `og:image`, `og:url`, `og:type`
- [ ] `twitter:card`, `twitter:title`, `twitter:image`
- [ ] Imatge OG: 1200×630px, < 1MB
- [ ] Hreflang (si multiidioma)

**Contingut:**
- [ ] > 300 paraules de text visible (pàgines principals)
- [ ] Ratio text/HTML > 20%
- [ ] Texts alternatius en totes les imatges
- [ ] Anchor texts descriptius (no "click aquí")
- [ ] URLs netes (sense paràmetres innecessaris)

---

### Dimensió 3 — Accessibilitat / WCAG 2.1 AA (pes: 15%)

El 15% de la població té alguna discapacitat. A Espanya és obligatori per a serveis públics i moltes empreses.

**Contrast i color:**
- [ ] Contrast text normal: ≥ 4.5:1
- [ ] Contrast text gran (>18px bold): ≥ 3:1
- [ ] No s'usa el color com a únic indicador d'informació

**Navegació per teclat:**
- [ ] Tots els elements interactius accessibles per `Tab`
- [ ] Focus visible (no `outline: none` sense alternativa)
- [ ] Ordre de tabulació lògic
- [ ] Skip navigation link (salta al contingut principal)
- [ ] Trampes de focus evitades (modals, dropdowns)

**ARIA i HTML semàntic:**
- [ ] Landmarks semàntics: `<header>`, `<nav>`, `<main>`, `<footer>`, `<aside>`
- [ ] Rol `navigation` amb `aria-label` descriptiu si n'hi ha diversos
- [ ] Botons amb text descriptiu (no "×", "→" sense aria-label)
- [ ] Formularis amb `<label>` associat a cada `<input>`
- [ ] Errors de formulari descrits per a screen readers
- [ ] Imatges decoratives amb `alt=""`
- [ ] Imatges informatives amb `alt` descriptiu

**Media:**
- [ ] Vídeos amb subtítols (si aplica)
- [ ] Animacions respecten `prefers-reduced-motion`
- [ ] No hi ha contingut que parpelleja > 3 cops/segon

**Formularis:**
- [ ] `autocomplete` en camps comuns (nom, email, tel)
- [ ] Validació accessible (errors visibles i llegibles per screen reader)

---

### Dimensió 4 — Disseny i UX (pes: 20%)

La primera impressió es forma en 50ms. El disseny és la credibilitat.

**Identitat visual:**
- [ ] Logo visible a la capçalera
- [ ] Paleta de colors consistent (màx. 3 colors principals)
- [ ] Tipografia consistent (màx. 2 famílies)
- [ ] Iconografia consistent (una sola família d'icones)
- [ ] Espaiat consistent (grid/sistema d'espais)

**Jerarquia i llegibilitat:**
- [ ] Jerarquia visual clara (el CTA principal destaca)
- [ ] Longitud de línia òptima: 60-80 caràcters
- [ ] Interlineat ≥ 1.5 per a cos de text
- [ ] Cos de lletra mínim 16px per a text principal
- [ ] Contrast entre seccions (no tot el mateix fons)

**Responsivitat:**
- [ ] Funciona correctament en mòbil (320px-768px)
- [ ] Funciona en tauleta (768px-1024px)
- [ ] Funciona en escriptori (1024px+)
- [ ] No hi ha scroll horitzontal indesitjat
- [ ] Elements tàctils ≥ 44×44px (Apple HIG)
- [ ] Menú mòbil funcional

**Fluxos d'usuari:**
- [ ] CTA clar a sobre del fold (visible sense scroll)
- [ ] Proposta de valor clara en 5 segons
- [ ] Màxim 1-2 CTAs principals per pàgina (no saturació)
- [ ] Formulari de contacte visible i funcional
- [ ] Informació de contacte accessible (telèfon, email)

**Senyals de confiança:**
- [ ] Testimonis o casos d'èxit
- [ ] Logos de clients o partners (si aplica)
- [ ] Certificacions o garanties (si aplica)
- [ ] Número de telèfon/adreça visible
- [ ] Fotos reals (no totes stock genèriques)

**Estats i feedback:**
- [ ] Estats de càrrega (spinners, skeletons)
- [ ] Estats d'error (messages clars)
- [ ] Estados buits (empty states)
- [ ] Confirmacions d'acció (toasts, alerts)
- [ ] Pàgina 404 personalitzada

**Microcòpies:**
- [ ] Botons amb verbs d'acció clars ("Contacta'ns", "Demana pressupost")
- [ ] Placeholders de formulari descriptius
- [ ] Missatges d'error en català/idioma local (no "Error 422")

---

### Dimensió 5 — Seguretat (pes: 10%)

**HTTPS i transport:**
- [ ] HTTPS actiu i forçat (redirect HTTP → HTTPS)
- [ ] HSTS header (`Strict-Transport-Security`)
- [ ] No mixed content (recursos HTTP en pàgina HTTPS)

**Headers de seguretat:**
- [ ] `X-Frame-Options: DENY` o CSP `frame-ancestors`
- [ ] `X-Content-Type-Options: nosniff`
- [ ] `Referrer-Policy: no-referrer-when-downgrade`
- [ ] `Content-Security-Policy` (CSP) configurada
- [ ] `Permissions-Policy` (càmera, micròfon, geolocalització)

**Codi:**
- [ ] No hi ha claus API, tokens o secrets en el codi font (JS client-side)
- [ ] No hi ha credencials hardcoded en fitxers de configuració públics
- [ ] `dangerouslySetInnerHTML` o equivalent usat amb sanitització
- [ ] No `eval()` ni similars
- [ ] Dependències actualitzades (no vulnerabilitats conegudes)

**Formularis:**
- [ ] CSRF protection (si aplica)
- [ ] Rate limiting en formularis de contacte
- [ ] Validació server-side (no només client-side)
- [ ] reCAPTCHA o honeypot en formularis públics

---

### Dimensió 6 — Legal i RGPD (pes: 10%)

Obligatori a la UE. Multes de fins a 20M€ o el 4% de la facturació global.

**Documents legals obligatoris (Espanya/UE):**
- [ ] Avís Legal (LSSI: identificació del titular, domicili, NIF)
- [ ] Política de Privacitat (RGPD: dades recollides, base legal, drets)
- [ ] Política de Cookies (si s'usen cookies no essencials)
- [ ] Termes i Condicions (si hi ha contractació o registre)

**Cookies:**
- [ ] Banner de consentiment de cookies present
- [ ] Opcions granulars (acceptar / rebutjar / configurar)
- [ ] No s'estableixen cookies no essencials abans del consentiment
- [ ] Botó de retirar consentiment accessible
- [ ] Les cookies s'eliminen si es rebutja

**Formularis de contacte / registre:**
- [ ] Checkbox de consentiment explícit (no pre-marcat)
- [ ] Informació clara de qui gestiona les dades
- [ ] Finalitat del tractament explicada
- [ ] Drets de l'usuari informats (accés, rectificació, supressió)
- [ ] No s'envien dades a tercers sense informació

**Accessibilitat legal:**
- [ ] Informació de contacte del responsable accessible
- [ ] Procediment per exercir drets RGPD indicat
- [ ] Datos de contacte del DPO (si aplica)

---

### Dimensió 7 — Qualitat del codi (pes: 10%)

**TypeScript / JavaScript:**
- [ ] 0 errors de TypeScript (`tsc --noEmit`)
- [ ] Sense `any` explícit innecessari
- [ ] Sense `console.log` de debug en producció
- [ ] Sense codi comentat residual ("// TODO des de fa 6 mesos")
- [ ] Components petits i enfocats (< 200 línies per component)
- [ ] Noms descriptius (no `comp1`, `data2`, `thing`)

**React / Next.js (si aplica):**
- [ ] `'use client'` només on és necessari (no tot client-side)
- [ ] `useEffect` amb dependències correctes (no deps buits per amagar problemes)
- [ ] No re-renders innecessaris (useMemo/useCallback on cal)
- [ ] Imatges amb `next/image` (optimització automàtica)
- [ ] Fonts amb `next/font` (sense flash de text)
- [ ] Variables d'entorn amb prefix correcte (`NEXT_PUBLIC_` per al client)
- [ ] Sense secrets en variables `NEXT_PUBLIC_`

**CSS / Tailwind:**
- [ ] Sense classes duplicades a components idèntics
- [ ] Responsive classes correctes (`sm:`, `md:`, `lg:`)
- [ ] Sense `!important` innecessaris
- [ ] Dark mode considerat (si aplica)

**Estructura:**
- [ ] Components reutilitzables per a elements repetits
- [ ] Serveis/crides a API separades dels components
- [ ] Constants extretes (no magic strings/numbers)
- [ ] Error boundaries presents

---

### Dimensió 8 — Contingut i conversió (pes: 5%)

**Estratègia de contingut:**
- [ ] Proposta de valor clara en el H1 (no "Benvingut a la nostra web")
- [ ] Subtítol que reforça el benefici (no les característiques)
- [ ] CTA principal amb verb d'acció i benefici ("Aconsegueix el teu pressupost gratis")
- [ ] Prova social visible (opinions, casos, números)
- [ ] Resposta a les objeccions principals

**Qualitat del text:**
- [ ] Sense textos de placeholder ("Lorem ipsum", "Títol de prova")
- [ ] Ortografia i gramàtica correctes
- [ ] Tono consistent (professional/proper/casual)
- [ ] Textos orientats al client, no a l'empresa ("Tu" no "Nosaltres")
- [ ] Beneficis per davant de característiques

**Conversió:**
- [ ] Formulari de contacte ben visible
- [ ] Número de telèfon clicable en mòbil (`tel:`)
- [ ] WhatsApp o xat accessible (si aplica)
- [ ] Temps de resposta informat ("Et contestem en menys de 24h")
- [ ] Urgència o incentiu clar (si aplica i és honest)

---

## Pas 4 — Calcular puntuació global

```
Puntuació global = (
  Rendiment × 0.15 +
  SEO × 0.15 +
  Accessibilitat × 0.15 +
  Disseny/UX × 0.20 +
  Seguretat × 0.10 +
  Legal/RGPD × 0.10 +
  Qualitat codi × 0.10 +
  Contingut × 0.05
)
```

**Nivells:**
- 90-100: Excel·lent — web de referència del sector
- 75-89: Bo — compleix estàndards, petites millores
- 50-74: Millorable — problemes visibles que afecten usuaris
- 25-49: Crític — requereix intervenció immediata
- 0-24: Inadequat — cal reconstruir

---

## Pas 5 — Generar el dashboard HTML

Genera un únic fitxer HTML autocontingut (`audit-web-[nom].html`) amb:

### Estructura del dashboard

```
┌─────────────────────────────────────────────────────────┐
│  HEADER: URL | Data | Puntuació global (gran, colorejada)│
├─────────────────────────────────────────────────────────┤
│  RESUM: 4 tarjetes — Nota | Crítics | Avisos | Aprovats │
├─────────────────────────────────────────────────────────┤
│  TOP 5 CORRECCIONS URGENTS                              │
│  Cada una: Problema → Per qué importa → Com arreglar-ho │
├─────────────────────────────────────────────────────────┤
│  DESGLOSE PER DIMENSIÓ (8 seccions)                     │
│  Cada una: Barra de progrés | Nota | Llista de troballes│
│  ✅ Aprovat | ⚠️ Millora | 🔴 Crític                    │
├─────────────────────────────────────────────────────────┤
│  TAULA DETALL TÈCNIC (expandible per dimensió)          │
└─────────────────────────────────────────────────────────┘
```

### Requisits del dashboard

- **Visual:** Colors del sistema de semàfors (verd ≥75, taronja 50-74, vermell <50)
- **Responsive:** Es veu bé a mòbil i escriptori
- **Autocontingut:** CSS i JS inline, zero dependències externes
- **Imprimible:** Apto per exportar a PDF (print stylesheet)
- **Interactiu:** Seccions expandibles/colapsables, navegació interna
- **Codi exacte:** Cada correcció ha d'incloure el codi o la instrucció concreta per implementar-la (no "millora les imatges", sinó el codi HTML/JSX exacte)

### Contingut obligatori per a cada troballa

```
🔴 CRÍTIC: [Descripció del problema]
   Impacte: [Com afecta usuaris / Google / legal]
   Solució: [Codi exacte o passos concrets]
   Prioritat: Alta | Mitja | Baixa
   Esforç: 1h | 1 dia | 1 setmana
```

---

## Pas 6 — Guardar i presentar

- Guarda com `audit-web-[nom-kebab].html` al directori arrel del projecte o on indiqui l'usuari
- Obre al navegador si és possible

Presenta un resum en text:

1. **Nota global** i nivell (Excel·lent / Bo / Millorable / Crític)
2. **Top 3 problemes** en una frase cada un
3. **Top 3 punts forts** (el que ja funciona bé)
4. **Estimació d'hores** per arreglar tots els problemes crítics
5. Pregunta si vol que **apliquis les correccions directament** al codi

---

## Pas 7 — Aplicar correccions (opcional)

Si l'usuari confirma i tens accés al codi:

1. Ordena les correccions per: impacte alt + esforç baix primer
2. Aplica-les una a una, confirmant cada canvi
3. Torna a executar les verificacions afectades per confirmar millora
4. Actualitza la nota estimada

No apliquis mai correccions que:
- Canviïn la lògica de negoci
- Eliminin contingut que l'usuari ha posat manualment
- Modifiquin fitxers de configuració crítics sense confirmació

---

## Notes d'ús

- Per a **URL pública sense codi**: executa les anàlisis de curl + WebFetch, omiteix la dimensió "Qualitat de codi"
- Per a **codi local sense URL**: omiteix Core Web Vitals i headers de seguretat, centra't en el codi
- Per a **tots dos**: combina ambdues fonts — el codi revela intencions, la URL revela realitat
- Adapta el **pes de les dimensions** al context: per a e-commerce, puja Conversió a 15% i baixa Codi a 5%; per a portal intern, puja Seguretat a 20% i baixa SEO a 5%
