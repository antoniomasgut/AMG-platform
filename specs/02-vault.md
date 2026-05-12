# Mòdul 02: Perfils de Servei + Vault (AES-256)

> **Versió:** 1.0
> **Data:** 2026-05-12
> **Dependències:** Mòdul 01 (Auth) — tots els endpoints requereixen JWT + RBAC

---

## 1. Objectius

- Definir **perfils de servei** (paquets) que contenen **fases** → **serveis** → **credencials**
- Assignar perfils a clients i guiar la configuració fase per fase
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

#### Service (Servei configurable)

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
| serviceId | UUID | @Column(nullable=false) | FK a Service |
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
| startedAt | Instant | @CreatedDate | |
| completedAt | Instant | @Column | Quan es completa tot el perfil |

**Restricció única:** `(tenantId, profileId)`

#### TenantService (Servei per tenant)

Cada servei (del perfil o add-on) té un registre per tenant.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | Tenant |
| serviceId | UUID | @Column(nullable=false) | FK a Service |
| phaseId | UUID | @Column(nullable=true) | Fase a la qual pertany (null si add-on) |
| isConfigured | Boolean | @Column(nullable=false) | Si totes les credencials requerides estan configurades |
| configuredAt | Instant | @Column | Quan es va completar la configuració |
| createdAt | Instant | @CreatedDate | |
| updatedAt | Instant | @LastModifiedDate | |

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

Quan s'afegeix un servei a la carta, es regeustra aquí (a més de `TenantService`).

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | Tenant |
| serviceId | UUID | @Column(nullable=false) | FK a Service (isAddon=true) |
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
        └── Service (ordenats per sortOrder dins la fase)
              └── CredentialField (camps del formulari)

TenantProfile → ServiceProfile (assignació) → currentPhaseId
TenantService → Service (per tenant) → isConfigured
TenantCredential → CredentialField (valor xifrat per tenant)
```

- Un perfil pot tenir 0 o més fases
- Una fase pot tenir 0 o més serveis
- Un servei pot tenir 0 o més camps de credencials
- Un tenant pot tenir múltiples perfils
- Un servei add-on (`isAddon=true`) es pot afegir a qualsevol tenant independentment del perfil

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

Retorna l'estat de tots els perfils, fases i serveis del tenant, amb indicació de què està pendent i què està configurat.

Response:
```json
{
  "profiles": [
    {
      "profile": { "id": "uuid", "name": "Pla Avançat", "slug": "pla-avancat" },
      "currentPhase": { "id": "uuid", "name": "Configuració bàsica", "sortOrder": 1 },
      "progress": { "configured": 1, "pending": 1, "total": 6 },
      "phases": [
        {
          "phase": { "id": "uuid", "name": "Configuració bàsica", "sortOrder": 1 },
          "services": [
            {
              "service": { "id": "uuid", "name": "WhatsApp Business", "type": "CREDENTIALS" },
              "isConfigured": true,
              "fields": [
                { "id": "uuid", "key": "apiKey", "label": "API Key", "isSet": true, "maskedValue": "***key" }
              ]
            }
          ]
        }
      ]
    }
  ],
  "addons": [
    { "service": { "id": "uuid", "name": "Landing extra" }, "isConfigured": false }
  ]
}
```

- CLIENT veu `maskedValue`; SUPER_ADMIN i ADMIN veuen `clearValue`
- `isConfigured` = true quan tots els `CredentialField` requerits del servei tenen `isSet=true`

### 4.5 Gestió de credencials

#### `PUT /api/v1/vault/tenants/{tenantId}/services/{serviceId}/fields/{fieldId}` — Establir credencial

**Rols:** SUPER_ADMIN, ADMIN

Request:
```json
{ "value": "la-api-key-en-clar" }
```

Response 200: `{ "id": "uuid", "isSet": true, "maskedValue": "***key" }`

Quan s'estableix l'últim camp d'un servei, el `TenantService.isConfigured` passa a `true`.

#### `POST /api/v1/vault/tenants/{tenantId}/services/{serviceId}/verify` — Verificar servei

**Rols:** SUPER_ADMIN, ADMIN

Response 200: `{ "verified": true, "message": "Connexió correcta" }`

(Lògica de verificació depèn del tipus de servei. Per fases.)

### 4.6 Serveis add-on

#### `POST /api/v1/vault/tenants/{tenantId}/addons/{serviceId}` — Afegir servei add-on

**Rols:** SUPER_ADMIN, ADMIN

El servei ha de tenir `isAddon=true`. Crea `TenantService` + `TenantServiceAddon`.

#### `DELETE /api/v1/vault/tenants/{tenantId}/addons/{serviceId}` — Eliminar servei add-on

**Rols:** SUPER_ADMIN, ADMIN

### 4.7 Mapa complet d'endpoints

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
| GET | /vault/tenants/{tId}/budget | Generar pressupost | SUPER_ADMIN, ADMIN, CLIENT (propi) |
| POST | /vault/tenants/{tId}/profiles/{pId} | Assignar perfil | SUPER_ADMIN, ADMIN |
| DELETE | /vault/tenants/{tId}/profiles/{pId} | Desassignar perfil | SUPER_ADMIN, ADMIN |
| GET | /vault/tenants/{tId}/setup | Estat configuració | SUPER_ADMIN, ADMIN, CLIENT (propi) |
| PUT | /vault/tenants/{tId}/services/{sId}/fields/{fId} | Establir credencial | SUPER_ADMIN, ADMIN |
| POST | /vault/tenants/{tId}/services/{sId}/verify | Verificar servei | SUPER_ADMIN, ADMIN |
| POST | /vault/tenants/{tId}/addons/{sId} | Afegir add-on | SUPER_ADMIN, ADMIN |
| DELETE | /vault/tenants/{tId}/addons/{sId} | Eliminar add-on | SUPER_ADMIN, ADMIN |

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
| 2 | Assignar perfil a tenant | 201, currentPhase = primera fase |
| 3 | Configurar tots els serveis d'una fase | `isConfigured` del servei = true |
| 4 | Completar una fase → avança a la següent | currentPhase actualitzat |
| 5 | CLIENT veu estat configuració (emmascarat) | 200, valors emmascarats |
| 6 | ADMIN veu estat configuració (en clar) | 200, valors en clar |
| 7 | Afegir servei add-on a tenant | 201, apareix a setup |
| 8 | CLIENT no pot establir credencials | 403 |
| 9 | Pressupost d'un perfil | GET /budget amb profileId | 200, total = suma de serveis |
| 10 | CLIENT veu pressupost sense costos | GET /budget | 200, no mostra `cost` |
| 11 | Crear servei amb salePrice <= cost | POST /services | 400 |

### 7.2 Seguretat

| # | Cas | Resultat |
|---|-----|---------|
| 1 | Valor xifrat a BD | Text il·legible |
| 2 | Accés sense JWT | 401 |
| 3 | CLIENT veu credencials d'altre tenant | 403 |

---

## 8. API pública / Contractes

El Vault exposa **interfícies** al paquet `application/` perquè altres mòduls (especialment 07 Billing) en depenguin sense acoblar-se a la implementació.

### 8.1 VaultService

Interfície principal per a la lògica de cicle de vida de clients (assignació, fases, serveis, credencials).

```java
public interface VaultService {
    AssignProfileResponse assignProfile(UUID tenantId, UUID profileId);
    void removeProfile(UUID tenantId, UUID profileId);
    ApprovePhaseResponse approvePhase(UUID tenantId, UUID phaseId);
    void rejectPhase(UUID tenantId, UUID phaseId);
    void advancePhase(UUID tenantId, UUID phaseId, ImplementationStatus status);
    void changeServiceStatus(UUID tenantId, UUID serviceId, ServiceStatus newStatus);
    void setCredential(UUID tenantId, UUID serviceId, UUID fieldId, String value, UUID userId);
    void verifyService(UUID tenantId, UUID serviceId);
    AddonResponse addAddon(UUID tenantId, UUID serviceId, UUID addedBy);
    void approveAddon(UUID tenantId, UUID serviceId);
    void removeAddon(UUID tenantId, UUID serviceId);
    SetupResponse getSetup(UUID tenantId, boolean includeClearValue);
    MonitoringResponse.InvoiceMonitoring getInvoiceMonitoring(UUID tenantId);
    MonitoringResponse.PaymentMonitoring getPaymentMonitoring(UUID tenantId);
    MonitoringResponse.PhaseMonitoring getPhaseMonitoring(UUID tenantId);
}
```

**Implementació:** `TenantVaultService implements VaultService`

### 8.2 ProfileService

Interfície per a la gestió de catàleg (perfils, fases, serveis, pressupost).

```java
public interface ProfileService {
    ProfileResponse createProfile(CreateProfileRequest request);
    List<ProfileResponse> listProfiles();
    ProfileResponse getProfile(UUID id);
    ProfileResponse updateProfile(UUID id, UpdateProfileRequest request);
    void deactivateProfile(UUID id);
    ProfileResponse addPhase(UUID profileId, CreatePhaseRequest request);
    ProfileResponse updatePhase(UUID profileId, UUID phaseId, UpdatePhaseRequest request);
    void deletePhase(UUID profileId, UUID phaseId);
    ProfileResponse addServiceToPhase(UUID phaseId, CreateServiceRequest request);
    ServiceResponse createAddonService(CreateAddonServiceRequest request);
    List<ServiceResponse> listServices();
    BudgetResponse calculateBudget(UUID profileId, List<UUID> addonIds, boolean includeCost);
}
```

**Implementació:** `ProfileManagementService implements ProfileService`

### 8.3 VaultEncryption (ja és una classe, es manté)

```java
public class VaultEncryption {
    String encrypt(String rawValue);
    String decrypt(String encryptedValue);
    String mask(String rawValue);
}
```

### 8.4 InvoiceService

Interfície per a facturació. El Mòdul 07 Billing o 08 FinOps en proporcionarà la implementació real.

```java
public interface InvoiceService {
    UUID createInvoice(UUID tenantId, UUID phaseId, BigDecimal amount);
    void updateInvoiceStatus(UUID invoiceId, InvoiceStatus status);
}
```

**Stub actual:** `InvoiceServiceStub implements InvoiceService` (retorna `INV-STUB-{UUID}`)

### 8.5 PaymentService

Interfície per a cobrament. El Mòdul 09 Payments en proporcionarà la implementació real.

```java
public interface PaymentService {
    PaymentResult charge(UUID tenantId, UUID invoiceId, BigDecimal amount);
    PaymentResult refund(UUID tenantId, UUID invoiceId);
}
```

**Stub actual:** `PaymentServiceStub implements PaymentService` (sempre retorna success)

### 8.6 Regla d'evolució

Quan una interfície necessiti canvis (ex: afegir un mètode nou), es crea una **nova versió** en lloc de modificar-la:

```java
// Nova versió sense trencar la v1
public interface VaultServiceV2 extends VaultService {
    void someNewMethod(UUID tenantId, UUID something);
}
```

La implementació nova implementa `VaultServiceV2`. Els consumidors antics segueixen usant `VaultService`.

---

## 9. Dependències

| Mòdul | Dependència | Tipus |
|-------|-----------|-------|
| Mòdul 01 (Auth) | Requerit per autenticació | Forta |
| Mòdul 04 (Engine) | Servirà landings als serveis type=LANDING | Debil |
| Mòdul 10 (Automations) | Executarà workflows referenciats | Debil |

**Contractes que exporta:** `VaultService`, `ProfileService`, `VaultEncryption`, `InvoiceService`, `PaymentService`
**Contractes que importa:** Cap (no depèn de cap altre mòdul de negoci)

---

## 10. Obert / Pendents

- [ ] Definir perfils inicials concrets (Pla Bàsic, Pla Intermedi, Pla Avançat)
- [ ] Definir lògica d'avanç de fase (automàtic en completar tots els serveis?)
- [ ] Verificador de connexió: implementar només per SMTP en primera versió
- [ ] Notificacions: email al client quan hi ha serveis pendents de configurar
