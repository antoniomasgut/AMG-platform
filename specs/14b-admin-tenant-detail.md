# Mòdul 14b: Admin Tenant Detail — Serveis i landings per tenant

> **Versió:** 1.0
> **Data:** 2026-05-15
> **Dependències:** Mòdul 01 (Auth), Mòdul 02 (Vault), Mòdul 04 (Engine), Mòdul 05 (Factory), Mòdul 14 (Admin Frontend)

---

## 1. Objectius

- Proporcionar una pàgina de detall de tenant per a SUPER_ADMIN/ADMIN on puguin:
  - Veure informació completa del tenant
  - Veure els serveis assignats (perfils, fases, serveis per fase)
  - Veure el catàleg de serveis disponibles
  - Crear una landing per al tenant (ja que SUPER_ADMIN no té `tenantId` propi)
- Resoldre el problema que SUPER_ADMIN no pot crear landings perquè no té `tenantId` a `sessionStorage`

---

## 2. Abast

### 2.1 Funcionalitats incloses

- Pàgina de detall de tenant a `/portal/admin/tenants/[id]`
- Targetes de resum: nombre de landings, perfils i serveis actius
- Secció "Serveis assignats": visualització dels perfils, fases i serveis del tenant (via Vault)
- Secció "Catàleg de serveis": taula de tots els serveis disponibles a la plataforma
- Botó "Crear landing": enllaça a `/portal/landings/new?tenantId={id}` passant el tenant com a paràmetre
- La pàgina de nova landing llegeix `tenantId` de la URL si existeix, en lloc de `sessionStorage`
- Enllaç "Gestionar" al llistat de tenants per navegar al detall

### 2.2 Funcionalitats excloses

- Assignar/desassignar serveis a un tenant (es fa des del backend o via API directa de moment)
- Editar informació del tenant des de la pàgina de detall (ja existeix el formulari d'edició)
- Gestió de dominis des de la pàgina de detall
- Historial d'incidències o factures del tenant (pot ser una futura ampliació)
- Mètriques d'ús o estadístiques per tenant

### 2.3 Actors

| Actor | Descripció | Permisos |
|-------|-----------|----------|
| SUPER_ADMIN | Accés complet a totes les pàgines d'admin | Veure qualsevol tenant, crear landings per a qualsevol tenant |
| ADMIN | Accés a secció admin limitada | Veure tenants, però NO pot crear landings (rol ADMIN no té permís a `POST /engine/tenants/{tId}/landings` segons spec 04) |
| CLIENT | Sense accés a admin | Redirigit al dashboard |

---

## 3. Canvis a la interfície

### 3.1 Llistat de tenants — enllaç a detall

Al llistat existent (`/portal/admin/tenants`), cada fila de tenant mostra un botó "Gestionar" a la columna d'accions, abans del botó "Activar/Desactivar".

```
[Gestionar] [Desactivar]
```

El botó "Gestionar" navega a `/portal/admin/tenants/{id}`.

### 3.2 Pàgina de detall de tenant

URL: `/[locale]/portal/admin/tenants/[id]`

Layout:
```
PortalShell (breadcrumb: "admin · tenants · {tenant.name}")
└── p-4 sm:p-8
    ├── Header
    │   ├── Nom del tenant + badge Actiu/Inactiu
    │   ├── Email, telèfon, slug, data creació
    │   └── Botó [Crear landing → /portal/landings/new?tenantId={id}]
    ├── Targetes de resum (3 columnes)
    │   ├── Landings: {count}
    │   ├── Perfils: {count}
    │   └── Serveis actius: {count}
    ├── "Serveis assignats"
    │   └── Llista de perfils → fases → serveis
    │       ├── Cada perfil: nom
    │       │   └── Cada fase: nom + badge estat (Aprovat/Pendent/Rebutjat)
    │       │       └── Cada servei: nom + tipus + badge estat (Actiu/Inactiu)
    │       └── Add-ons: nom + badge estat
    └── "Catàleg de serveis"
        └── Taula: Servei (nom + slug), Tipus, Preu venda, Addon (badge o "—")
```

### 3.3 Pàgina de nova landing — paràmetre `?tenantId=`

La pàgina `/[locale]/portal/landings/new` accepta un paràmetre query opcional `?tenantId={id}`.

Si el paràmetre és present:
- S'usa com a `tenantId` per a la crida `POST /engine/tenants/{tenantId}/landings`
- No es llegeix de `sessionStorage`
- Permet a SUPER_ADMIN crear landings per a qualsevol tenant

Si el paràmetre NO és present (flux normal per ADMIN/CLIENT):
- Es llegeix `tenantId` de `sessionStorage` com abans
- Si no hi ha `tenantId`, es mostra error "No tenant"

La pàgina embolica el formulari en un `Suspense` perquè `useSearchParams()` ho requereix a Next.js 14.

---

## 4. API REST

### 4.1 Endpoints consumits

| Mètode | Path | Rol | Propòsit |
|--------|------|-----|----------|
| GET | `/api/v1/tenants/{id}` | SUPER_ADMIN, ADMIN | Info del tenant |
| GET | `/api/v1/vault/services` | Autenticat | Catàleg de serveis |
| GET | `/api/v1/vault/tenants/{tenantId}/setup` | SUPER_ADMIN, ADMIN (o CLIENT del seu tenant) | Serveis assignats al tenant |
| GET | `/api/v1/engine/tenants/{tenantId}/landings` | SUPER_ADMIN, ADMIN | Landings del tenant (per al recompte) |
| POST | `/api/v1/engine/tenants/{tenantId}/landings` | SUPER_ADMIN, ADMIN | Crear landing |

### 4.2 Formats de resposta

#### GET `/api/v1/vault/services`

```json
[
  {
    "id": "uuid",
    "name": "Landing Pro",
    "slug": "landing-pro",
    "description": "Landing professional amb 8 blocs",
    "type": "LANDING",
    "isAddon": false,
    "cost": 25.00,
    "salePrice": 80.00
  }
]
```

#### GET `/api/v1/vault/tenants/{tenantId}/setup`

```json
{
  "profiles": [
    {
      "profile": { "id": "uuid", "name": "Professional", "slug": "professional" },
      "phases": [
        {
          "phase": { "id": "uuid", "name": "Fase 1", "sortOrder": 1 },
          "approvalStatus": "APPROVED",
          "services": [
            { "service": { "id": "uuid", "name": "Landing Pro", "type": "LANDING" }, "status": "ACTIVE" }
          ]
        }
      ]
    }
  ],
  "addons": [
    { "service": { "id": "uuid", "name": "Domini gestionat" }, "approvalRequired": true, "approvalStatus": "PENDING" }
  ]
}
```

---

## 5. Tipus TypeScript (service layer)

### 5.1 Nous tipus a `src/services/admin.ts`

```typescript
export interface CatalogService {
  id: string;
  name: string;
  slug: string;
  description: string;
  type: string;
  isAddon: boolean;
  cost: number;
  salePrice: number;
}

export interface TenantSetup {
  profiles: Array<{
    profile: { id: string; name: string; slug: string };
    phases: Array<{
      phase: { id: string; name: string; sortOrder: number };
      approvalStatus: string;
      services: Array<{
        service: { id: string; name: string; type: string };
        status: string;
      }>;
    }>;
  }>;
  addons: Array<{
    service: { id: string; name: string };
    approvalRequired: boolean;
    approvalStatus: string;
  }>;
}

export interface AssignProfileResponse {
  profileId: string;
  phases: Array<{
    phaseId: string; name: string; sortOrder: number;
    approvalStatus: string; totalServices: number; totalPrice: number;
  }>;
  totalPrice: number;
}
```

### 5.2 Funcions noves

| Funció | Mètode | Path |
|--------|--------|------|
| `listCatalogServices()` | GET | `/vault/services` |
| `getTenantSetup(tenantId)` | GET | `/vault/tenants/{tenantId}/setup` |
| `assignProfileToTenant(tenantId, profileId)` | POST | `/vault/tenants/{tenantId}/profiles/{profileId}` |

---

## 6. Components

### 6.1 `admin/tenants/[id]/page.tsx` (NOU)

**Pàgina client** (`'use client'`) que fa 4 crides en React Query:

1. `getTenant(id)` — info del tenant
2. `getTenantSetup(id)` — serveis assignats (només si tenant carregat)
3. `listCatalogServices()` — catàleg complet
4. `listLandings(id)` — landings del tenant (només si tenant carregat)

**Estats:**

| Estat | Comportament |
|-------|-------------|
| Loading (tenant) | Spinner centrat dins PortalShell |
| Error (tenant) | Missatge d'error + botó "Reintentar" |
| Loaded | Mostra totes les seccions |
| Loading (setup/services) | Spinner a cada secció individualment |
| Empty setup | Missatge "Cap servei assignat a aquest tenant" |
| Empty catalog | Missatge "Cap servei al catàleg" |

**Sub-components:**
- `SetupSection({ setup })` — renderitza perfils → fases → serveis, o missatge buit
- `ServiceCatalogTable({ services })` — taula de serveis, o missatge buit

### 6.2 `landings/new/page.tsx` (MODIFICAT)

- Afegir `useSearchParams()` de `next/navigation`
- Llegir `?tenantId=` de la URL
- Si `tenantId` de URL existeix, usar-lo com a `tid` principal
- Mantenir fallback a `sessionStorage` per ADMIN/CLIENT
- Embolicar en `Suspense` per compatibilitat amb `useSearchParams()`

### 6.3 `admin/tenants/page.tsx` (MODIFICAT)

- Afegir botó "Gestionar" a la columna d'accions
- Enllaça a `/portal/admin/tenants/{id}`

---

## 7. Flux d'usuari

### 7.1 SUPER_ADMIN crea landing per a un tenant

1. SUPER_ADMIN va a `/portal/admin/tenants` (llistat)
2. Fa clic a "Gestionar" al tenant desitjat
3. Veu el detall del tenant: info, serveis assignats, catàleg
4. Fa clic a "Crear landing"
5. És redirigit a `/portal/landings/new?tenantId={id}`
6. Selecciona plantilla, configura slug, fa clic a "Crear landing"
7. La landing es crea amb el `tenantId` de la URL
8. És redirigit a l'editor (`/portal/landings/{landingId}/edit`)

### 7.2 ADMIN/CLIENT crea landing (flux normal, sense canvis)

1. ADMIN/CLIENT va a `/portal/landings/new` (sense query params)
2. El sistema llegeix `tenantId` de `sessionStorage`
3. Flux normal de creació de landing

---

## 8. Seguretat

### 8.1 Autorització per rol

| Acció | SUPER_ADMIN | ADMIN | CLIENT |
|-------|------------|-------|--------|
| Veure detall de tenant | ✅ | ✅ | ❌ (redirigit a portal) |
| Crear landing (via URL tenantId) | ✅ | ❌ (backend no permet) | ❌ |
| Veure serveis assignats | ✅ | ✅ | ❌ |
| Veure catàleg de serveis | ✅ | ✅ | ❌ |

### 8.2 Protecció de rutes

- La pàgina de detall de tenant està sota `/portal/admin/*`
- El `AdminGuard`/`PortalShell` ja protegeix aquestes rutes per rol
- No cal protecció addicional

---

## 9. Tests (QA)

### 9.1 Funcionals

| # | Cas | Acció | Resultat esperat |
|---|-----|-------|-----------------|
| 1 | Veure detall de tenant | Navegar a `/ca/portal/admin/tenants/{id}` | Info del tenant, targetes, serveis assignats, catàleg |
| 2 | Tenant sense serveis | Tenant acabat de crear | Secció "Cap servei assignat" |
| 3 | Tenant amb serveis | Tenant amb perfil assignat | Perfils, fases i serveis renderitzats correctament |
| 4 | Crear landing des de detall | Clic "Crear landing" | Redirigeix a `/landings/new?tenantId={id}` |
| 5 | Crear landing amb tenantId URL | Seleccionar plantilla + crear | Landing creada pel tenant correcte, redirect a editor |
| 6 | ADMIN veu tenant sense crear landing | Login ADMIN | Pot veure detall, botó "Crear landing" pot fallar (backend) |
| 7 | CLIENT no veu admin | Login CLIENT | Sidebar sense admin, /admin/tenants redirigeix |
| 8 | Error de xarxa | Aturar backend | Missatge d'error + botó reintentar |

### 9.2 Integració

| # | Cas | Verificació |
|---|-----|------------|
| 1 | tenantId de URL té prioritat | Amb `?tenantId=X` i sessionStorage buit, crear landing funciona |
| 2 | tenantId de sessionStorage funciona | Sense query params, ADMIN crea landing normalment |
| 3 | Enllaços funcionen | Clic a "Gestionar" → URL correcta |

---

## 10. Dependències

| Mòdul | Dependència | Tipus |
|-------|-----------|-------|
| Mòdul 01 (Auth) | Endpoint `GET /api/v1/tenants/{id}` | Forta |
| Mòdul 02 (Vault) | Endpoints `GET /api/v1/vault/services` i `GET /api/v1/vault/tenants/{id}/setup` | Forta |
| Mòdul 04 (Engine) | Endpoint `GET /api/v1/engine/tenants/{id}/landings` | Forta |
| Mòdul 05 (Factory) | Pàgina `new/page.tsx` modificada per acceptar `?tenantId=` | Forta |
| Mòdul 14 (Admin Frontend) | Pàgines de llistat de tenants i estructura admin | Forta |

---

## 11. Obert / Pendents

- [ ] Decidir si ADMIN pot crear landings (backend spec 04 diu que NO, només SUPER_ADMIN i ADMIN).
- [ ] Afegir acció per assignar perfil a tenant des de la mateixa pàgina de detall (fer `POST /vault/tenants/{tenantId}/profiles/{profileId}`)?
- [ ] Afegir edició ràpida de camps del tenant des de la pàgina de detall (toggle isActive, editar email)?
- [ ] Traduccions: de moment la pàgina està en català. Caldria afegir suport i18n per als 4 idiomes si s'expandeix.
