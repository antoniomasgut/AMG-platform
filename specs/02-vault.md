# Mòdul 02: Perfils de Servei + Vault (AES-256)

> **Versió:** 1.1
> **Data:** 2026-05-12
> **Dependències:** Mòdul 01 (Auth) — tots els endpoints requereixen JWT + RBAC

---

## 1. Objectius

- Definir **perfils de servei** (paquets) que contenen **fases** → **serveis** → **credencials**
- Assignar perfils a clients amb seguiment d'aprovació, facturació, cobrament i implementació per fase
- Client aprova fases de forma independent (pot no aprovar tot el pla)
- En aprovar una fase → factura (Verifactu) + cobrament (Stripe) abans d'implementar
- Suport per actualitzacions gratuïtes o de pagament (nova fase aprovar)
- Emmagatzemar de forma segura (xifrat AES-256-GCM) les credencials de cada servei
- El CLIENT pot veure les seves pròpies credencials (valors emmascarats) al dashboard
- Monitorització de facturació, cobraments i implementació per fase
- Les credencials es poden verificar i rotar

---

## 2. Abast

### 2.1 Funcionalitats incloses

- CRUD de perfils de servei (nom, slug, descripció)
- CRUD de fases dins d'un perfil (ordre, nom)
- CRUD de serveis dins d'una fase (tipus, credencials necessàries)
- CRUD de camps de credencials per servei (apiKey, smtpHost, phoneId...)
- Assignació de perfils a tenants amb estat complet per fase
- Afegir serveis a la carta a un tenant (ex: landing extra)
- Aprovació de fase per part del client (rebutjar o aprovar)
- Facturació automàtica (a través de Mòdul 08) en aprovar fase
- Cobrament automàtic (a través de Mòdul 09) en aprovar fase
- Dashboard de monitorització: estat de cada fase, facturació i pagament
- Formulari dinàmic de configuració: fase actual → mostrar serveis pendents → demanar credencials
- Suport per actualitzacions: gratuïtes (sense factura) o de pagament (nova aprovació)
- Xifrat AES-256-GCM via `spring-security-crypto`
- Client dashboard: visualització de credencials (valors emmascarats: `***...abc`)
- Registre d'accessos a credencials (audit log)

### 2.2 Funcionalitats excloses

- Execució d'automatitzacions (es delega al mòdul 10 Automations / n8n)
- Renderització de landings (mòdul 04 Engine)
- Facturació Verifactu (mòdul 08 FinOps)
- Pagament Stripe (mòdul 09 Payments)
- **Nota:** El Mòdul 02 defineix interfícies per als mòduls 08 i 09 (hooks que es criden en aprovar una fase)

### 2.3 Actors

| Actor | Descripció | Permisos |
|-------|-----------|----------|
| SUPER_ADMIN | Propietari de la plataforma. | CRUD complet sobre perfils, fases, serveis, i credencials. Valors en clar. |
| ADMIN | Personal operatiu. | Assigna perfils, configura serveis, veu credencials en clar, veu dashboard monitorització. NO pot crear/eliminar perfils ni fases. |
| CLIENT | Usuari final del tenant. | Veure els seus serveis i credencials (emmascarades), aprovar/rebutjar fases, veure estat de la seva facturació. |

---

## 3. Model de dades

### 3.1 Entitats (PostgreSQL)

#### ServiceProfile (Perfil de servei / Paquet)

Defineix un paquet de serveis. Ex: "Pla Bàsic", "Pla Avançat", "WhatsApp + Landing"

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| name | String(100) | @Column(unique, nullable=false) | Nom del perfil |
| slug | String(60) | @Column(unique, nullable=false) | Identificador URL-friendly |
| description | String(255) | @Column | Descripció |
| isActive | Boolean | @Column(nullable=false) | Si està disponible |
| createdAt | Instant | @CreatedDate | |
| updatedAt | Instant | @LastModifiedDate | |

#### Phase (Fase dins d'un perfil)

Les fases defineixen l'ordre de configuració. Ex: Fase 1 "Configuració inicial", Fase 2 "Landing", Fase 3 "Automatitzacions"

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| profileId | UUID | @Column(nullable=false) | FK a ServiceProfile |
| name | String(100) | @Column(nullable=false) | Nom de la fase |
| description | String(255) | @Column | Descripció |
| sortOrder | Integer | @Column(nullable=false) | Ordre (1, 2, 3...) |
| createdAt | Instant | @CreatedDate | |
| updatedAt | Instant | @LastModifiedDate | |

**Relacions:** Un `ServiceProfile` té moltes `Phase` (1:N), ordenades per `sortOrder`

#### Service (Servei configurable)

Defineix un servei concret dins d'una fase. Ex: "WhatsApp Business", "Landing Pro", "SMTP"

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| phaseId | UUID | @Column(nullable=true) | FK a Phase (null si add-on) |
| name | String(100) | @Column(nullable=false) | Nom del servei |
| slug | String(60) | @Column(unique, nullable=false) | Identificador |
| description | String(255) | @Column | Descripció |
| type | Enum(STRING) | @Enumerated | Tipus: `CREDENTIALS`, `LANDING`, `AUTOMATION`, `BILLING`, `OTHER` |
| isAddon | Boolean | @Column(nullable=false) | Si es pot afegir a la carta |
| cost | BigDecimal(10,2) | @Column(nullable=false) | Cost per a nosaltres (ex: 20.00€) |
| salePrice | BigDecimal(10,2) | @Column(nullable=false) | Preu de venda al client (ex: 50.00€) |
| sortOrder | Integer | @Column | Ordre dins la fase |
| createdAt | Instant | @CreatedDate | |
| updatedAt | Instant | @LastModifiedDate | |

**Notes:**
- `phaseId` pot ser NULL si el servei és add-on (no pertany a cap fase concreta)
- `type=LANDING` indica que cal crear una landing (mòdul 04)
- `type=AUTOMATION` indica que cal configurar un flux n8n (mòdul 10)
- `type=CREDENTIALS` és un servei que només necessita credencials

#### CredentialField (Camp de credencial d'un servei)

Defineix quines claus necessita un servei. Ex: servei "WhatsApp" requereix `apiKey` + `phoneId`.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| serviceId | UUID | @Column(nullable=false) | FK a Service |
| key | String(100) | @Column(nullable=false) | Nom del camp (ex: "apiKey") |
| label | String(150) | @Column(nullable=false) | Etiqueta per al formulari |
| type | Enum(STRING) | @Enumerated | `PASSWORD`, `TEXT`, `HOST`, `PORT`, `EMAIL` |
| isRequired | Boolean | @Column(nullable=false) | Si és obligatori |
| placeholder | String(255) | @Column | Text d'ajuda |
| validationRegex | String(255) | @Column | Regex (opcional) |
| sortOrder | Integer | @Column | Ordre al formulari |

#### TenantProfile (Assignació de perfil a tenant)

Quan s'assigna un perfil a un tenant, es crea un registre.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | Tenant |
| profileId | UUID | @Column(nullable=false) | Perfil assignat |
| isActive | Boolean | @Column(nullable=false) | Si el perfil està actiu |
| startedAt | Instant | @CreatedDate | |
| completedAt | Instant | @Column(nullable=true) | Quan es completen totes les fases |

**Restricció única:** `(tenantId, profileId)`

#### TenantPhase (Estat complet per fase)

Entitat central que connecta pressupost, facturació, cobrament i implementació. Es crea en assignar el perfil (una per cada fase del perfil).

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | Tenant |
| profileId | UUID | @Column(nullable=false) | Perfil |
| phaseId | UUID | @Column(nullable=false) | Fase |
| approvalStatus | Enum(STRING) | @Column(nullable=false) | `PENDING_APPROVAL`, `APPROVED`, `REJECTED` |
| approvedAt | Instant | @Column(nullable=true) | Quan el client aprova |
| invoiceId | String(100) | @Column(nullable=true) | ID de la factura a Holded |
| invoiceAmount | BigDecimal(10,2) | @Column(nullable=true) | Import facturat |
| invoiceStatus | Enum(STRING) | @Column(nullable=true) | `PENDING`, `SENT`, `PAID`, `OVERDUE`, `CANCELLED` |
| paymentStatus | Enum(STRING) | @Column(nullable=true) | `PENDING`, `PAID`, `FAILED`, `REFUNDED` |
| paidAt | Instant | @Column(nullable=true) | Data de cobrament |
| implementationStatus | Enum(STRING) | @Column(nullable=false) | `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED` |
| completedAt | Instant | @Column(nullable=true) | Quan l'admin completa |
| createdAt | Instant | @CreatedDate | |
| updatedAt | Instant | @LastModifiedDate | |

**Restricció única:** `(tenantId, phaseId)` (una fase només pot estar en un TenantPhase per tenant)

**Cicle d'estats:**

```
PENDING_APPROVAL ─── aprova ───→ APPROVED ───→ (factura + cobrament)
                                     │
                                     ├── cobrament OK → NOT_STARTED → IN_PROGRESS → COMPLETED
                                     │
                                     └── cobrament KO → retry 3 dies + alerta
                ─── rebutja ──→ REJECTED
```

#### TenantService (Servei per tenant)

Cada servei (del perfil o add-on) té un registre per tenant.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | Tenant |
| serviceId | UUID | @Column(nullable=false) | FK a Service |
| phaseId | UUID | @Column(nullable=true) | Fase a la qual pertany (null si add-on) |
| status | Enum(STRING) | @Column(nullable=false) | `PENDING`, `AWAITING_CLIENT`, `CONFIGURED`, `VERIFIED` |
| statusChangedAt | Instant | @Column(nullable=true) | Data de l'últim canvi d'estat |
| createdAt | Instant | @CreatedDate | |
| updatedAt | Instant | @LastModifiedDate | |

**Cicle d'estats del servei:**

```
PENDING ─── demanar dades al client ───→ AWAITING_CLIENT
AWAITING_CLIENT ─── admin configura ───→ CONFIGURED
CONFIGURED ─── client verifica ─────────→ VERIFIED
```

**Restricció única:** `(tenantId, serviceId)`

#### TenantCredential (Valor xifrat d'una credencial)

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | Tenant |
| fieldId | UUID | @Column(nullable=false) | FK a CredentialField |
| encryptedValue | String(500) | @Column(nullable=false, length=500) | Valor xifrat AES-256-GCM (base64) |
| isSet | Boolean | @Column(nullable=false) | Si té valor |
| lastVerifiedAt | Instant | @Column | Última verificació |
| rotatedAt | Instant | @Column | Última rotació |
| createdAt | Instant | @CreatedDate | |
| updatedAt | Instant | @LastModifiedDate | |

**Restricció única:** `(tenantId, fieldId)`

#### TenantServiceAddon (Serveis add-on afegits a un tenant)

Quan s'afegeix un servei a la carta, es registra aquí (a més de `TenantService`).

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | Tenant |
| serviceId | UUID | @Column(nullable=false) | FK a Service (isAddon=true) |
| addedBy | UUID | @Column(nullable=false) | Qui va afegir el servei (userId) |
| approvalRequired | Boolean | @Column(nullable=false) | Si requereix aprovació del client |
| approvalStatus | Enum(STRING) | @Column(nullable=true) | `PENDING`, `APPROVED`, `REJECTED` (null si no requereix) |
| createdAt | Instant | @CreatedDate | |

**Restricció única:** `(tenantId, serviceId)`

#### CredentialAuditLog (Registre d'accés)

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| credentialId | UUID | @Column(nullable=false) | FK a TenantCredential |
| userId | UUID | @Column(nullable=false) | Qui ha accedit |
| action | Enum(STRING) | @Column(nullable=false) | `VIEW`, `CREATE`, `UPDATE`, `DELETE`, `VERIFY` |
| maskedValue | String(100) | @Column | Valor emmascarat (últims 4 caràcters) |
| createdAt | Instant | @CreatedDate | |

### 3.2 Relacions

```
ServiceProfile
  └── Phase (ordenades per sortOrder)
        └── Service (ordenats per sortOrder dins la fase)
              └── CredentialField (camps del formulari)

TenantProfile → ServiceProfile (assignació)
TenantPhase → Profile + Phase (estat complet: aprovació, factura, cobrament, implementació)
TenantService → Service (per tenant) → status (PENDING → VERIFIED)
TenantCredential → CredentialField (valor xifrat per tenant)
```

- Un perfil pot tenir 0 o més fases
- Una fase pot tenir 0 o més serveis
- Un servei pot tenir 0 o més camps de credencials
- Un tenant pot tenir múltiples perfils
- Un servei add-on (`isAddon=true`) es pot afegir a qualsevol tenant independentment del perfil
- Cada fase d'un perfil assignat té un `TenantPhase` amb el seu propi estat independent

### 3.3 Exemple: Perfil "Pla Avançat"

```
ServiceProfile: "Pla Avançat"
├── Phase 1: "Configuració bàsica" (210€)
│     ├── Service: "WhatsApp Business" (50€) — CREDENTIALS
│     │     ├── Field: "apiKey" (PASSWORD, obligatori)
│     │     └── Field: "phoneId" (TEXT, obligatori)
│     ├── Service: "SMTP Corporatiu" (30€) — CREDENTIALS
│     │     ├── Field: "smtpHost" (HOST, obligatori)
│     │     ├── Field: "smtpPort" (PORT, obligatori)
│     │     ├── Field: "smtpUser" (TEXT, obligatori)
│     │     └── Field: "smtpPassword" (PASSWORD, obligatori)
│     └── Service: "Landing Pro" (80€) — LANDING (sense credencials)
├── Phase 2: "Automatitzacions" (40€)
│     └── Service: "Benvinguda automàtica" (40€) — AUTOMATION
└── Phase 3: "Fidelització" (15€)
      └── Service: "Ressenya Google" (15€) — AUTOMATION

Total pla: 265€

Flux per Phase 1:
1. Client rep pressupost → aprova Phase 1 (210€)
2. Sistema crea factura 210€ + cobra via Stripe
3. Pagament OK → implementació de Phase 1
4. Admin demana API key WhatsApp al client → AWAITING_CLIENT
5. Client envia dades → admin configura → CONFIGURED
6. Client verifica que funciona → VERIFIED
7. Admin tanca la fase → COMPLETED
8. Es discuteix Phase 2...
```

---

## 4. API REST

Tots els endpoints requereixen `Authorization: Bearer JWT`. Prefix base: `/api/v1/vault`.

### 4.1 Gestió de perfils (SUPER_ADMIN + ADMIN)

#### `POST /api/v1/vault/profiles` — Crear perfil

**Rols:** SUPER_ADMIN

Request:
```json
{
  "name": "Pla Avançat",
  "slug": "pla-avancat",
  "description": "WhatsApp + Landing + Automatitzacions"
}
```

Response 201: perfil creat

#### `GET /api/v1/vault/profiles` — Llistar perfils

**Rols:** SUPER_ADMIN, ADMIN, CLIENT

**Query:** `page`, `size`, `search`

#### `GET /api/v1/vault/profiles/{id}` — Veure perfil (inclou fases, serveis i camps)

**Rols:** SUPER_ADMIN, ADMIN, CLIENT

Response:
```json
{
  "id": "uuid",
  "name": "Pla Avançat",
  "phases": [
    {
      "id": "uuid",
      "name": "Configuració bàsica",
      "sortOrder": 1,
      "services": [
        {
          "id": "uuid",
          "name": "WhatsApp Business",
          "type": "CREDENTIALS",
          "salePrice": 50.00,
          "fields": [
            { "id": "uuid", "key": "apiKey", "label": "API Key", "type": "PASSWORD", "isRequired": true }
          ]
        }
      ]
    }
  ]
}
```

#### `PUT /api/v1/vault/profiles/{id}` — Actualitzar perfil

**Rols:** SUPER_ADMIN

#### `DELETE /api/v1/vault/profiles/{id}` — Desactivar perfil

**Rols:** SUPER_ADMIN

### 4.2 Gestió de fases (SUPER_ADMIN)

#### `POST /api/v1/vault/profiles/{profileId}/phases` — Afegir fase

**Rols:** SUPER_ADMIN

#### `PUT /api/v1/vault/profiles/{profileId}/phases/{phaseId}` — Actualitzar fase

**Rols:** SUPER_ADMIN

#### `DELETE /api/v1/vault/profiles/{profileId}/phases/{phaseId}` — Eliminar fase

**Rols:** SUPER_ADMIN

### 4.3 Gestió de serveis (SUPER_ADMIN)

#### `POST /api/v1/vault/phases/{phaseId}/services` — Afegir servei a una fase

**Rols:** SUPER_ADMIN

#### `GET /api/v1/vault/services` — Llistar tots els serveis (inclòs add-ons)

**Rols:** SUPER_ADMIN, ADMIN, CLIENT

#### `POST /api/v1/vault/services` — Crear servei add-on (sense fase)

**Rols:** SUPER_ADMIN

### 4.4 Assignació i cicle de vida de fase

#### `POST /api/v1/vault/tenants/{tenantId}/profiles/{profileId}` — Assignar perfil a tenant

**Rols:** SUPER_ADMIN, ADMIN

Crea `TenantProfile` + `TenantService` per a cada servei + `TenantPhase` per a cada fase del perfil. Totes les fases comencen amb `approvalStatus=PENDING_APPROVAL`, `implementationStatus=NOT_STARTED`.

Response 201:
```json
{
  "profileId": "uuid",
  "phases": [
    {
      "phaseId": "uuid",
      "name": "Configuració bàsica",
      "sortOrder": 1,
      "approvalStatus": "PENDING_APPROVAL",
      "totalServices": 2,
      "totalPrice": 80.00
    }
  ],
  "totalPrice": 265.00
}
```

#### `DELETE /api/v1/vault/tenants/{tenantId}/profiles/{profileId}` — Desassignar perfil

**Rols:** SUPER_ADMIN, ADMIN

Marca `TenantProfile.isActive=false`, no s'eliminen registres per tenir traçabilitat.

#### `GET /api/v1/vault/tenants/{tenantId}/budget?profileId={id}&addonIds=id1,id2` — Generar pressupost

**Rols:** SUPER_ADMIN, ADMIN, CLIENT (propi)

Calcula el pressupost complet. **No crea res**, només calcula. Retorna desglossat per fase.

Response 200:
```json
{
  "profile": { "id": "uuid", "name": "Pla Avançat" },
  "phases": [
    {
      "phase": { "id": "uuid", "name": "Configuració bàsica", "sortOrder": 1 },
      "services": [
        { "id": "uuid", "name": "WhatsApp Business", "salePrice": 50.00, "cost": 10.00 },
        { "id": "uuid", "name": "SMTP Corporatiu", "salePrice": 30.00, "cost": 5.00 }
      ],
      "phaseTotal": 80.00,
      "phaseCost": 15.00,
      "phaseMargin": 65.00
    }
  ],
  "addons": [
    { "id": "uuid", "name": "Landing extra", "salePrice": 80.00, "cost": 30.00 }
  ],
  "total": 160.00,
  "totalCost": 45.00,
  "totalMargin": 115.00
}
```

**Lògica:**
- `phaseTotal = SUM(salePrice)` de tots els serveis de la fase
- `total = SUM(phaseTotal) + SUM(addon.salePrice)`
- CLIENT no veu `cost`, `phaseCost`, `totalCost`
- El pressupost és informatiu; la facturació real es fa al mòdul 08

#### `POST /api/v1/vault/tenants/{tenantId}/phases/{phaseId}/approve` — Aprovar fase (CLIENT)

**Rols:** CLIENT (propi tenant), SUPER_ADMIN, ADMIN (en nom del client)

Dispara:
1. `TenantPhase.approvalStatus = APPROVED`, `approvedAt = now`
2. Crida interface `InvoiceService.createInvoice(tenantId, phaseId, amount)`
3. Crida interface `PaymentService.charge(tenantId, invoiceId, amount)`
4. Si pagament OK → `implementationStatus = NOT_STARTED`
5. Si pagament KO → `paymentStatus = FAILED` + alerta admin

Response 200:
```json
{
  "phaseId": "uuid",
  "approvalStatus": "APPROVED",
  "paymentStatus": "PAID",
  "implementationStatus": "NOT_STARTED",
  "invoiceId": "inv_abc123",
  "amount": 80.00
}
```

#### `POST /api/v1/vault/tenants/{tenantId}/phases/{phaseId}/reject` — Rebutjar fase (CLIENT)

**Rols:** CLIENT (propi tenant), SUPER_ADMIN, ADMIN

`TenantPhase.approvalStatus = REJECTED`. No es factura res. La fase queda aturada.

#### `POST /api/v1/vault/tenants/{tenantId}/phases/{phaseId}/advance` — Avançar implementació (ADMIN)

**Rols:** SUPER_ADMIN, ADMIN

L'admin controla manualment l'avanç de la implementació:

Request:
```json
{ "status": "IN_PROGRESS" }
// o
{ "status": "COMPLETED" }
```

- Quan es marca una fase com `COMPLETED`:
  - Es verifica que tots els serveis de la fase estan `VERIFIED`
  - `TenantPhase.completedAt = now`
  - Es dispara qualsevol facturació recurrent si n'hi ha

#### `POST /api/v1/vault/tenants/{tenantId}/services/{serviceId}/status` — Canviar estat d'un servei

**Rols:** SUPER_ADMIN, ADMIN

Canvia l'estat d'un servei (PENDING → AWAITING_CLIENT → CONFIGURED → VERIFIED).

Request:
```json
{ "status": "AWAITING_CLIENT" }
```

### 4.5 Gestió de credencials

#### `PUT /api/v1/vault/tenants/{tenantId}/services/{serviceId}/fields/{fieldId}` — Establir credencial

**Rols:** SUPER_ADMIN, ADMIN

Request:
```json
{ "value": "la-api-key-en-clar" }
```

Response 200: `{ "id": "uuid", "isSet": true, "maskedValue": "***key" }`

Quan s'estableix l'últim camp d'un servei, el `TenantService.status` passa a `CONFIGURED`.

#### `POST /api/v1/vault/tenants/{tenantId}/services/{serviceId}/verify` — Verificar servei

**Rols:** SUPER_ADMIN, ADMIN

Response 200: `{ "verified": true, "message": "Connexió correcta" }`

### 4.6 Serveis add-on i actualitzacions

#### `POST /api/v1/vault/tenants/{tenantId}/addons/{serviceId}` — Afegir servei add-on

**Rols:** SUPER_ADMIN, ADMIN

El servei ha de tenir `isAddon=true`. Si `salePrice > 0`:
- Crea `TenantServiceAddon` amb `approvalRequired=true`
- Crea `TenantPhase` provisional si té fase associada

Si és **gratuït** (`salePrice = 0`):
- Es crea directament sense aprovació
- `TenantServiceAddon.approvalRequired=false`

Response 201:
```json
{
  "approvalRequired": true,
  "salePrice": 80.00,
  "status": "PENDING_CLIENT_APPROVAL"
}
```

#### `POST /api/v1/vault/tenants/{tenantId}/addons/{serviceId}/approve` — Aprovar add-on de pagament

**Rols:** CLIENT (propi), SUPER_ADMIN, ADMIN

Mateix flux que aprovar fase: factura + cobrament + implementació.

#### `DELETE /api/v1/vault/tenants/{tenantId}/addons/{serviceId}` — Eliminar servei add-on

**Rols:** SUPER_ADMIN, ADMIN

### 4.7 Dashboard de monitorització

#### `GET /api/v1/vault/tenants/{tenantId}/setup` — Estat de configuració del tenant

**Rols:** SUPER_ADMIN, ADMIN, CLIENT (propi)

Retorna l'estat de tots els perfils, fases i serveis del tenant.

Response:
```json
{
  "profiles": [
    {
      "profile": { "id": "uuid", "name": "Pla Avançat", "slug": "pla-avancat" },
      "phases": [
        {
          "phase": { "id": "uuid", "name": "Configuració bàsica", "sortOrder": 1 },
          "approvalStatus": "APPROVED",
          "paymentStatus": "PAID",
          "implementationStatus": "IN_PROGRESS",
          "services": [
            {
              "service": { "id": "uuid", "name": "WhatsApp Business", "type": "CREDENTIALS" },
              "status": "AWAITING_CLIENT",
              "fields": [
                { "id": "uuid", "key": "apiKey", "label": "API Key", "isSet": false }
              ]
            }
          ]
        }
      ]
    }
  ],
  "addons": [
    { "service": { "id": "uuid", "name": "Landing extra" }, "approvalRequired": true, "approvalStatus": "PENDING" }
  ]
}
```

#### `GET /api/v1/vault/tenants/{tenantId}/monitoring/invoices` — Dashboard facturació

**Rols:** SUPER_ADMIN, ADMIN, CLIENT (propi)

Retorna l'estat de totes les factures del tenant (per fase).

Response:
```json
{
  "phases": [
    {
      "phase": { "name": "Configuració bàsica", "sortOrder": 1 },
      "invoiceId": "inv_abc",
      "amount": 80.00,
      "invoiceStatus": "PAID",
      "paidAt": "2026-05-12T10:00:00Z"
    }
  ],
  "pendingInvoices": 0,
  "overdueInvoices": 0,
  "totalPaid": 80.00
}
```

- CLIENT veu el seu propi dashboard
- ADMIN/SUPER_ADMIN poden filtrar per tenant

#### `GET /api/v1/vault/tenants/{tenantId}/monitoring/payments` — Dashboard cobraments

**Rols:** SUPER_ADMIN, ADMIN, CLIENT (propi)

Response:
```json
{
  "phases": [
    {
      "phase": { "name": "Configuració bàsica" },
      "amount": 80.00,
      "paymentStatus": "PAID",
      "paidAt": "2026-05-12T10:00:00Z",
      "paymentMethod": "card"
    }
  ],
  "pendingPayments": 0,
  "failedPayments": 0
}
```

#### `GET /api/v1/vault/tenants/{tenantId}/monitoring/phases` — Dashboard implementació

**Rols:** SUPER_ADMIN, ADMIN

Visió general de l'estat d'avanç per fase:

Response:
```json
{
  "totalPhases": 3,
  "pendingApproval": 1,
  "inProgress": 1,
  "completed": 0,
  "rejected": 1,
  "phases": [
    { "name": "Configuració bàsica", "approvalStatus": "APPROVED", "implementationStatus": "IN_PROGRESS", "progress": "50%" }
  ]
}
```

### 4.8 Mapa complet d'endpoints

| Mètode | Ruta | Descripció | Rols |
|--------|------|-----------|------|
| POST | /vault/profiles | Crear perfil | SUPER_ADMIN |
| GET | /vault/profiles | Llistar perfils | Tots |
| GET | /vault/profiles/{id} | Veure perfil complert | Tots |
| PUT | /vault/profiles/{id} | Actualitzar perfil | SUPER_ADMIN |
| DELETE | /vault/profiles/{id} | Desactivar perfil | SUPER_ADMIN |
| POST | /vault/profiles/{pId}/phases | Afegir fase | SUPER_ADMIN |
| PUT | /vault/profiles/{pId}/phases/{phId} | Actualitzar fase | SUPER_ADMIN |
| DELETE | /vault/profiles/{pId}/phases/{phId} | Eliminar fase | SUPER_ADMIN |
| POST | /vault/phases/{phId}/services | Afegir servei a fase | SUPER_ADMIN |
| POST | /vault/services | Crear servei add-on | SUPER_ADMIN |
| GET | /vault/services | Llistar serveis | Tots |
| POST | /vault/tenants/{tId}/profiles/{pId} | Assignar perfil | SUPER_ADMIN, ADMIN |
| DELETE | /vault/tenants/{tId}/profiles/{pId} | Desassignar perfil | SUPER_ADMIN, ADMIN |
| GET | /vault/tenants/{tId}/budget | Generar pressupost | SUPER_ADMIN, ADMIN, CLIENT (propi) |
| POST | /vault/tenants/{tId}/phases/{phId}/approve | Aprovar fase | SUPER_ADMIN, ADMIN, CLIENT (propi) |
| POST | /vault/tenants/{tId}/phases/{phId}/reject | Rebutjar fase | SUPER_ADMIN, ADMIN, CLIENT (propi) |
| POST | /vault/tenants/{tId}/phases/{phId}/advance | Avançar implementació | SUPER_ADMIN, ADMIN |
| PUT | /vault/tenants/{tId}/services/{sId}/status | Canviar estat servei | SUPER_ADMIN, ADMIN |
| GET | /vault/tenants/{tId}/setup | Estat configuració | SUPER_ADMIN, ADMIN, CLIENT (propi) |
| PUT | /vault/tenants/{tId}/services/{sId}/fields/{fId} | Establir credencial | SUPER_ADMIN, ADMIN |
| POST | /vault/tenants/{tId}/services/{sId}/verify | Verificar servei | SUPER_ADMIN, ADMIN |
| POST | /vault/tenants/{tId}/addons/{sId} | Afegir add-on | SUPER_ADMIN, ADMIN |
| POST | /vault/tenants/{tId}/addons/{sId}/approve | Aprovar add-on pagament | SUPER_ADMIN, ADMIN, CLIENT (propi) |
| DELETE | /vault/tenants/{tId}/addons/{sId} | Eliminar add-on | SUPER_ADMIN, ADMIN |
| GET | /vault/tenants/{tId}/monitoring/invoices | Dashboard facturació | SUPER_ADMIN, ADMIN, CLIENT (propi) |
| GET | /vault/tenants/{tId}/monitoring/payments | Dashboard cobraments | SUPER_ADMIN, ADMIN, CLIENT (propi) |
| GET | /vault/tenants/{tId}/monitoring/phases | Dashboard implementació | SUPER_ADMIN, ADMIN |

---

## 5. Seguretat

- **Xifrat:** AES-256-GCM via `spring-security-crypto`. Clau mestra via env var `VAULT_MASTER_KEY` (mínim 32 chars base64).
- **Accés:** SUPER_ADMIN i ADMIN veuen valors en clar. CLIENT veu emmascarat.
- **Emmascarament:** Últims 4 caràcters, prefix `***`. Ex: `***key`, `***3456`.
- **Preus:** `salePrice > cost` sempre. Si s'intenta crear/modificar un servei amb `salePrice <= cost`, retornar 400.
- **Audit log:** Tots els accessos a `TenantCredential` es registren.

---

## 6. RGPD / LSSI

- Les credencials són dades tècniques vinculades a un tenant.
- `CredentialAuditLog` registra qui accedeix a cada credencial.
- En eliminar un tenant, totes les seves credencials s'eliminen (cascada).
- Les factures es conserven segons obligació legal (Verifactu).

---

## 7. Tests (QA)

### 7.1 Funcionals

| # | Cas | Resultat |
|---|-----|---------|
| 1 | Crear perfil amb 2 fases i 3 serveis | 201 |
| 2 | Assignar perfil a tenant | 201, TenantPhase creat per cada fase |
| 3 | CLIENT aprova una fase | APPROVED, es dispara facturació |
| 4 | CLIENT rebutja una fase | REJECTED, no es factura |
| 5 | Configurar tots els serveis d'una fase | TenantService.status = CONFIGURED |
| 6 | Marcar servei com AWAITING_CLIENT | 200, admin demana dades al client |
| 7 | CLIENT veu estat configuració (emmascarat) | 200, valors emmascarats |
| 8 | ADMIN veu estat configuració (en clar) | 200, valors en clar |
| 9 | Afegir servei add-on de pagament a tenant | approvalRequired=true |
| 10 | Afegir servei add-on gratuït | approvalRequired=false, creat directament |
| 11 | Avançar implementació a COMPLETED | 200, completedAt establert |
| 12 | Dashboard facturació (monitoring) | 200, llista fases amb invoiceStatus |
| 13 | Dashboard cobraments | 200, paymentStatus per fase |
| 14 | CLIENT no pot establir credencials | 403 |
| 15 | Pressupost d'un perfil | GET /budget amb profileId | 200, total = suma de serveis |
| 16 | CLIENT veu pressupost sense costos | 200, no mostra `cost` |
| 17 | Crear servei amb salePrice <= cost | 400 |

### 7.2 Seguretat

| # | Cas | Resultat |
|---|-----|---------|
| 1 | Valor xifrat a BD | Text il·legible |
| 2 | Accés sense JWT | 401 |
| 3 | CLIENT veu credencials d'altre tenant | 403 |

---

## 8. Dependències

| Mòdul | Dependència | Tipus |
|-------|-----------|-------|
| Mòdul 01 (Auth) | Requerit per autenticació | Forta |
| Mòdul 08 (FinOps) | Facturació Verifactu via interface | Forta (interface) |
| Mòdul 09 (Payments) | Cobrament Stripe via interface | Forta (interface) |
| Mòdul 04 (Engine) | Servirà landings als serveis type=LANDING | Debil |
| Mòdul 10 (Automations) | Executarà workflows referenciats | Debil |

**Interfícies definides al Mòdul 02 (implementades als mòduls 08 i 09):**

```java
interface InvoiceService {
    String createInvoice(UUID tenantId, UUID phaseId, BigDecimal amount);
    void updateInvoiceStatus(String invoiceId, InvoiceStatus status);
}

interface PaymentService {
    PaymentResult charge(UUID tenantId, String invoiceId, BigDecimal amount);
    PaymentResult refund(String invoiceId);
}
```

---

## 9. Pendents (post-implementació)

- [ ] Verificador de connexió: implementar només per SMTP en primera versió
- [ ] Notificacions: email al client quan hi ha serveis pendents de configurar
- [ ] Retry automàtic de pagaments fallits (3 intents en 3 dies)
- [ ] Càlcul de mensualitat recurrent en completar fases amb serveis subscripció
