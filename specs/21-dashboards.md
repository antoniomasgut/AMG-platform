# Spec 21: Dashboards del Portal

> **Versió:** 1.0
> **Data:** 2026-05-19
> **Branca:** `feat/modul-08-finops`
> **Fitxer principal:** `frontend/src/app/[locale]/portal/page.tsx`
> **Dependències:** Mòduls 03 (Leads), 07 (Billing), 08 (FinOps), 09 (Payments), 11 (Ops), 12 (Prospecting), 19 (InfraOps)

---

## 1. Objectius

- Proporcionar a cada rol una pàgina d'inici útil que reflecteixi la seva perspectiva de treball
- Evitar mostrar informació irrellevant o confusa al rol equivocat
- Permetre accions ràpides sense navegar per tot el menú
- Reflectir l'estat real del sistema en temps quasi-real (dades en paral·lel, refresc automàtic)

---

## 2. Rols i vistes

El dashboard és **únic per URL** (`/portal`) però renderitza contingut diferent:

| Rol | Vista | Perspectiva |
|-----|-------|-------------|
| `SUPER_ADMIN` | Dashboard de negoci | Control total: tenants, finances globals, sistema, leads |
| `ADMIN` | Dashboard de negoci | Igual que SUPER_ADMIN però sense mètriques de sistema/infraestructura |
| `CLIENT` | Dashboard personal | Els seus serveis i finances |

La lògica de selecció: `isAdmin || isSuperAdmin` → vista de negoci; altrament → vista client.

---

## 3. Dashboard SUPER_ADMIN / ADMIN

### 3.1 Greeter (barra superior)

**Contingut:** `Bon dia, {firstName}` en format mono, subratllat per la barra de `PortalShell`.

**Comportament:** Estàtic. No hi ha acció associada. El nom és el primer token de `user.name`.

---

### 3.2 Franja de KPIs (4 targetes)

Càrrega en paral·lel de 4 fonts via `Promise.all`. Cada targeta té:
- **Barra de color** a la part superior (2px horitzontal) que canvia de color segons l'estat
- **Label** (etiqueta en majúscules, color apagat)
- **Valor** principal (número gran, negreta)
- **Subtítol** (detall addicional en minúscules)
- **Icona** a la cantonada superior dreta
- Si hi ha `href`: la targeta sencera és clicable

#### KPI 1 — Tenants actius

| Camp | Valor |
|------|-------|
| Label | `Tenants actius` |
| Valor | Nombre total de tenants (`totalElements` de `GET /api/v1/tenants?size=1`) |
| Subtítol | `clients` |
| Color barra | Taronja (accent, sempre) |
| Icona | `I.Building` |
| Clicable → | `/portal/admin/tenants` |

**Per a ADMIN:** NO mostra aquesta targeta (ADMIN no té accés a `/admin/tenants`). La seva primera KPI és "Leads actius".

> **Futur:** Afegir comparativa amb mes anterior (`+N aquest mes`).

#### KPI 2 — Leads actius

| Camp | Valor |
|------|-------|
| Label | `Leads actius` |
| Valor | `total - byStage.LOST - byStage.WON` (leads en pipeline obert) |
| Subtítol | `{total} total · {conversionRate}% conv.` |
| Color barra | Taronja (accent, sempre) |
| Icona | `I.Users` |
| Clicable → | `/portal/leads` |
| Font | `GET /api/v1/leads/stats` → `LeadStats` |

**Si l'API falla o retorna null:** mostra `—` al valor i subtítol buit.

#### KPI 3 — Serveis monitorats

| Camp | Valor |
|------|-------|
| Label | `Serveis monitorats` |
| Valor | `{up}/{services}` (serveis operatius sobre total) |
| Subtítol | Si `openIncidents > 0`: `{N} incidents oberts` · Si no: `Sense incidents` |
| Color barra | Verd si `up === services && openIncidents === 0` · Vermell si qualsevol falla |
| Icona | `I.Activity` |
| Clicable → | `/portal/ops` |
| Font | `GET /api/v1/ops/dashboard` → `OpsDashboard` |

**Si l'API falla:** mostra `—` al valor, barra taronja neutre.

**Per a ADMIN:** Visible però sense accés a incidents (llegit)

#### KPI 4 — Infraestructura

| Camp | Valor |
|------|-------|
| Label | `Infraestructura` |
| Valor | `{cpu.percent}% CPU` |
| Subtítol | `RAM {ram.percent}% · Disk {disk.percent}%` |
| Color barra | **Verd** si tots < 75% · **Groc** si algun entre 75-90% · **Vermell** si algun ≥ 90% |
| Icona | `I.Server` |
| Clicable → | `/portal/admin/infraops` |
| Font | `GET /api/v1/infraops/status` → `InfraStatus` |

**Per a ADMIN:** NO mostra aquesta targeta (no té accés a infraops). La seva quarta KPI pot ser Pagaments pendents (`PaymentDashboard.totalPending`).

---

### 3.3 Secció central (dues columnes en desktop, una columna en mòbil)

#### Columna esquerra — Pipeline de leads

**Encapçalament:**
- Eyebrow: `Captació`
- Títol: `Pipeline de leads`
- Botó dret: `VEURE TOT →` → navega a `/portal/leads`

**Cos:** Llista d'etapes del pipeline amb barra de progrés horitzontal per a cadascuna.

Etapes mostrades (per ordre): `PROSPECTING`, `CONTACT`, `PROPOSAL`, `NEGOTIATION`, `WON`.
L'etapa `LOST` NO es mostra (és negativa i distorsiona la vista).

Per cada etapa:
```
[Nom etapa]  [======         ] 4
```
- Amplada de la barra = `count / total * 100%`
- `WON` usa color verd (`#39d353`); la resta taronja (`#FF6B00`)
- El número a la dreta és el recompte d'aquella etapa
- `total` = `LeadStats.total`

**Estat buit:** Text `Sense dades` centrat si `LeadStats` és null.

**Font:** `GET /api/v1/leads/stats` → `LeadStats { total, byStage: Record<string,number>, conversionRate }`

---

#### Columna dreta — Recursos del servidor

**Encapçalament:**
- Eyebrow: `Servidor`
- Títol: `Recursos`
- Botó dret: `DETALL →` → navega a `/portal/admin/infraops`

**Cos:** 4 barres de gauge (una per recurs):

| Gauge | Font |
|-------|------|
| CPU | `InfraStatus.cpu.percent` |
| RAM | `InfraStatus.ram.percent` |
| Disc | `InfraStatus.disk.percent` |
| Connexions DB | `InfraStatus.database.percent` |

Cada gauge:
```
CPU                              78%
[========================       ]
```
- Barra vermella si ≥ 90%, groga si ≥ 75%, verda si < 75%
- El percentatge a la dreta s'acoloreix igual que la barra
- Si ≥ 90% (crític): el percentatge és vermell i en negreta

**Peu de targeta:** Si `InfraStatus.tenants` existeix, mostra `Tenants n8n: {active}` separat per una línia.

**Estat buit:** Text `Sense dades de servidor` si `InfraStatus` és null.

**Per a ADMIN:** No veu aquesta targeta. En el seu lloc pot mostrar un resum de factures recents (pendent).

---

### 3.4 Accions ràpides

**Encapçalament:** Etiqueta `Accions ràpides` (mono, apagat).

**Botons** (en horitzontal, es reencadenan en mòbil):

| Botó | Icona | Acció | Rol |
|------|-------|-------|-----|
| `PROCÉS COMPLET` | `I.Flow` | Navega a `/portal/process` | ADMIN + SUPER_ADMIN |
| `NOU LEAD` | `I.Plus` | Navega a `/portal/leads/new` | ADMIN + SUPER_ADMIN |
| `TENANTS` | `I.Building` | Navega a `/portal/admin/tenants` | SUPER_ADMIN only |
| `API KEYS` | `I.Key` | Navega a `/portal/admin/config` | SUPER_ADMIN only |
| `BACKUP` | `I.Database` | Navega a `/portal/admin/backup` | SUPER_ADMIN only |

> **Nota d'implementació:** ADMIN no veu els botons `TENANTS`, `API KEYS` i `BACKUP` (corresponen a rutes de SUPER_ADMIN). Cal filtrar per `isSuperAdmin`.

---

### 3.5 Estat de càrrega (loading)

Mentre les APIs responen:
- 3 rectangles `animate-pulse bg-[#212140]` apilats verticalment
- L'altura de cada rectangle simula el contingut real (24px, 24px, 24px)
- El `PortalShell` i el greeter ja estan renderitzats (no és un skeleton de pàgina completa)

---

### 3.6 Comportament d'error

Si una API individual falla (ex. infraops no disponible):
- La targeta KPI afectada mostra `—` al valor
- La secció afectada mostra el missatge "Sense dades"
- NO es bloca tot el dashboard (els errors s'absorbeixen amb `.catch(() => null)`)
- NO hi ha banner d'error general (un servei caigut no ha d'alarmar innecessàriament)

---

## 4. Dashboard CLIENT

### 4.1 Greeter

Igual que en el dashboard d'admin: `Bon dia, {firstName}`.

---

### 4.2 Onboarding guide (condicional)

**Condició de visibilitat:**
- `!onboardingSkipped` (no ha premut "Salta")
- `!onboardingComplete` (no ha marcat tots els passos com a fets)
- `pendingSteps.length > 0` (hi ha almenys un recurs sense crear)

**Steps que es comptabilitzen:**
- Cap landing → pendent `landing`
- Cap workflow → pendent `automation`
- Cap factura → pendent `billing`

**Quan es mostra l'onboarding, SUBSTITUEIX tota la resta del contingut del dashboard.**

**Botó "Salta":**
- Guarda `localStorage.setItem('amg_onboarding_skipped_{tenantId}', 'true')`
- Oculta l'onboarding i mostra el dashboard normal (hero + factures)
- NO fa cap crida a l'API

**Botó "He acabat" / "Marcar com completat":**
- Crida callback `onComplete` que posa `onboardingComplete = true`
- Oculta l'onboarding i mostra el dashboard normal
- NO fa cap crida a l'API (l'onboarding és purament local)

> **Pendent futur:** Persistir l'estat d'onboarding a l'API per sincronitzar entre dispositius.

---

### 4.3 Hero billing

Targeta gran que dóna una visió ràpida de la situació financera del client.

**Graella 2×2 en mòbil, 4 columnes en desktop:**

#### Columna 1 — Total invertit
- **Valor:** `BillingDashboard.totalSpent` formatat com a € (sense decimals)
- **Etiqueta:** `Total invertit`
- **Badge sota:** `{N} pendents` (warning) si `pendingBudgets > 0` · `AL DIA` (success) si no
- **Font:** `GET /api/v1/billing/tenants/{tenantId}/dashboard`

#### Columna 2 — Últim pressupost
- **Valor:** `BillingDashboard.lastBudget.budgetNumber` o `Cap` si null
- **Etiqueta:** `Últim pressupost`
- **Subtítol:** Data d'enviament (`sentAt`) o `—` si null
- **Clicable:** Si existeix, navega a `/portal/billing` (pendent implementar link directe)

#### Columna 3 — Import
- **Valor:** `BillingDashboard.lastBudget.total` formatat com a € en color accent
- **Etiqueta:** `Import`
- **Subtítol:** Estat del pressupost (`ACCEPTED` / `PENDING` / `DRAFT` / etc.)

#### Columna 4 — Comptadors de serveis
Dues mini-KPIs en graella 2×1:
- **Webs actives:** `landings.filter(PUBLISHED|ACTIVE).length` · Sub: `{total} total`
- **Workflows actius:** `workflows.filter(ACTIVE).length` · Sub: `{total} total`

---

### 4.4 Taula de factures recents

**Encapçalament:**
- Eyebrow: `Historial`
- Títol: `Últimes factures`
- Botó dret: `VEURE TOTES →` → navega a `/portal/finops`

**Files:** Fins a 5 factures (les més recents). Per cada fila:

| Camp | Descripció |
|------|------------|
| Data | `createdAt` formatada (`dia mes any`) |
| Import | `amount` en € amb format català |
| Estat | Badge: PAID→success, PENDING→warning, OVERDUE/FAILED→danger, DRAFT→accent |
| Descàrrega | Icona `I.Download` → obre `invoicePdfUrl` en nova pestanya · Desactivat si null |

**Estat buit:** Icona de rebut + text `Cap factura encara` si `invoices.length === 0`.

**Font:** `GET /api/v1/finops/invoices` (les factures del tenant autenticat via JWT)

---

### 4.5 Card d'ajuda

**Visibilitat:** Sempre visible al final del dashboard (quan no hi ha onboarding).

**Contingut:**
- Icona `I.Sparkles`
- Títol: `NECESSITES AJUDA?`
- Text: `El teu tècnic assignat està disponible per respondre els teus dubtes.`
- Botó `CONTACTE` (icona `I.Mail`) → obre `mailto:hola@amgdigital.com`

> **Pendent futur:** Substituir el mailto per un formulari o integració WhatsApp Business.

---

### 4.6 Estat de càrrega i error

**Loading:** 3 rectangles `animate-pulse` (igual que admin dashboard).

**Error:** Si el `fetchBillingDashboard` falla, el hero mostra `€0` i `—` en tots els camps (`.catch(() => null)`). Cap banner d'error visible.

---

## 5. Dades i endpoints

### 5.1 SUPER_ADMIN / ADMIN — càrrega en paral·lel

```typescript
const [tenants, leads, ops, infra] = await Promise.all([
  listTenants({ size: 1 }),              // GET /api/v1/tenants?size=1
  getLeadStats(),                         // GET /api/v1/leads/stats
  getOpsDashboard(),                      // GET /api/v1/ops/dashboard
  getInfraStatus(),                       // GET /api/v1/infraops/status
]);
```

Tots els errors absorbits amb `.catch(() => null)`.

### 5.2 CLIENT — càrrega en paral·lel

```typescript
const [bill, inv, lnd, wf] = await Promise.all([
  fetchBillingDashboard(tenantId),        // GET /api/v1/billing/tenants/{id}/dashboard
  fetchInvoices(),                        // GET /api/v1/finops/invoices
  fetchLandings(tenantId),               // GET /api/v1/engine/tenants/{id}/landings
  fetchWorkflows(tenantId),              // GET /api/v1/automations/tenants/{id}/workflows
]);
```

Si `user.tenantId` és null (usuari sense tenant assignat), les crides dependents de `tenantId` retornen `[]` directament sense crida a l'API.

---

## 6. Refresc automàtic

| Situació | Refresc |
|----------|---------|
| Onboarding actiu (client) | Cada 30s (s'actualitzen els comptadors per detectar quan s'ha creat alguna cosa) |
| Dashboard admin normal | Cap refresc automàtic (dades suficientment estables) |
| Dashboard client normal | Cap refresc automàtic |

> **Pendent futur:** Afegir un botó de refresc manual visible al greeter per a admins.

---

## 7. Navegació des del dashboard

### 7.1 SUPER_ADMIN

| Element | Destí |
|---------|-------|
| KPI Tenants | `/portal/admin/tenants` |
| KPI Leads | `/portal/leads` |
| KPI Serveis | `/portal/ops` |
| KPI Infraestructura | `/portal/admin/infraops` |
| Pipeline → VEURE TOT | `/portal/leads` |
| Recursos → DETALL | `/portal/admin/infraops` |
| Botó PROCÉS COMPLET | `/portal/process` |
| Botó NOU LEAD | `/portal/leads/new` |
| Botó TENANTS | `/portal/admin/tenants` |
| Botó API KEYS | `/portal/admin/config` |
| Botó BACKUP | `/portal/admin/backup` |

### 7.2 ADMIN

| Element | Destí |
|---------|-------|
| KPI Leads | `/portal/leads` |
| KPI Serveis | `/portal/ops` |
| Pipeline → VEURE TOT | `/portal/leads` |
| Botó PROCÉS COMPLET | `/portal/process` |
| Botó NOU LEAD | `/portal/leads/new` |

### 7.3 CLIENT

| Element | Destí |
|---------|-------|
| Factures → VEURE TOTES | `/portal/finops` |
| Botó descàrrega factura | `invoicePdfUrl` (nova pestanya) |
| Botó CONTACTE | `mailto:hola@amgdigital.com` |

---

## 8. Millores futures (backlog)

Ordenades per impacte estimat:

1. **Activitat recent (admin):** Llista de les darreres 5 accions del sistema (leads creats, pressupostos enviats, pagaments rebuts) al lateral dret del dashboard admin.

2. **Finançament global (admin):** Ampliar el dashboard admin amb 4 KPIs financeres globals via `getGlobalFinOpsDashboard()`: total facturat, total cobrat, total pendent, total vençut.

3. **Comparativa mensual (admin):** A cada KPI afegir `+N vs mes anterior` en verd/vermell.

4. **Alerta de configuració (super_admin):** Si hi ha API keys MISSING (via `getSystemConfig()`), mostrar un banner "X serveis sense configurar" amb link a `/portal/admin/config`.

5. **Agents actius (client):** Afegir comptador d'agents IA actius a la columna 4 del hero.

6. **Refresc manual:** Botó de recàrrega a la barra del greeter.

7. **Estat de pagaments (client):** Afegir una secció de pagaments pendents (Stripe) si el client en té.

8. **Widget d'onboarding persistent (client):** Mostrar progrés d'onboarding com a barra a la part superior fins que estigui completat del tot (no substituir el dashboard sencer).

9. **Dark mode toggle:** Selector clar/fosc accessible des del dashboard (ara tota l'app és sempre fosca).

---

## 9. Casos de prova (QA)

### QA-01 — Vista per rol
- [ ] `superadmin@test.com` veu les 4 KPIs (Tenants / Leads / Serveis / Infraestructura)
- [ ] `admin@test.com` veu KPIs de Leads i Serveis (sense Tenants ni Infraestructura)
- [ ] `client@test.com` veu el hero billing i les factures (sense KPIs de negoci)

### QA-02 — Càrrega de dades
- [ ] Dashboard admin carrega en < 3s amb totes les APIs disponibles
- [ ] Si infraops retorna 503, el dashboard segueix funcionant (mostra `—` a la KPI)
- [ ] Si leads retorna error, el pipeline mostra "Sense dades"

### QA-03 — Navegació
- [ ] Clicar la KPI de Tenants porta a `/portal/admin/tenants`
- [ ] El botó PROCÉS COMPLET porta a `/portal/process`
- [ ] El botó VEURE TOTES de factures porta a `/portal/finops`
- [ ] La icona de descàrrega obre la URL del PDF en nova pestanya
- [ ] La icona de descàrrega és inactiva quan `invoicePdfUrl` és null

### QA-04 — Client sense dades
- [ ] Client sense factures veu "Cap factura encara"
- [ ] Client sense landings ni workflows veu les KPIs a `0`
- [ ] Client sense `tenantId` no genera cap crida a l'API (no dóna error 404)

### QA-05 — Onboarding
- [ ] Client nou (sense landings, workflows ni factures) veu l'onboarding en lloc del dashboard normal
- [ ] Clicar "Salta" amaga l'onboarding i guarda a localStorage
- [ ] Tornar a carregar la pàgina no mostra l'onboarding (ha quedat guardat)
- [ ] Client que ja té landings NO veu l'onboarding

### QA-06 — Mòbil (< 640px)
- [ ] Les 4 KPIs es distribueixen en 2 columnes (no en 1)
- [ ] Pipeline i Recursos s'apilen verticalment (una columna)
- [ ] Els botons d'accions ràpides es reencadenan en diverses files
- [ ] El hero billing es distribueix en 2 columnes × 2 files

---

## 10. Historial de canvis

| Versió | Data | Canvi |
|--------|------|-------|
| 1.0 | 2026-05-19 | Spec inicial. Dashboard diferenciat per rol: admin (KPIs negoci + pipeline + servidor) vs client (billing hero + factures). |
