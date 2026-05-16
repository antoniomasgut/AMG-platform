# Mòdul 02b: CRUD de Perfils, Fases i Serveis (Admin)

> **Versió:** 1.0
> **Data:** 2026-05-15
> **Dependències:** Mòdul 02 (Vault) — entitats JPA existents, endpoints base
> **Basat en:** spec 02-vault.md (model de dades secció 3), perfils-cataleg.md, serveis-cataleg.md

---

## 1. Objectius

- CRUD complet de perfils de servei (paquets) per a SUPER_ADMIN
- CRUD de fases dins d'un perfil (agrupació de serveis per ordre d'implementació)
- CRUD de serveis, que poden pertànyer a una fase O directament a un perfil
- Visió completa de l'estructura: perfil → fases + serveis directes → serveis dins de fase
- L'ADMIN (no SUPER_ADMIN) pot veure perfils i assignar-los, però NO crear/editar/eliminar

---

## 2. Model de dades (modificació sobre 02-vault.md)

### Canvis respecte a l'spec original

| Entitat | Canvi |
|---------|-------|
| CatalogService | Afegir `profileId` (UUID, nullable, FK a ServiceProfile) |

Un servei pot estar en **un** dels tres estats:
- `phaseId != null` → servei dins d'una fase
- `profileId != null` → servei directe del perfil (fora de fase)
- `isAddon = true` → servei add-on (no pertany a cap perfil ni fase)

**Regla de negoci:** Un servei NO pot tenir `phaseId` i `profileId` alhora.

### Model conceptual

```
ServiceProfile (perfil)
├── serveis directes (profileId ≠ null, phaseId = null)
│     └── CatalogService: "SMTP Corporatiu"
│     └── CatalogService: "Google Analytics"
├── fases (ordenades per sortOrder)
│     ├── Phase: "F1: Landing"
│     │     └── CatalogService: "Landing Pro"
│     │     └── CatalogService: "Galeria fotos"
│     └── Phase: "F2: CRM"
│           └── CatalogService: "CRM + Pipeline"
│           └── CatalogService: "Reenganxament"
└── (heretats indirectament) — els serveis dins de fases també són del perfil

Add-ons globals (isAddon = true):
└── CatalogService: "Landing extra"
```

---

## 3. API REST

Base: `/api/v1/vault` — Tots els endpoints requereixen `Authorization: Bearer JWT`

### 3.1 Perfils (ServiceProfile)

#### `GET /api/v1/vault/profiles` — Llistar perfils

**Rols:** SUPER_ADMIN, ADMIN

Query params: `?search=`, `?page=0`, `?size=20`

Response 200:
```json
{
  "content": [
    {
      "id": "uuid",
      "name": "Professionals amb cita",
      "slug": "professionals-cita",
      "description": "Perfil per a fisioterapeutes, dentistes, perruqueries...",
      "isActive": true,
      "phaseCount": 5,
      "serviceCount": 9,
      "directServiceCount": 2,
      "createdAt": "2026-05-15T10:00:00Z"
    }
  ],
  "totalElements": 7,
  "totalPages": 1
}
```

#### `GET /api/v1/vault/profiles/{id}` — Veure perfil complet

**Rols:** SUPER_ADMIN, ADMIN

Response 200:
```json
{
  "id": "uuid",
  "name": "Professionals amb cita",
  "slug": "professionals-cita",
  "description": "Perfil per a...",
  "isActive": true,
  "directServices": [
    {
      "id": "uuid",
      "name": "SMTP Corporatiu",
      "slug": "smtp-corporatiu",
      "type": "CREDENTIALS",
      "cost": 5.00,
      "salePrice": 30.00
    }
  ],
  "phases": [
    {
      "id": "uuid",
      "name": "Configuració bàsica",
      "description": null,
      "sortOrder": 1,
      "services": [
        {
          "id": "uuid",
          "name": "WhatsApp Business",
          "slug": "whatsapp-business",
          "type": "CREDENTIALS",
          "cost": 10.00,
          "salePrice": 50.00,
          "sortOrder": 1
        }
      ]
    }
  ],
  "totalPrice": 330.00,
  "createdAt": "2026-05-15T10:00:00Z",
  "updatedAt": "2026-05-15T10:00:00Z"
}
```

#### `POST /api/v1/vault/profiles` — Crear perfil

**Rols:** SUPER_ADMIN

```json
{
  "name": "Professionals amb cita",
  "slug": "professionals-cita",
  "description": "Perfil per a negocis amb cita prèvia"
}
```

Response 201: perfil creat (id, name, slug, isActive=true, createdAt)

#### `PUT /api/v1/vault/profiles/{id}` — Actualitzar perfil

**Rols:** SUPER_ADMIN

```json
{
  "name": "Professionals amb cita (nou nom)",
  "slug": "professionals-cita",
  "description": "Descripció actualitzada",
  "isActive": true
}
```

Response 200: perfil actualitzat

#### `DELETE /api/v1/vault/profiles/{id}` — Desactivar/Eliminar perfil

**Rols:** SUPER_ADMIN

Per soft-delete: posa `isActive = false`. Si no té fases ni serveis assignats a tenants, elimina físicament.

Response 204

---

### 3.2 Fases (Phase)

#### `POST /api/v1/vault/profiles/{profileId}/phases` — Afegir fase

**Rols:** SUPER_ADMIN

```json
{
  "name": "Configuració bàsica",
  "description": "WhatsApp + SMTP",
  "sortOrder": 1
}
```

Response 201:
```json
{
  "id": "uuid",
  "name": "Configuració bàsica",
  "sortOrder": 1,
  "services": []
}
```

#### `PUT /api/v1/vault/profiles/{profileId}/phases/{phaseId}` — Actualitzar fase

**Rols:** SUPER_ADMIN

```json
{
  "name": "Configuració bàsica (editat)",
  "description": "Nova descripció",
  "sortOrder": 2
}
```

Response 200: fase actualitzada

#### `DELETE /api/v1/vault/profiles/{profileId}/phases/{phaseId}` — Eliminar fase

**Rols:** SUPER_ADMIN

Elimina la fase i tots els seus serveis (els CatalogService amb aquest phaseId).

Response 204

**Nota:** Si la fase té serveis assignats a tenants (TenantService), retornar 409 Conflict amb missatge "Fase amb serveis assignats a clients. Desassigna els serveis abans d'eliminar la fase."

---

### 3.3 Serveis (CatalogService)

#### `GET /api/v1/vault/services` — Llistar serveis del catàleg

**Rols:** SUPER_ADMIN, ADMIN

Query params: `?type=LANDING` (filtre opcional per tipus), `?addonOnly=true` (només add-ons)

Response 200: array de serveis

#### `POST /api/v1/vault/profiles/{profileId}/services` — Afegir servei directe al perfil

**Rols:** SUPER_ADMIN

Crea un servei vinculat directament al perfil (sense fase).

```json
{
  "name": "SMTP Corporatiu",
  "slug": "smtp-corporatiu",
  "type": "CREDENTIALS",
  "cost": 5.00,
  "salePrice": 30.00,
  "description": "Correu transaccional"
}
```

Response 201:
```json
{
  "id": "uuid",
  "name": "SMTP Corporatiu",
  "type": "CREDENTIALS",
  "profileId": "uuid",
  "phaseId": null,
  "cost": 5.00,
  "salePrice": 30.00
}
```

#### `POST /api/v1/vault/phases/{phaseId}/services` — Afegir servei a una fase

**Rols:** SUPER_ADMIN

Request (igual que directe):
```json
{
  "name": "WhatsApp Business",
  "slug": "whatsapp-business",
  "type": "CREDENTIALS",
  "cost": 10.00,
  "salePrice": 50.00,
  "sortOrder": 1
}
```

Response 201: servei creat amb phaseId assignat

#### `PUT /api/v1/vault/services/{serviceId}` — Actualitzar servei

**Rols:** SUPER_ADMIN

```json
{
  "name": "WhatsApp Business API",
  "type": "CREDENTIALS",
  "cost": 10.00,
  "salePrice": 55.00,
  "description": "Descripció actualitzada"
}
```

No es pot canviar `slug` ni `phaseId`/`profileId` (cal eliminar i recrear per moure).

Response 200: servei actualitzat

#### `DELETE /api/v1/vault/services/{serviceId}` — Eliminar servei

**Rols:** SUPER_ADMIN

Response 204

**Nota:** Si el servei està assignat a algun tenant (TenantService), retornar 409 Conflict.

#### `POST /api/v1/vault/services` — Crear servei add-on

**Rols:** SUPER_ADMIN

```json
{
  "name": "Landing extra",
  "slug": "landing-extra",
  "type": "LANDING",
  "cost": 25.00,
  "salePrice": 60.00,
  "isAddon": true,
  "description": "Landing addicional per a negocis amb múltiples línies"
}
```

Response 201: servei add-on creat (isAddon=true, phaseId=null, profileId=null)

---

### 3.4 Mapa complet d'endpoints

| Mètode | Ruta | Descripció | Rols |
|--------|------|-----------|------|
| GET | /api/v1/vault/profiles | Llistar perfils | SUPER_ADMIN, ADMIN |
| GET | /api/v1/vault/profiles/{id} | Veure perfil complet | SUPER_ADMIN, ADMIN |
| POST | /api/v1/vault/profiles | Crear perfil | SUPER_ADMIN |
| PUT | /api/v1/vault/profiles/{id} | Actualitzar perfil | SUPER_ADMIN |
| DELETE | /api/v1/vault/profiles/{id} | Desactivar/eliminar perfil | SUPER_ADMIN |
| POST | /api/v1/vault/profiles/{pId}/phases | Afegir fase | SUPER_ADMIN |
| PUT | /api/v1/vault/profiles/{pId}/phases/{phId} | Actualitzar fase | SUPER_ADMIN |
| DELETE | /api/v1/vault/profiles/{pId}/phases/{phId} | Eliminar fase (+ serveis) | SUPER_ADMIN |
| POST | /api/v1/vault/profiles/{pId}/services | Afegir servei directe al perfil | SUPER_ADMIN |
| POST | /api/v1/vault/phases/{phId}/services | Afegir servei a fase | SUPER_ADMIN |
| PUT | /api/v1/vault/services/{sId} | Actualitzar servei | SUPER_ADMIN |
| DELETE | /api/v1/vault/services/{sId} | Eliminar servei | SUPER_ADMIN |
| POST | /api/v1/vault/services | Crear servei add-on | SUPER_ADMIN |
| GET | /api/v1/vault/services | Llistar serveis | SUPER_ADMIN, ADMIN |

**Nous respecte a 02-vault.md:**
- `POST /api/v1/vault/profiles/{pId}/services` — servei directe al perfil
- `PUT /api/v1/vault/services/{sId}` — actualitzar servei
- `DELETE /api/v1/vault/services/{sId}` — eliminar servei

**Canvis respecte a 02-vault.md:**
- `CatalogService` guanya `profileId` (UUID, nullable) al model de dades
- `DELETE /api/v1/vault/profiles/{pId}/phases/{phId}` ara elimina també els serveis fills
- `GET /api/v1/vault/profiles/{id}` inclou `directServices` al response

---

## 4. Canvis al frontend

### 4.1 Pàgina de llistat de perfils (`/portal/admin/vault`)

**Estat:** Ja implementada (pestanya "Perfils" mostra targetes amb nom, slug, descripció, comptador de fases i serveis)

**Millores necessàries:**
- [ ] Afegir comptador de "serveis directes" a cada targeta
- [ ] Botó "Nou servei directe" al detall del perfil (secció separada de les fases)

### 4.2 Pàgina de detall de perfil (`/portal/admin/vault/profiles/{id}`)

**Ja implementat:**
- Capçalera amb nom, slug, edit button
- Stat cards: fases, serveis, preu total
- Gestió de fases: afegir, editar, eliminar
- Modal per afegir servei a fase (seleccionar del catàleg o crear nou)

**Millores necessàries:**
- [ ] Secció "Serveis directes" (abans de les fases): llista de serveis sense fase
- [ ] Botó "Afegir servei directe" que obra un modal (seleccionar catàleg o crear nou, sense fase)
- [ ] Poder eliminar un servei directe del perfil

### 4.3 Detall de tenant (`/portal/admin/tenants/{id}`)

**Ja implementat:**
- Assignar perfil a tenant
- Veure fases i serveis assignats al setup section
- Botó "Configurar Landing" per serveis LANDING en CONFIGURING

---

## 5. Casos d'ús (Admin UI)

### Crear un perfil complet

```
1. SUPER_ADMIN va a /portal/admin/vault → pestanya "Perfils"
2. Clica "Nou perfil" → omple nom, slug, descripció
3. Entra al detall del perfil
4. Clica "Afegir servei directe" → selecciona o crea serveis base (ex: SMTP)
5. Clica "Nova fase" → afegeix fase 1
6. Dins la fase, clica "Afegir servei" → selecciona o crea serveis de la fase
7. Repeteix per a cada fase
8. El perfil queda llest per assignar a tenants
```

### Afegir servei directe vs fase

| Situació | Servei directe | Dins de fase |
|----------|---------------|-------------|
| SMTP corporatiu (sempre va inclòs) | ✅ | ❌ |
| WhatsApp (configurable a qualsevol moment) | ✅ | ❌ |
| Landing (requereix ordre: crear contingut, publicar) | ❌ | ✅ |
| Automatització (depèn d'altres serveis) | ❌ | ✅ |

---

## 6. Tests (QA)

### 6.1 Perfils

| # | Cas | Resultat esperat |
|---|-----|-----------------|
| 1 | Crear perfil només amb nom i slug | 201, isActive=true |
| 2 | Crear perfil amb slug duplicat | 400 |
| 3 | Llistar perfils | 200, array paginat |
| 4 | Veure perfil complet amb fases i serveis | 200, inclou directServices + phases |
| 5 | Actualitzar nom i descripció | 200 |
| 6 | Desactivar perfil | 204, isActive=false |
| 7 | ADMIN intenta crear perfil | 403 |

### 6.2 Fases

| # | Cas | Resultat esperat |
|---|-----|-----------------|
| 8 | Afegir fase a perfil | 201, buida (0 serveis) |
| 9 | Afegir 3 fases amb sortOrder 1,2,3 | 201, ordenades |
| 10 | Actualitzar sortOrder | 200 |
| 11 | Eliminar fase amb serveis | 204, serveis eliminats |
| 12 | Eliminar fase amb serveis assignats a tenants | 409 |

### 6.3 Serveis

| # | Cas | Resultat esperat |
|---|-----|-----------------|
| 13 | Afegir servei directe a perfil | 201, profileId set, phaseId=null |
| 14 | Afegir servei a fase | 201, phaseId set, profileId=null |
| 15 | Crear servei add-on | 201, isAddon=true, sense phaseId ni profileId |
| 16 | Crear servei amb salePrice <= cost | 400 |
| 17 | Actualitzar preu i descripció | 200 |
| 18 | Eliminar servei no assignat | 204 |
| 19 | Eliminar servei assignat a tenant | 409 |
| 20 | Llistar serveis filtrant per type | 200, filtrats |

---

## 7. Canvis a entitats existents

### CatalogService (afegir camp)

```java
@Column(name = "profile_id", nullable = true)
private UUID profileId;
```

**Regles de validació:**
- Si `isAddon = true` → `phaseId = null` i `profileId = null`
- Si `phaseId != null` → `profileId = null`
- Si `profileId != null` → `phaseId = null`
- `slug` es manté unique globalment

### Consultes noves necessàries

```java
// CatalogServiceRepository
List<CatalogService> findByProfileIdAndPhaseIdIsNull(UUID profileId);
List<CatalogService> findByPhaseIdOrderBySortOrder(UUID phaseId);
List<CatalogService> findByIsAddonTrue();
boolean existsByPhaseId(UUID phaseId);  // per comprovar si una fase té serveis
```
