# Mòdul 07: Billing — Pressupostos i Descomptes

> **Versió:** 1.0
> **Data:** 2026-05-13
> **Dependències:** Mòdul 01 (Auth), Mòdul 02 (Vault)

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

- Creació de pressupostos a partir d'un perfil de servei + add-ons
- Càlcul de subtotal, descomptes i total
- CRUD de descomptes (percentatge o fix) amb dates de validesa
- Aplicació de descomptes al pressupost
- Enviament de pressupost (genera token d'acceptació/rebutjament)
- Acceptació i rebutjament públic via token (sense JWT)
- Cancel·lació de pressupostos (DRAFT o SENT)
- Dashboard de facturació per tenant
- Integració amb Vault (approvePhase en acceptar pressupost)

### 2.2 Funcionalitats excloses

- Facturació real i pagaments (Stripe — Mòdul 09 Payments, Holded — Mòdul 08 FinOps)
- Enviament d'email al client (futur)
- Pressupostos recurrents

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
| POST | /billing/tenants/{tenantId}/budgets | Crear pressupost | SUPER_ADMIN, ADMIN |
| GET | /billing/tenants/{tenantId}/budgets | Llistar pressupostos | Tots autenticats |
| GET | /billing/tenants/{tenantId}/budgets/{id} | Veure pressupost | Tots autenticats |
| PUT | /billing/budgets/{id} | Actualitzar (només DRAFT) | SUPER_ADMIN, ADMIN |
| DELETE | /billing/budgets/{id} | Cancel·lar | SUPER_ADMIN |
| POST | /billing/budgets/{id}/send | Enviar (genera token) | SUPER_ADMIN, ADMIN |
| POST | /billing/budgets/accept?token= | Acceptar (públic) | Públic (token) |
| POST | /billing/budgets/reject?token= | Rebutjar (públic) | Públic (token) |

### 4.2 Descomptes

| Mètode | Ruta | Descripció | Rols |
|--------|------|-----------|------|
| POST | /billing/discounts | Crear descompte | SUPER_ADMIN, ADMIN |
| GET | /billing/discounts | Llistar descomptes | SUPER_ADMIN, ADMIN |
| PUT | /billing/discounts/{id} | Actualitzar | SUPER_ADMIN |
| DELETE | /billing/discounts/{id} | Desactivar | SUPER_ADMIN |

### 4.3 Dashboard

| Mètode | Ruta | Descripció | Rols |
|--------|------|-----------|------|
| GET | /billing/tenants/{tenantId}/dashboard | Dashboard facturació | Tots autenticats |

### 4.4 Detall d'endpoints

#### `POST /api/v1/billing/tenants/{tenantId}/budgets` — Crear pressupost

Request:
```json
{
  "profileId": "uuid",
  "phaseIds": [],
  "addonIds": ["uuid1", "uuid2"],
  "notes": "Pressupost inicial",
  "clientNotes": "Oferta vàlida 30 dies",
  "discountIds": ["uuid"],
  "validUntil": "2026-06-13"
}
```

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

Canvia l'estat a ACCEPTED, invalida el token, aprova les fases al Vault.

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
| Mòdul 02 (Vault) | ServiceProfile, VaultService.approvePhase | Forta |

---

## 9. Obert / Pendents

- [ ] `createdBy` a DiscountManager es queda amb UUID.randomUUID() — cal passar l'usuari real
- [ ] `appliesTo` query param a GET /discounts no s'usa al servei
- [ ] BudgetNumber usa `count()` global, no per tenant
- [ ] `phaseIds` als request DTOs no s'usa mai (les fases venen del perfil)
- [ ] `InvoiceService` i `PaymentService` estan injectats però mai cridats
- [ ] `rejectionUrl` a BudgetResponse sempre és null
- [ ] `recentPhases` a DashboardResponse sempre és buit
