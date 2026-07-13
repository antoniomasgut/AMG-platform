# Spec 58 — Content Planner & Brief Automation

> **Versió:** 1.1 (incorpora la validació contra el codi: routing de foto entrant, sub-flux propi, aïllament per itemId, Flyway V94, idioma de publicacions)
> **Data:** 2026-07-13
> **Estat:** Validat — llest per implementar
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
| `content_language` | VARCHAR(5) | **idioma de les publicacions** (`ca`/`es`/`en`/`de`); s'inicialitza amb el **default del tenant** (§3.4) i es pot canviar per pla. La IA genera els captions en aquest idioma. |
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
| `content_language` | VARCHAR(5) | opcional; sobreescriu l'idioma del pla per a aquesta publicació (bilingüe = un item per idioma) |
| `photo_deadline` | DATE | data límit de la foto (p. ex. dijous) |
| `target_publish_date` | DATE | data prevista de publicació (p. ex. divendres) |
| `status` | VARCHAR(20) | veure màquina d'estats (§4) |
| `media_url` | TEXT | foto rebuda (MinIO/S3) |
| `caption` | TEXT | text generat/aprovat |
| `brief_sent_at` / `reminder_sent_at` | TIMESTAMPTZ | control d'enviaments (guard d'idempotència) |
| `created_at` / `updated_at` | TIMESTAMPTZ | |
| | | UNIQUE(`plan_id`, `week_number`) |

### 3.3 Relació item ↔ publicacions (multi-xarxa)
Un item amb `networks = "INSTAGRAM,FACEBOOK,GOOGLE_BUSINESS"` genera **un `SocialPost` per xarxa** (comportament de `publishAsync`/`savePost`). Per tant **NO** posem un `social_post_id` únic a l'item. En comptes d'això, afegim una **referència inversa** a `SocialPost`:
- `social_post` ALTER: `ADD COLUMN content_plan_item_id UUID` (nullable, FK a `content_plan_items`).
- L'item només guarda l'**estat** (`PUBLISHED`); els posts publicats es recuperen per `SocialPost.content_plan_item_id`.

### 3.4 Idioma de les publicacions (jerarquia)
Tres nivells, amb **un default a nivell de tenant que es pot canviar quan es vulgui**:
1. **Default del tenant** → `social_meta_configs.default_content_language` (VARCHAR(5), default `ca`). És l'idioma que s'aplica a **totes** les publicacions per defecte. Es canvia des de la configuració social del tenant (§6.4).
2. **Pla** → `content_plans.content_language` s'**inicialitza** amb el default del tenant en crear el pla; es pot canviar per pla.
3. **Item** → `content_plan_items.content_language` opcional; sobreescriu el del pla (per a bilingüe: un item per idioma).

Resolució efectiva: `item.content_language ?? plan.content_language ?? tenant.default_content_language ?? 'ca'`.

### 3.5 Migració
Migració Flyway **`V94__content_planner.sql`** (última existent = V93): crea `content_plans` + `content_plan_items` + `ALTER TABLE social_post ADD COLUMN content_plan_item_id UUID` + `ALTER TABLE social_meta_configs ADD COLUMN default_content_language VARCHAR(5) DEFAULT 'ca'`. **No** es crea manualment a prod: s'aplica per Flyway (com V91/V92/V93).

---

## 4. Màquina d'estats de l'item

```
PLANNED ──(dilluns: brief enviat)──▶ PHOTO_REQUESTED
PHOTO_REQUESTED ──(dimecres, sense foto)──▶ (recordatori; segueix PHOTO_REQUESTED)
PHOTO_REQUESTED ──(tenant envia foto)──▶ PHOTO_RECEIVED
PHOTO_RECEIVED ──(IA redacta caption des del brief+pilar+idioma)──▶ AWAITING_APPROVAL
AWAITING_APPROVAL ──(tenant ✅)──▶ (publica) ──ok──▶ PUBLISHED
                                              └─error──▶ FAILED (reintentable)
AWAITING_APPROVAL ──(tenant ✍️)──▶ (regenera caption; segueix AWAITING_APPROVAL)
PHOTO_REQUESTED ──(sense foto al deadline / pla B)──▶ SKIPPED
FAILED ──(reintent manual/cron)──▶ PUBLISHED
```

> **⚠️ Comportament NOU (no reutilitza `handleStep` tal com és):** la màquina d'estats actual de `SocialPublisherOrchestrator` és `AWAIT_NETWORKS → AWAIT_TYPE → AWAIT_CAPTION → AWAIT_MEDIA → AWAIT_SCHEDULE → AWAIT_CONFIRM` (demana el caption **abans** de la foto) i la confirmació és només SÍ/NO. El planner implementa un **sub-flux propi**: xarxes+pilar+idioma ja vénen de l'item, es **genera el caption a partir de la foto rebuda**, i hi ha un estat `AWAITING_APPROVAL` amb botons ✅ / ✍️ (regeneració) que **avui no existeix**. Es **reutilitzen** els publishers i `publishAsync`/`savePost`, no el flux de passos.

---

## 5. Schedulers

| Job | Cron | Acció |
|-----|------|-------|
| **Brief setmanal** | dilluns 10:00 | Per cada pla `ACTIVE` de tenants amb toggle actiu: agafa l'item de la **setmana en curs** en estat `PLANNED`, `→ PHOTO_REQUESTED`, envia el brief, marca `brief_sent_at`. |
| **Recordatori** | dimecres 10:00 | Items `PHOTO_REQUESTED` amb `media_url IS NULL` i `reminder_sent_at IS NULL` → envia recordatori, marca `reminder_sent_at`. |
| **Publicació** | diari 08:00 | Items confirmats amb `target_publish_date <= avui` → publica (reusa publishers). Ok → `PUBLISHED`; excepció → `FAILED` (amb `error`). |
| **Reintent** | diari 08:30 | Items `FAILED` → reintenta la publicació. |

**Definicions per evitar ambigüitats (buits detectats a la validació):**
- **"Setmana en curs"**: es determina per **dates de l'item**, no per `week_number` abstracte. El brief agafa l'item `PLANNED` amb `photo_deadline` dins de la setmana ISO actual (o el més proper endavant). `week_number` (1-5) és només una etiqueta d'ordre/UI.
- **Desempat**: si hi ha diversos items elegibles la mateixa setmana, s'agafa el de `target_publish_date` més primerenc. Un tenant té **un sol pla `ACTIVE`** alhora (el del període actual); en activar-ne un de nou, l'anterior passa a `DONE`.
- **Idempotència**: els guards `brief_sent_at`/`reminder_sent_at` eviten reenviaments (mateix patró que `SocialSuggestionScheduler` + `AppointmentReminderScheduler`, amb guard addicional Redis si cal).

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
`POST .../generate` construeix N items (per defecte 4) rotant els pilars del sector del tenant, amb `brief_text` + `example_text` + dates (dijous foto / divendres publicació). Els **briefs** (instruccions al tenant) es generen en l'idioma d'ús del tenant (default `ca`); els **captions** es generaran en el `content_language` del pla (§3.1).
> Nota: `SocialContentGeneratorService.generateCaption(network, businessContext, userBrief)` **no té** paràmetre d'idioma ni de pilar avui. El planner injecta pilar + idioma dins de `userBrief`/`businessContext` (o s'amplia la signatura). La generació a partir de la **imatge** (vision) no existeix: el caption es genera del brief/pilar, no analitzant la foto.

### 6.3 Tenant (CLIENT — només el seu tenant)
Endpoints amb `tenantId` a la ruta: `@PreAuthorize("... or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")`.
**Endpoints per `itemId`/`planId` (sense `tenantId` a la ruta): l'aïllament NO es pot fer amb `@PreAuthorize`** → verificació d'**ownership a la capa de servei**: carregar l'item/pla, comprovar `item.tenantId == principal.tenantId` per a CLIENT; si no, `404`. Aplica a `PUT /items/{itemId}`, `POST /items/{itemId}/photo`, `POST /items/{itemId}/send-brief`, `GET /content-plans/{planId}`, `DELETE /content-plans/{planId}`.

| Mètode | Ruta | Acció |
|--------|------|-------|
| GET | `/api/v1/content-plans/tenants/{tenantId}` | Veure el seu pla |
| GET | `/api/v1/content-plans/tenants/{tenantId}/pending` | **Fotos que falten fer** (items `PHOTO_REQUESTED` sense foto) |
| POST/PUT | `/api/v1/content-plans/...` | Crear/editar el seu propi pla (autonomia) |
| POST | `/api/v1/content-plans/items/{itemId}/photo` | Pujar la foto de l'item (alternativa a Telegram) — ownership al servei |

### 6.4 Idioma per defecte del tenant
| Mètode | Ruta | Acció |
|--------|------|-------|
| GET | `/api/v1/content-plans/tenants/{tenantId}/default-language` | Consultar el default (`ca` si no s'ha fixat) |
| PUT | `/api/v1/content-plans/tenants/{tenantId}/default-language` | Canviar-lo (`{ "language": "ca\|es\|en\|de" }`) → desa a `social_meta_configs.default_content_language` |

Accessible a SUPER_ADMIN/ADMIN i al CLIENT del propi tenant (`#principal.tenantId == #tenantId`). Els plans creats a partir d'aquí n'hereten el valor.

---

## 7. Integració amb el Social Publisher (Spec 52)

- **Foto entrant per Telegram (⚠️ correcció crítica):** avui `TelegramWebhookController` només passa la foto a l'orchestrator si `hasDraft(chatId)` (draft Redis actiu). En el planner, el tenant envia la foto **dijous sense cap draft obert** → no arribaria mai. Cal un **nou branch al webhook**: si arriba `photoFileId` i `!hasDraft(chatId)`, fer `TenantChatLinkRepository.findByTelegramChatId(chatId) → tenantId`, buscar l'item `PHOTO_REQUESTED` de la setmana en curs i, si existeix, **iniciar el sub-flux del planner** (vincular `media_url`, generar caption des de pilar+idioma, `→ AWAITING_APPROVAL`). Si no hi ha item, es manté el comportament actual (agents).
- **Aprovació ✅ / ✍️:** és un **estat/flux nou** (la confirmació actual és només SÍ/NO). En ✅: crea un `SocialPost` **per xarxa**, publica amb els publishers actuals (`publishAsync`/`savePost`), enllaça'ls per `SocialPost.content_plan_item_id`, item `→ PUBLISHED` (o `FAILED` si peta). En ✍️: regenera el caption i segueix a `AWAITING_APPROVAL`.
- **Idioma:** el caption es genera en el `content_language` del pla/item; els briefs i recordatoris al tenant, en el seu idioma d'ús (default `ca`, ja que `Tenant` no té camp de locale).
- **Textos de brief/recordatori:** plantilles del Mòdul 43, amb els textos de `docs/flux-brief-recordatori.html`.
- **Toggle per tenant `content_planner`:** implica **ampliar el `record SocialFeatures`** (avui 5 booleans tancats) + l'endpoint `PUT /features` + el frontend de toggles + els **tests** (al mateix commit) + les **claus i18n als 4 idiomes**.

---

## 8. Frontend

### 8.1 Admin — `/portal/admin/tenants/[id]/content-plan`
- Crear pla del mes (o auto-generar amb IA) + **selector d'idioma de les publicacions** (`content_language`: ca/es/en/de), amb override per item si cal.
- Graella de setmanes amb pilar, brief editable, xarxes, dates i **estat** (PLANNED/PHOTO_REQUESTED/…/PUBLISHED/FAILED).
- Botons: generar amb IA, enviar brief ara, activar pla, reintentar (FAILED).

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
3. Tenant envia foto per Telegram **sense draft actiu** → el nou branch del webhook la vincula a l'item `PHOTO_REQUESTED` → `PHOTO_RECEIVED` → caption IA en el `content_language` → `AWAITING_APPROVAL`.
4. Tenant ✅ → es publica a **cada** xarxa de l'item (un `SocialPost` per xarxa amb `content_plan_item_id`), item `PUBLISHED`.
5. Tenant ✍️ → es regenera el caption sense publicar.
6. Publicació falla (un publisher llança excepció) → item `FAILED` amb `error`; el cron de reintent el torna a intentar.
7. CLIENT accedeix a `/pending` del seu tenant → només els seus items; d'un altre tenant → 403.
8. **Cross-tenant per `itemId`:** CLIENT fa `PUT /items/{itemId}` d'un item d'un **altre** tenant → `404` (ownership al servei).
9. Foto no arriba al deadline → item `SKIPPED` (o pla B manual).
10. `POST /generate` amb `content_language=es` → 4 items amb pilars rotats; briefs en l'idioma del tenant i captions preparats per generar-se en castellà.

---

## 10. Fora d'abast (v1)
- Generació automàtica de la **imatge** (només text; la foto la posa el tenant o banc d'imatges manual).
- Publicació programada per hora fina (n'hi ha prou amb data).
- Aprovació per part d'AMG abans del tenant (v1: el tenant confirma; AMG pot editar el pla, no cada post).
