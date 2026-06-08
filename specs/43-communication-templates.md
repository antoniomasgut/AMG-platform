# Mòdul 43: Communication Templates — Plantilles de missatge per WhatsApp i Email

> **Versió:** 1.0
> **Data:** 2026-06-08
> **Dependències:** Mòdul 39 (System Config), Mòdul 15 (Demo), Mòdul 07 (Billing), Mòdul 33 (Absence/Reschedule)

---

## 1. Objectius

- Gestionar **plantilles de missatge** per a WhatsApp i Email, classificades per **acció** i **sector**
- Permetre a l'admin enviar comunicacions des de la plataforma (demo, pressupost, cites)
- Variables dinàmiques com `{NOM_NEGOCI}`, `{URL_DEMO}`, `{DATA_CITA}`, etc.

---

## 2. Abast

### Accions suportades

| Acció | Descripció |
|-------|-----------|
| DEMO_SEND | Enviar demo personalitzada al prospect |
| BUDGET_ACCEPTED | Confirmació d'acceptació de pressupost |
| APPOINTMENT_CONFIRM | Confirmació de cita |
| APPOINTMENT_REMINDER | Recordatori de cita (24h/1h abans) |
| APPOINTMENT_CANCEL | Notificació de cita cancel·lada |

### Canals suportats

| Canal | Proveïdor |
|-------|----------|
| EMAIL | Brevo EU REST API |
| WHATSAPP | Twilio (sistema) |

### Sectors

- `ALL` — plantilla genèrica (fallback si no existeix per al sector específic)
- Qualsevol sector de la plataforma (PERRUQUERIA, RESTAURANTE, etc.)

---

## 3. Model de dades

### Taula: `communication_templates`

| Camp | Tipus | Descripció |
|------|-------|-----------|
| id | UUID PK | Identificador |
| sector | VARCHAR(50) | Sector ('ALL' o valor de sector) |
| channel | VARCHAR(20) | 'EMAIL' o 'WHATSAPP' |
| action | VARCHAR(50) | Acció (DEMO_SEND, etc.) |
| subject | VARCHAR(200) | Assumpte (només EMAIL) |
| body | TEXT | Cos del missatge amb variables `{VAR}` |
| language | VARCHAR(5) DEFAULT 'ca' | Idioma: ca / es / en / de |
| is_active | BOOLEAN DEFAULT true | Si la plantilla és activa |
| sort_order | INT DEFAULT 0 | Ordre de visualització |
| created_at | TIMESTAMPTZ | Auditing |
| updated_at | TIMESTAMPTZ | Auditing |
| UNIQUE(sector, channel, action, language) | — | Una plantilla per combinació + idioma |

### Variables disponibles per acció

| Acció | Variables |
|-------|----------|
| DEMO_SEND | `{NOM_NEGOCI}`, `{SECTOR}`, `{URL_DEMO}`, `{HORES_VALIDA}` |
| BUDGET_ACCEPTED | `{NOM_NEGOCI}`, `{NOM_CLIENT}`, `{NUM_PRESSUPOST}`, `{IMPORT}`, `{DATA_ACCEPTACIO}` |
| APPOINTMENT_CONFIRM | `{NOM_NEGOCI}`, `{NOM_CLIENT}`, `{DATA_CITA}`, `{HORA_CITA}`, `{SERVEI}` |
| APPOINTMENT_REMINDER | `{NOM_NEGOCI}`, `{NOM_CLIENT}`, `{DATA_CITA}`, `{HORA_CITA}`, `{SERVEI}` |
| APPOINTMENT_CANCEL | `{NOM_NEGOCI}`, `{NOM_CLIENT}`, `{DATA_CITA}`, `{HORA_CITA}`, `{MOTIU}` |

---

## 4. Endpoints API

### Admin (requereix autenticació)

| Mètode | Ruta | Descripció |
|--------|------|-----------|
| GET | /api/v1/admin/comm/templates | Llista totes les plantilles |
| POST | /api/v1/admin/comm/templates | Crear nova plantilla |
| PUT | /api/v1/admin/comm/templates/{id} | Actualitzar plantilla |
| DELETE | /api/v1/admin/comm/templates/{id} | Eliminar plantilla |
| POST | /api/v1/admin/comm/send | Enviar missatge usant una plantilla |

#### POST /api/v1/admin/comm/send
```json
{
  "channel": "EMAIL",
  "to": "client@email.com",
  "action": "DEMO_SEND",
  "sector": "PERRUQUERIA",
  "variables": {
    "NOM_NEGOCI": "Perruqueria Mireia",
    "URL_DEMO": "https://...",
    "HORES_VALIDA": "24"
  }
}
```
Resposta: `{ "sent": true, "renderedBody": "..." }`

---

## 5. Lògica de resolució de plantilla

1. Cerca `sector={sector}` + `channel` + `action` + `is_active=true`
2. Si no existeix, cerca `sector='ALL'` + `channel` + `action` + `is_active=true`
3. Si no existeix cap, retorna error 404

---

## 6. Plantilles per defecte (seed)

Plantilles genèriques (`sector='ALL'`) per a totes les accions × canals.

---

## 7. Fitxers principals

| Fitxer | Propòsit |
|--------|---------|
| `comm/domain/CommunicationTemplate.java` | Entitat JPA |
| `comm/domain/CommunicationTemplateRepository.java` | Repositori |
| `comm/application/CommunicationTemplateService.java` | CRUD + render + send |
| `comm/api/CommunicationTemplateController.java` | Endpoints REST |
| `comm/bootstrap/CommunicationTemplateSeeder.java` | Seed per defecte |

---

## 8. Migracions

| Migració | Contingut |
|----------|-----------|
| V36 | Crea taula `communication_templates` |
