# Mòdul 07: Billing — Pressupostos i Descomptes

> **Versió:** 1.1
> **Data:** 2026-05-15
> **Dependències:** Mòdul 01 (Auth), Mòdul 02 (Vault) — inclou flux d'intake, selecció de fases a pressupostar, i transició d'estats en acceptar pressupost

---

## 1. Objectius

- Generar pressupostos automàtics a partir de perfils de servei i add-ons
- Aplicar descomptes (percentatge o fix) a pressupostos
- Gestionar el cicle de vida del pressupost: esborrany → enviat → acceptat/rebutjat/cancel·lat
- Acceptar/rebutjar pressupostos públicament via token (sense autenticació)
- Dashboard de facturació per tenant (pendents, últim pressupost, total gastat)

---

## 2. Abast

### 2.1 Funcionalitats incloses

- Creació de pressupostos a partir d'un perfil de servei + fases seleccionades + add-ons
- Les fases NO seleccionades per pressupostar es marquen com a `FUTURE_EXPANSION` (ampliació futura)
- En acceptar el pressupost, les fases/serveis seleccionats passen a `ACCEPTED` / `BUDGET_ACCEPTED` al Vault
- Càlcul de subtotal, descomptes i total
- CRUD de descomptes (percentatge o fix) amb dates de validesa
- Aplicació de descomptes al pressupost
- Enviament de pressupost (genera token d'acceptació/rebutjament)
- Acceptació i rebutjament públic via token (sense JWT)
- Cancel·lació de pressupostos (DRAFT o SENT)
- Dashboard de facturació per tenant
- Integració amb Vault (confirm-phase en acceptar pressupost)

### 2.2 Funcionalitats excloses

- Facturació real i pagaments (Stripe — Mòdul 09 Payments, Holded — Mòdul 08 FinOps)
- Enviament d'email al client (futur)
- Pressupostos recurrents de setup (cada client rep un sol pressupost de setup per perfil)

> **Nota:** La facturació recurrent mensual NO és un pressupost — és un procés separat que genera factures a Holded a final de mes. Veure secció 3.3 i Mòdul 08 FinOps.

### 2.3 Actors

| Actor | Descripció | Permisos |
|-------|-----------|----------|
| SUPER_ADMIN | Opera tota la plataforma | CRUD pressupostos i descomptes, eliminar descomptes |
| ADMIN | Personal operatiu | Crear/editar pressupostos i descomptes, enviar. NO pot eliminar descomptes |
| CLIENT | Usuari final | Veure pressupostos del seu tenant, dashboard. Acceptar/rebutjar via token |
| Visitant | Públic (token) | Acceptar/rebutjar pressupost via token sense autenticació |

---

## 3. Model de dades

### 3.1 Entitats (PostgreSQL)

#### Budget (Pressupost)

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | FK lògica a Tenant |
| budgetNumber | String(20) | @Column | Número humà (BUD-2026-0001) |
| profileId | UUID | @Column | FK lògica a ServiceProfile |
| version | Integer | @Column | Incrementa en cada actualització |
| status | Enum(STRING) | @Enumerated @Builder.Default | `DRAFT` |
| subtotal | BigDecimal(10,2) | @Column(nullable=false) | Suma de serveis |
| discountTotal | BigDecimal(10,2) | @Column(nullable=false) | Suma descomptes aplicats |
| total | BigDecimal(10,2) | @Column(nullable=false) | subtotal - discountTotal |
| validUntil | LocalDate | @Column | Per defecte 30 dies des de creació |
| notes | TEXT | @Column(columnDefinition="TEXT") | Notes internes |
| clientNotes | TEXT | @Column(columnDefinition="TEXT") | Visibles per al client |
| acceptanceToken | String(64) | @Column | Token d'un sol ús (en enviar) |
| sentAt | Instant | @Column | |
| acceptedAt | Instant | @Column | |
| rejectedAt | Instant | @Column | |
| rejectedReason | String(255) | @Column | |
| createdAt | Instant | @CreatedDate | |
| updatedAt | Instant | @LastModifiedDate | |

**Índexs:** (tenantId, status), (tenantId, budgetNumber)

**BudgetStatus enum:** `DRAFT`, `SENT`, `ACCEPTED`, `REJECTED`, `EXPIRED`, `CANCELLED`

#### BudgetLine (Línia de pressupost)

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| budgetId | UUID | @Column(nullable=false) | FK a Budget |
| phaseId | UUID | @Column | FK lògica a Phase (null si add-on) |
| serviceId | UUID | @Column(nullable=false) | FK lògica a CatalogService |
| serviceName | String(100) | @Column | Denormalitzat del catàleg |
| quantity | Integer | @Builder.Default | 1 |
| unitPrice | BigDecimal(10,2) | @Column(nullable=false) | Preu en creació (snapshot) |
| total | BigDecimal(10,2) | @Column(nullable=false) | quantity * unitPrice |
| sortOrder | Integer | @Column | Per ordenació visual |

#### Discount (Descompte)

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column | Tenant al qual aplica |
| type | Enum(STRING) | @Enumerated | `PERCENTAGE`, `FIXED` |
| value | BigDecimal(10,2) | @Column(name="discount_value") | Valor (percentatge o import fix) |
| appliesTo | Enum(STRING) | @Enumerated | `BUDGET`, `PHASE`, `SERVICE` |
| referenceId | UUID | @Column | ID contextual (fase/servei) |
| label | String(100) | @Column | Nom visible |
| isActive | Boolean | @Column @Builder.Default | true |
| validFrom | LocalDate | @Column | Inici de validesa |
| validUntil | LocalDate | @Column | Fi de validesa |
| maxApplications | Integer | @Column | Límit d'usos |
| appliedCount | Integer | @Builder.Default | 0 |
| createdBy | UUID | @Column(nullable=false) | Qui va crear el descompte |
| createdAt | Instant | @CreatedDate | |

**DiscountType enum:** `PERCENTAGE`, `FIXED`

**DiscountAppliesTo enum:** `BUDGET`, `PHASE`, `SERVICE`

### 3.3 Facturació mensual recurrent

La facturació mensual s'origina al Mòdul 08 (FinOps) via un job programat. El Billing aporta la lògica de càlcul:

**Font de dades:** `TenantService.monthlyPriceLocked` per a cada servei en estat `IMPLEMENTATION_ACCEPTED`.

**Pro-rata del primer mes:**
```
import = monthlyPriceLocked × (dies_restants_al_mes / dies_totals_al_mes)
```
- Dies restants = dies des d'`activatedAt` fins a final de mes (inclòs)
- A partir del segon mes: import complet

**Exemple:** Client activa Landing Pro el 16 de maig (monthlyPriceLocked = 10€)
- Primera factura (31 de maig): 10 € × 16/31 = **5.16 €**
- A partir de juny: **10 €/mes**

**Agrupació per tenant:** Una sola factura mensual per tenant amb totes les línies de servei. No una factura per servei.

**Format de la factura mensual a Holded:**
```
Factura F-MENS-2026-05-0001
Tenant: Restaurant Can Pedro
Període: 01/05/2026 – 31/05/2026

Línies:
  Landing Pro         10.00 € × (16/31 dies) =  5.16 €
  WhatsApp Business   10.00 € × (16/31 dies) =  5.16 €
  ─────────────────────────────────────────────────────
  Total                                         10.32 €
```

---

### 3.2 Càlcul de pressupost

1. Es carrega el perfil de servei (ServiceProfile) amb totes les seves fases i serveis
2. Es calcula `subtotal = SUM(salePrice)` de tots els serveis del perfil
3. S'afegeixen els add-ons sol·licitats: `subtotal += SUM(salePrice)`
4. S'apliquen descomptes: `discountTotal = SUM(descomptes)`
5. `total = subtotal - discountTotal`

**Snapshot de preus:** Els preus (`unitPrice`, `serviceName`) es copien al BudgetLine en crear el pressupost. Canvis futurs al catàleg no afecten pressupostos existents.

---

## 4. API REST

Prefix base: `/api/v1/billing`

### 4.1 Pressupostos

| Mètode | Ruta | Descripció | Rols |
|--------|------|-----------|------|
| POST | /api/v1/billing/tenants/{tenantId}/budgets | Crear pressupost | SUPER_ADMIN, ADMIN |
| GET | /api/v1/billing/tenants/{tenantId}/budgets | Llistar pressupostos | Tots autenticats |
| GET | /api/v1/billing/tenants/{tenantId}/budgets/{id} | Veure pressupost | Tots autenticats |
| PUT | /api/v1/billing/budgets/{id} | Actualitzar (només DRAFT) | SUPER_ADMIN, ADMIN |
| DELETE | /api/v1/billing/budgets/{id} | Cancel·lar | SUPER_ADMIN |
| POST | /api/v1/billing/budgets/{id}/send | Enviar (genera token) | SUPER_ADMIN, ADMIN |
| POST | /api/v1/billing/budgets/accept?token= | Acceptar (públic) | Públic (token) |
| POST | /api/v1/billing/budgets/reject?token= | Rebutjar (públic) | Públic (token) |

### 4.2 Descomptes

| Mètode | Ruta | Descripció | Rols |
|--------|------|-----------|------|
| POST | /api/v1/billing/discounts | Crear descompte | SUPER_ADMIN, ADMIN |
| GET | /api/v1/billing/discounts | Llistar descomptes | SUPER_ADMIN, ADMIN |
| PUT | /api/v1/billing/discounts/{id} | Actualitzar | SUPER_ADMIN |
| DELETE | /api/v1/billing/discounts/{id} | Desactivar | SUPER_ADMIN |

### 4.3 Dashboard

| Mètode | Ruta | Descripció | Rols |
|--------|------|-----------|------|
| GET | /api/v1/billing/tenants/{tenantId}/dashboard | Dashboard facturació | Tots autenticats |

### 4.4 Detall d'endpoints

#### `POST /api/v1/billing/tenants/{tenantId}/budgets` — Crear pressupost

Crea un pressupost per a les **fases seleccionades** d'un perfil (més add-ons opcionals). Les fases NO incloses a `phaseIds` es marquen automàticament com a `FUTURE_EXPANSION` al Vault.

Request:
```json
{
  "profileId": "uuid",
  "phaseIds": ["uuid1", "uuid2"],
  "addonIds": ["uuid3"],
  "notes": "Pressupost inicial",
  "clientNotes": "Oferta vàlida 30 dies",
  "discountIds": ["uuid"],
  "validUntil": "2026-06-13"
}
```

**Comportament:**
- `phaseIds` buit = pressupostar TOTES les fases del perfil
- `phaseIds` amb fases concretes = només aquestes fases es pressuposten
- Les fases del perfil NO incloses a `phaseIds` es marquen com a `FUTURE_EXPANSION` al Vault
- `subtotal` = suma de `salePrice` dels serveis de les fases seleccionades + add-ons

Response 201: BudgetResponse

**Errors:**
| Codi | Situació |
|------|---------|
| 400 | Perfil no trobat, add-on no existeix |

#### `POST /api/v1/billing/budgets/{id}/send` — Enviar pressupost

Genera un token d'un sol ús i canvia l'estat a SENT.

Response 200:
```json
{
  "id": "uuid",
  "status": "SENT",
  "sentAt": "2026-05-13T10:00:00Z",
  "acceptanceUrl": "https://portal.amg.cat/accept-budget?token=abc123..."
}
```

#### `POST /api/v1/billing/budgets/accept?token=abc123` — Acceptar públicament

Canvia l'estat a ACCEPTED, invalida el token.

**Efectes al Vault:**
- Totes les `TenantProfile.phaseStatus` de les fases pressupostades → `BUDGET_ACCEPTED`
- Tots els `TenantService.configStatus` dels serveis pressupostats → `ACCEPTED`
- Les fases `FUTURE_EXPANSION` no es modifiquen
- La primera fase amb `BUDGET_ACCEPTED` passa a `CONFIGURING` (s'inicia la implementació)
- El primer servei de la fase passa a `CONFIGURING`

Response 200: `{ "status": "ACCEPTED", "message": "Pressupost acceptat correctament" }`

### 4.5 BudgetResponse

```json
{
  "id": "uuid",
  "budgetNumber": "BUD-2026-0001",
  "status": "DRAFT",
  "phases": [
    {
      "name": "Configuració bàsica",
      "sortOrder": 1,
      "lines": [
        { "serviceName": "WhatsApp Business", "unitPrice": 50.00, "total": 50.00 },
        { "serviceName": "SMTP Corporatiu", "unitPrice": 30.00, "total": 30.00 }
      ],
      "phaseTotal": 80.00
    }
  ],
  "addons": [
    { "serviceName": "Landing extra", "unitPrice": 80.00 }
  ],
  "subtotal": 160.00,
  "discountTotal": 16.00,
  "total": 144.00,
  "sentAt": null,
  "acceptedAt": null,
  "rejectedAt": null,
  "rejectionUrl": null,
  "validUntil": "2026-06-13",
  "createdAt": "2026-05-13T10:00:00Z"
}
```

---

## 5. Seguretat

### 5.1 Autenticació
- Gestió: JWT (Mòdul 01 Auth)
- Acceptació/rebutjament: Token d'un sol ús (públic, sense JWT)

### 5.2 Autorització (RBAC)
- SUPER_ADMIN: CRUD complet, eliminar descomptes
- ADMIN: Crear/editar pressupostos i descomptes. NO eliminar descomptes
- CLIENT: Veure pressupostos i dashboard del seu tenant

---

## 6. RGPD / LSSI

- **Dades personals:** Cap (els pressupostos contenen dades comercials, no personals)
- **Conservació:** Indefinida (registre financer)
- **Audit trail:** `sentAt`, `acceptedAt`, `rejectedAt` registren el cicle de vida

---

## 7. Tests (QA)

Els tests d'integració es troben a `backend/src/test/java/com/amg/digitalitzacio/billing/api/BillingControllerTest.java` (14 tests).

### 7.1 Funcionals

| # | Cas | Resultat |
|---|-----|---------|
| 1 | Crear descompte percentatge | 201 |
| 2 | Llistar descomptes | 200 |
| 3 | ADMIN pot crear descompte | 201 |
| 4 | CLIENT NO pot crear descompte | 403 |
| 5 | Actualitzar descompte | 200 |
| 6 | Eliminar (desactivar) descompte | 204 |
| 7 | ADMIN NO pot eliminar descompte | 403 |
| 8 | Crear pressupost amb perfil vàlid | 201 |
| 9 | CLIENT NO pot crear pressupost | 403 |
| 10 | Llistar pressupostos | 200 |
| 11 | Cancel·lar pressupost DRAFT | 204, status CANCELLED |
| 12 | Dashboard amb pressupostos pendents | 200, pendingBudgets > 0 |
| 13 | CLIENT NO pot cancel·lar pressupost | 403 |
| 14 | Accés sense JWT | 401 |

---

## 8. Dependències

| Mòdul | Dependència | Tipus |
|-------|-----------|-------|
| Mòdul 01 (Auth) | Autenticació JWT + RBAC | Forta |
| Mòdul 02 (Vault) | ServiceProfile, confirm-phase endpoint | Forta |

---

## 10. Programes comercials

> **Versió:** 2.0 — extensió del Mòdul 07

### 10.1 Visió general

Tres mecàniques comercials per accelerar la captació i fidelització de clients. Totes s'integren amb el sistema de descomptes existent ampliant-lo amb nous tipus i entitats.

---

### 10.2 Extensions a l'entitat Discount

Nous camps a afegir:

| Camp | Tipus | Descripció |
|------|-------|-----------|
| `program` | Enum(STRING) | `MANUAL` \| `EARLY_ADOPTER` \| `ANNUAL_CONTRACT` \| `REFERRAL` |
| `appliesToSetup` | Boolean | S'aplica al setup únic (default true) |
| `appliesToMonthly` | Boolean | S'aplica a la quota mensual recurrent (default false) |
| `isLifetime` | Boolean | El descompte no caduca mai (early adopter mensual) |
| `commitmentMonths` | Integer | Mesos de permanència mínima (0 = sense compromís) |

**DiscountProgram enum:** `MANUAL`, `EARLY_ADOPTER`, `ANNUAL_CONTRACT`, `REFERRAL`

---

### 10.3 Entitat EarlyAdopterProgram

Configuració global del programa early adopter (una sola fila a la BD).

| Camp | Tipus | Descripció |
|------|-------|-----------|
| `id` | UUID | PK |
| `maxSlots` | Integer | Màxim de clients early adopter (ex: 20) |
| `usedSlots` | Integer | Quants slots s'han usat |
| `setupDiscountPct` | BigDecimal | % de descompte al setup (100 = gratuït) |
| `monthlyDiscountPct` | BigDecimal | % de descompte mensual de per vida (ex: 30) |
| `commitmentMonths` | Integer | Permanència mínima (ex: 6) |
| `active` | Boolean | Si el programa està obert |
| `updatedAt` | Instant | |

**Regla:** Quan `usedSlots >= maxSlots`, el programa tanca automàticament (`active = false`).

---

### 10.4 Entitat ReferralCode

| Camp | Tipus | Descripció |
|------|-------|-----------|
| `id` | UUID | PK |
| `ownerTenantId` | UUID | Tenant que posseeix el codi (el referidor) |
| `code` | String(20) | Únic, generat automàticament (ex: `AMG-PEDRO-7X2K`) |
| `usedByTenantId` | UUID | Tenant que ha usat el codi (null si encara no usat) |
| `usedAt` | Instant | Quan s'ha aplicat |
| `referrerCreditMonths` | Integer | Mesos gratuïts per al referidor (default: 1) |
| `referredSetupFree` | Boolean | Si el referit té setup gratuït (default: true) |
| `creditApplied` | Boolean | Si el crèdit del referidor ja s'ha aplicat (default: false) |
| `createdAt` | Instant | |

**Índex:** `code` (unique), `ownerTenantId`

---

### 10.5 Mecàniques detallades

#### Early Adopter
- Setup gratuït (100% descompte sobre setup)
- 30% de descompte sobre la quota mensual, de per vida (`isLifetime = true`)
- Compromís mínim de 6 mesos (`commitmentMonths = 6`)
- Disponible per als primers N clients (`EarlyAdopterProgram.maxSlots`)
- En aplicar: `usedSlots++`, es creen dos `Discount` automàticament (setup + mensual)

#### Contracte Anual
- Setup gratuït (100% descompte sobre setup)
- Quota mensual al preu normal (sense descompte mensual)
- Compromís de 12 mesos (`commitmentMonths = 12`)
- Sense límit de slots

#### Programa de Referits
- El tenant referidor rep un `ReferralCode` únic en registrar-se
- Quan un nou client usa el codi: setup gratuït per al referit
- En activar el primer servei del referit: el referidor obté 1 mes gratuït (`creditApplied = true`)
- El crèdit del referidor es desconta de la factura mensual de Holded (Mòdul 08)
- Sense límit de referits per tenant

---

### 10.6 API REST — Programes comercials

Prefix: `/api/v1/billing/programs`

| Mètode | Ruta | Descripció | Rols |
|--------|------|-----------|------|
| GET | /programs/early-adopter | Estat del programa (slots disponibles) | SUPER_ADMIN |
| PUT | /programs/early-adopter | Configurar programa | SUPER_ADMIN |
| POST | /programs/early-adopter/apply/{tenantId} | Aplicar al tenant | SUPER_ADMIN, ADMIN |
| GET | /programs/annual-contract/apply/{tenantId} | Aplicar contracte anual | SUPER_ADMIN, ADMIN |
| GET | /programs/referrals | Llistar tots els referits | SUPER_ADMIN |
| GET | /programs/referrals/{tenantId} | Codi i historial del tenant | Autenticat (propi tenant) |
| POST | /programs/referrals/apply | Aplicar codi referit a un pressupost | SUPER_ADMIN, ADMIN |

#### `GET /programs/early-adopter` — Estat del programa

Response:
```json
{
  "active": true,
  "maxSlots": 20,
  "usedSlots": 7,
  "availableSlots": 13,
  "setupDiscountPct": 100,
  "monthlyDiscountPct": 30,
  "commitmentMonths": 6
}
```

#### `POST /programs/referrals/apply` — Aplicar codi referit

Request:
```json
{
  "code": "AMG-PEDRO-7X2K",
  "newTenantId": "uuid"
}
```

Response 200: `{ "referrerTenantId": "uuid", "setupFree": true, "referrerCreditMonths": 1 }`

Errors: 404 si codi no existeix · 409 si codi ja usat

---

### 10.7 Frontend — `/portal/billing/programs`

Accessible només per SUPER_ADMIN. Tres pestanyes:

**Early Adopter**
- Barra de progrés de slots (7/20 usats)
- Configuració editable (max slots, %)
- Botó "Aplicar a tenant" (cerca per nom/email)

**Contracte Anual**
- Botó "Aplicar a tenant"
- Llista de tenants amb contracte anual actiu + data de venciment

**Referits**
- Taula de tots els codis: referidor → referit → estat crèdit
- Filtre per estat (actius, usats, crèdit pendent)

---

### 10.8 Casos QA — Programes comercials

| # | Cas | Resultat esperat |
|---|-----|-----------------|
| P-01 | Aplicar early adopter a tenant | Dos descomptes creats (setup + mensual lifetime), `usedSlots++` |
| P-02 | Aplicar quan no hi ha slots | 409 Conflict "No hi ha slots disponibles" |
| P-03 | Generar codi referit per tenant nou | Codi únic creat automàticament en activar primer servei |
| P-04 | Aplicar codi referit vàlid | Setup gratuït al referit, crèdit pendent al referidor |
| P-05 | Aplicar codi referit ja usat | 409 Conflict "Codi ja usat" |
| P-06 | Aplicar contracte anual | Descompte setup 100%, commitmentMonths = 12 |
| P-07 | Consultar slots early adopter | Retorna disponibles = max - used |
| P-08 | CLIENT veu el seu codi referit | 200 amb codi i historial |
| P-09 | ADMIN no pot configurar early adopter | 403 |

---

## 9. Obert / Pendents

- [ ] `createdBy` a DiscountManager es queda amb UUID.randomUUID() — cal passar l'usuari real
- [ ] `appliesTo` query param a GET /discounts no s'usa al servei
- [ ] BudgetNumber usa `count()` global, no per tenant
- [ ] `InvoiceService` i `PaymentService` estan injectats però mai cridats
- [ ] `rejectionUrl` a BudgetResponse sempre és null
- [ ] `recentPhases` a DashboardResponse sempre és buit
- [ ] **[NOU]** Implementar lògica de càlcul de pro-rata mensual (secció 3.3) — consumit pel job de FinOps
- [ ] **[NOU]** `BudgetLine.unitPrice` conté el `salePrice` (setup); afegir `monthlyPrice` a la línia per mostrar el cost recurrent al pressupost
- [ ] **[NOU]** El pressupost hauria de mostrar el **total de setup** i el **total mensual recurrent estimat** com a informació addicional (no és vinculant, però orienta el client)
