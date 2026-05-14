# Mòdul 16: Onboarding — Guia de primers passos per a nous clients

> **Versió:** 1.0
> **Data:** 2026-05-14
> **Autor:** [per determinar]
> **Dependències:** Mòdul 01 (Auth), Mòdul 04 (Engine — landings), Mòdul 10 (Automations), Mòdul 07 (Billing)

---

## 1. Objectius

- Detectar automàticament quan un client accedeix al portal per primera vegada o sense dades.
- Substituir el dashboard buit per una guia interactiva de primers passos.
- Guiar el client fins a tenir al menys una landing publicada, una automatització activa i un pressupost creat.
- Reduir la fricció d'adopció i el temps fins al primer valor percebut (time-to-value).
- Frontend-only: no requereix nous endpoints de backend.

---

## 2. Abast

### 2.1 Funcionalitats incloses

- Detecció d'estat buit (0 landings, 0 workflows, 0 pressupostos).
- Component `OnboardingGuide` que substitueix el dashboard quan s'detecta estat buit.
- Indicador de progrés visual (X / 3 passos completats).
- 3 passos ordenats: crear landing → activar automatització → crear pressupost.
- Cada pas: descripció, CTA directe a la secció corresponent, i estat (pendent / fet).
- Botó "Salta l'onboarding" per a clients avançats que ja saben on van.
- Persistència de "skip" via `localStorage` per no mostrar-lo si ja s'ha saltat.
- Animació de felicitació quan els 3 passos estan completats (confetti o missatge).
- Accessible: foco correcte, landmarks ARIA, compatible amb `prefers-reduced-motion`.

### 2.2 Funcionalitats excloses

- Nous endpoints de backend (usa les dades ja carregades al dashboard).
- Onboarding interactiu pas a pas amb overlays i tooltips (product tour) — fora d'abast per complexitat.
- Enviament d'email de benvinguda (gestionat fora de la plataforma).
- Vídeos tutorials integrats.
- Personalització per pla de subscripció.

### 2.3 Actors

| Actor | Descripció | Comportament |
|-------|-----------|--------------|
| CLIENT | Usuari final del tenant | Veu l'onboarding si no té dades o no l'ha saltat |
| ADMIN | Personal operatiu | Pot veure l'onboarding per compte d'un client; té botó de skip |
| SUPER_ADMIN | Propietari de la plataforma | Igual que ADMIN; rarament veurà l'onboarding |

---

## 3. Lògica de detecció d'estat buit

```typescript
// Condició d'activació de l'onboarding
const showOnboarding =
  !isOnboardingSkipped &&           // no s'ha saltat via localStorage
  landings.length === 0 &&          // cap landing creada
  workflows.length === 0 &&         // cap automatització activa
  invoices.length === 0;            // cap pressupost creat
```

**Prioritat:** si qualsevol dels 3 recursos té dades, el dashboard normal es mostra sense l'onboarding. L'onboarding és tot-o-res: o tots els passos buids, o dashboard normal.

**`localStorage` key:** `amg_onboarding_skipped_<tenantId>` — inclou tenantId per si un admin opera múltiples tenants en el mateix navegador.

---

## 4. Rutes del frontend

No es creen noves rutes. El component s'integra dins del dashboard existent:

```
/portal          → portal/page.tsx (ja existent)
                    └── si showOnboarding: <OnboardingGuide /> en lloc dels widgets
                    └── si no: dashboard normal (comportament actual)
```

---

## 5. Components del frontend

### 5.1 `OnboardingGuide` (`src/components/portal/OnboardingGuide.tsx`)

Component principal. Rep les dades ja carregades i computa quins passos estan completats.

**Props:**
```typescript
interface OnboardingGuideProps {
  tenantId: string;
  landingCount: number;
  workflowCount: number;
  invoiceCount: number;
  onSkip: () => void;
}
```

**Lògica interna:**
- Calcula `steps: OnboardingStep[]` on cada pas té `{ id, title, description, href, done }`.
- `completedCount = steps.filter(s => s.done).length`.
- Si `completedCount === 3`, mostra el component `OnboardingComplete` en lloc dels passos.

### 5.2 `OnboardingStep` (`src/components/portal/OnboardingStep.tsx`)

Targeta individual per a cada pas. Variants visuals: `pending` (borde taronja) i `done` (borde verd + check).

**Props:**
```typescript
interface OnboardingStepProps {
  step: number;       // número del pas (1, 2, 3)
  title: string;
  description: string;
  href: string;       // ruta on s'ha de fer l'acció
  done: boolean;
}
```

### 5.3 `OnboardingComplete` (`src/components/portal/OnboardingComplete.tsx`)

Pantalla de felicitació que apareix quan els 3 passos estan completats. Inclou un missatge de congratulació i un CTA per anar al dashboard complet. Usa `prefers-reduced-motion` per eliminar animacions si l'usuari ho té activat.

### 5.4 Modificació de `portal/page.tsx`

Afegir la lògica de detecció i renderitzar condicionalment:

```tsx
// Dins portal/page.tsx, un cop carregades les dades:
const [onboardingSkipped, setOnboardingSkipped] = useState(() =>
  typeof window !== 'undefined'
    ? localStorage.getItem(`amg_onboarding_skipped_${tenantId}`) === 'true'
    : false
);

const showOnboarding =
  !onboardingSkipped &&
  landings.length === 0 &&
  workflows.length === 0 &&
  invoices.length === 0;

const handleSkipOnboarding = () => {
  localStorage.setItem(`amg_onboarding_skipped_${tenantId}`, 'true');
  setOnboardingSkipped(true);
};
```

---

## 6. Definició dels 3 passos

| Pas | Títol | Descripció | Ruta CTA | Condició "Fet" |
|-----|-------|-----------|----------|----------------|
| 1 | Crea la teva primera landing | Publica la teva web en menys de 5 minuts amb l'editor visual. | `/portal/factory/new` | `landingCount > 0` |
| 2 | Connecta una automatització | Connecta n8n per enviar emails, WhatsApp o rebre notificacions automàtiques. | `/portal/automations` | `workflowCount > 0` |
| 3 | Genera el teu primer pressupost | Crea un pressupost personalitzat i envia'l al client en PDF. | `/portal/billing/new` | `invoiceCount > 0` |

---

## 7. UX i disseny

### 7.1 Layout de l'OnboardingGuide

```
┌─────────────────────────────────────────────────────────────┐
│  Benvingut, [Nom]! Comencem.                                │
│  Completa els 3 passos per treure el màxim profit del portal│
│                                                             │
│  ████████░░░░░░░░░░  1 / 3 passos completats               │
│                                                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │ ① Crea landing  │  │ ② Automatitza   │  │ ③ Pressupost│ │
│  │                 │  │                 │  │             │ │
│  │ [Descripció]    │  │ [Descripció]    │  │ [Descripció]│ │
│  │                 │  │                 │  │             │ │
│  │ [Comença →]     │  │ [Comença →]     │  │ [Comença →] │ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
│                                                             │
│                              [Salta l'onboarding]          │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 Colors i estils

- Fons del component: `#0d0d1a` (consistent amb el portal)
- Pas pendent: borde `border-[#FF6B00]/40`, fons `bg-[#1a1a2e]`
- Pas fet: borde `border-emerald-500/60`, fons `bg-emerald-900/10`, check verd visible
- Barra de progrés: gradient taronja `from-[#FF6B00] to-[#FF9A3C]`
- Text "Salta": `text-[#64748b] hover:text-white transition` (discret però accessible)

### 7.3 Estats de càrrega

Mentre les dades del dashboard encara s'estan carregant (React Query `isLoading`), el dashboard mostra el skeleton existent — no s'activa l'onboarding fins que `isSuccess` és `true` i s'han rebut les dades reals.

---

## 8. Accessibilitat

- `OnboardingGuide` té `role="region"` i `aria-label="Guia d'inici"`.
- Cada `OnboardingStep` té el seu `aria-label` descriptiu: `"Pas 1: Crea la teva primera landing — pendent"`.
- El botó "Salta l'onboarding" té `aria-label="Salta la guia d'inici i ves al dashboard"`.
- `OnboardingComplete`: animació CSS amb `@media (prefers-reduced-motion: reduce) { animation: none; }`.
- Focus gestionat: quan l'onboarding desapareix (skip o completat), el focus passa al primer element del dashboard.

---

## 9. Internacionalització

El component usa les claus de traducció existents de `next-intl`. Afegir les claus noves a `messages/[locale].json`:

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

Els 4 idiomes (ca, es, en, de) han de tenir les traduccions corresponents.

---

## 10. Seguretat

- No es fan crides de xarxa noves — usa únicament les dades ja consultades per React Query al dashboard.
- La clau de `localStorage` inclou el `tenantId` per evitar conflictes entre tenants en el mateix navegador (ús admin).
- No es guarden dades sensibles a `localStorage`.

---

## 11. Tests QA

### 11.1 Tests unitaris (`OnboardingGuide.test.tsx`)

| ID | Descripció | Resultat esperat |
|----|-----------|-----------------|
| OB-01 | Renderitza onboarding si landings=0, workflows=0, invoices=0 | `OnboardingGuide` visible |
| OB-02 | No renderitza onboarding si landings > 0 | Dashboard normal visible |
| OB-03 | No renderitza onboarding si workflows > 0 | Dashboard normal visible |
| OB-04 | No renderitza onboarding si invoices > 0 | Dashboard normal visible |
| OB-05 | Pas 1 marcat com "fet" si landingCount > 0 | Check verd al pas 1 |
| OB-06 | Botó "Salta" escriu `localStorage` i amaga onboarding | Dashboard normal visible, key present al localStorage |
| OB-07 | Si `localStorage` té la key, no es mostra l'onboarding | Dashboard normal al renderitzar |
| OB-08 | Barra de progrés mostra 0/3 quan cap pas és fet | Ample del 0% |
| OB-09 | Barra de progrés mostra 2/3 si dos passos fets | Ample del 66% |
| OB-10 | `OnboardingComplete` apareix quan els 3 passos estan fets | Pantalla de felicitació visible |

### 11.2 Tests d'accessibilitat (jest-axe)

| ID | Descripció | Resultat esperat |
|----|-----------|-----------------|
| OB-A1 | Cap violació d'accessibilitat al `OnboardingGuide` | 0 violations |
| OB-A2 | Tots els botons tenen `aria-label` vàlid | 0 violations |

---

## 12. Dependències

| Mòdul | Motiu |
|-------|-------|
| Mòdul 01 — Auth | `tenantId` i `user.name` per a la detecció i la clau de localStorage |
| Mòdul 04 — Engine | Comptador de landings (`landings.length`) |
| Mòdul 10 — Automations | Comptador de workflows (`workflows.length`) |
| Mòdul 07 — Billing | Comptador de pressupostos (`invoices.length`) |

---

## 13. Criteris d'acceptació

- [ ] Un client nou sense dades veu l'onboarding en obrir el portal.
- [ ] Un client existent amb dades veu el dashboard normal sense onboarding.
- [ ] Els passos completats es mostren amb check verd en temps real (sense recàrrega).
- [ ] El botó "Salta" amaga l'onboarding permanentment per a aquell tenant.
- [ ] La pantalla de felicitació apareix automàticament quan els 3 passos estan fets.
- [ ] L'onboarding és funcional i accessible amb teclat (tab + enter).
- [ ] Llum verda a jest-axe sense violations.
- [ ] Textos disponibles en els 4 idiomes: ca, es, en, de.
