# Mòdul 16: Onboarding — Guia de primers passos per a nous clients

> **Versió:** 2.0
> **Data:** 2026-05-15
> **Autor:** [per determinar]
> **Dependències:** Mòdul 01 (Auth), Mòdul 02 (Vault), Mòdul 04 (Engine), Mòdul 07 (Billing), Mòdul 10 (Automations)
>
> **Canvis v2.0:**
> - Afegit concepte de dual-layer onboarding (Vault setup + dashboard)
> - Afegida gestió d'estats transicionals (refetch, reactivació)
> - Afegit disseny responsive mòbil
> - Afegits estats d'error i càrrega específics per onboarding
> - Afegit flux per a ADMIN operant en nom del client
> - Afegits tests d'integració amb dades reals via Playwright
> - Actualitzat layout responsive per a mòbil

---

## 1. Objectius

- Detectar automàticament quan un client accedeix al portal per primera vegada o sense dades.
- Substituir el dashboard buit per una guia interactiva de primers passos.
- Guiar el client fins a tenir almenys una landing publicada, una automatització activa i un pressupost creat.
- Connectar aquest onboarding superficial amb el **Vault guided setup** (configuració real de serveis, credencials i fases).
- Reduir la fricció d'adopció i el temps fins al primer valor percebut (time-to-value).
- Frontend-only: no requereix nous endpoints de backend.

---

## 2. Abast

### 2.1 Funcionalitats incloses

- Detecció d'estat buit (0 landings, 0 workflows, 0 pressupostos) amb refetch automàtic.
- Component `OnboardingGuide` que substitueix el dashboard quan es detecta estat buit.
- Indicador de progrés visual (X / 3 passos completats).
- 3 passos ordenats: crear landing → activar automatització → crear pressupost.
- Cada pas: descripció, CTA directe a la secció corresponent, i estat (pendent / fet).
- Botó "Salta l'onboarding" per a clients avançats.
- Persistència de "skip" via `localStorage` amb clau per tenant.
- Pantalla de felicitació amb animació CSS (`OnboardingComplete`).
- Responsive: 3 columnes en escriptori, 1 columna en mòbil (stack vertical).
- Accessible: focus correcte, landmarks ARIA, compatible amb `prefers-reduced-motion`.
- Estats transicionals: si l'usuari esborra l'única landing, el pas torna a "pendent" al següent refetch.
- Detecció de xarxa: si les dades fallen, no es mostra onboarding ni dashboard buit — es mostra error banner.

### 2.2 Funcionalitats excloses

- Nous endpoints de backend (usa les dades ja carregades al dashboard + Vault).
- Onboarding interactiu pas a pas amb overlays i tooltips (product tour).
- Enviament d'email de benvinguda (gestionat fora de la plataforma).
- Videos tutorials integrats.
- Personalització per pla de subscripció.
- **Gestió de dependències entre serveis** (ex: WhatsApp necessita web → landing com a prerequisit). Això es gestiona al Vault, no a l'onboarding.

### 2.3 Actors

| Actor | Descripció | Comportament |
|-------|-----------|--------------|
| CLIENT | Usuari final del tenant | Veu l'onboarding si no té dades. Pot saltar-lo. Un cop completat, veu dashboard normal. |
| ADMIN | Personal operatiu que configura el tenant d'un client | Veu l'onboarding si el tenant no té dades. Pot saltar-lo. El localStorage usa `tenantId`, així que l'estat és independent per cada tenant que gestiona. |
| SUPER_ADMIN | Propietari de la plataforma | Igual que ADMIN. Rarament veurà l'onboarding perquè no té tenant propi. |

---

## 3. Principi fonamental: els passos són dinàmics segons el Vault

**Les landings, automatitzacions i pressupostos NO són obligatoris per a tots els tenants.** Cada tenant té un perfil assignat (via Vault) que determina quins serveis ha contractat. L'onboarding ha de reflectir això:

- Si un tenant **té el servei de landing** al seu perfil → es mostra el pas de landing
- Si un tenant **NO té el servei de landing** → el pas NO es mostra (no és aplicable)
- Un pas només es considera "pendent" si el servei està **assignat però el recurs no existeix**
- Si cap servei està assignat, o tots els passos aplicables estan complets, l'onboarding no es mostra

### Lògica de construcció de passos

```typescript
interface OnboardingStepDef {
  id: 'landing' | 'automation' | 'billing';
  title: string;
  description: string;
  href: string;
  serviceType: string;        // tipus de servei al Vault (ex: 'LANDING', 'AUTOMATION', 'BILLING')
  resourceCount: number;      // landings.length, workflows.length, invoices.length
}

function buildSteps(
  services: CatalogService[],          // del Vault (Mòdul 02)
  landings: LandingSummary[],
  workflows: TenantWorkflow[],
  invoices: Invoice[]
): OnboardingStepDef[] {
  const assignedTypes = new Set(
    services.filter(s => !s.isAddon).map(s => s.type)
  );

  const steps: OnboardingStepDef[] = [];

  if (assignedTypes.has('LANDING')) {
    steps.push({ id: 'landing', title: ..., href: ..., resourceCount: landings.length, serviceType: 'LANDING' });
  }
  if (assignedTypes.has('AUTOMATION')) {
    steps.push({ id: 'automation', title: ..., href: ..., resourceCount: workflows.length, serviceType: 'AUTOMATION' });
  }
  if (assignedTypes.has('BILLING')) {
    steps.push({ id: 'billing', title: ..., href: ..., resourceCount: invoices.length, serviceType: 'BILLING' });
  }

  return steps;
}
```

### Relació entre layers

```
Vault (perfil del tenant)          Layer 1 (Dashboard)                  Layer 2 (Setup guiat)
┌────────────────────────┐        ┌─────────────────────┐             ┌──────────────────────┐
│ Serveis assignats:     │        │ Passos dinàmics:    │             │ Fases del perfil:    │
│                        │        │                     │             │                      │
│ LANDING    ✅         │───────▶│ ✓ Pas Landing       │◄────────────│ Fase Landing         │
│ AUTOMATION ✅         │───────▶│ ✓ Pas Automatització│◄────────────│ Fase Automatitzacions │
│ WHATSAPP   ✅         │        │ (no surt al dashboard)            │ Fase Comms           │
│ SMTP       ✅         │        │                     │             │                      │
│ BILLING    ❌         │        │ (pas no es mostra)  │             │ (fase no existeix)   │
└────────────────────────┘        └─────────────────────┘             └──────────────────────┘
```

**Conseqüències:**
- La barra de progrés mostra `X / Y` on `Y` és el nombre de passos **aplicables** (no sempre 3)
- Si un tenant només té LANDING + AUTOMATION, la barra serà `0 / 2` o `2 / 2`
- Si un tenant no té cap servei assignat, l'onboarding no es mostra (dashboard buit normal)
- L'onboarding es pot donar per complet encara que hi hagi passos que no s'han completat mai (perquè no són aplicables)
- Un pas marcat com "fet" ho queda fins que el recurs esborri l'últim element (refetch ho detecta)

El Layer 1 es mostra sempre. El Layer 2 es mostra com a secció addicional al dashboard o a la pàgina del tenant quan hi ha serveis pendents de configurar.

---

## 4. Lògica de detecció d'estat buit

### 4.1 Condició d'activació

L'onboarding es mostra si:
1. No s'ha saltat (localStorage)
2. Les dades estan carregades (isSuccess)
3. Hi ha **algun pas aplicable pendent** (servei assignat al Vault però recurs no creat)

```typescript
// Construir passos dinàmics basats en els serveis del Vault
const steps = buildSteps(assignedServices, landings, workflows, invoices);
const pendingSteps = steps.filter(s => s.resourceCount === 0);

const showOnboarding =
  !onboardingSkipped &&
  dataLoaded &&
  steps.length > 0 &&           // hi ha serveis assignats al Vault
  pendingSteps.length > 0;      // almenys un servei assignat no té recurs creat
```

**Casos especials:**

| Situació | Passos | Progrés | Què es mostra? |
|----------|--------|---------|----------------|
| Tenant acabat de crear, servei LANDING assignat | 1 pas (landing) | 0/1 | Onboarding amb 1 pas |
| Tenant amb serveis LANDING + AUTOMATION | 2 passos | 0/2 o 1/2 | Onboarding amb 2 passos |
| Tenant amb servei LANDING, ja té landing | 1 pas | 1/1 | Dashboard normal (tot complet) |
| Tenant sense cap servei assignat | 0 passos | — | Dashboard normal (no onboarding) |
| Tenant amb serveis, tots complets | Y passos | Y/Y | Dashboard normal (tot complet) |

### 4.2 Persistència de skip

**`localStorage` key:** `amg_onboarding_skipped_<tenantId>` — inclou `tenantId` per evitar conflictes quan un admin opera múltiples tenants. El skip és permanent per aquell tenant: un cop saltat, ja no es mostra encara que després s'afegixin nous serveis al Vault.

### 4.3 Estats transicionals

| Acció de l'usuari | Efecte a l'onboarding |
|-------------------|----------------------|
| Crea una landing (si té servei LANDING) | Refetch → pas landing marcat "fet" |
| Esborra l'única landing | Refetch → pas landing torna a "pendent" |
| Crea workflow (si té servei AUTOMATION) | Refetch → pas automation marcat "fet" |
| Desactiva l'únic workflow | Refetch → pas automation torna a "pendent" |
| Crea invoice (si té servei de facturació) | Refetch → pas billing marcat "fet" |
| Es completen TOTS els passos aplicables | Automàticament: es mostra `OnboardingComplete` |
| Es completen ALGUNS passos (no tots) | Barra de progrés s'actualitza, passos fets en verd |
| S'afegeix un nou servei al Vault | Al següent refetch, apareix un nou pas (si no s'ha saltat l'onboarding) |
| Fa clic a "Salta" | localStorage + dashboard normal |

**Refetch automàtic:** Les dades del dashboard fan refetch cada 30 segons (React Query `refetchInterval: 30000`) mentre l'onboarding està actiu, per detectar canvis sense recàrrega de pàgina. Un cop l'onboarding es completa o es salta, el refetchInterval torna al valor normal (o es desactiva).

### 4.3 Error de xarxa

Si les 4 crides del dashboard fallen:
```typescript
if (isError) {
  // No mostrar onboarding ni dashboard buit
  // Mostrar error banner amb botó de reintentar
  return <DashboardError onRetry={refetch} />;
}
```

Si només fallen algunes crides (ex: landings OK, workflows error, invoices OK):
```typescript
const partialData = landings !== undefined && invoices !== undefined;
if (partialData && landings.length === 0 && invoices.length === 0) {
  // Mostrar onboarding però amb una nota: "No s'han pogut carregar automatitzacions"
  // Marcar pas 2 com a "indeterminat" (icona de rellotge, no check ni cercle buit)
}
```

### 4.4 Detecció de Vault pendent

A més dels 3 passos, si `getTenantSetup(tenantId)` retorna perfils amb fases no aprovades, es mostra una secció addicional:
```
┌─────────────────────────────────────┐
│ ⚠ Configuració pendent             │
│ El tenant té fases per configurar.  │
│ [Anar a configuració →]             │
└─────────────────────────────────────┘
```

Això connecta Layer 1 amb Layer 2.

---

## 5. Rutes del frontend

No es creen noves rutes. El component s'integra dins del dashboard existent:

```
/portal          → portal/page.tsx (ja existent)
                    └── si isLoading: <DashboardSkeleton />
                    └── si isError: <DashboardError onRetry={refetch} />
                    └── si showOnboarding: <OnboardingGuide />
                    └── si no: dashboard normal (targetes, taules, etc.)
```

---

## 6. Components del frontend

### 6.1 `OnboardingGuide` (`src/components/portal/OnboardingGuide.tsx`)

Component principal que substitueix el dashboard quan l'estat és buit.

**Props:**
```typescript
interface OnboardingGuideProps {
  tenantId: string;
  userName: string;
  assignedServices: CatalogService[];    // serveis del Vault assignats al tenant
  landings: LandingSummary[];            // llista completa (per comptar)
  workflows: TenantWorkflow[];
  invoices: Invoice[];
  onSkip: () => void;
  onComplete: () => void;               // es crida quan tots els passos aplicables estan fets
}
```

**Lògica interna:**
- `buildSteps(assignedServices, landings, workflows, invoices)` — genera només els passos per serveis assignats
- `completedCount = steps.filter(s => s.resourceCount > 0).length`
- `totalCount = steps.length` (no és sempre 3)
- Si `completedCount === totalCount`, mostra `OnboardingComplete`
- La barra de progrés mostra `completedCount / totalCount`

**Estats que pot renderitzar:**
1. **Normal**: Welcome + progress bar + 3 step cards
2. **Completat**: Pantalla de felicitació (quan completedCount === 3)
3. **Amb vault pendent**: Welcome + progress bar + 3 step cards + alerta vault

**Props d'animació:**
- Transició suau entre els passos i la pantalla de completat
- `prefers-reduced-motion` respectat: si actiu, `transition: none`

### 6.2 `OnboardingStep` (`src/components/portal/OnboardingStep.tsx`)

Targeta individual per a cada pas.

**Props:**
```typescript
interface OnboardingStepProps {
  step: number;
  title: string;
  description: string;
  href: string;
  done: boolean;
  indeterminate?: boolean;     // true si la dada no s'ha pogut carregar
}
```

**Variants visuals:**

| Estat | Aparença | aria-label |
|-------|---------|------------|
| `done=true` | Borde `border-emerald-500/60`, fons `bg-emerald-900/10`, check verd ✓ | "Pas {N}: {títol} — completat" |
| `done=false, indeterminate=false` | Borde `border-amber-500/40`, fons `bg-[#1a1a2e]`, cercle buit ○ | "Pas {N}: {títol} — pendent" |
| `indeterminate=true` | Borde `border-slate-500/30`, fons `bg-[#1a1a2e]`, rellotge ⟳ | "Pas {N}: {títol} — no disponible" |

**CTA Link:**
- Si `done=false`: enllaç amb classe `btn-clip bg-accent hover:bg-accent-light`
- Si `done=true`: text verd "Fet!" sense enllaç
- Si `indeterminate=true`: enllaç deshabilitat amb indicador de error

**Responsive:**
- Desktop: `col-span-1` (3 columnes)
- Mobile (< 768px): `col-span-full` (stack vertical), les targetes ocupen ample complet

**Animacions:**
- Quan un pas passa de `done=false` a `done=true`: fons verd amb transició CSS `transition-all duration-500`
- Reduit si `prefers-reduced-motion: reduce`

### 6.3 `OnboardingComplete` (`src/components/portal/OnboardingComplete.tsx`)

Pantalla de felicitació quan els 3 passos estan complets.

**Props:**
```typescript
interface OnboardingCompleteProps {
  userName: string;
  onGoToDashboard: () => void;
}
```

**Layout:**
```
┌────────────────────────────────────┐
│                                    │
│          🎉 (o check gegant)       │
│                                    │
│    Estàs a punt, [Nom]!            │
│    Ja tens tot configurat.         │
│                                    │
│    [Anar al dashboard →]            │
│                                    │
└────────────────────────────────────┘
```

**Animacions:**
- Checkmark amb animació de dibuix (SVG stroke-dasharray)
- Text amb fade-in
- `prefers-reduced-motion: reduce → animation: none`

### 6.4 `DashboardSkeleton` (ja existent)

Mentre les dades del dashboard encara s'estan carregant (React Query `isLoading`), es mostra l'skeleton existent. No s'activa l'onboarding fins que `isSuccess` és true i les dades són conegudes.

### 6.5 `DashboardError` (ja existent o nou)

Si les dades fallen (error de xarxa):
```
┌────────────────────────────────────┐
│ ⚠ Error de connexió               │
│ No s'han pogut carregar les dades  │
│                                    │
│ [Reintentar]                       │
└────────────────────────────────────┘
```

### 6.6 Modificació de `portal/page.tsx`

Lògica completa al dashboard:

```typescript
// Estat de skip
const [onboardingSkipped, setOnboardingSkipped] = useState(() =>
  typeof window !== 'undefined'
    ? localStorage.getItem(`amg_onboarding_skipped_${tenantId}`) === 'true'
    : false
);
const [onboardingComplete, setOnboardingComplete] = useState(false);

// Refetch mentre onboarding estigui actiu
const refetchInterval = (!onboardingSkipped && !onboardingComplete) ? 30000 : false;

// Queries amb refetchInterval condicional
const { data: landings, isLoading, isError, refetch } = useQuery({ ...queryFn, refetchInterval });

// Nova query: serveis del Vault assignats al tenant (per determinar quins passos mostrar)
const { data: catalogServices } = useQuery({
  queryKey: ['catalog-services'],
  queryFn: () => listCatalogServices(),
});
const assignedServices = (catalogServices ?? []).filter(s => !s.isAddon);

// Construir passos dinàmics
const steps = buildSteps(assignedServices, landings ?? [], workflows ?? [], invoices ?? []);
const pendingSteps = steps.filter(s => s.resourceCount === 0);
const showOnboarding = !onboardingSkipped && !isLoading && !isError && steps.length > 0 && pendingSteps.length > 0;

// Handle events
const handleSkip = () => {
  localStorage.setItem(`amg_onboarding_skipped_${tenantId}`, 'true');
  setOnboardingSkipped(true);
};
const handleComplete = () => {
  setOnboardingComplete(true);
};

// Render
if (isLoading) return <DashboardSkeleton />;
if (isError) return <DashboardError onRetry={refetch} />;
if (showOnboarding) {
  return <OnboardingGuide
    tenantId={tenantId}
    userName={userName}
    assignedServices={assignedServices}
    landings={landings ?? []}
    workflows={workflows ?? []}
    invoices={invoices ?? []}
    onSkip={handleSkip}
    onComplete={handleComplete}
  />;
}
return <DashboardWidgets ... />;
```

---

## 7. Definició dels passos (dinàmics)

Els passos es construeixen dinàmicament a partir dels **serveis assignats al tenant** (Vault). Cada servei no-addon es mapeja a un pas d'onboarding:

| Servei Vault (type) | Pas | Títol | Descripció | Ruta CTA | Condició "Fet" |
|--------------------|-----|-------|-----------|----------|----------------|
| `LANDING` | landing | Crea la teva primera landing | Publica la teva web en menys de 5 minuts amb l'editor visual | `/[locale]/portal/landings/new` | `landings.length > 0` |
| `AUTOMATION` | automation | Connecta una automatització | Connecta n8n per enviar emails, WhatsApp o rebre notificacions | `/[locale]/portal/automations` | `workflows.length > 0` |
| `BILLING` | billing | Genera el teu primer pressupost | Crea un pressupost personalitzat i envia'l al client en PDF | `/[locale]/portal/billing` | `invoices.length > 0` |

Altres tipus de servei (`WHATSAPP`, `SMTP`, `BOT_IA`, etc.) NO generen un pas d'onboarding al dashboard. Pertanyen al Layer 2 (Vault guided setup).

**Exemples de passos generats:**

| Tenant | Serveis Vault | Passos onboarding | Progrés màxim |
|--------|--------------|-------------------|---------------|
| Restaurant Can Pedro | LANDING, WHATSAPP | [landing] | 0/1 → 1/1 |
| Advocats Mallorca | LANDING, AUTOMATION, BILLING | [landing, automation, billing] | 0/3 → 3/3 |
| Botiga Maria | AUTOMATION, WHATSAPP, SMTP | [automation] | 0/1 → 1/1 |
| Gestoria Pau | (cap servei assignat) | [] | No onboarding |

---

## 8. UX i disseny

### 8.1 Layout escriptori (≥ 768px)

```
┌─────────────────────────────────────────────────────────────┐
│  Benvingut, [Nom]! Comencem.                                │
│  Completa els 3 passos per treure el màxim profit del portal│
│                                                             │
│  ████████░░░░░░░░░░░░  1 / 3 passos completats             │
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ ① Landing   │  │ ② Automat.  │  │ ③ Pressup.  │         │
│  │ [desc]      │  │ [desc]      │  │ [desc]      │         │
│  │ [Comença →] │  │ [Comença →] │  │ [Comença →] │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│                                                             │
│  [Salta l'onboarding]                                       │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 Layout mòbil (< 768px)

```
┌──────────────────────────────┐
│ Benvingut, [Nom]! Comencem. │
│                              │
│ ████████░░░░  1 / 3         │
│                              │
│ ┌────────────────────────┐  │
│ │ ① Crea la teva landing │  │
│ │ [descripció llarga]    │  │
│ │ [Comença →]            │  │
│ └────────────────────────┘  │
│                              │
│ ┌────────────────────────┐  │
│ │ ② Automatització      │  │
│ │ [descripció llarga]    │  │
│ │ [Comença →]            │  │
│ └────────────────────────┘  │
│                              │
│ ┌────────────────────────┐  │
│ │ ③ Pressupost           │  │
│ │ [descripció llarga]    │  │
│ │ [Comença →]            │  │
│ └────────────────────────┘  │
│                              │
│ [Salta l'onboarding]        │
└──────────────────────────────┘
```

Diferències clau mòbil:
- 3 targetes en **stack vertical** (1 columna, ample complet)
- Fletxa del CTA orientada cap avall (→) en lloc de dreta (↓) per indicar navegació
- Barra de progrés més compacta
- Padding reduït (p-4 vs p-8)

### 8.3 Colors i estils

- Fons del component: `#0d0d1a` (bg-bg-0, consistent amb el portal)
- Pas pendent: borde `border-amber-500/40`, fons `bg-[#1a1a2e]`
- Pas fet: borde `border-emerald-500/60`, fons `bg-emerald-900/10`, check verd
- Pas indeterminat: borde `border-slate-500/30`, fons `bg-[#1a1a2e]`
- Barra de progrés: gradient taronja `from-[#FF6B00] to-[#FF9A3C]`
- Text "Salta": `text-ink-2 hover:text-ink-0 transition` (discret però accessible)
- Títol: `f-display font-bold text-xl`
- Subtítol: `text-ui text-ink-1 mt-1`

### 8.4 Animacions

- **Transició pas pendent → fet**: `background-color 0.5s ease`, `border-color 0.5s ease`
- **Barra de progrés**: `width 0.6s cubic-bezier(0.4, 0, 0.2, 1)`
- **OnboardingComplete**: SVG checkmark animat amb `stroke-dashoffset` + fade-in del text
- **Skip**: transició ràpida `opacity 0.2s` + el component es desmunta

Totes les animacions s'aturen si `prefers-reduced-motion: reduce`:
```css
@media (prefers-reduced-motion: reduce) {
  .onboarding-transition,
  .progress-bar,
  .step-card,
  .complete-check {
    transition: none !important;
    animation: none !important;
  }
}
```

---

## 9. Accessibilitat

- `OnboardingGuide` té `role="region"` i `aria-label="Guia d'inici ràpid"`.
- Cada `OnboardingStep` té `aria-label` descriptiu que canvia segons l'estat:
  - Pendent: `"Pas 1: Crea la teva primera landing — pendent"`.
  - Fet: `"Pas 1: Crea la teva primera landing — completat"`.
  - Indeterminat: `"Pas 1: Crea la teva primera landing — dades no disponibles"`.
- El botó "Salta" té `aria-label="Salta la guia d'inici i ves al dashboard"`.
- Focus management:
  - Quan l'onboarding es mostra per primer cop: el focus va al primer pas (`tabindex="-1"` + `focus()`).
  - Quan es fa skip: el focus va al primer widget del dashboard (`document.querySelector('.dashboard-content')`).
  - Quan es completa: el focus va al botó "Anar al dashboard".
- Tots els CTA són `<a>` o `<Link>` navegables amb teclat (Tab + Enter).
- `OnboardingComplete`: animació CSS respecta `prefers-reduced-motion`.

---

## 10. Internacionalització

Claus de traducció per a `next-intl`:

```json
{
  "onboarding": {
    "title": "Benvingut, {name}! Comencem.",
    "subtitle": "Completa els 3 passos per treure el màxim profit del portal",
    "progress": "{completed} / {total} passos completats",
    "skip": "Salta l'onboarding",
    "complete_title": "Estàs a punt!",
    "complete_subtitle": "Ja tens tot configurat. El teu portal està llest.",
    "complete_cta": "Anar al dashboard",
    "vault_pending": "Configuració pendent",
    "vault_pending_desc": "El teu compte té serveis pendents de configurar. Revisa'ls per activar-los.",
    "vault_pending_cta": "Revisar configuració",
    "error_title": "Error de connexió",
    "error_desc": "No s'han pogut carregar les dades del teu portal.",
    "error_retry": "Reintentar",
    "steps": {
      "landing_title": "Crea la teva primera landing",
      "landing_desc": "Publica la teva web en menys de 5 minuts amb l'editor visual.",
      "landing_cta": "Comença",
      "automation_title": "Connecta una automatització",
      "automation_desc": "Connecta n8n per enviar emails, WhatsApp o rebre notificacions automàtiques.",
      "automation_cta": "Comença",
      "billing_title": "Genera el teu primer pressupost",
      "billing_desc": "Crea un pressupost personalitzat i envia'l al client en PDF.",
      "billing_cta": "Comença"
    }
  }
}
```

Els 4 idiomes (ca, es, en, de) tenen les seves traduccions. Exemple per `en.json`:
```json
{
  "onboarding": {
    "title": "Welcome, {name}! Let's get started.",
    "subtitle": "Complete the 3 steps to get the most out of your portal",
    "progress": "{completed} / {total} steps completed",
    "skip": "Skip onboarding",
    "complete_title": "You're all set!",
    "complete_subtitle": "Everything is configured. Your portal is ready.",
    "complete_cta": "Go to dashboard",
    "steps": {
      "landing_title": "Create your first landing page",
      "landing_desc": "Publish your website in under 5 minutes with the visual editor.",
      "landing_cta": "Get started",
      "automation_title": "Connect an automation",
      "automation_desc": "Connect n8n to send emails, WhatsApp, or receive automatic notifications.",
      "automation_cta": "Get started",
      "billing_title": "Generate your first budget",
      "billing_desc": "Create a custom budget and send it to your client as a PDF.",
      "billing_cta": "Get started"
    }
  }
}
```

---

## 11. Seguretat

- No es fan crides de xarxa noves — usa únicament les dades ja consultades per React Query al dashboard.
- La clau de `localStorage` inclou el `tenantId` per evitar conflictes entre tenants al mateix navegador.
- No es guarden dades sensibles a `localStorage`.
- L'estat `onboardingSkipped` es llegeix UNA VEGADA al mount del component (useState amb funció inicialitzadora). No se sincronitza entre pestanyes (no cal).

---

## 12. Tests QA

### 12.1 Tests unitaris

#### OnboardingGuide.test.tsx

| ID | Descripció | Arrange | Assert |
|----|-----------|---------|--------|
| OB-01 | Renderitza onboarding amb serveis pendents | Serveis=[LANDING, AUTOMATION], landings=[], workflows=[], invoices=[] | 2 passos (landing + automation), 0/2 |
| OB-02 | No renderitza si tots els passos complets | Serveis=[LANDING], landings.length>0 | Component NO visible |
| OB-03 | No renderitza si cap servei assignat | Serveis=[], landings=[], workflows=[], invoices=[] | Component NO visible |
| OB-04 | Només mostra passos per serveis assignats | Serveis=[AUTOMATION], workflows=[] | 1 pas (automation), 0/1 |
| OB-05 | Pas marcat fet si resourceCount > 0 | Serveis=[LANDING], landings.length>0 | Step landing: done=true, progrés 1/1 |
| OB-06 | Botó "Salta" escriu localStorage i crida onSkip | Click "Salta" | localStorage té clau, onSkip cridat 1 cop |
| OB-07 | Si localStorage té la key, no es mostra onboarding | Mock localStorage.getItem return true | Component NO visible |
| OB-08 | Barra de progrés mostra 0/2 amb 2 passos | Serveis=[LANDING, AUTOMATION], 0 resources | width=0% |
| OB-09 | Barra de progrés mostra 1/2 | Serveis=[LANDING, AUTOMATION], 1 resource creat | width=50% |
| OB-10 | OnboardingComplete si tots els passos fets | Serveis=[LANDING], landings.length>0 | Pantalla de felicitació visible (1/1) |
| OB-11 | Step indeterminate si dada undefined | Serveis=[AUTOMATION], workflows=undefined | Step automation té `indeterminate=true` |
| OB-12 | Skip NO es mostra si tot complet | Serveis=[LANDING], landings.length>0 | Botó "Salta" no existeix al DOM |
| OB-13 | Tenant sense serveis no mostra onboarding | Serveis=[] | Dashboard normal (no onboarding) |
| OB-14 | Layout mòbil (viewport < 768px) | Mock viewport, 3 passos | Steps en stack vertical (1 columna) |

### 12.2 Tests d'accessibilitat (jest-axe)

| ID | Descripció | Assert |
|----|-----------|--------|
| OB-A1 | Cap violació d'accessibilitat a OnboardingGuide | 0 axe violations |
| OB-A2 | Cap violació a OnboardingComplete | 0 axe violations |
| OB-A3 | Tots els botons tenen aria-label vàlid | 0 violations |
| OB-A4 | Focus management: en mostrar onboarding, focus al primer step | document.activeElement és el step 1 |

### 12.3 Tests d'integració (Playwright)

| ID | Descripció | Steps | Assert |
|----|-----------|-------|--------|
| OB-E1 | Client nou amb serveis pendents | Client amb servei LANDING assignat, sense landings | OnboardingGuide visible, 1 pas, 0/1 |
| OB-E2 | Client amb pas complet NO veu onboarding | Client amb servei LANDING + landing creada | Dashboard normal, onboarding no visible |
| OB-E3 | Client sense serveis NO veu onboarding | Client sense serveis Vault | Dashboard normal, onboarding no visible |
| OB-E4 | Skip funciona i persisteix | Click "Salta", refresca pàgina | Dashboard normal, onboarding no torna |
| OB-E5 | Completar pas marca check | Crear landing des de l'onboarding | Refetch automàtic, Step verd |
| OB-E6 | Admin veu onboarding del client | Login ADMIN, tenant amb LANDING sense landing | OnboardingGuide visible per al tenant específic |

---

## 13. Dependències

| Mòdul | Motiu | Tipus |
|-------|-------|-------|
| Mòdul 01 — Auth | `tenantId` i `user.name` per a la detecció i la clau de localStorage | Forta |
| Mòdul 02 — Vault | `getTenantSetup()` per detectar configuració pendent (vaultPending) | Debil (opcional) |
| Mòdul 04 — Engine | Comptador de landings (`landings.length`) | Forta |
| Mòdul 07 — Billing | Comptador de pressupostos (`invoices.length`) | Forta |
| Mòdul 10 — Automations | Comptador de workflows (`workflows.length`) | Forta |
| Mòdul 13 — i18n | Rutes `/[locale]/` i traduccions als 4 idiomes | Forta |

La dependència del Vault (Mòdul 02) és dèbil: si no està disponible, l'onboarding funciona igual sense la secció de "configuració pendent".

---

## 14. Criteris d'acceptació

- [ ] Un client amb serveis assignats al Vault però sense recursos creats veu l'onboarding
- [ ] Un client sense serveis assignats NO veu l'onboarding (dashboard normal)
- [ ] Un client amb tots els serveis configurats NO veu l'onboarding (dashboard normal)
- [ ] Els passos es generen dinàmicament segons els serveis del Vault (no són fixos)
- [ ] Els passos completats es mostren amb check verd en temps real (refetch 30s)
- [ ] Si l'usuari esborra un recurs, el pas torna a "pendent" al següent refetch
- [ ] El botó "Salta" amaga l'onboarding permanentment per a aquell tenant (localStorage)
- [ ] La pantalla de felicitació apareix automàticament quan TOTS els passos aplicables estan fets
- [ ] Layout responsive: Y columnes escriptori, 1 columna mòbil (Y = nombre de passos, max 3)
- [ ] L'onboarding és funcional i accessible amb teclat (tab + enter)
- [ ] Llum verda a jest-axe sense violations
- [ ] Texts disponibles en els 4 idiomes: ca, es, en, de
- [ ] Si les dades fallen (error de xarxa), es mostra error banner, no onboarding
- [ ] Si hi ha dades parcials (algunes crides fallen), els passos afectats es mostren com a "indeterminat"
