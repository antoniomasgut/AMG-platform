# Mòdul 07: Billing — Pressupostos, Mensualitats i Descomptes

> **Versió:** 1.0
> **Data:** 2026-05-12
> **Dependències:** Mòdul 01 (Auth), Mòdul 02 (Vault)

---

## 1. Objectius

- Formalitzar el **pressupost** que el Vault calcula (per fase/servei) en un document oficial enviable al client
- Gestionar el cicle de vida del pressupost: esborrany → enviat → acceptat → rebutjat → expirat
- Gestionar **mensualitats recurrents** (pla Bàsic 29€, Intermedi 49€, Avançat 99€ + serveis amb quota mensual)
- Aplicar **descomptos** percentuals o fixos per fase, servei, perfil o globals
- Connectar amb Mòdul 08 (FinOps / Holded) per a facturació Verifactu i Mòdul 09 (Stripe) per a cobrament
- **No reemplaça** els stubs del Vault — coexisteixen fins als mòduls 08/09

---

## 2. Abast

### 2.1 Funcionalitats incloses

- CRUD de pressupostos formals (basats en perfils+addons del Vault)
- Càlcul automàtic: total = SUM(salePrice) dels serveis seleccionats + descomptes
- PDF del pressupost (generació server-side)
- Enviament automàtic per email al client
- Acceptació/rebuig per part del client (via link segur + token)
- Expiració automàtica (per defecte 30 dies)
- Revisións: nou número de versió cada cop que es modifica
- CRUD de mensualitats (subscripció) — serveis amb quota mensual
- CRUD de descomptes — aplicables a pressupostos i/o subscripcions
- Dashboard de billing per al client: pressupostos pendents, històric, subscripció activa

### 2.2 Funcionalitats excloses

- Facturació real amb Verifactu (Mòdul 08 FinOps)
- Cobrament real amb Stripe (Mòdul 09 Payments)
- Conciliació bancària (Mòdul 09)
- Comptabilitat (Mòdul 08)

### 2.3 Actors

| Actor | Descripció | Permisos |
|-------|-----------|----------|
| SUPER_ADMIN | Propietari de la plataforma. | CRUD complet de pressupostos, subscripcions i descomptes. Veu costos i marges. |
| ADMIN | Personal operatiu. | Crea i envia pressupostos, aplica descomptes, veu subscripcions. NO pot eliminar pressupostos ni modificar preus base. |
| CLIENT | Usuari final del tenant. | Veure els seus pressupostos (pendents + històric), acceptar/rebutjar, veure subscripció activa. |

---

## 3. Model de dades

### 3.1 Entitats (PostgreSQL)

#### Budget (Pressupost)

Pressupost formal enviat al client. Es genera a partir d'un perfil del Vault + add-ons seleccionats.

| Camp | Tipus | Descripció |
|------|-------|------------|
| id | UUID | PK |
| tenantId | UUID | FK a Tenant |
| budgetNumber | String(20) | Número de pressupost (ex: `BUD-2026-0001`) |
| profileId | UUID | FK a ServiceProfile (pot ser null si és custom) |
| version | Integer | Versió (1, 2, 3...) |
| status | Enum | `DRAFT`, `SENT`, `ACCEPTED`, `REJECTED`, `EXPIRED`, `CANCELLED` |
| subtotal | BigDecimal(10,2) | Suma de preus de venda sense descompte |
| discountTotal | BigDecimal(10,2) | Total descomptes aplicats |
| total | BigDecimal(10,2) | Subtotal - discountTotal |
| validUntil | LocalDate | Data de validesa (per defecte 30 dies des de la creació) |
| notes | Text | Notes internes (només ADMIN/SUPER_ADMIN) |
| clientNotes | Text | Notes visibles al client (ex: "Descompte per fidelitat") |
| acceptanceToken | String(64) | Token únic per acceptar/rebutjar (generat en enviar) |
| sentAt | Instant | Quan es va enviar al client |
| acceptedAt | Instant | Quan el client va acceptar |
| rejectedAt | Instant | Quan el client va rebutjar |
| rejectedReason | String(255) | Motiu del rebuig (opcional) |
| createdAt | Instant | @CreatedDate |
| updatedAt | Instant | @LastModifiedDate |

**Índexs:** `(tenantId, status)`, `(tenantId, budgetNumber)`

#### BudgetLine (Línia de pressupost)

Cada servei del perfil (o add-on) amb el seu preu.

| Camp | Tipus | Descripció |
|------|-------|------------|
| id | UUID | PK |
| budgetId | UUID | FK a Budget |
| phaseId | UUID | FK a Phase (null si és add-on o servei solt) |
| serviceId | UUID | FK a CatalogService |
| serviceName | String(100) | Snapshot del nom del servei (per si es modifica després) |
| chargeType | Enum | `ONE_TIME` (setup) o `MONTHLY` (recurrent) |
| quantity | Integer | Per defecte 1 |
| unitPrice | BigDecimal(10,2) | Preu unitari (snapshot del salePrice en crear) |
| total | BigDecimal(10,2) | quantity * unitPrice |
| sortOrder | Integer | Ordre al pressupost |

**NOTA:** `serviceName` i `unitPrice` es guarden com a snapshot perquè el pressupost no canviï si es modifiquen els preus al catàleg després.

#### Subscription (Subscripció / Mensualitat)

Subscripció activa d'un tenant.

| Camp | Tipus | Descripció |
|------|-------|------------|
| id | UUID | PK |
| tenantId | UUID | FK a Tenant (unique) |
| planType | Enum | `BASIC` (29€), `INTERMEDIATE` (49€), `ADVANCED` (99€), `CUSTOM` |
| baseFee | BigDecimal(10,2) | Preu base del pla |
| servicesFee | BigDecimal(10,2) | Suma de preus mensuals dels serveis actius |
| totalMonthly | BigDecimal(10,2) | baseFee + servicesFee - descomptes |
| status | Enum | `ACTIVE`, `PAUSED`, `CANCELLED` |
| billingDay | Integer | Dia del mes de facturació (1-28) |
| startedAt | Instant | Inici de la subscripció |
| pausedAt | Instant | Quan es va pausar |
| cancelledAt | Instant | Quan es va cancel·lar |
| createdAt | Instant | @CreatedDate |
| updatedAt | Instant | @LastModifiedDate |

**Restricció:** Un tenant només pot tenir UNA subscripció activa alhora.
**Índex:** `tenantId` (unique)

#### SubscriptionLine (Servei dins la subscripció)

Serveis que tenen quota mensual dins la subscripció.

| Camp | Tipus | Descripció |
|------|-------|------------|
| id | UUID | PK |
| subscriptionId | UUID | FK a Subscription |
| serviceId | UUID | FK a CatalogService |
| serviceName | String(100) | Snapshot |
| monthlyPrice | BigDecimal(10,2) | Preu mensual del servei |
| isActive | Boolean | Si està actiu (false si s'ha retirat però queda historial) |
| addedAt | Instant | Quan es va afegir |
| removedAt | Instant | Quan es va retirar |

#### Discount (Descompte)

| Camp | Tipus | Descripció |
|------|-------|------------|
| id | UUID | PK |
| tenantId | UUID | FK a Tenant (nullable — pot ser global) |
| type | Enum | `PERCENTAGE` o `FIXED` |
| value | BigDecimal(10,2) | Percentatge (0-100) o import fix en EUR |
| appliesTo | Enum | `BUDGET`, `SUBSCRIPTION`, `PHASE`, `SERVICE` |
| referenceId | UUID | ID del que s'aplica (null si GLOBAL) |
| label | String(100) | Per què es va aplicar (ex: "Descompte fidelitat") |
| isActive | Boolean | Si està vigent |
| validFrom | LocalDate | Inici de validesa |
| validUntil | LocalDate | Fi de validesa (null = indefinit) |
| maxApplications | Integer | Número màxim de vegades (null = il·limitat) |
| appliedCount | Integer | Quantes vegades s'ha aplicat |
| createdBy | UUID | FK a User |
| createdAt | Instant | @CreatedDate |

**Lògica:**
- `PERCENTAGE` aplicat a un `BUDGET`: redueix el subtotal X%
- `FIXED` aplicat a un `BUDGET`: resta l'import del total
- `PERCENTAGE` aplicat a una `SUBSCRIPTION`: redueix la quota mensual X%
- `FIXED` aplicat a una `SUBSCRIPTION`: resta l'import de la quota mensual
- Si `referenceId` és null i `appliesTo` és `BUDGET`/`SUBSCRIPTION`, s'aplica al pressupost/subscripció concreta
- Si `tenantId` és null, és un descompte global (aplicable per ADMIN/SUPER_ADMIN)

### 3.2 Relacions

```
Budget
  └── BudgetLine (1:N) — serveis inclosos al pressupost

Subscription (1 per tenant)
  └── SubscriptionLine (1:N) — serveis amb quota mensual

Discount (aplicable a Budget, Subscription, Phase o Service)
```

**Flux d'acceptació:**
```
Budget DRAFT → Budget SENT (enviat al client) → Budget ACCEPTED
                                                     ↓
                                              Subscription.CREATED (si no existeix)
                                              Vault.approvePhase (per a cada fase del pressupost)
                                              InvoiceService.createInvoice (stub)
```

---

## 4. API REST

Tots els endpoints requereixen `Authorization: Bearer JWT`. Prefix base: `/api/v1/billing`.

### 4.1 Pressupostos (Quotes)

#### `POST /api/v1/billing/tenants/{tenantId}/budgets` — Crear pressupost

**Rols:** SUPER_ADMIN, ADMIN

Crea un pressupost DRAFT a partir d'un perfil del Vault + add-ons seleccionats. Consulta els preus actuals del catàleg i fa snapshot.

Request:
```json
{
  "profileId": "uuid",
  "addonIds": ["uuid1", "uuid2"],
  "notes": "Descompte especial per client nou",
  "clientNotes": "Oferta vàlida fins al 15/06/2026",
  "discountIds": ["uuid"],
  "validUntil": "2026-06-15"
}
```

Response 201:
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
        { "serviceName": "WhatsApp Business", "chargeType": "ONE_TIME", "unitPrice": 50.00, "total": 50.00 }
      ],
      "phaseTotal": 80.00
    }
  ],
  "addons": [
    { "serviceName": "Landing extra", "unitPrice": 60.00 }
  ],
  "subtotal": 140.00,
  "discountTotal": 14.00,
  "total": 126.00,
  "validUntil": "2026-06-15"
}
```

#### `GET /api/v1/billing/tenants/{tenantId}/budgets` — Llistar pressupostos

**Rols:** SUPER_ADMIN, ADMIN, CLIENT (propi)

**Query params:** `status`, `page`, `size`, `sort=createdAt,desc`

CLIENT només veu els pressupostos del seu propi tenant. ADMIN/SUPER_ADMIN veuen tots els del tenant.

#### `GET /api/v1/billing/tenants/{tenantId}/budgets/{id}` — Veure pressupost

**Rols:** SUPER_ADMIN, ADMIN, CLIENT (propi)

CLIENT veu versió pública (inclou `clientNotes`, NO `notes` internes).

#### `PUT /api/v1/billing/budgets/{id}` — Actualitzar pressupost

**Rols:** SUPER_ADMIN, ADMIN

Només si `status == DRAFT`. Si s'actualitzen línies, incrementa `version`.

#### `DELETE /api/v1/billing/budgets/{id}` — Cancel·lar pressupost

**Rols:** SUPER_ADMIN

Només si `status == DRAFT || status == SENT`. Passa a `CANCELLED`.

#### `POST /api/v1/billing/budgets/{id}/send` — Enviar pressupost al client

**Rols:** SUPER_ADMIN, ADMIN

- Canvia `status` de DRAFT a SENT
- Genera `acceptanceToken` (UUID aleatori)
- Registra `sentAt`
- Envia email al client amb link: `https://portal.amg.cat/accept-budget?token={token}`
- (Stub d'email per ara — log + retornar link)

Response 200:
```json
{
  "id": "uuid",
  "status": "SENT",
  "sentAt": "2026-05-12T10:00:00Z",
  "acceptanceUrl": "https://portal.amg.cat/accept-budget?token=abc123..."
}
```

#### `POST /api/v1/billing/budgets/accept?token={token}` — Acceptar pressupost (sense JWT)

**Rols:** Públic (token-based). Qualsevol amb el token pot acceptar.

- Valida que el token existeixi i el pressupost estigui SENT
- Canvia `status` a ACCEPTED
- Registra `acceptedAt`
- Crea/actualitza Subscription amb els serveis del pressupost
- Crida `Vault.approvePhase` per a cada fase inclosa (stub)
- Crida `InvoiceService.createInvoice` per a cada fase (stub)

Response 200:
```json
{ "status": "ACCEPTED", "message": "Pressupost acceptat correctament" }
```

#### `POST /api/v1/billing/budgets/reject?token={token}` — Rebutjar pressupost (sense JWT)

**Rols:** Públic (token-based)

Accepta body opcional amb `reason`.

Request:
```json
{ "reason": "Preu massa elevat" }
```

Response 200:
```json
{ "status": "REJECTED", "message": "Pressupost rebutjat" }
```

**NOTA:** L'acceptació/rebuig via token públic és intencionada — el client no té per què tenir sessió al portal per acceptar un pressupost. El token és un UUID aleatori d'un sol ús.

### 4.2 Subscripcions (Subscriptions)

#### `GET /api/v1/billing/tenants/{tenantId}/subscription` — Veure subscripció activa

**Rols:** SUPER_ADMIN, ADMIN, CLIENT (propi)

Response 200:
```json
{
  "id": "uuid",
  "planType": "INTERMEDIATE",
  "baseFee": 49.00,
  "servicesFee": 50.00,
  "totalMonthly": 99.00,
  "status": "ACTIVE",
  "billingDay": 15,
  "lines": [
    { "serviceName": "WhatsApp Business", "monthlyPrice": 20.00, "isActive": true },
    { "serviceName": "Landing Pro hosting", "monthlyPrice": 30.00, "isActive": true }
  ],
  "discounts": [
    { "label": "Fidelitat 10%", "type": "PERCENTAGE", "value": 10 }
  ]
}
```

#### `POST /api/v1/billing/tenants/{tenantId}/subscription` — Crear/actualitzar subscripció

**Rols:** SUPER_ADMIN, ADMIN

Request:
```json
{
  "planType": "INTERMEDIATE",
  "billingDay": 15,
  "serviceIds": ["uuid1", "uuid2"]
}
```

Si ja existeix una subscripció activa, l'actualitza. Si està PAUSED, la reactiva.

**Lògica de preus mensuals:**
- `baseFee` = preu del pla segons el `planType` (29/49/99)
- `servicesFee` = suma dels `monthlyPrice` dels serveis marcats com a MONTHLY
- `totalMonthly` = baseFee + servicesFee - descomptes

#### `POST /api/v1/billing/tenants/{tenantId}/subscription/pause` — Pausar subscripció

**Rols:** SUPER_ADMIN, ADMIN

Passa a PAUSED. NO s'eliminen les dades.

#### `POST /api/v1/billing/tenants/{tenantId}/subscription/cancel` — Cancel·lar subscripció

**Rols:** SUPER_ADMIN

Passa a CANCELLED. Es manté com a historial.

### 4.3 Descomptes (Discounts)

#### `POST /api/v1/billing/discounts` — Crear descompte

**Rols:** SUPER_ADMIN, ADMIN

Request:
```json
{
  "tenantId": "uuid",
  "type": "PERCENTAGE",
  "value": 10,
  "appliesTo": "BUDGET",
  "label": "Descompte fidelitat 10%",
  "validFrom": "2026-05-12",
  "validUntil": "2026-12-31"
}
```

#### `GET /api/v1/billing/discounts` — Llistar descomptes

**Rols:** SUPER_ADMIN, ADMIN

Query: `tenantId`, `isActive`, `appliesTo`

#### `PUT /api/v1/billing/discounts/{id}` — Actualitzar descompte

**Rols:** SUPER_ADMIN

#### `DELETE /api/v1/billing/discounts/{id}` — Eliminar descompte

**Rols:** SUPER_ADMIN

Desactivació lògica (isActive=false).

### 4.4 Dashboard CLIENT

#### `GET /api/v1/billing/tenants/{tenantId}/dashboard` — Dashboard de billing

**Rols:** SUPER_ADMIN, ADMIN, CLIENT (propi)

Retorna un resum complet per al client:

Response 200:
```json
{
  "activeSubscription": {
    "planType": "INTERMEDIATE",
    "totalMonthly": 99.00,
    "status": "ACTIVE"
  },
  "pendingBudgets": 2,
  "lastBudget": { "budgetNumber": "BUD-2026-0001", "total": 126.00, "status": "SENT", "sentAt": "..." },
  "totalSpent": 240.00,
  "nextBillingDate": "2026-06-15"
}
```

### 4.5 Mapa complet d'endpoints

| Mètode | Ruta | Descripció | Rols |
|--------|------|-----------|------|
| POST | /billing/tenants/{tId}/budgets | Crear pressupost | SUPER_ADMIN, ADMIN |
| GET | /billing/tenants/{tId}/budgets | Llistar pressupostos | Tots (propi tenant) |
| GET | /billing/tenants/{tId}/budgets/{id} | Veure pressupost | Tots (propi tenant) |
| PUT | /billing/budgets/{id} | Actualitzar pressupost | SUPER_ADMIN, ADMIN |
| DELETE | /billing/budgets/{id} | Cancel·lar pressupost | SUPER_ADMIN |
| POST | /billing/budgets/{id}/send | Enviar pressupost | SUPER_ADMIN, ADMIN |
| POST | /billing/budgets/accept | Acceptar (token) | Públic |
| POST | /billing/budgets/reject | Rebutjar (token) | Públic |
| GET | /billing/tenants/{tId}/subscription | Veure subscripció | Tots (propi tenant) |
| POST | /billing/tenants/{tId}/subscription | Crear/actualitzar subscripció | SUPER_ADMIN, ADMIN |
| POST | /billing/tenants/{tId}/subscription/pause | Pausar subscripció | SUPER_ADMIN, ADMIN |
| POST | /billing/tenants/{tId}/subscription/cancel | Cancel·lar subscripció | SUPER_ADMIN |
| POST | /billing/discounts | Crear descompte | SUPER_ADMIN, ADMIN |
| GET | /billing/discounts | Llistar descomptes | SUPER_ADMIN, ADMIN |
| PUT | /billing/discounts/{id} | Actualitzar descompte | SUPER_ADMIN |
| DELETE | /billing/discounts/{id} | Eliminar descompte | SUPER_ADMIN |
| GET | /billing/tenants/{tId}/dashboard | Dashboard billing | Tots (propi tenant) |

---

## 5. Seguretat

- **Acceptance token:** UUID aleatori. Un sol ús (un cop acceptat/rebutjat, el token expira).
- **Expiració de pressupostos:** Un `CronJob` diari passa els pressupostos SENT amb `validUntil < today` a EXPIRED.
- **Descomptes:** El SUPER_ADMIN pot crear/modificar qualsevol descompte. ADMIN pot aplicar descomptes existents a pressupostos però NO crear-ne de nous.
- **Snapshot de preus:** Els preus es copien al crear el pressupost. Canvis posteriors al catàleg no afecten pressupostos existents.

### 5.1 Regles de negoci

- Un pressupost ACCEPTAT no es pot modificar ni cancel·lar
- Un pressupost EXPIRED es pot reenviar (crea una nova versió amb `version++` i nou token)
- La subscripció es crea automàticament en acceptar el primer pressupost
- Cada vegada que s'accepta un pressupost, els serveis ONE_TIME s'afegeixen a la subscripció com a MONTHLY (si tenen quota mensual)

---

## 6. RGPD / LSSI

- Els pressupostos contenen dades de facturació del client (retenció 1 any després de l'acceptació)
- En eliminar un tenant, tots els seus pressupostos i subscripcions s'anonymitzen (no s'eliminen per requisits fiscals)
- Els tokens d'acceptació expiren en acceptar/rebutjar el pressupost

---

## 7. Tests (QA)

### 7.1 Funcionals

| # | Cas | Resultat esperat |
|---|-----|-----------------|
| 1 | Crear pressupost DRAFT a partir d'un perfil | 201, status=DRAFT, total correcte |
| 2 | Crear pressupost amb descompte | 201, total = subtotal - descompte |
| 3 | Enviar pressupost | 200, status=SENT, acceptanceToken generat |
| 4 | CLIENT veu llista de pressupostos | 200, només els seus |
| 5 | Acceptar pressupost via token | 200, status=ACCEPTED |
| 6 | Rebutjar pressupost via token | 200, status=REJECTED |
| 7 | Token d'un sol ús (re-acceptar) | 400, token ja usat |
| 8 | Pressupost expirat automàticament | CronJob passa a EXPIRED |
| 9 | Crear subscripció | 200, status=ACTIVE, totalMonthly correcte |
| 10 | Pausar subscripció | 200, status=PAUSED |
| 11 | Cancel·lar subscripció | 200, status=CANCELLED |
| 12 | Dashboard retorna resum correcte | 200, tots els camps |
| 13 | Crear descompte PERCENTAGE | 201, aplicat correctament al pressupost |
| 14 | Crear descompte FIXED | 201, aplicat correctament |

### 7.2 Seguretat

| # | Cas | Resultat esperat |
|---|-----|-----------------|
| 1 | Accés sense JWT | 401 |
| 2 | CLIENT veu pressupost d'altre tenant | 403 |
| 3 | CLIENT intenta crear pressupost | 403 |
| 4 | Acceptar amb token invàlid | 404 |
| 5 | ADMIN intenta eliminar pressupost | 403 |

---

## 8. API pública / Contractes

El Billing exporta interfícies pròpies perquè altres mòduls (08 FinOps, 09 Payments) en puguin dependre.

### 8.1 BillingService

Interfície principal per al Billing.

```java
public interface BillingService {
    BudgetResponse createBudget(UUID tenantId, CreateBudgetRequest request);
    List<BudgetSummaryResponse> listBudgets(UUID tenantId, String status, int page, int size);
    BudgetDetailResponse getBudget(UUID budgetId);
    BudgetDetailResponse updateBudget(UUID budgetId, UpdateBudgetRequest request);
    void cancelBudget(UUID budgetId);
    BudgetSendResponse sendBudget(UUID budgetId);
    AcceptRejectResponse acceptBudget(String token);
    AcceptRejectResponse rejectBudget(String token, String reason);
    SubscriptionResponse getSubscription(UUID tenantId);
    SubscriptionResponse createOrUpdateSubscription(UUID tenantId, CreateSubscriptionRequest request);
    void pauseSubscription(UUID tenantId);
    void cancelSubscription(UUID tenantId);
    DashboardResponse getDashboard(UUID tenantId);
}
```

**Implementació:** `BillingOrchestrator implements BillingService`

### 8.2 DiscountService

```java
public interface DiscountService {
    DiscountResponse createDiscount(CreateDiscountRequest request);
    List<DiscountResponse> listDiscounts(UUID tenantId, Boolean isActive, String appliesTo);
    DiscountResponse updateDiscount(UUID discountId, UpdateDiscountRequest request);
    void deactivateDiscount(UUID discountId);
}
```

**Implementació:** `DiscountManager implements DiscountService`

### 8.3 Connexió amb Vault (contractes)

El Billing NO importa classes concretes del Vault. Només depèn de les interfícies:

| Interface Vault | Ús al Billing |
|----------------|---------------|
| `VaultService.approvePhase()` | Quan el client accepta un pressupost |
| `VaultService.getSetup()` | Per al dashboard (estat actual del tenant) |
| `ProfileService.calculateBudget()` | Per crear el pressupost inicial |
| `ProfileService.getProfile()` | Per obtenir les dades del perfil |
| `InvoiceService.createInvoice()` | Per crear factures (stub) |
| `PaymentService.charge()` | Per cobrar (stub) |

**Injecció de dependències (Spring DI):**

```java
@Service
public class BillingOrchestrator implements BillingService {
    
    private final VaultService vaultService;        // ← interfície
    private final ProfileService profileService;    // ← interfície
    private final InvoiceService invoiceService;    // ← interfície
    private final PaymentService paymentService;    // ← interfície
    
    public BillingOrchestrator(VaultService vs, ProfileService ps, 
                               InvoiceService is, PaymentService ps2) {
        this.vaultService = vs;
        this.profileService = ps;
        this.invoiceService = is;
        this.paymentService = ps2;
    }
}
```

**Avantatges d'aquest patró:**
- El Billing no s'assabenta si el Vault canvia la implementació
- Quan arribi el Mòdul 08 FinOps, només canvia la implementació injectada (`HoldedInvoiceService implements InvoiceService`), el Billing no es toca
- Es poden testejar amb mocks del Vault sense carregar tot el context Spring

---

## 9. Dependències

| Mòdul | Dependència | Tipus |
|-------|-----------|-------|
| Mòdul 01 (Auth) | Requerit per autenticació | Forta |
| Mòdul 02 (Vault) | Requerit per perfils, serveis i preus (només interfícies) | Forta |
| Mòdul 08 (FinOps) | Facturació real (stubs per ara) | Debil |
| Mòdul 09 (Payments) | Cobrament real (stubs per ara) | Debil |

**Contractes que exporta:** `BillingService`, `DiscountService`
**Contractes que importa:** `VaultService`, `ProfileService`, `InvoiceService`, `PaymentService` (del Vault)

---

## 10. Obert / Pendents

- [ ] Definir quins serveis tenen quota mensual (MONTHLY) i quins són ONE_TIME
- [ ] Implementar generació de PDF (iText / JasperReports?)
- [ ] Implementar enviament d'email real (Mòdul 10 Automations o SES)
- [ ] Definir esquema de preus mensuals per servei (ex: WhatsApp 20€/mes, Landing hosting 30€/mes)
- [ ] Connexió real amb Holded (Mòdul 08) per a facturació amb Verifactu
- [ ] Connexió real amb Stripe (Mòdul 09) per a cobrament recurrent
