# Mòdul 03: Leads CRM

> **Versió:** 1.0
> **Data:** 2026-05-12
> **Autor:** [per determinar]
> **Dependències:** Mòdul 01 (Auth) — tots els endpoints requereixen JWT + RBAC

---

## 1. Objectius

- Proporcionar un CRM senzill per gestionar leads (possibles clients) de cada tenant.
- Gestionar el pipeline de vendes amb etapes fixes: Nou → Contactat → Qualificat → Pressupost → Negociació → Guanyat/Perdut.
- Registrar activitats sobre cada lead (trucades, emails, reunions, notes).
- Assignar leads a usuaris del tenant.
- Multi-tenant: cada tenant veu només els seus leads. SUPER_ADMIN pot veure tots.

---

## 2. Abast

### 2.1 Funcionalitats incloses

- CRUD de leads (nom, email, telèfon, origen, valor estimat, notes, etiquetes).
- Pipeline visual amb 7 etapes fixes.
- Moure leads entre etapes del pipeline (drag & drop o selector).
- Registre d'activitats per lead (tipus, descripció, data).
- Assignació de leads a usuaris del tenant.
- Cerca i filtratge de leads (per etapa, origen, usuari assignat, data).
- Paginació als llistats.
- Dashboard amb comptes per etapa del pipeline.
- Tancament de lead com a "Guanyat" o "Perdut" (amb motiu).

### 2.2 Funcionalitats excloses

- Pipeline stages configurables per tenant (es deixen fixes en v1).
- Automatització d'enviament d'emails/SMS (futur mòdul 10 Automations).
- Importació massiva de leads des de CSV/Excel (futur).
- Integració amb formularis web (futur).
- Scoring/puntuació automàtica de leads.

### 2.3 Actors

| Actor | Descripció | Permisos CRM |
|-------|-----------|--------------|
| SUPER_ADMIN | Propietari de la plataforma | CRUD tots els leads de tots els tenants |
| ADMIN | Personal operatiu | CRUD leads del seu tenant, assignar usuaris |
| CLIENT | Usuari final del tenant | Veure i editar leads assignats al seu tenant |

---

## 3. Model de dades

### 3.1 PipelineStage (enum fix)

```
NEW           → "Nou"
CONTACTED     → "Contactat"
QUALIFIED     → "Qualificat"
PROPOSAL      → "Pressupost enviat"
NEGOTIATION   → "Negociació"
WON           → "Guanyat"
LOST          → "Perdut"
```

Ordre del pipeline: `NEW(0) → CONTACTED(1) → QUALIFIED(2) → PROPOSAL(3) → NEGOTIATION(4) → WON(5) / LOST(5)`

Un lead es pot moure endavant o endarrere entre etapes, excepte:
- De WON/LOST no es pot tornar enrere (requereix reobrir)
- En marcar com a WON, es registra `convertedAt`
- En marcar com a LOST, cal especificar `lostReason`

### 3.2 LeadSource (enum fix)

```
WHATSAPP     → "WhatsApp"
WEB          → "Web / Landing"
REFERRAL     → "Recomanació"
MANUAL       → "Entrada manual"
OTHER        → "Altres"
```

### 3.3 ActivityType (enum fix)

```
CALL         → "Trucada"
EMAIL        → "Email"
MEETING      → "Reunió"
NOTE         → "Nota"
TASK         → "Tasca pendent"
```

### 3.4 Entitats (PostgreSQL)

#### Lead

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | Identificador únic |
| tenantId | UUID | @Column(nullable=false) | Tenant propietari del lead |
| name | String(150) | @Column(nullable=false) | Nom del contacte |
| email | String(150) | @Column | Email del contacte |
| phone | String(20) | @Column | Telèfon del contacte |
| source | Enum | @Enumerated(STRING) | Origen del lead (WHATSAPP, WEB, etc.) |
| stage | Enum | @Enumerated(STRING) | Etapa actual del pipeline (NEW, CONTACTED, etc.) |
| assignedTo | UUID | @Column | Usuari assignat (nullable) |
| estimatedValue | BigDecimal(12,2) | @Column | Valor estimat del deal (nullable) |
| notes | Text | @Column | Notes internes |
| tags | String(500) | @Column | Etiquetes separades per coma (ex: "urgent,vip") |
| lostReason | String(255) | @Column | Motiu de pèrdua (obligatori si stage=LOST) |
| convertedAt | Instant | @Column | Data de conversió a Guanyat |
| isActive | Boolean | @Column(nullable=false) | Si el lead està actiu (default true) |
| createdAt | Instant | @CreatedDate | Data de creació |
| updatedAt | Instant | @LastModifiedDate | Data de darrera modificació |

#### Activity

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | Identificador únic |
| leadId | UUID | @Column(nullable=false) | Lead al qual pertany |
| userId | UUID | @Column(nullable=false) | Usuari que va crear l'activitat |
| type | Enum | @Enumerated(STRING) | Tipus d'activitat (CALL, EMAIL, etc.) |
| description | Text | @Column(nullable=false) | Descripció de l'activitat |
| dueDate | Instant | @Column | Data límit (per a tasques) |
| completedAt | Instant | @Column | Quan es va completar (per a tasques) |
| createdAt | Instant | @CreatedDate | Data de creació |

**Relacions:**
- Un `Tenant` té molts `Lead` (1:N)
- Un `Lead` té moltes `Activity` (1:N)
- Un `User` pot tenir molts `Lead` assignats (1:N)
- Un `User` pot crear moltes `Activity` (1:N)

---

## 4. API REST

Tots els endpoints sota `/api/v1/leads` requereixen autenticació JWT.

### 4.1 Endpoints de leads

#### `POST /api/v1/leads` — Crear lead

**Rols permesos:** SUPER_ADMIN, ADMIN, CLIENT

**Request:**
```json
{
  "name": "string (obligatori)",
  "email": "string (opcional)",
  "phone": "string (opcional)",
  "source": "WHATSAPP | WEB | REFERRAL | MANUAL | OTHER",
  "estimatedValue": 0.00,
  "notes": "string",
  "tags": "string (coma-separat)",
  "assignedTo": "uuid (opcional)"
}
```

**Lògica:**
- Si el rol és CLIENT, `tenantId` es pren del JWT (no del body)
- Si es proporciona `assignedTo`, es verifica que l'usuari pertanyi al mateix tenant
- Stage inicial: `NEW`

**Response 201:**
```json
{
  "id": "uuid",
  "name": "string",
  "email": "string",
  "phone": "string",
  "source": "WHATSAPP",
  "stage": "NEW",
  "assignedTo": { "id": "uuid", "name": "string" },
  "estimatedValue": 0.00,
  "notes": "string",
  "tags": "string",
  "lostReason": null,
  "convertedAt": null,
  "isActive": true,
  "createdAt": "instant",
  "updatedAt": "instant"
}
```

---

#### `GET /api/v1/leads` — Llistar leads

**Rols permesos:** SUPER_ADMIN, ADMIN, CLIENT

**Query params:**
| Paràmetre | Tipus | Descripció |
|-----------|-------|-----------|
| page | Integer | Número de pàgina (default 0) |
| size | Integer | Mida de pàgina (default 20) |
| stage | String | Filtrar per etapa del pipeline |
| source | String | Filtrar per origen |
| assignedTo | UUID | Filtrar per usuari assignat |
| search | String | Cerca per nom, email o telèfon |
| tenantId | UUID | (Només SUPER_ADMIN) Filtrar per tenant |

**Lògica:**
- CLIENT: només veu leads del seu tenant
- ADMIN: només veu leads del seu tenant
- SUPER_ADMIN: pot filtrar per tenantId o veure tots
- CLIENT i ADMIN no poden usar el filtre `tenantId`

**Response 200 (Page):**
```json
{
  "content": [
    {
      "id": "uuid",
      "name": "string",
      "email": "string",
      "phone": "string",
      "source": "WHATSAPP",
      "stage": "NEW",
      "assignedTo": { "id": "uuid", "name": "string" },
      "estimatedValue": 0.00,
      "tags": "string",
      "isActive": true,
      "createdAt": "instant"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

---

#### `GET /api/v1/leads/{id}` — Veure lead

**Rols permesos:** SUPER_ADMIN (qualsevol), ADMIN (del seu tenant), CLIENT (del seu tenant)

**Response 200:** LeadResponse complet (inclou notes, lostReason, convertedAt)

---

#### `PUT /api/v1/leads/{id}` — Actualitzar lead

**Rols permesos:** SUPER_ADMIN, ADMIN, CLIENT

**Request:** Mateixos camps que create, tots opcionals excepte name

**Lògica:**
- Si es canvia `assignedTo`, es verifica que l'usuari pertanyi al mateix tenant
- No es pot canviar `stage` des d'aquest endpoint (usar PATCH /stage)
- No es pot canviar `tenantId`

---

#### `DELETE /api/v1/leads/{id}` — Eliminar lead (desactivació lògica)

**Rols permesos:** SUPER_ADMIN, ADMIN

**Lògica:** Marca `isActive = false` (no s'elimina físicament)

---

#### `PATCH /api/v1/leads/{id}/stage` — Moure lead d'etapa

**Rols permesos:** SUPER_ADMIN, ADMIN, CLIENT

**Request:**
```json
{
  "stage": "CONTACTED | QUALIFIED | PROPOSAL | NEGOTIATION | WON | LOST",
  "lostReason": "string (obligatori si stage=LOST)"
}
```

**Lògica:**
- No cal que el lead hagi passat per totes les etapes intermèdies (es pot moure directe)
- Si stage=WON: registrar `convertedAt`, crear Activity automàtica "Lead guanyat"
- Si stage=LOST: `lostReason` obligatori, crear Activity automàtica "Lead perdut: {motiu}"
- Si el lead estava a WON/LOST i es mou a una altra etapa: reiniciar (reobertura)

---

#### `GET /api/v1/leads/stats` — Estadístiques del pipeline

**Rols permesos:** SUPER_ADMIN, ADMIN, CLIENT

**Query params:** `tenantId` (només SUPER_ADMIN)

**Response 200:**
```json
{
  "total": 42,
  "byStage": {
    "NEW": 10,
    "CONTACTED": 8,
    "QUALIFIED": 6,
    "PROPOSAL": 5,
    "NEGOTIATION": 4,
    "WON": 5,
    "LOST": 4
  },
  "bySource": {
    "WHATSAPP": 20,
    "WEB": 10,
    "REFERRAL": 5,
    "MANUAL": 5,
    "OTHER": 2
  },
  "conversionRate": 0.119
}
```

---

### 4.2 Endpoints d'activitats

#### `GET /api/v1/leads/{leadId}/activities` — Llistar activitats d'un lead

**Rols permesos:** SUPER_ADMIN, ADMIN, CLIENT

**Query params:** `page`, `size`

**Response 200 (Page):**
```json
{
  "content": [
    {
      "id": "uuid",
      "type": "NOTE",
      "description": "string",
      "user": { "id": "uuid", "name": "string" },
      "dueDate": "instant",
      "completedAt": "instant",
      "createdAt": "instant"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 5,
  "totalPages": 1
}
```

#### `POST /api/v1/leads/{leadId}/activities` — Crear activitat

**Rols permesos:** SUPER_ADMIN, ADMIN, CLIENT

**Request:**
```json
{
  "type": "CALL | EMAIL | MEETING | NOTE | TASK",
  "description": "string (obligatori)",
  "dueDate": "instant (opcional, només per a TASK)"
}
```

**Lògica:** `userId` es pren del JWT automàticament

**Response 201:** ActivityResponse

#### `PATCH /api/v1/leads/{leadId}/activities/{activityId}/complete` — Completar tasca

**Rols permesos:** SUPER_ADMIN, ADMIN, CLIENT

**Lògica:** Marca `completedAt` amb la data actual. Només per a activitats type=TASK.

---

### 4.3 Mapa complet d'endpoints

| Mètode | Ruta | Descripció | Rols |
|--------|------|-----------|------|
| POST | /api/v1/leads | Crear lead | Tots |
| GET | /api/v1/leads | Llistar leads | Tots |
| GET | /api/v1/leads/stats | Estadístiques pipeline | Tots |
| GET | /api/v1/leads/{id} | Veure lead | Tots |
| PUT | /api/v1/leads/{id} | Actualitzar lead | Tots |
| DELETE | /api/v1/leads/{id} | Eliminar lead (lògic) | SUPER_ADMIN, ADMIN |
| PATCH | /api/v1/leads/{id}/stage | Moure lead d'etapa | Tots |
| GET | /api/v1/leads/{leadId}/activities | Llistar activitats | Tots |
| POST | /api/v1/leads/{leadId}/activities | Crear activitat | Tots |
| PATCH | /api/v1/leads/{leadId}/activities/{id}/complete | Completar tasca | Tots |

---

## 5. Seguretat

### 5.1 Autorització (RBAC)

- **SUPER_ADMIN:** Accés total a tots els leads i activitats de tots els tenants.
- **ADMIN:** Accés a leads i activitats del seu tenant. No veu dades d'altres tenants.
- **CLIENT:** Accés a leads i activitats del seu tenant. No pot eliminar leads.
- **Aïllament multi-tenant:** Totes les queries filtren per `tenantId` automàticament basant-se en el JWT.

### 5.2 Validació

- `name` obligatori (màxim 150 caràcters)
- `email` amb format vàlid si es proporciona
- `estimatedValue` no pot ser negatiu
- `lostReason` obligatori si `stage=LOST`
- `tags` màxim 500 caràcters
- `phone` format flexible (no es valida estrictament)

---

## 6. RGPD / LSSI

- **Dades personals:** nom, email, telèfon, notes internes.
- **Base legal:** Interès legítim del tractament de dades de clients potencials.
- **Conservació:** Les dades de leads es conserven mentre el lead estigui actiu. En desactivar-se, es conserven 1 any per anàlisi comercial.
- **Supressió:** En sol·licitud de supressió, es fa anonimització irreversible (email → `deleted-{uuid}@anonymized`, nom → `Contacte eliminat`).
- **Registre d'accessos:** Es registren les operacions CRUD sobre leads (timestamp, userId, acció) per compliment LSSI.

---

## 7. Tests (QA)

### 7.1 Test cases funcionals

| # | Cas | Acció | Resultat esperat |
|---|-----|-------|-----------------|
| 1 | Crear lead | POST /leads amb dades vàlides | 201 + lead creat amb stage=NEW |
| 2 | Crear lead sense nom | POST /leads sense name | 400 |
| 3 | Llistar leads (CLIENT) | GET /leads | 200 + només leads del seu tenant |
| 4 | Llistar leads (ADMIN) | GET /leads | 200 + només leads del seu tenant |
| 5 | Llistar leads (SUPER_ADMIN) | GET /leads | 200 + tots els leads |
| 6 | Filtrar per etapa | GET /leads?stage=NEW | 200 + només leads en NEW |
| 7 | Cercar per nom | GET /leads?search=Joan | 200 + leads que contenen "Joan" |
| 8 | Moure lead a CONTACTED | PATCH /leads/{id}/stage amb stage=CONTACTED | 200 |
| 9 | Moure lead a LOST sense motiu | PATCH /leads/{id}/stage amb stage=LOST | 400 |
| 10 | Moure lead a WON | PATCH amb stage=WON | 200 + convertedAt establert |
| 11 | Reobrir lead WON | PATCH a etapa diferent | 200 + convertedAt a null |
| 12 | CLIENT elimina lead | DELETE /leads/{id} | 403 |
| 13 | ADMIN elimina lead | DELETE /leads/{id} | 204 + isActive=false |
| 14 | Crear activitat NOTE | POST /leads/{id}/activities | 201 |
| 15 | Llistar activitats | GET /leads/{id}/activities | 200 + llista d'activitats |
| 16 | Completar tasca | PATCH /leads/{id}/activities/{aid}/complete | 200 + completedAt establert |
| 17 | Estadístiques pipeline | GET /leads/stats | 200 + comptes per etapa |
| 18 | CLIENT veu lead d'altre tenant | GET /leads/{id-daltre-tenant} | 403 o 404 |
| 19 | Crear lead amb assignedTo incorrecte | assignedTo usuari d'altre tenant | 400 |

---

## 8. Dependències entre mòduls

| Mòdul | Dependència | Tipus |
|-------|-----------|-------|
| Mòdul 03 (Leads) | Mòdul 01 (Auth) | Forta — requereix JWT + RBAC + multi-tenant |
| Mòdul 03 (Leads) | Mòdul 13 (i18n) | Feba — frontend amb traduccions |

---

## 9. Obert / Pendents

- [ ] Decidir si els leads es poden reassignar entre tenants (SUPER_ADMIN)
- [ ] Confirmar si volem notificacions automàtiques en assignar un lead (email/WhatsApp)
- [ ] Decidir si afegir camps personalitzats (customFields JSON) ja a v1
- [ ] Confirmar si l'estadística de conversionRate ha de ser global o per tenant
- [ ] Decidir si les etiquetes (tags) es fan com a String CSV o entitat separada (Tag)
