---
name: frontend-impl
description: "Implementa un mòdul frontend complet per al portal AMG: llegeix l'spec, analitza els endpoints del backend, genera pàgines Next.js, serveis API, hooks React Query i components seguint el design system AMG. S'activa amb frases com: 'implementa el frontend del mòdul', 'fes el frontend de', 'crea les pàgines per al mòdul', 'implementa el mòdul frontend', 'front del mòdul', 'pàgines del mòdul NN', 'implement frontend module', 'crea el frontend', 'frontend del mòdul'."
---

# Frontend-Impl — Implementació de mòduls frontend per al portal AMG

Implementa mòduls frontend complets per al portal AMG Digitalització. Llegeix l'spec, analitza el backend existent, i genera tot el codi necessari seguint els patrons del projecte.

**Regla fonamental:** MAI inventes endpoints ni camps que no existeixin al backend. Tot ha d'estar suportat per l'spec o pel codi Java existent.

---

## Pas 1 — Identificar el mòdul

1. Llegeix `CLAUDE.md` de l'arrel per obtenir el context general del projecte.
2. Identifica quin mòdul implementar (número + nom).
3. Llegeix l'spec complet: `specs/NN-nom.md`.
4. Si l'spec no existeix, atura't i demana a l'usuari que primer executi `/spec-writer`.

---

## Pas 2 — Analitzar el backend

Per cada mòdul, localitza i llegeix:

```
backend/src/main/java/com/amg/digitalitzacio/{modul}/
├── api/
│   ├── {Modul}Controller.java       ← endpoints, paths, mètodes HTTP
│   └── dto/                         ← request/response shapes
└── domain/
    └── {Entitat}.java               ← camps i tipus
```

Extreu:
- **URL base** de cada controller (`@RequestMapping`)
- **Endpoints** disponibles (GET/POST/PUT/DELETE + path)
- **DTOs** (noms dels camps, tipus, validacions `@NotNull`, etc.)
- **Seguretat** (`@PreAuthorize`, rols requerits)

---

## Pas 3 — Analitzar el frontend existent

Llegeix per entendre els patrons del projecte:

```
frontend/src/
├── services/auth.ts              ← patró de serveis API (fetch + error handling)
├── app/[locale]/portal/page.tsx  ← patró de pàgina portal (useAuth, useQuery, layout)
├── app/[locale]/portal/landings/ ← patró CRUD complet (llista + crear + editar)
├── components/ui/                ← components reutilitzables (button, icons, badge, stat, input)
├── lib/auth-context.tsx          ← useAuth hook
└── lib/toast-context.tsx         ← useToast hook
```

---

## Pas 4 — Planificar la implementació

Defineix quines peces cal crear:

### Fitxers a crear per un mòdul CRUD típic:

```
frontend/src/
├── services/{modul}.ts                          ← funcions fetch (getAll, getById, create, update, delete)
├── app/[locale]/portal/{modul}/
│   ├── page.tsx                                 ← llista principal
│   ├── new/page.tsx                             ← formulari de creació (si escau)
│   └── [id]/
│       ├── page.tsx                             ← detall/edició
│       └── edit/page.tsx                        ← edició (si es separa del detall)
└── components/{modul}/
    ├── {Modul}Card.tsx                          ← card per a la llista
    ├── {Modul}Form.tsx                          ← formulari compartit create/edit
    └── {Modul}StatusBadge.tsx                   ← badge d'estat (si té estats)
```

Per mòduls de lectura o dashboards (FinOps, Ops):
```
frontend/src/
├── services/{modul}.ts
└── app/[locale]/portal/{modul}/
    └── page.tsx                                 ← dashboard de métriques
```

---

## Pas 5 — Implementar

### 5.1 Servei API (`services/{modul}.ts`)

Segueix exactament el patró de `services/auth.ts`. **No uses axios** — usa `fetch` natiu amb el helper d'error:

```typescript
const API = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = typeof window !== 'undefined' ? localStorage.getItem('amg_token') : null;
  const res = await fetch(`${API}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...options,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw Object.assign(new Error(err.message ?? res.statusText), { status: res.status });
  }
  return res.json();
}

// Exporta una funció per cada endpoint del backend
export const get{Moduls} = () => apiFetch<{Entitat}[]>('/api/{modul}s');
export const get{Modul} = (id: number) => apiFetch<{Entitat}>(`/api/{modul}s/${id}`);
export const create{Modul} = (data: Create{Modul}Request) => apiFetch<{Entitat}>('/api/{modul}s', { method: 'POST', body: JSON.stringify(data) });
export const update{Modul} = (id: number, data: Partial<Create{Modul}Request>) => apiFetch<{Entitat}>(`/api/{modul}s/${id}`, { method: 'PUT', body: JSON.stringify(data) });
export const delete{Modul} = (id: number) => apiFetch<void>(`/api/{modul}s/${id}`, { method: 'DELETE' });
```

### 5.2 Pàgina de llista (`portal/{modul}/page.tsx`)

Patrons obligatoris:
- `'use client'` al capdamunt
- `useAuth()` per obtenir el tenant/usuari actual
- `useQuery` de `@tanstack/react-query` per a les dades
- `useToast()` per a notificacions d'error
- Layout consistent amb la resta del portal:

```tsx
'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { useLocale } from 'next-intl';
import { useAuth } from '@/lib/auth-context';
import { useToast } from '@/lib/toast-context';
import { I } from '@/components/ui/icons';

export default function {Modul}sPage() {
  const { user } = useAuth();
  const { addToast } = useToast();
  const locale = useLocale();
  const router = useRouter();
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ['{modul}s'],
    queryFn: get{Moduls},
  });

  // ...
  return (
    <div className="p-6 sm:p-8 max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <div className="w-1.5 h-1.5 bg-accent" />
            <span className="f-mono text-label uppercase tracking-widest text-accent-light">Mòdul</span>
          </div>
          <h1 className="f-display font-black text-2xl">TÍTOL</h1>
        </div>
        <button onClick={() => router.push(`/${locale}/portal/{modul}/new`)}
          className="btn-clip bg-accent hover:bg-accent-light text-black f-mono text-xs uppercase font-semibold px-4 h-9 flex items-center gap-2 transition-colors">
          <I.Plus size={14} /> NOU
        </button>
      </div>

      {/* Contingut: loading / error / llista */}
      {isLoading && <LoadingState />}
      {error && <ErrorState />}
      {data && <LlistaComponent items={data} />}
    </div>
  );
}
```

### 5.3 Design system AMG — referències obligatòries

**Colors (sempre tokens, mai valors inline):**
- Fons: `bg-bg-0` (#0d0d1a), `bg-bg-1` (#13132a), `bg-bg-2` (#1a1a2e)
- Accent: `text-accent-light`, `bg-accent`, `border-border-base`, `border-border-medium`
- Text: `text-ink-0` (principal), `text-ink-1` (secundari), `text-ink-2`, `text-ink-3` (suau)
- Semàntic: `text-success`, `text-danger-light`, `text-warning`, `text-info`

**Tipografia:**
- `f-display font-black` → títols (Orbitron)
- `f-mono` → etiquetes, badges, botons, codis (Share Tech Mono)
- Mides: `text-label` (10px), `text-caption` (11px), `text-data` (12px), `text-ui` (13px)
- Tracking: `tracking-widest`, `tracking-label`, `tracking-caption`

**Components UI disponibles:**
- `@/components/ui/icons` → `I.Check`, `I.AlertCircle`, `I.ArrowRight`, `I.Plus`, `I.Edit`, `I.Trash`, etc.
- `@/components/ui/button` → `<AMGButton>`
- `@/components/ui/badge` → `<AMGBadge>`
- `@/components/ui/stat` → `<StatCard>`
- `@/components/ui/input` → `<AMGInput>`

**Cards:**
```tsx
<div className="amg-card card-clip p-6">...</div>
```

**Botons primaris:**
```tsx
<button className="btn-clip bg-accent hover:bg-accent-light text-black f-mono text-xs uppercase font-semibold px-4 h-9 flex items-center gap-2 transition-colors">
```

**Botons secundaris/destructius:**
```tsx
<button className="f-mono text-caption uppercase text-ink-3 hover:text-danger-light transition-colors">
```

**Badges d'estat:**
```tsx
// Verd: success, Groc: warning, Vermell: danger, Blau: info
<span className="f-mono text-label uppercase px-2 py-0.5 bg-success/10 text-success border border-success/20">ACTIU</span>
```

**Taules:**
```tsx
<div className="amg-card card-clip overflow-hidden">
  <div className="grid grid-cols-[...] border-b border-border-subtle px-4 py-2">
    <span className="f-mono text-label uppercase text-ink-3">COL·LUMNA</span>
  </div>
  {items.map(item => (
    <div key={item.id} className="grid grid-cols-[...] border-b border-border-subtle px-4 py-3 hover:bg-bg-2 transition-colors">
      ...
    </div>
  ))}
</div>
```

**Formularis:**
```tsx
<label className="block">
  <span className="block f-mono uppercase text-label tracking-label text-ink-1 mb-1.5">CAMP</span>
  <div className="relative flex items-center h-10 bg-bg-2/80 border border-border-base focus-within:border-accent transition">
    <div className="pl-3 text-ink-3"><I.NomIcon size={14} /></div>
    <input className="flex-1 bg-transparent outline-none px-3 text-sm text-ink-0 placeholder:text-ink-3" />
  </div>
</label>
```

**Estats loading/empty/error:**
```tsx
// Loading
<div className="flex items-center justify-center py-16">
  <span className="w-5 h-5 border-2 border-accent border-t-transparent rounded-full animate-spin" />
</div>

// Empty
<div className="amg-card card-clip p-12 text-center">
  <p className="f-mono text-label uppercase text-ink-3">CAP ELEMENT</p>
</div>

// Error
<div className="p-3 border-l-2 border-l-danger bg-danger/5 flex gap-2">
  <I.AlertCircle size={14} stroke="#ff6666" className="shrink-0 mt-0.5" />
  <span className="text-data text-danger-light">{error.message}</span>
</div>
```

### 5.4 Mutations (crear, editar, eliminar)

```typescript
const mutation = useMutation({
  mutationFn: create{Modul},
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['{modul}s'] });
    addToast({ type: 'success', message: 'Creat correctament' });
    router.push(`/${locale}/portal/{modul}s`);
  },
  onError: (err: Error) => {
    addToast({ type: 'error', message: err.message });
  },
});
```

---

## Pas 6 — Sidebar navigation

Afegir el nou mòdul al sidebar del portal (`app/[locale]/portal/page.tsx` o `portal/layout.tsx`). Segueix el patró existent dels nav items:

```tsx
{ icon: I.NomIcon, label: 'NOM MÒDUL', href: `/${locale}/portal/{modul}s` }
```

---

## Pas 7 — Verificació

Un cop implementat, executa:

```bash
cd frontend
npx tsc --noEmit          # zero errors TypeScript
npx next build            # build net sense warnings crítics
```

Si hi ha errors de tipus, corregueix-los. No deixis `any` implícit.

---

## Pas 8 — Commit

```bash
git add frontend/src/services/{modul}.ts \
        frontend/src/app/[locale]/portal/{modul}/ \
        frontend/src/components/{modul}/
git commit -m "feat(frontend): implementa Mòdul NN — {nom} frontend"
```

---

## Patrons específics per tipus de mòdul

### Mòdul CRUD (Billing, Leads, Assets)
→ Llista amb paginació + formulari create/edit + vista detall + confirmació de delete

### Mòdul Dashboard (FinOps, Ops & Health)
→ Grid de mètriques amb `<StatCard>`, gràfics simples (si cal), taula d'esdeveniments recents

### Mòdul d'automatitzacions (Automations)
→ Llista de workflows amb estat (actiu/inactiu), toggle on/off, logs d'execució recents

### Mòdul Admin (Mòdul 14)
→ Tabs: Usuaris / Tenants / Configuració. CRUD complet amb gestió de rols.

---

## Notes importants

- **MAI** afegeixis `'use client'` a components que no necessitin hooks o handlers
- **MAI** uses valors de color inline (`text-[#FF6B00]`) — sempre tokens (`text-accent-light`)
- **MAI** inventes endpoints — si el backend no l'exposa, no el crides
- Els `catch` sempre com `catch (err: unknown)` amb `err instanceof Error ? err.message : 'Error'`
- Els formularis han de tenir `disabled` mentre `mutation.isPending`
- Sempre afegeix `aria-label` als botons d'acció (editar, eliminar, tancar)
