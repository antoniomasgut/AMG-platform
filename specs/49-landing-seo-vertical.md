# Spec 49 — Landing SEO Vertical (Pàgines Pilar + Schema + Fixes Crítics)

**Versió**: 1.0  
**Estat**: Aprovat  
**Mòdul**: 49  
**Depèn de**: Mòdul 13 (i18n + SEO), Mòdul 04 (Engine / Landing Renderer)

---

## 1. Problema

La landing actual d'AMG Digitalitzacions té tres problemes que bloquen el posicionament:

1. **Crític**: `canonical`, `og:url` i `og:image` apunten a `localhost:3000`. Google indexa malament i els compartits a xarxes no mostren imatge.
2. **Estructural**: Tota la web és una sola pàgina (`/`). No hi ha URLs per dolor-tipus, no hi ha contingut indexable per intenció de cerca.
3. **Tècnic**: Manca Schema `LocalBusiness` / `ProfessionalService`, titles sense geolocalització, meta descriptions repetides.

L'objectiu és crear una arquitectura de contingut verticalitzada en tres pàgines pilar que capturin cerca local orgànica per tipus de negoci, alhora que es corregeixen els errors tècnics crítics.

---

## 2. Abast

### 2.1 Inclòs

- Fix `NEXT_PUBLIC_SITE_URL` per eliminar el localhost
- Schema JSON-LD `ProfessionalService` a la home
- Tres pàgines pilar amb URL pròpia (català + castellà):
  - `/[locale]/cita-previa` — sectors amb agenda
  - `/[locale]/pressupostos` — sectors amb pressupostos
  - `/[locale]/despatxos` — sectors professionals / consultes recurrents
- Bloc "Per a qui" a la home linkant les tres pàgines (evitar pàgines orfes)
- Secció "Altres negocis" a la home (conversió, no SEO)
- Titles únics per pàgina amb geolocalització Mallorca
- Meta descriptions úniques per pàgina

### 2.2 Exclòs

- Sub-pàgines per sector (`/cita-previa/fisioterapeutes`) — fase posterior
- Traducció en/de de les pàgines pilar — prioritat català/castellà primer
- CMS de contingut

---

## 3. Fix crític — URL base

### 3.1 Problema

A `next.config.ts` o a les variables d'entorn, `NEXT_PUBLIC_SITE_URL` no està definida a producció. Per tant el codi que construeix canonicals usa `process.env.NEXT_PUBLIC_SITE_URL || 'http://localhost:3000'`.

### 3.2 Solució

**`infra/docker-compose.yml`** — afegir al bloc `args` del build del frontend:

```yaml
args:
  NEXT_PUBLIC_API_URL: https://api.amgdl.com
  NEXT_PUBLIC_SITE_URL: https://amgdl.com
```

I a `environment`:

```yaml
environment:
  NODE_ENV: production
  NEXT_PUBLIC_API_URL: https://api.amgdl.com
  NEXT_PUBLIC_SITE_URL: https://amgdl.com
```

**`frontend/src/app/[locale]/layout.tsx`** (o on es construeixi l'OG):

```typescript
const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? 'https://amgdl.com';
```

**Verificació post-deploy**: `curl -s https://amgdl.com/ca | grep 'og:url'` ha de retornar `https://amgdl.com/ca`, no `localhost:3000`.

---

## 4. Schema JSON-LD — ProfessionalService

Afegir al `<head>` de la home (i pàgines pilar si escau):

```json
{
  "@context": "https://schema.org",
  "@type": "ProfessionalService",
  "name": "AMG Digitalitzacions",
  "description": "Disseny web i automatització amb agent IA per a negocis locals a Mallorca. Webs, WhatsApp automatitzat, reserves i seguiment de clients.",
  "url": "https://amgdl.com",
  "email": "info@amgdl.com",
  "telephone": "+34614492062",
  "priceRange": "€€",
  "areaServed": {
    "@type": "Place",
    "name": "Mallorca"
  },
  "address": {
    "@type": "PostalAddress",
    "addressRegion": "Illes Balears",
    "addressCountry": "ES"
  },
  "sameAs": []
}
```

> **Nota**: `address.addressLocality` i `sameAs` s'ompliran quan es tinguin les URLs de Google Business, Instagram i LinkedIn. El bloc `address` pot ometre's fins llavors — `areaServed` és suficient per a SEO local.

**Implementació**: Component `<SchemaOrg />` al layout de la landing pública (no al portal autenticat).

---

## 5. Meta tags per pàgina

### 5.1 Home (`/[locale]`)

| Camp | Valor |
|------|-------|
| `<title>` | `AMG Digitalitzacions · Webs i agents IA per a negocis locals a Mallorca` |
| `meta description` | `Automatitzem la gestió de cites, pressupostos i consultes per a pimes i autònoms a Mallorca. Web + agent IA + WhatsApp des de 59€/mes.` |
| `og:title` | igual que title |
| `og:description` | igual que meta description |
| `og:url` | `https://amgdl.com/ca` |
| `og:image` | `https://amgdl.com/og-image.jpg` (1200×630) |

### 5.2 `/cita-previa`

| Camp | Valor |
|------|-------|
| `<title>` | `Web i agenda automàtica per a negocis amb cita prèvia · Mallorca \| AMG Digitalitzacions` |
| `meta description` | `Redueix les faltes d'assistència i deixa de gestionar l'agenda a mà. Reserves, recordatoris i confirmacions automàtiques per WhatsApp. Des de 59€/mes.` |

### 5.3 `/pressupostos`

| Camp | Valor |
|------|-------|
| `<title>` | `Seguiment automàtic de pressupostos per a negocis · Mallorca \| AMG Digitalitzacions` |
| `meta description` | `Que cap pressupost es quedi sense resposta. Generació en PDF, seguiment automàtic i registre d'aprovacions per WhatsApp. Des de 59€/mes.` |

### 5.4 `/despatxos`

| Camp | Valor |
|------|-------|
| `<title>` | `Agent IA per a despatxos i serveis professionals · Mallorca \| AMG Digitalitzacions` |
| `meta description` | `Filtra consultes repetitives i recupera el teu temps. L'agent respon dubtes, qualifica contactes i et passa només el que importa. Des de 59€/mes.` |

---

## 6. Pàgines pilar — estructura de component compartida

Les tres pàgines comparteixen el mateix layout. Component: `VerticalLandingPage` (o page individual per a més flexibilitat SEO).

### 6.1 Estructura de cada pàgina pilar

```
1. Hero
   - H1 (dolor principal)
   - Subtítol (sectors que representa)
   - CTA primari: "Consulta gratuïta" → WhatsApp tel:+34614492062
   - CTA secundari: "Veure com funciona" → ancora #com-funciona

2. Bloc "El problema" (3 punts de dolor, icones)

3. Bloc "Com t'ho resolem" (4 blocs solució, cadascun amb títol + descripció + 3 bullet points)

4. Bloc "Per a qui" (llista de sectors representatius)

5. FAQ (4 preguntes, acordió)

6. Testimoni (1 testimoni real)

7. CTA final
   - Títol
   - "Consulta gratuïta i sense compromís. Pressupost en 24h."
   - Botó: "Consulta per WhatsApp" → wa.me/34614492062
```

---

## 7. Contingut — `/cita-previa`

**URL**: `/[locale]/cita-previa`  
**Sectors representats**: fisioterapeutes, clíniques dentals, psicòlegs, centres d'estètica i depilació, perruqueries, veterinaris, nutricionistes, podòlegs.

### H1
`Menys faltes d'assistència. Una agenda que es gestiona sola.`

### Subtítol
`Per a fisioterapeutes, clíniques, centres d'estètica, veterinaris i qualsevol negoci que treballi amb cita prèvia. Reserves, recordatoris i confirmacions automàtiques per WhatsApp — sense que hagis de respondre cada missatge.`

### Bloc dolor ("El problema")

**Titular**: `CADA HORA BUIDA ÉS DINER QUE NO TORNA.`

| Icona | Títol | Descripció |
|-------|-------|------------|
| 📵 | Faltes d'assistència | El pacient que no apareix deixa un buit que ja no recuperes. |
| 💬 | Agenda gestionada a mà | Hores cada dia confirmant cites per WhatsApp entre client i client. |
| 🔁 | Clients que no tornen | Acaben el tractament i desapareixen, i ningú fa el seguiment. |

### Blocs solució ("Com t'ho resolem")

| Bloc | Títol | Descripció | Bullets |
|------|-------|------------|---------|
| 1 | Recordatoris automàtics | L'agent envia un recordatori el dia abans i demana confirmació. Si el client no pot venir, l'hora queda lliure per a un altre. | Menys buits a l'agenda · Confirmació automàtica · Reprogramació senzilla |
| 2 | Reserves per WhatsApp 24/7 | Els clients demanen hora sols, a qualsevol hora, sense que hagis de contestar. L'agenda s'actualitza sola. | Reserves fora d'horari · Sense trucades perdudes · Disponibilitat sempre al dia |
| 3 | Reactivació de clients | L'agent detecta qui fa temps que no ve i li proposa tornar, automàticament. | Recuperació d'inactius · Seguiment postractament · Més visites recurrents |
| 4 | Ressenyes automàtiques | Després de cada visita, l'agent demana una ressenya a Google. Més valoracions, més clients nous. | Sol·licitud automàtica · Millor reputació · Més captació per Google |

### FAQ

| Pregunta | Resposta |
|----------|----------|
| Es connecta amb el meu sistema d'agenda actual? | Sí. Treballem amb les eines que ja fas servir o et muntem una de nova si ho prefereixes. |
| El client nota que parla amb un bot? | L'agent respon de manera natural, i tu pots entrar a la conversa en qualsevol moment al mateix fil. |
| Quant triga a estar en marxa? | La web es publica en 48h i l'agent es configura per fases segons les teves necessitats. |
| Què passa si vull canviar alguna cosa? | El sistema evoluciona amb tu: implementem ajustos i millores quan els necessites, dins de la quota. |

### Testimoni
Clínica Rosselló (testimoni real — text a definir pel client).

### CTA final
`Parlem de la teva agenda. Consulta gratuïta i sense compromís. Pressupost en 24h.`

---

## 8. Contingut — `/pressupostos`

**URL**: `/[locale]/pressupostos`  
**Sectors representats**: reformes i construcció, instal·ladors (llum, aigua, clima), arquitectes i aparelladors, fotògrafs i caterings, tallers, empreses de mudances i serveis a mida.

### H1
`Que cap pressupost es quedi sense resposta.`

### Subtítol
`Per a reformes, instal·ladors, arquitectes, fotògrafs i qualsevol negoci que treballi amb pressupostos a mida. L'agent els genera, en fa el seguiment i registra les respostes — perquè no perdis feines per no trucar a temps.`

### Bloc dolor ("El problema")

**Titular**: `UN PRESSUPOST SENSE SEGUIMENT ÉS UNA FEINA PERDUDA.`

| Icona | Títol | Descripció |
|-------|-------|------------|
| 📤 | Pressupostos que s'obliden | L'envies, passen els dies i ningú fa el seguiment. |
| 🤷 | No saps qui està interessat | No tens manera de saber qui l'ha obert ni qui dubta. |
| ⏱️ | Hores fent pressupostos a mà | Temps que treus de la feina real per redactar i enviar documents. |

### Blocs solució ("Com t'ho resolem")

| Bloc | Títol | Descripció | Bullets |
|------|-------|------------|---------|
| 1 | Pressupostos en PDF automàtics | L'agent genera el pressupost en PDF amb el teu format i l'envia al client al moment, per WhatsApp o email. | Generació automàtica · El teu disseny · Enviament immediat |
| 2 | Seguiment de no contestats | Si el client no respon, l'agent fa el seguiment sol als dies que decideixis, amb el missatge adequat cada vegada. | Recordatoris programats · Sense insistir tu · Més pressupostos tancats |
| 3 | Registre de respostes i aprovacions | Saps en tot moment qui ha acceptat, qui dubta i qui ha dit que no. Tot registrat automàticament. | Estat de cada pressupost · Aprovacions registrades · Visió clara del que tens obert |
| 4 | Reactivació de clients | L'agent recupera clients antics i pressupostos que es van quedar a mitges, automàticament. | Recuperació d'oportunitats · Seguiment de feines pendents · Més conversió |

### FAQ

| Pregunta | Resposta |
|----------|----------|
| Puc fer servir el meu format de pressupost? | Sí. L'agent genera els PDF amb el teu disseny i les teves dades. |
| Cada quan fa el seguiment? | El decideixes tu: per exemple als 2 dies i als 5. L'agent ho fa sol amb el to que vulguis. |
| Es queda el client amb sensació de "robot insistent"? | No. Els missatges són naturals i pots aturar o entrar al fil quan vulguis. |
| Quant triga a estar en marxa? | La web es publica en 48h i el sistema de pressupostos es configura per fases. |

### Testimoni
Fullana Reformes (testimoni real — text a definir pel client).

### CTA final
`Parlem dels teus pressupostos. Consulta gratuïta i sense compromís. Pressupost en 24h.`

---

## 9. Contingut — `/despatxos`

**URL**: `/[locale]/despatxos`  
**Sectors representats**: assessories i gestories, despatxos d'advocats, consultories, agències, graduats socials, qualsevol servei professional amb consultes recurrents.

### H1
`Filtra les consultes repetitives i recupera el teu temps.`

### Subtítol
`Per a assessories, despatxos d'advocats, consultories i serveis professionals. L'agent respon els dubtes habituals, qualifica els contactes i et passa només les consultes que de veritat necessiten la teva atenció.`

### Bloc dolor ("El problema")

**Titular**: `EL TEMPS QUE PERDS RESPONENT EL MATEIX UNA I ALTRA VEGADA.`

| Icona | Títol | Descripció |
|-------|-------|------------|
| 🔁 | Les mateixes preguntes cada dia | Documents, terminis, "què necessito per a...". Sempre el mateix. |
| ☎️ | Trucades que no van enlloc | Gent que pregunta coses que no són el teu àmbit o que no contractarà. |
| 📥 | Consultes que es perden | Missatges que arriben fora d'horari i quan els veus ja és tard. |

### Blocs solució ("Com t'ho resolem")

| Bloc | Títol | Descripció | Bullets |
|------|-------|------------|---------|
| 1 | Respostes automàtiques a les FAQs | L'agent respon les preguntes habituals 24/7: documentació, terminis, horaris, preus. Tu deixes de repetir-te. | Disponible sempre · Respostes consistents · Menys interrupcions |
| 2 | Qualificació de contactes | Abans de passar-te ningú, l'agent recull la informació clau i descarta el que no encaixa. Reps només el que val la pena. | Filtratge previ · Contactes preparats · Menys trucades perdudes |
| 3 | Recollida de documentació | L'agent demana i recull els documents que necessites de cada client, de manera ordenada i automàtica. | Sol·licitud automàtica · Tot recollit i ordenat · Menys anar i tornar |
| 4 | Seguiment i recordatoris | Terminis, renovacions, venciments. L'agent avisa el client (i a tu) abans que sigui tard. | Avisos de venciment · Recordatoris automàtics · Res se't passa |

### FAQ

| Pregunta | Resposta |
|----------|----------|
| L'agent pot donar informació sensible o legal? | No. Respon dubtes generals i recull informació; les qüestions de fons sempre passen per tu. |
| Es connecta amb les meves eines de gestió? | Treballem amb el que ja fas servir sempre que sigui possible, o et proposem alternatives. |
| El client sap quan parla amb una persona? | Sí, i tu pots entrar al fil en qualsevol moment. Mode híbrid bot + persona. |
| Quant triga a estar en marxa? | La web es publica en 48h i l'agent es configura per fases segons el teu despatx. |

### Testimoni
Aiguabella Assessors (testimoni real — text a definir pel client).

### CTA final
`Parlem del teu despatx. Consulta gratuïta i sense compromís. Pressupost en 24h.`

---

## 10. Modificacions a la home (`/`)

### 10.1 Bloc "Per a qui" (nou)

S'afegeix una secció a la home per evitar que les pàgines pilar quedin orfes. Estructura:

```
Títol: "Per a quin negoci ets?"

[Card: Cita prèvia]            [Card: Pressupostos]         [Card: Despatxos]
Fisios · Clíniques             Reformes · Instal·ladors     Assessories · Advocats
Estètica · Veterinaris         Arquitectes · Fotògrafs      Consultories · Gestories
→ Veure solució                → Veure solució              → Veure solució
```

Posicionament: entre la secció "Serveis" i la secció "Com treballem".

### 10.2 Secció "Altres negocis" (nova, a la home)

Secció de conversió (no SEO) per capturar sectors no coberts per les tres pàgines pilar:

```
Títol: "El teu negoci no és aquí?"
Subtítol: "Treballem amb qualsevol sector. Si tens un procés repetitiu —
           cites, pressupostos, consultes, factures— l'automatitzem."
CTA: "Explica'ns el teu cas" → WhatsApp
```

Sectors exemple (llista visual): Farmàcies · Hostals · Immobiliàries · Escoles · Autoescoles · Tintoreries · Electricistes · Jardins...

### 10.3 Revisió de "8+ Pimes actives"

Substituir o ocultar el comptador fins que el volum justifiqui el claim. Alternativa: mostrar sectors actius en lloc de nombre (ex. "Clíniques, assessories i reformes a Mallorca").

---

## 11. Implementació tècnica

### 11.1 Fitxers a crear/modificar

| Fitxer | Acció | Descripció |
|--------|-------|------------|
| `infra/docker-compose.yml` | Modificar | Afegir `NEXT_PUBLIC_SITE_URL` |
| `frontend/src/components/landing/SchemaOrg.tsx` | Crear | JSON-LD `ProfessionalService` |
| `frontend/src/app/[locale]/layout.tsx` | Modificar | Usar `NEXT_PUBLIC_SITE_URL` per a canonicals/OG |
| `frontend/src/app/[locale]/cita-previa/page.tsx` | Crear | Pàgina pilar cita prèvia |
| `frontend/src/app/[locale]/pressupostos/page.tsx` | Crear | Pàgina pilar pressupostos |
| `frontend/src/app/[locale]/despatxos/page.tsx` | Crear | Pàgina pilar despatxos |
| `frontend/src/components/landing/VerticalLandingPage.tsx` | Crear | Component compartit pàgines pilar |
| `frontend/src/components/landing/PerAQuiSection.tsx` | Crear | Secció "Per a qui" per a la home |
| `frontend/src/app/[locale]/page.tsx` | Modificar | Afegir bloc "Per a qui" i "Altres negocis" |
| `frontend/src/messages/ca.json` | Modificar | Traduccions pàgines pilar |
| `frontend/src/messages/es.json` | Modificar | Traduccions pàgines pilar (castellà) |
| `frontend/src/messages/en.json` | Modificar | Títols i CTAs (anglès) |
| `frontend/src/messages/de.json` | Modificar | Títols i CTAs (alemany) |
| `frontend/src/app/sitemap.ts` | Modificar | Afegir les 3 URLs noves al sitemap |

### 11.2 Prioritat d'implementació

```
FASE A (crític, aquesta setmana):
  1. Fix NEXT_PUBLIC_SITE_URL → canònic/OG corregit
  2. Schema JSON-LD ProfessionalService
  3. Títols únics amb "Mallorca" per home + 3 pàgines pilar

FASE B (SEO estructural):
  4. Crear les 3 pàgines pilar (/cita-previa, /pressupostos, /despatxos)
  5. Bloc "Per a qui" a la home (links a les pàgines pilar)
  6. Actualitzar sitemap.ts

FASE C (conversió):
  7. Secció "Altres negocis" a la home
  8. Revisió claim "8+ Pimes actives"
```

### 11.3 Verificacions post-deploy

```bash
# Canonical OK
curl -s https://amgdl.com/ca | grep 'og:url'
# → content="https://amgdl.com/ca"

# Schema OK
curl -s https://amgdl.com/ca | python3 -c "import sys,json,re; m=re.search(r'<script type=\"application/ld\+json\">(.*?)</script>', sys.stdin.read(), re.DOTALL); print(json.loads(m.group(1))['telephone'] if m else 'NOT FOUND')"
# → +34614492062

# Pàgines pilar accessibles
curl -o /dev/null -s -w "%{http_code}" https://amgdl.com/ca/cita-previa
# → 200

# Sitemap actualitzat
curl -s https://amgdl.com/sitemap.xml | grep 'cita-previa'
# → <loc>https://amgdl.com/ca/cita-previa</loc>
```

---

## 12. SEO en IA — Optimització per LLMs (GEO)

Més enllà del SEO tradicional, els motors d'IA (ChatGPT, Claude, Perplexity, Gemini) cada cop responen preguntes comercials directament. Pautes per aparèixer en respostes de LLMs:

### 12.1 Redacció orientada a preguntes concretes

Les pàgines pilar han d'incloure preguntes de la forma exacta que fa la gent al buscador/IA:

- "com gestionar cites d'un fisioterapeuta amb WhatsApp"
- "programa per enviar recordatoris de cita automàtics"
- "com fer seguiment de pressupostos de reforma"
- "agent IA per a gestories i assessories Mallorca"

Incloure-les naturalment al cos del text, no només al FAQ.

### 12.2 Dades factuals cites

Incloure a la pàgina xifres i afirmacions verificables:

- "Reducció mitjana de no-shows del 40% amb recordatoris automàtics"
- "Estalvi de 2-3 hores setmanals en confirmació de cites"
- Testimonials reals amb nom i sector

### 12.3 Perfil Google Business

Crear/reclamar fitxa a Google Business Profile amb:
- Categories: "Servei de màrqueting digital" + "Servei de consultoria empresarial"
- Zona de servei: Mallorca
- Telèfon: +34 614 492 062
- Web: https://amgdl.com

La fitxa de Google Business pesa tant com el Schema per a cerca local.

### 12.4 Mentions externes (link building)

Per aparèixer en respostes d'IA cal que fonts externes mencionin AMG Digitalitzacions:
- Col·legis professionals locals (COAIB, Col·legi de Fisioterapeutes IB)
- Directoris de serveis tecnològics a Balears
- Articles de blog a la pròpia web (ex. "Com un fisioterapeuta pot reduir els no-shows")

---

## 13. Dades de contacte (referència)

| Canal | Valor |
|-------|-------|
| WhatsApp | +34 614 492 062 |
| Trucades | +34 614 492 062 |
| Email | info@amgdl.com |
| Web | https://amgdl.com |
| WhatsApp link CTA | `https://wa.me/34614492062` |
