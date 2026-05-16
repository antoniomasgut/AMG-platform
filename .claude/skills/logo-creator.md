---
name: logo-creator
description: "Crea i itera logotips SVG moderns i professionals per a AMG Digitalitzacions. Aplica els tokens de marca (taronja #FF6B00, fons navy #0d0d1a, clip-path angular, Orbitron + Space Grotesk), principis de disseny 2025 i genera sempre 4 variants: icona, wordmark, combinat, mono. S'activa amb: 'crea un logo', 'dissenya una icona de marca', 'millora el logotip', 'logo creator', 'nou concepte de logo', 'brand identity', 'identitat visual', 'icona app', 'favicon'."
---

# Logo Creator — AMG Digitalitzacions

Genera logotips SVG professionals i escalables. No produeix propostes abstractes — sempre acaba amb fitxers SVG concrets a la carpeta `brand/`.

---

## Identitat de marca AMG (tokens fixos)

### Colors
```
Accent principal:  #FF6B00  (taronja vibrant — 100% saturació)
Accent clar:       #FF9A3C  (hover, gradients)
Accent fosc:       #E05A00  (pressed, ombres)

Fons-0 (base):     #0d0d1a  (navy molt fosc)
Fons-1 (cards):    #13132a
Fons-2 (inputs):   #1a1a2e
Fons-3 (borders):  #212140

Text principal:    #e2e8f0
Text secundari:    #94a3b8
Text muted:        #8896aa  (mínim WCAG AA 4.5:1 sobre #0d0d1a)
```

### Tipografia
```
Display/Tech:   Orbitron (700, 800, 900) — per a AMG wordmark i títols
Body/UI:        Space Grotesk (400, 500, 600) — per a subtítols i taglines
Mono/Data:      Share Tech Mono — per a dades, metadades
```

### Estètica i patrons
```
Forma contenidor: clip-path polygon — cantonades tallades en diagonal (no radis)
  Exemple 200×200: polygon(184,4 196,16 196,196 16,196 4,184 4,4)
  
Quadrícula fons: línies #FF6B00 al 4% d'opacitat, cada 30-40px
Pattern:        linearGradient i feGaussianBlur per a efecte "glow" d'accent
Nodes circuit:  quadrats 5-7px a les interseccions de línies (sense border-radius)
```

---

## Anatomia del logo AMG (concepte "A-Delta")

El logotip es basa en la **lletra A geometritzada** (delta digital):
- Representa la "A" d'AMG + el símbol Δ (delta = canvi, transformació digital)
- Dues cames rectes + barra horitzontal = "A" lletrista estilitzada
- Nodes de circuit als vèrtexs = tecnologia, connexió, automatització
- Contenidor clip-path angular = coherència amb el portal

```
        ■  ← node àpex
       / \
      /   \
     /     \
    ■───────■  ← barra (crossbar) a 2/3 de l'alçada des del fons
   /         \
  ■           ■  ← nodes inferiors
```

### Càlcul de la barra horitzontal
Si l'àpex és a (cx, top) i els vèrtexs inferiors a (left, bot) i (right, bot):
```
t = 0.667  (2/3 de la distància de l'àpex al fons)
y_barra = top + t * (bot - top)
x_esq   = cx + t * (left - cx)
x_dret  = cx + t * (right - cx)
```

---

## Variants a generar sempre

| Fitxer | Contingut | Ús |
|--------|-----------|-----|
| `brand/amg-icon.svg` | Icona sola (200×200) sobre fons fosc | Favicon, app icon, avatar |
| `brand/amg-logo.svg` | Icona + wordmark (340×84) fons transparent | Navbar, sobre fons fosc |
| `brand/amg-logo-dark.svg` | Logo complet sobre fons (480×180) | OG image, presentacions |
| `brand/amg-logo-mono.svg` | Versió blanca/monocromàtica | Documents, estampació |

---

## Patrons SVG reutilitzables

### Contenidor card-clip (NN×MM)
```svg
<polygon
  points="N-16,0 N,16 N,M M-16,M 0,M-16 0,0"
  fill="#13132a"
  stroke="#FF6B00"
  stroke-width="0.75"
  stroke-opacity="0.22"
/>
```
On N=amplada, M=alçada, el tall diagonal és de 16px (ajustable).

### Efecte glow (per a línies i nodes principals)
```svg
<filter id="glow" x="-40%" y="-40%" width="180%" height="180%">
  <feGaussianBlur in="SourceGraphic" stdDeviation="2" result="blur"/>
  <feMerge>
    <feMergeNode in="blur"/>
    <feMergeNode in="SourceGraphic"/>
  </feMerge>
</filter>
```
Aplicar amb `filter="url(#glow)"` als elements principals. `stdDeviation` entre 1.5-3.

### Gradient de fons radial
```svg
<radialGradient id="bgGrad" cx="50%" cy="50%" r="50%">
  <stop offset="0%"   stop-color="#1a1a2e"/>
  <stop offset="100%" stop-color="#0d0d1a"/>
</radialGradient>
```

### Gradient de l'interior del triangle (molt subtil)
```svg
<linearGradient id="triGrad" x1="cx" y1="top" x2="cx" y2="bot" gradientUnits="userSpaceOnUse">
  <stop offset="0%"   stop-color="#FF6B00" stop-opacity="0.14"/>
  <stop offset="100%" stop-color="#FF6B00" stop-opacity="0.02"/>
</linearGradient>
```

### Quadrícula de fons (pattern)
```svg
<pattern id="grid" x="0" y="0" width="30" height="30" patternUnits="userSpaceOnUse">
  <path d="M 30 0 L 0 0 0 30" fill="none"
        stroke="#FF6B00" stroke-width="0.5" stroke-opacity="0.04"/>
</pattern>
<rect width="W" height="H" fill="url(#grid)"/>
```

### Node de circuit (quadrat sense arrodoniment)
```svg
<!-- Centrat a (cx, cy) amb mida S -->
<rect x="cx - S/2" y="cy - S/2" width="S" height="S" fill="#FF6B00"/>
<!-- Amb glow per als nodes principals -->
<rect x="cx - S/2" y="cy - S/2" width="S" height="S" fill="#FF6B00" filter="url(#glow)"/>
```

---

## Principis de disseny (2025)

### El que fa un logo tech modern
1. **Geometria pura**: 1-3 formes. Cap ornamentació.
2. **Contrast alt**: accent brillant sobre fons molt fosc (ratio ≥ 4.5:1).
3. **Escalabilitat**: recognoscible a 32px i a 500px.
4. **Monocromàtica possible**: el logo ha de funcionar en blanc pur sobre qualsevol fons.
5. **Tipografia sans-serif geomètrica**: Orbitron/Space Grotesk per a tech.
6. **Paleta mínima**: 2 colors màxim (accent + neutre).

### Errors a evitar
- Gradients complexos a l'icona (no funcionen a mida petita)
- Border-radius en logos tech (trenca l'estètica angular)
- Més de 2 pesos de font en un logo
- Subtext massa llarg (> 18 caràcters) o massa gran (> 1/3 de l'alçada total)
- Text rasteritzat en SVG sense fallback font stack

### Proporcions
```
Icona : Wordmark = 1 : 2.5 (amplada)
"AMG" : "DIGITALITZACIONS" = 3.5× font-size
Separador: 0.75px, 25-30% opacitat
Padding intern contenidor: 15-20% de la mida total
```

---

## Procés d'iteració

Quan el usuari demana canvis o nous conceptes:

1. **Identifica el vector de canvi**: forma, color, tipografia, proporcions, contenidor
2. **Modifica el paràmetre concret** en el SVG, no tot el fitxer
3. **Comprova sempre**:
   - Nodes centrats exactament als vèrtexs/interseccions
   - Barra horitzontal de la A a t≈0.667 de l'alçada
   - Separador alineat amb les cotes verticals del text
   - `viewBox` coherent amb la mida visual real del contingut
4. **Genera i desa** a `brand/` amb nom descriptiu (p. ex. `amg-logo-v2.svg`)
5. **Resumeix els canvis** en una taula: element | valor antic | valor nou

---

## Checklist de qualitat

Abans de donar el logo per acabat:

- [ ] Funciona a 32×32px (favicon)?
- [ ] Funciona en mode mono (blanc sobre negre i viceversa)?
- [ ] Nodes quadrats, no rodons
- [ ] Cap ombra exterior (box-shadow), només filter glow intern
- [ ] Font stack amb fallback: `Orbitron, 'Share Tech Mono', monospace`
- [ ] Tots els `defs` usats (cap filtre o gradient definit però no aplicat)
- [ ] SVG vàlid (sense xmlns mancat, sense paths trencats)
- [ ] `<title>` present per a accessibilitat

---

## Context del projecte

Logo existent a `brand/`:
- `amg-icon.svg` — icona "A-Delta" 200×200 sobre fons fosc
- `amg-logo.svg` — combinat 340×84 fons transparent
- `amg-logo-dark.svg` — combinat 480×180 amb fons (per a OG/previews)
- `amg-logo-mono.svg` — versió blanca monocromàtica

Quan iteris sobre el logo, parteix d'aquests fitxers i guarda les variants com `amg-logo-v2.svg`, `amg-icon-variant-hex.svg`, etc.
