# Mòdul 02: Perfils de Servei + Vault (AES-256)

> **Versió:** 1.1
> **Data:** 2026-05-13
> **Dependències:** Mòdul 01 (Auth) — tots els endpoints requereixen JWT + RBAC. Mòdul 01 (Tenant) — preferredChannel per comunicació guiada.

---

## 1. Objectius

- Definir **perfils de servei** (paquets) que contenen **fases** → **serveis** → **credencials**
- Assignar perfils a clients i guiar la configuració fase per fase de forma **automatitzada i remota**
- Quan necessitem dades del client (API keys, permisos...), contactar-lo automàticament pel seu **canal preferit** (WhatsApp, Telegram o email) i posar la configuració en pausa fins que respongui
- Reprendre la configuració automàticament quan el client respon (missatge o click d'un botó)
- En completar una fase, notificar al client per explicar-li el funcionament i validar que està conforme
- Permetre afegir serveis a la carta (ex: crear una landing) a un client o fase
- Emmagatzemar de forma segura (xifrat AES-256-GCM) les credencials de cada servei
- El CLIENT pot veure les seves pròpies credencials (valors emmascarats) al dashboard
- Les credencials es poden verificar i rotar

---

## 2. Abast

### 2.1 Funcionalitats incloses

- CRUD de perfils de servei (nom, slug, descripció)
- CRUD de fases dins d'un perfil (ordre, nom)
- CRUD de serveis dins d'una fase (tipus, credencials necessàries)
- CRUD de camps de credencials per servei (apiKey, smtpHost, phoneId...)
- Assignació de perfils a tenants amb seguiment de fase actual
- Afegir serveis a la carta a un tenant (ex: landing extra)
- Formulari dinàmic de configuració: fase actual → mostrar serveis pendents → demanar credencials
- **Configuració guiada remota**: enviar sol·licitud d'informació al client via `preferredChannel` (WhatsApp, Telegram, email) i reprendre automàticament en rebre resposta
- **Cicle de vida de servei**: PENDING → AWAITING_CLIENT → CONFIGURED → VERIFIED
- **Notificació de fase completada**: en acabar una fase, contactar al client per explicar funcionament i obtenir confirmació
- Xifrat AES-256-GCM via `spring-security-crypto`
- Client dashboard: visualització de credencials (valors emmascarats: `***...abc`)
- Registre d'accessos a credencials (audit log)

### 2.2 Funcionalitats excloses

- Execució d'automatitzacions (es delega al mòdul 10 Automations / n8n)
- Renderització de landings (mòdul 04 Engine)
- Facturació (mòdul 07 Billing)

### 2.3 Actors

| Actor | Descripció | Permisos |
|-------|-----------|----------|
| SUPER_ADMIN | Propietari de la plataforma. | CRUD complet sobre perfils, fases, serveis, i credencials. Valors en clar. |
| ADMIN | Personal operatiu. | Assigna perfils, configura serveis, veu credencials en clar. NO pot crear/eliminar perfils ni fases. |
| CLIENT | Usuari final del tenant. | Veure els seus serveis i credencials (emmascarades). |

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

#### CatalogService (Servei configurable del catàleg)

Defineix un servei concret dins d'una fase. Ex: "WhatsApp Business", "Landing Pro", "SMTP"

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| phaseId | UUID | @Column(nullable=false) | FK a Phase (pot ser null si és add-on) |
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
| serviceId | UUID | @Column(nullable=false) | FK a CatalogService |
| key | String(100) | @Column(nullable=false) | Nom del camp (ex: "apiKey") |
| label | String(150) | @Column(nullable=false) | Etiqueta per al formulari |
| type | Enum(STRING) | @Enumerated | `PASSWORD`, `TEXT`, `HOST`, `PORT`, `EMAIL` |
| isRequired | Boolean | @Column(nullable=false) | Si és obligatori |
| placeholder | String(255) | @Column | Text d'ajuda |
| validationRegex | String(255) | @Column | Regex (opcional) |
| sortOrder | Integer | @Column | Ordre al formulari |

#### TenantProfile (Assignació de perfil a tenant)

Quan s'assigna un perfil a un tenant, es crea un registre amb la fase actual.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | Tenant |
| profileId | UUID | @Column(nullable=false) | Perfil assignat |
| currentPhaseId | UUID | @Column(nullable=true) | Fase actual (NULL = tot completat) |
| phaseStatus | Enum | @Enumerated(STRING) @Column(nullable=false) | Estat de la fase actual: `CONFIGURING`, `AWAITING_CONFIRMATION`, `COMPLETED` |
| startedAt | Instant | @CreatedDate | |
| completedAt | Instant | @Column | Quan es completa tot el perfil |

**Restricció única:** `(tenantId, profileId)`

- `CONFIGURING` — S'estan configurant els serveis de la fase actual
- `AWAITING_CONFIRMATION` — Tots els serveis de la fase estan configurats, pendent que el client confirmi que funciona
- `COMPLETED` — Client ha confirmat, fase finalitzada

#### TenantService (Servei per tenant)

Cada servei (del perfil o add-on) té un registre per tenant amb seguiment del seu estat de configuració.

**Enums:**

`ServiceConfigStatus`: `PENDING`, `AWAITING_CLIENT`, `CONFIGURED`, `VERIFIED`

- `PENDING` — Servei pendent de configurar (acabat de crear)
- `AWAITING_CLIENT` — En espera de resposta del client (s'ha enviat sol·licitud)
- `CONFIGURED` — Totes les credencials/configuracions requerides estan completes
- `VERIFIED` — Configuració verificada i validada

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | Tenant |
| serviceId | UUID | @Column(nullable=false) | FK a CatalogService |
| phaseId | UUID | @Column(nullable=true) | Fase a la qual pertany (null si add-on) |
| configStatus | Enum | @Enumerated(STRING) @Column(nullable=false) | Estat del servei: `PENDING`, `AWAITING_CLIENT`, `CONFIGURED`, `VERIFIED` |
| configuredAt | Instant | @Column | Quan es va completar la configuració |
| verifiedAt | Instant | @Column | Quan es va verificar |
| createdAt | Instant | @CreatedDate | |
| updatedAt | Instant | @LastModifiedDate | |

**Restricció única:** `(tenantId, serviceId)`

Comportament per estat:
- `PENDING` → `AWAITING_CLIENT`: Quan ADMIN prem "Sol·licitar al client" i s'envia el missatge
- `AWAITING_CLIENT` → `CONFIGURED`: Quan el client respon amb les dades necessàries (o ADMIN les introdueix manualment)
- `CONFIGURED` → `VERIFIED`: Quan ADMIN verifica que el servei funciona correctament

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

#### CommunicationRequest (Sol·licitud d'informació al client)

Registra cada intent de contacte amb el client durant la configuració guiada. Quan un ADMIN prem "Sol·licitar al client", es crea un registre, s'envia el missatge, i el `TenantService` passa a `AWAITING_CLIENT`.

**Enums:**

`CommunicationChannel`: `WHATSAPP`, `TELEGRAM`, `EMAIL` (mirall de `PreferredChannel` del Tenant)

`CommunicationStatus`: `SENT`, `DELIVERED`, `RESPONDED`, `EXPIRED`, `FAILED`

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | Tenant destinatari |
| tenantServiceId | UUID | @Column(nullable=false) | FK a TenantService (context del servei) |
| channel | Enum | @Enumerated(STRING) @Column(nullable=false) | Canal usat: `WHATSAPP`, `TELEGRAM`, `EMAIL` |
| recipient | String(200) | @Column(nullable=false) | Telèfon / email / chatId del destinatari |
| subject | String(200) | @Column | Assumpte / títol del missatge |
| body | Text | @Column(nullable=false, length=2000) | Cos del missatge |
| status | Enum | @Enumerated(STRING) @Column(nullable=false) | `SENT`, `DELIVERED`, `RESPONDED`, `EXPIRED`, `FAILED` |
| responseData | Text(JSON) | @Column(columnDefinition="TEXT") | Dades rebudes del client (valor de la credencial, confirmació, etc.) |
| expiresAt | Instant | @Column | Data d'expiració (si no respon en X temps) |
| sentAt | Instant | @Column | Quan es va enviar |
| respondedAt | Instant | @Column | Quan va respondre |
| createdAt | Instant | @CreatedDate | |

**Comportament per canal:**

| Canal | Com s'envia | Com repon el client |
|-------|------------|-------------------|
| WHATSAPP | Missatge de text amb botons (API WhatsApp Business) | El client respon al xat o prem un botó → webhook |
| TELEGRAM | Missatge amb botons inline (Bot API) | El client prem un botó o escriu → webhook |
| EMAIL | Enllaç al portal del client amb formulari | Client obre enllaç, omple formulari, i envia → callback al backend |

#### TenantServiceAddon (Serveis add-on afegits a un tenant)

Quan s'afegeix un servei a la carta, es regeustra aquí (a més de `TenantService`).

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | Tenant |
| serviceId | UUID | @Column(nullable=false) | FK a CatalogService (isAddon=true) |
| addedBy | UUID | @Column(nullable=false) | Qui va afegir el servei (userId) |
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
        └── CatalogService (ordenats per sortOrder dins la fase)
              └── CredentialField (camps del formulari)

TenantProfile → ServiceProfile (assignació) → currentPhaseId → phaseStatus
TenantService → CatalogService (per tenant) → configStatus (PENDING → AWAITING_CLIENT → CONFIGURED → VERIFIED)
TenantCredential → CredentialField (valor xifrat per tenant)
CommunicationRequest → TenantService (sol·licitud al client) → canal preferit
```

- Un perfil pot tenir 0 o més fases
- Una fase pot tenir 0 o més serveis
- Un servei pot tenir 0 o més camps de credencials
- Un tenant pot tenir múltiples perfils
- Un servei add-on (`isAddon=true`) es pot afegir a qualsevol tenant independentment del perfil
- Un `TenantService` pot tenir múltiples `CommunicationRequest` (reintents, fases de la conversa)

### 3.3 Exemple: Perfil "Pla Avançat"

```
ServiceProfile: "Pla Avançat"
├── Phase 1: "Configuració bàsica"
│     ├── Service: "WhatsApp Business" (CREDENTIALS)
│     │     ├── Field: "apiKey" (PASSWORD, obligatori)
│     │     └── Field: "phoneId" (TEXT, obligatori)
│     └── Service: "SMTP Corporatiu" (CREDENTIALS)
│           ├── Field: "smtpHost" (HOST, obligatori)
│           ├── Field: "smtpPort" (PORT, obligatori)
│           ├── Field: "smtpUser" (TEXT, obligatori)
│           └── Field: "smtpPassword" (PASSWORD, obligatori)
├── Phase 2: "Landing"
│     └── Service: "Landing Pro" (LANDING)
│           └── (sense credencials — la landing es configura al mòdul 04)
└── Phase 3: "Automatitzacions"
      └── Service: "Benvinguda automàtica" (AUTOMATION)
            └── (referència a workflow n8n)

Add-ons disponibles:
└── Service: "Landing extra" (LANDING, isAddon=true)
```

---

### 3.4 Flux de configuració guiada

La configuració dels serveis és un procés guiat que alterna accions internes (ADMIN/SUPER_ADMIN) amb sol·licituds al client. El flux complet per a cada fase és:

```
Assignar perfil al tenant
  │
  ▼
Fase 1: CONFIGURING
  │
  ├── Servei A (CREDENTIALS) → PENDING
  │     ├── ADMIN estableix credencials internament
  │     │     └── → CONFIGURED
  │     └── ADMIN necessita API Key del client
  │           ├── → AWAITING_CLIENT
  │           ├── Crear CommunicationRequest
  │           ├── Enviar missatge via preferredChannel
  │           │     └── "Hola (client), per configurar WhatsApp necessitam la teva API Key.
  │           │          Pots respondre aquest missatge o fer clic aquí."
  │           ├── Client respon (webhook/callback)
  │           │     └── → CONFIGURED
  │           └── (Si no respon en 7 dies → EXPIRED, reenviar)
  │
  ├── Servei B (LANDING) → PENDING
  │     └── ADMIN crea landing al Engine →
  │           └── → CONFIGURED
  │
  └── TOTS els serveis de la fase → CONFIGURED
        │
        ▼
  Fase 1: AWAITING_CONFIRMATION
        │
        ├── Enviar missatge al client:
        │     └── "(client), la Fase 1 (Configuració bàsica) està llesta!
        │          Tens WhatsApp Business i Landing configurats.
        │          Prem Confirma si tot funciona correctament."
        │
        ├── Client confirma (webhook/botó)
        │     └── Fase 1 → COMPLETED
        │
        ▼
  Fase 2: CONFIGURING (mateix procés)
        │
        ...
        │
        ▼
  Totes les fases COMPLETED → perfil finalitzat
```

**Tipus de sol·licituds al client:**

| Tipus | Exemple | Com respon el client |
|-------|---------|---------------------|
| `REQUEST_CREDENTIAL` | "Necessitam la teva API Key de WhatsApp" | Envia el valor per xat/formulari |
| `REQUEST_PERMISSION` | "Dona'ns permís per emmagatzemar les teves credencials" | Prem botó "Autoritzo" |
| `REQUEST_CONFIRMATION` | "La fase 1 està llesta. Confirma que tot funciona." | Prem botó "Confirma" |
| `REQUEST_INFO` | "Necessitam el teu NIF per la factura" | Escriu i envia |

**Regles de negoci:**
- Quan un servei passa a `AWAITING_CLIENT`, la configuració de la fase es **pausa** (no s'avança al següent servei)
- El `CommunicationRequest` té expiració de **7 dies**. Si expira, es pot reenviar
- Quan el client respon, el sistema intenta **processar automàticament**:
  - Si és una API Key → es xifra i es guarda a `TenantCredential`
  - Si és una confirmació → es canvia l'estat corresponent
- Si el processament automàtic no és possible, queda en estat `RESPONDED` pendent d'acció de l'ADMIN
- En completar una fase, s'envia notificació automàtica al client i es posa en `AWAITING_CONFIRMATION`
- L'ADMIN pot saltar-se el flux guiat i configurar manualment (establir credencials directament)

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

### 4.3 Gestió de serveis (SUPER_ADMIN + ADMIN)

#### `POST /api/v1/vault/phases/{phaseId}/services` — Afegir servei a una fase

**Rols:** SUPER_ADMIN (ADMIN no pot crear serveis)

#### `GET /api/v1/vault/services` — Llistar tots els serveis (inclòs add-ons)

**Rols:** SUPER_ADMIN, ADMIN, CLIENT

#### `POST /api/v1/vault/services` — Crear servei add-on (sense fase)

**Rols:** SUPER_ADMIN

### 4.4 Assignació i configuració de clients

#### `POST /api/v1/vault/tenants/{tenantId}/profiles/{profileId}` — Assignar perfil a tenant

**Rols:** SUPER_ADMIN, ADMIN

Crea `TenantProfile` + `TenantService` per a cada servei del perfil. `currentPhaseId` = primera fase.

Response 201:
```json
{
  "profileId": "uuid",
  "currentPhase": { "id": "uuid", "name": "Configuració bàsica", "sortOrder": 1 },
  "pendingServices": 2,
  "totalServices": 6
}
```

#### `DELETE /api/v1/vault/tenants/{tenantId}/profiles/{profileId}` — Desassignar perfil

**Rols:** SUPER_ADMIN, ADMIN

Elimina `TenantProfile`, `TenantService` i `TenantCredential` associats.

#### `GET /api/v1/vault/tenants/{tenantId}/budget?profileId={id}&addonIds=id1,id2` — Generar pressupost

**Rols:** SUPER_ADMIN, ADMIN, CLIENT (propi)

Calcula el pressupost per a un perfil + add-ons seleccionats. **No crea res**, només calcula.

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
- `cost` es mostra per a SUPER_ADMIN/ADMIN; CLIENT no veu `cost` ni `phaseCost` ni `totalCost`
- El pressupost és informatiu; la facturació real es fa al Mòdul 07 Billing

#### `GET /api/v1/vault/tenants/{tenantId}/setup` — Estat de configuració del tenant

**Rols:** SUPER_ADMIN, ADMIN, CLIENT (propi)

Retorna l'estat de tots els perfils, fases i serveis del tenant, amb indicació de l'estat de configuració (configStatus), sol·licituds pendents al client, i fase actual.

Response:
```json
{
  "profiles": [
    {
      "profile": { "id": "uuid", "name": "Pla Avançat", "slug": "pla-avancat" },
      "currentPhase": { "id": "uuid", "name": "Configuració bàsica", "sortOrder": 1 },
      "phaseStatus": "CONFIGURING",
      "progress": { "configured": 1, "pending": 1, "awaitingClient": 1, "total": 6 },
      "phases": [
        {
          "phase": { "id": "uuid", "name": "Configuració bàsica", "sortOrder": 1 },
          "phaseStatus": "CONFIGURING",
          "services": [
            {
              "service": { "id": "uuid", "name": "WhatsApp Business", "type": "CREDENTIALS" },
              "configStatus": "CONFIGURED",
              "fields": [
                { "id": "uuid", "key": "apiKey", "label": "API Key", "isSet": true, "maskedValue": "***key" }
              ]
            },
            {
              "service": { "id": "uuid", "name": "SMTP Corporatiu", "type": "CREDENTIALS" },
              "configStatus": "AWAITING_CLIENT",
              "pendingRequest": {
                "id": "uuid",
                "channel": "WHATSAPP",
                "subject": "API Key SMTP",
                "status": "SENT",
                "sentAt": "2026-05-13T10:00:00Z",
                "expiresAt": "2026-05-20T10:00:00Z"
              },
              "fields": [
                { "id": "uuid", "key": "smtpHost", "label": "Servidor SMTP", "isSet": true },
                { "id": "uuid", "key": "smtpPassword", "label": "Contrasenya SMTP", "isSet": false }
              ]
            }
          ]
        }
      ]
    }
  ],
  "addons": [
    { "service": { "id": "uuid", "name": "Landing extra" }, "configStatus": "PENDING" }
  ]
}
```

- CLIENT veu `maskedValue`; SUPER_ADMIN i ADMIN veuen `clearValue`
- `configStatus` es calcula així:
  - `PENDING` — Acabat de crear, pendent d'iniciar configuració
  - `AWAITING_CLIENT` — S'ha enviat sol·licitud al client, s'està esperant resposta
  - `CONFIGURED` — Tots els `CredentialField` requerits tenen `isSet=true` (o el servei no en necessita)
  - `VERIFIED` — S'ha verificat que el servei funciona correctament

### 4.5 Gestió de credencials

#### `PUT /api/v1/vault/tenants/{tenantId}/services/{serviceId}/fields/{fieldId}` — Establir credencial

**Rols:** SUPER_ADMIN, ADMIN

Request:
```json
{ "value": "la-api-key-en-clar" }
```

Response 200: `{ "id": "uuid", "isSet": true, "maskedValue": "***key" }`

Quan s'estableix l'últim camp requerit d'un servei, el `TenantService.configStatus` passa a `CONFIGURED` (si estava `PENDING` o `AWAITING_CLIENT`).

#### `POST /api/v1/vault/tenants/{tenantId}/services/{serviceId}/verify` — Verificar servei

**Rols:** SUPER_ADMIN, ADMIN

Response 200: `{ "verified": true, "message": "Connexió correcta" }`

(Lògica de verificació depèn del tipus de servei. Per fases.)

### 4.6 Configuració guiada

Endpoints per gestionar el flux de configuració remot amb el client.

#### `POST /api/v1/vault/tenants/{tenantId}/services/{serviceId}/request` — Sol·licitar informació al client

**Rols:** SUPER_ADMIN, ADMIN

Quan ADMIN necessita dades del client per configurar un servei, prem "Sol·licitar al client". El sistema:
1. Crea un `CommunicationRequest` amb el canal preferit del tenant
2. Envia el missatge automàtic
3. Posa el `TenantService.configStatus` a `AWAITING_CLIENT`

**Request:**
```json
{
  "requestType": "REQUEST_CREDENTIAL | REQUEST_PERMISSION | REQUEST_INFO",
  "fieldId": "uuid (opcional, si es demana una credencial concreta)",
  "customMessage": "Necessitam la teva API Key de WhatsApp Business per configurar el servei. (opcional, sinó es genera automàtic)"
}
```

**Response 200:**
```json
{
  "requestId": "uuid",
  "channel": "WHATSAPP",
  "status": "SENT",
  "message": "Missatge enviat a +34 600 123 456",
  "expiresAt": "2026-05-20T10:00:00Z"
}
```

**Generació automàtica del missatge:**
- Si `requestType=REQUEST_CREDENTIAL` i `fieldId` està present:
  - `"(Nom client), per configurar (nom del servei) necessitam el camp (label del field).`"
  - Si `CredentialField.type=PASSWORD`: `"Pots respondre aquest missatge amb el valor."`
  - Si no: `"Fes clic aquí per introduir-ho al formulari segur."`
- Si `requestType=REQUEST_PERMISSION`: `"Dona'ns permís per (acció)? Prem 'Autoritzo' per continuar."`
- Si `requestType=REQUEST_INFO`: `"Necessitam més informació: (customMessage)"`

#### `POST /api/v1/vault/communication/{requestId}/respond` — Rebre resposta del client (webhook)

**Autenticació:** Interna (amb API Key de sistema, no JWT d'usuari)

Aquest endpoint rep les respostes dels canals de comunicació (WhatsApp, Telegram, email). Cada canal té el seu adaptador que normalitza la resposta a aquest format.

**Request:**
```json
{
  "channel": "WHATSAPP",
  "channelMessageId": "wamid.123456789",
  "responseType": "TEXT | BUTTON_CLICK",
  "text": "la-meva-api-key",
  "buttonId": "AUTHORIZE (opcional, si ve d'un botó)"
}
```

**Lògica de processament:**
1. Buscar `CommunicationRequest` per `requestId` (passat al missatge original com a context)
2. Actualitzar `status = RESPONDED`, `respondedAt`, `responseData`
3. Segons `requestType` original:
   - `REQUEST_CREDENTIAL` amb `fieldId` → xifrar el valor i guardar a `TenantCredential` → servei `CONFIGURED`
   - `REQUEST_PERMISSION` → registrar permís → continuar configuració
   - `REQUEST_INFO` → guardar resposta a `responseData` → quedar pendent d'ADMIN
4. Si tots els serveis de la fase estan `CONFIGURED`:
   - `TenantProfile.phaseStatus` → `AWAITING_CONFIRMATION`
   - Enviar missatge automàtic al client: "La fase (nom) està llesta! Confirma que tot funciona."
   - Cada servei passa a `VERIFIED` (pendent de verificació real)

**Response 200:**
```json
{
  "processed": true,
  "action": "credential_stored",
  "serviceStatus": "CONFIGURED"
}
```

#### `POST /api/v1/vault/tenants/{tenantId}/profiles/{profileId}/confirm-phase` — Client confirma fase

**Autenticació:** Webhook intern o JWT (CLIENT pot confirmar des del dashboard)

Aquest endpoint s'activa quan el client prem "Confirma" (via botó de WhatsApp/Telegram o des del seu portal).

**Request:**
```json
{
  "communicationRequestId": "uuid (opcional, si ve d'un missatge)",
  "phaseId": "uuid"
}
```

**Lògica:**
1. Marcar la fase actual com `COMPLETED`
2. Si hi ha una fase següent → avançar `currentPhaseId` a la nova fase, posar `phaseStatus = CONFIGURING`
3. Si és l'última fase → marcar `TenantProfile.completedAt`
4. Enviar missatge de confirmació al client:
   - Si avança de fase: "Passam a la fase (nom de la següent)!"
   - Si és l'última: "Perfil completat! El teu negoci ja està online."

**Response 200:**
```json
{
  "phaseStatus": "COMPLETED",
  "nextPhase": { "id": "uuid", "name": "Fase següent", "sortOrder": 2 },
  "profileCompleted": false
}
```

### 4.7 Serveis add-on

#### `POST /api/v1/vault/tenants/{tenantId}/addons/{serviceId}` — Afegir servei add-on

**Rols:** SUPER_ADMIN, ADMIN

El servei ha de tenir `isAddon=true`. Crea `TenantService` + `TenantServiceAddon`.

#### `DELETE /api/v1/vault/tenants/{tenantId}/addons/{serviceId}` — Eliminar servei add-on

**Rols:** SUPER_ADMIN, ADMIN

### 4.8 Mapa complet d'endpoints

| Mètode | Ruta | Descripció | Rols |
|--------|------|-----------|------|
| POST | /api/v1/vault/profiles | Crear perfil | SUPER_ADMIN |
| GET | /api/v1/vault/profiles | Llistar perfils | Tots |
| GET | /api/v1/vault/profiles/{id} | Veure perfil complert | Tots |
| PUT | /api/v1/vault/profiles/{id} | Actualitzar perfil | SUPER_ADMIN |
| DELETE | /api/v1/vault/profiles/{id} | Desactivar perfil | SUPER_ADMIN |
| POST | /api/v1/vault/profiles/{pId}/phases | Afegir fase | SUPER_ADMIN |
| PUT | /api/v1/vault/profiles/{pId}/phases/{phId} | Actualitzar fase | SUPER_ADMIN |
| DELETE | /api/v1/vault/profiles/{pId}/phases/{phId} | Eliminar fase | SUPER_ADMIN |
| POST | /api/v1/vault/phases/{phId}/services | Afegir servei a fase | SUPER_ADMIN |
| POST | /api/v1/vault/services | Crear servei add-on | SUPER_ADMIN |
| GET | /api/v1/vault/services | Llistar serveis | Tots |
| GET | /api/v1/vault/tenants/{tId}/budget | Generar pressupost | SUPER_ADMIN, ADMIN, CLIENT (propi) |
| POST | /api/v1/vault/tenants/{tId}/profiles/{pId} | Assignar perfil | SUPER_ADMIN, ADMIN |
| DELETE | /api/v1/vault/tenants/{tId}/profiles/{pId} | Desassignar perfil | SUPER_ADMIN, ADMIN |
| GET | /api/v1/vault/tenants/{tId}/setup | Estat configuració | SUPER_ADMIN, ADMIN, CLIENT (propi) |
| PUT | /api/v1/vault/tenants/{tId}/services/{sId}/fields/{fId} | Establir credencial | SUPER_ADMIN, ADMIN |
| POST | /api/v1/vault/tenants/{tId}/services/{sId}/verify | Verificar servei | SUPER_ADMIN, ADMIN |
| POST | /api/v1/vault/tenants/{tId}/services/{sId}/request | Sol·licitar info al client | SUPER_ADMIN, ADMIN |
| POST | /api/v1/vault/communication/{reqId}/respond | Webhook resposta client | Intern (API Key) |
| POST | /api/v1/vault/tenants/{tId}/profiles/{pId}/confirm-phase | Confirmar fase | CLIENT (propi), ADMIN |
| POST | /api/v1/vault/tenants/{tId}/addons/{sId} | Afegir add-on | SUPER_ADMIN, ADMIN |
| DELETE | /api/v1/vault/tenants/{tId}/addons/{sId} | Eliminar add-on | SUPER_ADMIN, ADMIN |

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
- En eliminar un tenant, totes les seves credencials s'eliminen.

---

## 7. Tests (QA)

### 7.1 Funcionals

| # | Cas | Resultat |
|---|-----|---------|
| 1 | Crear perfil amb 2 fases i 3 serveis | 201 |
| 2 | Assignar perfil a tenant | 201, currentPhase = primera fase, configStatus=PENDING |
| 3 | Configurar tots els camps d'un servei | `configStatus` = `CONFIGURED` |
| 4 | Sol·licitar informació al client | 200, CommunicationRequest creat, servei AWAITING_CLIENT |
| 5 | Client respon amb API Key via webhook | 200, credencial guardada xifrada, servei CONFIGURED |
| 6 | Client respon confirmació (button click) | 200, fase completa, avança a següent fase |
| 7 | Tots els serveis d'una fase configurats | `phaseStatus` = AWAITING_CONFIRMATION, notificació enviada |
| 8 | Confirmar fase (CLIENT) | 200, fase COMPLETED, avança o finalitza |
| 9 | CLIENT veu estat configuració (emmascarat) | 200, valors emmascarats |
| 10 | ADMIN veu estat configuració (en clar) | 200, valors en clar |
| 11 | CommunicationRequest expira | Estat EXPIRED, ADMIN pot reenviar |
| 12 | Afegir servei add-on a tenant | 201, apareix a setup |
| 13 | CLIENT no pot establir credencials | 403 |
| 14 | Pressupost d'un perfil | GET /budget amb profileId | 200, total = suma de serveis |
| 15 | CLIENT veu pressupost sense costos | GET /budget | 200, no mostra `cost` |
| 16 | Crear servei amb salePrice <= cost | POST /services | 400 |

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
| Mòdul 01 (Tenant) | preferredChannel, contactPhone per comunicació guiada | Forta |
| Mòdul 04 (Engine) | Servirà landings als serveis type=LANDING | Debil |
| Mòdul 10 (Automations) | Executarà workflows referenciats | Debil |
| **Mòdul futur: Comunicacions** | Adaptadors per WhatsApp/Telegram/Email | Forta |

---

## 9. Obert / Pendents

- [ ] **Mòdul de Comunicacions**: Implementar adaptadors concrets per a cada canal (WhatsApp Business API, Telegram Bot API, SMTP). Aquest mòdul exposarà una interfície unificada que Vault consumirà per enviar missatges i rebre webhooks.
- [ ] Definir perfils inicials concrets (Pla Bàsic, Pla Intermedi, Pla Avançat)
- [ ] Verificador de connexió: implementar només per SMTP en primera versió
- [ ] Definir plantilles de missatges per a cada tipus de sol·licitud (REQUEST_CREDENTIAL, REQUEST_PERMISSION, REQUEST_CONFIRMATION, REQUEST_INFO)
- [ ] Implementar reintent automàtic per CommunicationRequest expirat (al cap de 7 dies, reenviar)
- [ ] Dashboard ADMIN: vista de "Configuracions pendents de client" amb totes les sol·licituds AWAITING_CLIENT
