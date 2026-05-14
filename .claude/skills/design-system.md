---
name: design-system
description: "Analitza el sistema de disseny d'un frontend (colors, tipografia, escala tipogràfica, espaiat) i genera un design system modern i coherent. Produeix codi concret: tokens CSS, config Tailwind, globals.css actualitzat. S'activa amb: 'analitza el disseny', 'millora els colors', 'crea un design system', 'selecciona fonts', 'grandària tipografies', 'sistema de colors', 'disseny modern', 'design system', 'paleta de colors', 'escala tipogràfica', 'modernitza el frontend', 'font pairing'."
---

# Design System Analyst — Disseny Modern per a Frontends

Analitza el sistema de disseny actual, aplica principis moderns (2025) i genera codi concret: tokens, Tailwind config i globals.css. No genera recomanacions abstractes — sempre acaba amb codi llest per aplicar.

---

## Pas 1 — Llegir l'estat actual

Llegeix en paral·lel:
- `tailwind.config.js` / `tailwind.config.ts`
- `src/app/globals.css`
- `src/app/layout.tsx` (fonts carregades)
- Un component representatiu (portal/page.tsx o equivalent)

Identifica:
- **Colors actuals**: valors hex, com s'usen (fons, text, accent, semàntic)
- **Fonts actuals**: quines famílies, pesos, variables CSS
- **Escala tipogràfica**: mides de text usades (text-xs, text-sm, text-xl, etc.)
- **Espaiat i radis**: spacing, border-radius dominants
- **Estètica general**: dark/light, minimalista, editorial, tech, orgànic

---

## Pas 2 — Avaluar el sistema actual

### Colors — Criteris d'avaluació

**Estructura d'una paleta moderna:**
```
Brand primary:  50 · 100 · 200 · 300 · 400 · 500 · 600 · 700 · 800 · 900 · 950
Neutral:        50 · 100 · 200 · 300 · 400 · 500 · 600 · 700 · 800 · 900 · 950
Semantic:       success · warning · danger · info (cadascun amb variant light/dark)
Surface tokens: background · surface · surface-raised · border · border-subtle
Text tokens:    foreground · muted · placeholder · disabled
```

**Contrast WCAG (comprovació obligatòria):**
- Text normal (< 18px bold, < 24px regular): ≥ 4.5:1
- Text gran (≥ 18px bold o ≥ 24px regular): ≥ 3:1
- Elements UI (botons, inputs, icones): ≥ 3:1
- Eina de comprovació: https://webaim.org/resources/contrastchecker/

**Errors habituals a detectar:**
- Gris secundari massa clar sobre fons fosc (contrast < 4.5:1)
- Massa variació de grocs/taronges (accent ≠ warning)
- Falta de color de surface intermedi entre bg i bg-raised
- Accent únic sense variants (light, dark, hover, pressed)
- Mancança de color de border semàntic (success-border, danger-border)

**Principis de color moderns (2025):**
- **Dark mode first**: fons molt fosc (#0a0a0f a #1a1a2e), no negre pur
- **Accents vibrants**: saturació alta (S>70%) però lluminositat controlada (L 45-60%)
- **Neutrals amb tint**: mai gris pur, sempre lleugera tonalitat (blau fred o calidesa)
- **3-tonal rule**: fons + surface + accent. Tot el reste és variació d'aquests 3.
- **Semantic ≠ Brand**: el taronja d'accent NO és el groc de warning

### Tipografia — Criteris d'avaluació

**Anatomia d'un bon pairing de fonts:**
```
DISPLAY font:  per a títols H1/H2. Ha de tenir personalitat. Carregada amb weight 700-900.
BODY font:     per a cos de text i UI (labels, botons, navegació). Óptima llegibilitat.
MONO font:     per a codis, números, metadades. Ha de combinar amb el body.
```

**Estils de pairing per estètica:**

| Estètica | Display | Body | Mono |
|----------|---------|------|------|
| **Tech / SaaS industrial** | Orbitron, Space Grotesk, Monument Extended | Inter, DM Sans, Geist | JetBrains Mono, Fira Code, Share Tech Mono |
| **Minimalista / Product** | Geist, Plus Jakarta Sans, Syne | Inter, DM Sans, Outfit | Geist Mono, IBM Plex Mono |
| **Editorial / Premium** | Fraunces, Playfair Display, Cormorant | DM Sans, Libre Baskerville | Roboto Mono |
| **Startup / Energètic** | Cabinet Grotesk, Clash Display | Inter, Manrope | JetBrains Mono |
| **Corporatiu / Trust** | Lora, Merriweather | Source Sans 3, Nunito | IBM Plex Mono |

**Paràmetres de qualitat tipogràfica:**
- **Display**: letter-spacing negatiu per a mides grans (`tracking-tight` a `-0.04em`)
- **Body**: letter-spacing neutre o lleugerament positiu; `tracking-normal` a `tracking-wide`
- **Mono uppercase labels**: `tracking-[0.12em]` a `tracking-[0.25em]` + `text-[10px]` a `text-[11px]`
- **Line height**: display `leading-[1.05]` a `leading-tight`; body `leading-relaxed` (1.6-1.7)

**Escala tipogràfica moderna (Major Third — factor 1.25):**
```
--text-xs:   10px  / 0.625rem  → labels mono
--text-sm:   12px  / 0.75rem   → captions, metadades
--text-base: 14px  / 0.875rem  → cos de text UI
--text-md:   16px  / 1rem      → body estàndard
--text-lg:   18px  / 1.125rem  → subtítols
--text-xl:   20px  / 1.25rem   → títols petits
--text-2xl:  24px  / 1.5rem    → títols de secció
--text-3xl:  30px  / 1.875rem  → títols de pàgina
--text-4xl:  36px  / 2.25rem   → hero secundari
--text-5xl:  48px  / 3rem      → hero principal
--text-6xl:  60px  / 3.75rem   → display gran
--text-7xl:  72px  / 4.5rem    → display màxim
```

**Fluid typography** (recomanat per responsive):
```css
/* Exemple: títol que va de 36px (mòbil) a 72px (desktop) */
font-size: clamp(2.25rem, 5vw + 1rem, 4.5rem);
```

### Espaiat i Radis — Principis

**Sistema d'espaiat consistent (base 4px):**
```
4px · 8px · 12px · 16px · 20px · 24px · 32px · 40px · 48px · 64px · 80px · 96px · 128px
```

**Border-radius per estètica:**
- Tech/Industrial: `0px` (cap radius) o `2-4px` (molt subtil)
- Minimalista modern: `6-8px` (suau)
- Startup amigable: `12-16px` (arrodonit)
- Pills/tags: sempre `999px` independent de l'estètica

---

## Pas 3 — Diagnosticar i classificar l'estètica

Analitza el codi llegit i classifica l'estètica en una de les categories:
1. **Tech Industrial** — dark, geomètric, mono, orange/cyan accents
2. **SaaS Minimalista** — light/dark, Inter, radis suaus, blau/violeta
3. **Editorial Premium** — contrast alt, serifs, espai blanc generós
4. **Startup Energètic** — colorit, gradients, radis grans, sans-serif modern
5. **Corporatiu** — neutral, conservador, tipografia clàssica

---

## Pas 4 — Generar proposta de design system

Basant-te en l'estètica detectada, genera un design system complet.

### 4.1 Paleta de colors

Genera la paleta completa amb hex values concrets. Estructura recomanada:

```typescript
// tailwind.config.ts — colors section
colors: {
  // Brand
  brand: {
    50:  '#fff3e0',
    100: '#ffe0b2',
    // ... fins a 950
    DEFAULT: '#FF6B00',  // el color principal
  },
  // Neutrals (amb lleugera tonalitat de brand)
  neutral: {
    0:   '#ffffff',
    50:  '#f8f9fa',
    100: '#f1f2f4',
    // ...
    900: '#111827',
    950: '#0a0a12',
  },
  // Semantic
  success: { DEFAULT: '#22c55e', light: '#dcfce7', dark: '#166534' },
  warning: { DEFAULT: '#f59e0b', light: '#fef3c7', dark: '#92400e' },
  danger:  { DEFAULT: '#ef4444', light: '#fee2e2', dark: '#991b1b' },
  info:    { DEFAULT: '#3b82f6', light: '#dbeafe', dark: '#1e40af' },
  // Surface tokens (per dark mode)
  surface: {
    0: '#0d0d1a',   // background base
    1: '#13132a',   // cards, sidebar
    2: '#1a1a2e',   // inputs, hover
    3: '#212140',   // borders, dividers
  },
  // Text tokens
  ink: {
    0: '#e2e8f0',   // text principal
    1: '#94a3b8',   // text secundari
    2: '#8896aa',   // text muted (ha de passar WCAG AA)
    3: '#64748b',   // text molt discret (NOMÉS per elements decoratius)
  },
}
```

**Per cada color proposat, cal indicar:**
- Hex value
- Ús recomanat
- Contrast ratio sobre el fons principal (cal que passi 4.5:1 per a text)

### 4.2 Sistema de fonts

```typescript
// tailwind.config.ts — fontFamily section
fontFamily: {
  display: ['NomFont', 'fallback', 'sans-serif'],
  sans:    ['NomFont', 'system-ui', 'sans-serif'],
  mono:    ['NomFont', 'ui-monospace', 'monospace'],
},
```

```typescript
// layout.tsx — carrega de fonts
import { NomFont1, NomFont2 } from 'next/font/google';
const font1 = NomFont1({
  subsets: ['latin'],
  weight: ['400', '600', '700'],
  display: 'swap',
  variable: '--font-display',
  preload: true,
});
```

### 4.3 Escala tipogràfica i tokens CSS

```css
/* globals.css — Typography tokens */
:root {
  /* Mides */
  --text-xs:    0.625rem;   /* 10px */
  --text-sm:    0.75rem;    /* 12px */
  --text-base:  0.875rem;   /* 14px */
  --text-md:    1rem;       /* 16px */
  --text-lg:    1.125rem;   /* 18px */
  --text-xl:    1.25rem;    /* 20px */
  --text-2xl:   1.5rem;     /* 24px */
  --text-3xl:   1.875rem;   /* 30px */
  --text-4xl:   2.25rem;    /* 36px */
  --text-5xl:   3rem;       /* 48px */
  --text-6xl:   3.75rem;    /* 60px */
  --text-7xl:   4.5rem;     /* 72px */

  /* Line heights */
  --leading-none:    1;
  --leading-tight:   1.1;
  --leading-snug:    1.25;
  --leading-normal:  1.5;
  --leading-relaxed: 1.65;
  --leading-loose:   2;

  /* Letter spacing */
  --tracking-tighter: -0.04em;
  --tracking-tight:   -0.02em;
  --tracking-normal:  0em;
  --tracking-wide:    0.04em;
  --tracking-wider:   0.1em;
  --tracking-widest:  0.2em;
  --tracking-label:   0.14em;   /* per a labels mono uppercase */
}
```

---

## Pas 5 — Implementar els canvis

Un cop generada la proposta, aplica-la directament als fitxers:

1. **`tailwind.config.ts`** — actualitza `colors` i `fontFamily`
2. **`src/app/globals.css`** — afegeix tokens CSS, actualitza `body` i classes utilitàries
3. **`src/app/layout.tsx`** — actualitza imports de fonts de Google Fonts
4. Si hi ha canvis de noms de colors/classes, fes una cerca global i actualitza els usos

**Ordre d'aplicació:**
```
1. Actualitza tailwind.config.ts (tokens base)
2. Actualitza globals.css (tokens CSS + body styles)
3. Actualitza layout.tsx (font imports)
4. Comprova TypeScript: npx tsc --noEmit
5. Si hi ha classes Tailwind que han canviat de nom, cerca i substitueix
```

---

## Pas 6 — Validar i resumir

Verifica:
- `npx tsc --noEmit` sense errors
- Contrast ràtios dels colors de text calculats
- Tots els fitxers actualitzats coherentment

Genera un resum en format taula:

```
## Canvis aplicats

### Colors
| Token       | Antic      | Nou        | Contrast s/fons |
|-------------|------------|------------|-----------------|
| ink-2       | #64748b    | #8896aa    | 5.1:1 ✅        |
| ...

### Fonts
| Rol     | Antic          | Nou            | Motiu          |
|---------|----------------|----------------|----------------|
| Display | Orbitron       | Space Grotesk  | Més llegible   |
| ...

### Tipus mides
| Ús        | Actual   | Nou      |
|-----------|----------|----------|
| H1        | text-5xl | 4.5rem fluid |
| ...
```

---

## Notes sobre el projecte AMG (context específic)

Aquest projecte usa una estètica **Tech Industrial** amb:
- Fons molt fosc (`#0d0d1a`) — correcte, modern
- Accent taronja (`#FF6B00`) — energètic, distintiu, funciona bé
- Rajdhani + Orbitron + Share Tech Mono — combinació cohesiva però Rajdhani pot semblar datat
- Principals millores potencials:
  - **Body font**: Rajdhani → **Space Grotesk** o **DM Sans** (més modern, millor llegibilitat)
  - **Display**: Orbitron — mantenir (és part de la identitat AMG)
  - **Escala**: definir tokens CSS explícits (ara tot és inline amb `text-[10px]`)
  - **Ink-2**: ja actualitzat a `#8896aa` (contrast correcte)
  - **Variants d'accent**: afegir `accent-hover (#E05A00)` i `accent-subtle (#FF6B00/15)`
