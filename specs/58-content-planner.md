# Spec 58 — Content Planner & Brief Automation

> **Versió:** 1.0
> **Data:** 2026-07-13
> **Estat:** Proposat
> **Depèn de:** Spec 52 (Social Publisher), Spec 20 (Agents Telegram), Spec 43 (Communication Templates), Spec 55/56 (Social Engagement/Capabilities), Spec 22 (Sector Pricing — pilars per sector)

---

## 1. Objectiu

Planificar el contingut social d'un tenant i **automatitzar tot el cicle**: crear un pla mensual de publicacions, demanar al tenant la foto de cada setmana, generar el text amb IA, confirmar i publicar — amb el mínim d'intervenció d'AMG.

**Principi clau:** AMG (o el propi tenant) defineix *què* es publicarà cada setmana; el sistema envia el **brief**, recorda les fotos pendents, redacta i publica. La feina del tenant es redueix a **enviar la foto i confirmar**.

**Dos rols d'ús:**
- **AMG (SUPER_ADMIN/ADMIN):** crea i gestiona el pla d'un tenant (servei gestionat).
- **Tenant (CLIENT):** veu el seu pla, les fotos que falten fer, i pot crear/editar el pla ell mateix (autonomia, Via D).

Reutilitza al màxim el que ja existeix: `SocialPublisherOrchestrator` (flux Telegram foto→caption→confirm→publica), `SocialContentGeneratorService` (IA), els publishers (IG/FB/Google), `SocialSuggestionScheduler` (patró de cron dilluns) i les plantilles de comunicació (Mòdul 43).

---

## 2. Model de contingut per pilars

Cada publicació té un **pilar** (tema recurrent). Es roten cada mes i s'adapten a la temporada. Set per defecte (configurable per sector):

| Pilar | Descripció | Brief tipus (què fotografiar) |
|-------|-----------|-------------------------------|
| `NOVELTY` | Novetat / producte nou | Peça/producte nou, llum natural, horitzontal + vertical |
| `COMBINE` | Com combinar-ho / ús | Conjunt o ús del producte (2-3 elements) |
| `SHOP` | La botiga / la persona | El local, l'aparador o el propietari |
| `SOCIAL_PROOF` | Prova social / temporada | Client content (amb permís) o selecció de temporada |

Els pilars i briefs per sector es poden **auto-generar amb IA** (secció 6.2). El catàleg de pilars viu a codi (enum + textos per sector), no en BD.

---

## 3. Model de dades

### 3.1 `content_plans`
| Camp | Tipus | Nota |
|------|-------|------|
| `id` | UUID PK | |
| `tenant_id` | UUID | |
| `period` | VARCHAR(7) | `YYYY-MM` (mes del pla) |
| `status` | VARCHAR(20) | `DRAFT` / `ACTIVE` / `DONE` |
| `created_by` | UUID | usuari que el crea (AMG o tenant) |
| `notes` | TEXT | |
| `created_at` / `updated_at` | TIMESTAMPTZ | |
| | | UNIQUE(`tenant_id`, `period`) |

### 3.2 `content_plan_items` (una per publicació planificada)
| Camp | Tipus | Nota |
|------|-------|------|
| `id` | UUID PK | |
| `plan_id` | UUID | FK content_plans |
| `tenant_id` | UUID | desnormalitzat per a queries |
| `week_number` | INTEGER | 1-5 dins el mes |
| `pillar` | VARCHAR(20) | enum (NOVELTY…) |
| `brief_text` | TEXT | què ha de fotografiar el tenant |
| `example_text` | TEXT | exemple aclaridor |
| `networks` | VARCHAR(100) | CSV: `INSTAGRAM,FACEBOOK,GOOGLE_BUSINESS` |
| `photo_deadline` | DATE | data límit de la foto (p. ex. dijous) |
| `target_publish_date` | DATE | data prevista de publicació (p. ex. divendres) |
| `status` | VARCHAR(20) | veure màquina d'estats (§4) |
| `media_url` | TEXT | foto rebuda (MinIO/S3) |
| `caption` | TEXT | text generat/aprovat |
| `social_post_id` | UUID | FK SocialPost quan es publica |
| `brief_sent_at` / `reminder_sent_at` | TIMESTAMPTZ | control d'enviaments |
| `created_at` / `updated_at` | TIMESTAMPTZ | |

> **Producció:** taules noves → crear manualment a prod (`ddl-auto: validate`).

---

## 4. Màquina d'estats de l'item

```
PLANNED ──(dilluns: brief enviat)──▶ PHOTO_REQUESTED
PHOTO_REQUESTED ──(dimecres, sense foto)──▶ (recordatori; segueix PHOTO_REQUESTED)
PHOTO_REQUESTED ──(tenant envia foto)──▶ PHOTO_RECEIVED
PHOTO_RECEIVED ──(IA redacta)──▶ AWAITING_APPROVAL
AWAITING_APPROVAL ──(tenant ✅)──▶ PUBLISHED  (crea SocialPost + publica)
AWAITING_APPROVAL ──(tenant ✍️)──▶ (regenera caption; segueix AWAITING_APPROVAL)
qualsevol ──(sense foto al deadline / pla B)──▶ SKIPPED
```

---

## 5. Schedulers

| Job | Cron | Acció |
|-----|------|-------|
| **Brief setmanal** | dilluns 10:00 | Per cada pla `ACTIVE`: agafa l'item de la setmana en curs en estat `PLANNED` → `PHOTO_REQUESTED`, envia el brief (Telegram + opcional WhatsApp/Email via Mòdul 43), marca `brief_sent_at`. |
| **Recordatori** | dimecres 10:00 | Items `PHOTO_REQUESTED` sense `media_url` i sense `reminder_sent_at` → envia recordatori, marca `reminder_sent_at`. |
| **Publicació** | segons `target_publish_date` | Items `AWAITING_APPROVAL` ja confirmats → publica (reusa publishers) i passa a `PUBLISHED`. (Si es vol publicació immediata post-confirmació, es fa al mateix pas de confirmació.) |

Reutilitzen el patró de `SocialSuggestionScheduler` (cron + iteració de tenants amb toggle actiu) i de `AppointmentReminderScheduler`.

---

## 6. API

### 6.1 Admin (SUPER_ADMIN/ADMIN)
| Mètode | Ruta | Acció |
|--------|------|-------|
| POST | `/api/v1/content-plans/tenants/{tenantId}` | Crear pla (period). Opcional `?generate=true` → auto-genera items amb IA |
| GET | `/api/v1/content-plans/tenants/{tenantId}` | Llista de plans del tenant |
| GET | `/api/v1/content-plans/{planId}` | Pla + items |
| POST | `/api/v1/content-plans/{planId}/generate` | (Re)genera items amb IA a partir dels pilars del sector |
| PUT | `/api/v1/content-plans/items/{itemId}` | Editar pilar/brief/xarxes/dates |
| POST | `/api/v1/content-plans/items/{itemId}/send-brief` | Enviar el brief ara (manual) |
| POST | `/api/v1/content-plans/{planId}/activate` | DRAFT → ACTIVE |
| DELETE | `/api/v1/content-plans/{planId}` | Esborrar |

### 6.2 Auto-generació amb IA
`POST .../generate` construeix, amb `SocialContentGeneratorService`, N items (per defecte 4) rotant els pilars del sector del tenant, amb `brief_text` + `example_text` + dates (dijous foto / divendres publicació), tot en català. AMG revisa i ajusta.

### 6.3 Tenant (CLIENT — només el seu tenant)
`@PreAuthorize` amb `#principal.tenantId == #tenantId` (patró d'aïllament ja usat al projecte).
| Mètode | Ruta | Acció |
|--------|------|-------|
| GET | `/api/v1/content-plans/tenants/{tenantId}` | Veure el seu pla |
| GET | `/api/v1/content-plans/tenants/{tenantId}/pending` | **Fotos que falten fer** (items `PHOTO_REQUESTED` sense foto) |
| POST/PUT | `/api/v1/content-plans/...` | Crear/editar el seu propi pla (autonomia) |
| POST | `/api/v1/content-plans/items/{itemId}/photo` | Pujar la foto de l'item (alternativa a Telegram) |

---

## 7. Integració amb el Social Publisher (Spec 52)

- **Foto entrant per Telegram:** quan el tenant envia una foto, `SocialPublisherOrchestrator` comprova si hi ha un item `PHOTO_REQUESTED` de la setmana en curs. Si n'hi ha, hi vincula la foto (`media_url`), genera el caption amb el **context del pilar** (millor qualitat) i passa a `AWAITING_APPROVAL`. Si no, segueix el flux lliure actual.
- **Confirmació ✅ / ✍️:** reutilitza la confirmació existent; en aprovar, crea el `SocialPost`, publica (publishers actuals) i marca l'item `PUBLISHED` + `social_post_id`.
- **Textos de brief/recordatori:** plantilles del Mòdul 43 (per idioma), amb els textos del document intern `docs/flux-brief-recordatori.html`.
- **Toggle per tenant:** `content_planner` a `SocialFeatureService`.

---

## 8. Frontend

### 8.1 Admin — `/portal/admin/tenants/[id]/content-plan`
- Crear pla del mes (o auto-generar amb IA).
- Graella de setmanes amb pilar, brief editable, xarxes, dates i **estat** (PLANNED/PHOTO_REQUESTED/…/PUBLISHED).
- Botons: generar amb IA, enviar brief ara, activar pla.

### 8.2 Tenant — `/portal/content-plan` (o dins Social)
- **"Aquesta setmana toca":** l'item actiu amb el brief i el botó de pujar foto.
- **Fotos pendents** (llista de `/pending`).
- Vista del pla del mes i de les publicacions ja fetes.
- (Autonomia) crear/editar el seu propi pla.

Tot als 4 idiomes (ca/es/en/de).

---

## 9. Casos QA
1. Pla `ACTIVE` amb item de la setmana `PLANNED` → dilluns arriba el brief per Telegram i l'item passa a `PHOTO_REQUESTED`.
2. Sense foto dimecres → arriba recordatori, `reminder_sent_at` marcat; no s'envia un segon recordatori.
3. Tenant envia foto per Telegram → item `PHOTO_RECEIVED` → caption IA → `AWAITING_APPROVAL`.
4. Tenant ✅ → es publica a les xarxes de l'item, es crea `SocialPost`, item `PUBLISHED` amb `social_post_id`.
5. Tenant ✍️ → es regenera el caption sense publicar.
6. CLIENT accedeix a `/pending` del seu tenant → veu només els seus items pendents; d'un altre tenant → 403.
7. Foto no arriba al deadline → item `SKIPPED` (o pla B manual).
8. `POST /generate` crea 4 items amb pilars rotats i briefs en català coherents amb el sector.

---

## 10. Fora d'abast (v1)
- Generació automàtica de la **imatge** (només text; la foto la posa el tenant o banc d'imatges manual).
- Publicació programada per hora fina (n'hi ha prou amb data).
- Aprovació per part d'AMG abans del tenant (v1: el tenant confirma; AMG pot editar el pla, no cada post).
