# Mòdul 03: Leads CRM

> **Versió:** 1.2
> **Data:** 2026-06-11
> **Dependències:** Mòdul 01 (Auth) — tots els endpoints requereixen JWT + RBAC

---

## 1. Objectius

- Gestionar leads (contactes comercials) dels tenants amb pipeline de vendes
- Registrar activitats sobre cada lead (trucades, emails, reunions, notes, tasques)
- Proporcionar estadístiques del pipeline per rol (SUPER_ADMIN global, ADMIN/CLIENT per tenant)
- Suportar soft-delete i canvi d'etapa amb traçabilitat

**Model Lead = Client:** no existeix una entitat "Client" separada. Quan un lead arriba a
l'etapa `WON`, es considera client convertit. La F4 (Seguiment) treballa sobre leads
amb `convertedAt != null`. Un contacte que torna a escriure (via xat, WA o email) es
localitza pel telèfon (`findFirstByTenantIdAndPhone`) i es reutilitza el mateix registre —
mai es crea un duplicat.

---

## 2. Abast

### 2.1 Funcionalitats incloses

- CRUD de leads (nom, email, telèfon, font, valor estimat, notes, etiquetes, dades d'entrevista)
- Pipeline de 7 etapes: NEW → CONTACTED → QUALIFIED → PROPOSAL → NEGOTIATION → WON / LOST
- Canvi d'etapa amb validació (LOST requereix motiu, WON registra data de conversió)
- Obertura de leads tancats (WON/LOST → altra etapa neteja convertedAt)
- Soft-delete (isActive=false) només per SUPER_ADMIN i ADMIN
- Activitats per lead (tipus, descripció, data de venciment, completar tasca)
- Estadístiques del pipeline (total, per etapa, per font, taxa de conversió)
- Aïllament multi-tenant (CLIENT i ADMIN només veuen leads del seu tenant)
- SUPER_ADMIN pot filtrar per tenantId

### 2.2 Funcionalitats excloses

- Enviament d'emails des del CRM (futur)
- Importació massiva de leads (CSV)
- Integració amb formularis de landing (Mòdul 04 Engine)
- Automatitzacions de seguiment (Mòdul 10 Automations)

### 2.3 Actors

| Actor | Descripció | Permisos |
|-------|-----------|----------|
| SUPER_ADMIN | Opera tota la plataforma | CRUD qualsevol lead de qualsevol tenant, estadístiques globals |
| ADMIN | Personal operatiu | CRUD leads del seu tenant, activitats, estadístiques del tenant |
| CLIENT | Usuari final | Veure leads del seu tenant, activitats, estadístiques. NO pot eliminar leads |

---

## 3. Model de dades

### 3.1 Entitats (PostgreSQL)

#### Lead

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | FK lògica a Tenant |
| name | String(150) | @Column(nullable=false) | Nom del contacte |
| email | String(150) | @Column | Email |
| phone | String(20) | @Column | Telèfon |
| source | Enum(STRING) | @Enumerated | `WHATSAPP`, `WEB`, `LANDING_FORM`, `CHAT_WIDGET`, `EMAIL`, `REFERRAL`, `MANUAL`, `OTHER` |
| stage | Enum(STRING) | @Enumerated | `NEW` (per defecte) |
| assignedTo | UUID | @Column | FK lògica a User (responsable) |
| estimatedValue | BigDecimal(12,2) | @Column(precision=12, scale=2) | Valor estimat |
| notes | TEXT | @Lob | Notes internes |
| tags | String(500) | @Column(length=500) | Etiquetes separades per coma |
| lostReason | String(255) | @Column | Obligatori si stage=LOST |
| convertedAt | Instant | @Column | Quan es va marcar com WON (= data de conversió a client) |
| lastContactAt | Instant | @Column | Última vegada que el contacte va escriure (qualsevol canal) |
| lastServiceAt | Instant | @Column | Última data de servei confirmat (F2: quan es crea cita) |
| hasWhatsapp | Boolean | @Column | Si el contacte usa WhatsApp |
| interviewNotes | TEXT | @Column(columnDefinition="TEXT") | Notes de l'entrevista comercial (lliure) |
| webNeed | String(30) | @Column(name="web_need") | Necessitat web detectada (ex. `LANDING_BASIC`, `LANDING_PRO`, `NONE`) |
| interviewSector | String(50) | @Column(name="interview_sector") | Sector detectat durant l'entrevista |
| interviewBusinessSize | String(30) | @Column(name="interview_business_size") | Mida d'empresa detectada durant l'entrevista (`AUTONOMO`, `PETIT`, `MITJA`) |
| isActive | Boolean | @Column(nullable=false) | Per soft-delete, default true |
| createdAt | Instant | @CreatedDate | |
| updatedAt | Instant | @LastModifiedDate | |

**PipelineStage enum:** `NEW`, `CONTACTED`, `QUALIFIED`, `PROPOSAL`, `NEGOTIATION`, `WON`, `LOST`

| Etapa | Significat pràctic |
|-------|-------------------|
| NEW | Contacte acabat d'entrar (via xat, WA, email, formulari) |
| CONTACTED | S'ha respost o iniciat conversa |
| QUALIFIED | Ha mostrat interès real, es coneix la necessitat |
| PROPOSAL | Se li ha enviat pressupost o proposta |
| NEGOTIATION | En procés de tancament |
| WON | **Client convertit** — `convertedAt` establert |
| LOST | No ha prosperat — `lostReason` obligatori |

**LeadSource enum:** `WHATSAPP`, `WEB`, `LANDING_FORM`, `CHAT_WIDGET`, `EMAIL`, `REFERRAL`, `MANUAL`, `OTHER`

#### Activity

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| leadId | UUID | @Column(nullable=false) | FK lògica a Lead |
| userId | UUID | @Column(nullable=false) | FK lògica a User (qui ho va crear) |
| type | Enum(STRING) | @Enumerated | `CALL`, `EMAIL`, `MEETING`, `NOTE`, `TASK` |
| description | TEXT | @Column(nullable=false, columnDefinition="TEXT") | Descripció |
| dueDate | Instant | @Column | Data de venciment (per TASK) |
| completedAt | Instant | @Column | Quan es va completar (per TASK) |
| createdAt | Instant | @CreatedDate | |

**ActivityType enum:** `CALL`, `EMAIL`, `MEETING`, `NOTE`, `TASK`

### 3.2 Cicle de vida Lead → Client

```
Contacte entra (xat/WA/email/formulari)
        ↓
findFirstByTenantIdAndPhone  ←── Si existeix, reutilitza el registre
        ↓ (no existeix)
Lead creat (stage=NEW, source=CHAT_WIDGET|WHATSAPP|...)
        ↓
Conversa / seguiment manual
        ↓
stage=WON → convertedAt=now() ← és "client" a partir d'aquí
        ↓
lastContactAt s'actualitza cada vegada que torna a escriure
lastServiceAt s'actualitza quan F2 confirma una cita
        ↓
F4 (Seguiment): filtra per convertedAt != null
  → follow-up si lastServiceAt > N dies
  → reenganchament si lastContactAt > M mesos
```

### 3.3 Canvis d'etapa automàtics

- En canviar a **WON**: es registra `convertedAt = now()`, es crea Activity "Lead guanyat"
- En canviar a **LOST**: es requereix `lostReason`, es crea Activity "Lead perdut: {motiu}"
- En reobrir (WON/LOST → altra etapa): es neteja `convertedAt`, es crea Activity "Lead canviat a {ETAPA}"
- Altres canvis: es crea Activity "Lead canviat a {ETAPA}"

---

## 4. API REST

Prefix base: `/api/v1/leads`

### 4.1 Gestió de leads

| Mètode | Ruta | Descripció | Rols |
|--------|------|-----------|------|
| POST | /api/v1/leads | Crear lead | Tots autenticats |
| GET | /api/v1/leads | Llistar leads (paginate, filter) | Tots autenticats |
| GET | /api/v1/leads/stats | Estadístiques del pipeline | Tots autenticats |
| GET | /api/v1/leads/{id} | Veure lead | Tots autenticats (propi tenant) |
| PUT | /api/v1/leads/{id} | Actualitzar lead | Tots autenticats (propi tenant) |
| DELETE | /api/v1/leads/{id} | Soft-delete lead | SUPER_ADMIN, ADMIN |
| PATCH | /api/v1/leads/{id}/stage | Canviar etapa del pipeline | Tots autenticats |

### 4.2 Activitats

| Mètode | Ruta | Descripció | Rols |
|--------|------|-----------|------|
| GET | /api/v1/leads/{leadId}/activities | Llistar activitats d'un lead | Tots autenticats |
| POST | /api/v1/leads/{leadId}/activities | Crear activitat | Tots autenticats |
| PATCH | /api/v1/leads/{leadId}/activities/{activityId}/complete | Completar tasca | Tots autenticats |

### 4.3 Detall d'endpoints

#### `POST /api/v1/leads` — Crear lead

Request:
```json
{
  "name": "Joan Servera",
  "email": "joan@example.com",
  "phone": "+34600123456",
  "source": "WEB",
  "assignedTo": "uuid",
  "estimatedValue": 500.00,
  "notes": "Interessat en pla avançat",
  "tags": "hot, WhatsApp"
}
```

Response 201: LeadResponse (veure 4.4)

**Errors:**
| Codi | Situació |
|------|---------|
| 400 | Name buit, email mal format |

#### `GET /api/v1/leads` — Llistar leads

**Query params:** `page`, `size`, `sort`, `stage`, `source`, `assignedTo`, `search`, `tenantId` (SUPER_ADMIN només)

#### `GET /api/v1/leads/stats` — Estadístiques

Response 200:
```json
{
  "total": 25,
  "byStage": { "NEW": 10, "CONTACTED": 5, "QUALIFIED": 4, "PROPOSAL": 3, "NEGOTIATION": 2, "WON": 1, "LOST": 0 },
  "bySource": { "WHATSAPP": 10, "WEB": 8, "REFERRAL": 3, "MANUAL": 2, "OTHER": 2 },
  "conversionRate": 0.04
}
```

#### `PATCH /api/v1/leads/{id}/stage` — Canviar etapa

Request:
```json
{ "stage": "LOST", "lostReason": "No tenia pressupost" }
```

Response 200: LeadResponse actualitzat

### 4.4 LeadResponse

```json
{
  "id": "uuid",
  "name": "Joan Servera",
  "email": "joan@example.com",
  "phone": "+34600123456",
  "source": "WEB",
  "stage": "QUALIFIED",
  "assignedTo": { "id": "uuid", "name": "Maria Amengual" },
  "estimatedValue": 500.00,
  "notes": "Interessat en pla avançat",
  "tags": "hot, WhatsApp",
  "lostReason": null,
  "convertedAt": null,
  "isActive": true,
  "createdAt": "2026-05-13T10:00:00Z",
  "updatedAt": "2026-05-13T10:00:00Z"
}
```

### 4.5 ActivityResponse

```json
{
  "id": "uuid",
  "type": "CALL",
  "description": "Trucada de presentació",
  "user": { "id": "uuid", "name": "Maria Amengual" },
  "dueDate": null,
  "completedAt": null,
  "createdAt": "2026-05-13T10:00:00Z"
}
```

---

## 5. Seguretat

### 5.1 Autenticació
- Tots els endpoints requereixen JWT excepte els públics (cap en aquest mòdul)

### 5.2 Autorització (RBAC)
- **SUPER_ADMIN**: accés global, pot filtrar per tenantId, pot eliminar leads
- **ADMIN**: accés només al seu tenant, pot eliminar leads
- **CLIENT**: accés només al seu tenant, NO pot eliminar leads
- Aïllament per tenant: l'accés a leads d'altre tenant retorna 404 (no 403) per evitar leakage

---

## 6. RGPD / LSSI

- **Dades personals:** Nom, email, telèfon (Lead)
- **Base legal:** Interès legítim (relació comercial)
- **Conservació:** Mentre el lead estigui actiu. En soft-delete (isActive=false), les dades es conserven
- **Supressió:** No implementada (cal purga manual o batch)

---

## 7. Tests (QA)

Els tests d'integració es troben a `backend/src/test/java/com/amg/digitalitzacio/leads/api/LeadControllerTest.java` (16 tests).

### 7.1 Funcionals

| # | Cas | Resultat |
|---|-----|---------|
| 1 | Crear lead amb dades vàlides | 201 |
| 2 | Crear lead sense nom | 400 |
| 3 | Llistar leads paginats | 200 |
| 4 | Filtrar per etapa | 200, només els de l'etapa |
| 5 | Cercar per nom | 200, coincidències |
| 6 | Veure lead per ID | 200 |
| 7 | Actualitzar lead | 200, camps actualitzats |
| 8 | ADMIN elimina lead | 204 |
| 9 | CLIENT elimina lead | 403 |
| 10 | Canviar etapa a CONTACTED | 200 |
| 11 | Canviar a LOST sense motiu | 400 |
| 12 | Canviar a WON | 200, convertedAt establert |
| 13 | Estadístiques del pipeline | 200, total + byStage + conversionRate |
| 14 | Crear activitat | 201 |
| 15 | Llistar activitats | 200 |
| 16 | CLIENT no pot accedir lead d'altre tenant | 404 |

---

## 8. Dependències

| Mòdul | Dependència | Tipus |
|-------|-----------|-------|
| Mòdul 01 (Auth) | Autenticació JWT + RBAC + entitats User/Tenant | Forta |

---

## 9. Obert / Pendents

- [ ] `isActive` no es filtra als llistats (els leads soft-deleted segueixen apareixent)
- [ ] No hi ha endpoint per purgar leads vells
- [ ] `assignedTo` es resol amb `userRepository.findById()` per cada lead (N+1 en llistats)
