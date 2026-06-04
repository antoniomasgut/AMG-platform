# Spec 32 — Tenant Notifications (Notificacions al Negoci)

**Versió:** 1.0  
**Data:** 2026-06-03  
**Estat:** Esborrany  
**Depèn de:** Spec 20 (Agents Autònoms), Spec 24 (Agent Activation Flows), Spec 03 (Leads CRM), Spec 04 (Engine/Landing), Mòdul 30 (Landing Chat Widget)

---

## 1. Objectiu

Permetre que cada tenant rebi **notificacions proactives** al seu Telegram (o correu) quan succeeixi un esdeveniment rellevant al seu negoci: nou contacte, nova conversa, cita confirmada, lead nou, etc.

L'objectiu és que el propietari del negoci estigui informat en temps real sense haver d'obrir el portal, de forma no intrusiva i configurable.

---

## 2. Canals de notificació

| Canal | Ús | Implementació |
|-------|-----|---------------|
| **Telegram** | Notificacions immediates | `TenantChatLink.telegramChatId` + bot token global (`TELEGRAM_BOT_TOKEN`) |
| **Email** | Notificacions menys urgents / resum | `Tenant.email` via Brevo |

La prioritat és Telegram. Si el tenant no té Telegram vinculat (`telegramChatId` null), cau en silenci (best-effort, no és error crític).

El canal email és opcional i configurable per event.

---

## 3. Esdeveniments notificables

### 3.1 Taula d'events

| Codi | Descripció | Telegram | Email | Origen |
|------|------------|----------|-------|--------|
| `CONTACT_FORM` | Nou contacte via formulari de landing | ✅ per defecte ON | ✅ per defecte ON | `EngineOrchestrator.submitContact()` |
| `CHAT_WIDGET_NEW` | Primera missatge d'una nova sessió de chat widget | ✅ per defecte ON | ❌ per defecte OFF | `WidgetChatService.createSession()` |
| `WHATSAPP_NEW` | Primer missatge d'un nou contacte per WA | ✅ per defecte ON | ❌ per defecte OFF | `ConversationalAgentService.handleIncoming()` |
| `EMAIL_NEW` | Primer missatge d'un nou contacte per email | ✅ per defecte ON | ❌ per defecte OFF | `ConversationalAgentService.handleIncoming()` |
| `LEAD_CREATED` | Nou lead creat manualment des del portal | ✅ per defecte OFF | ❌ per defecte OFF | `LeadService.create()` |
| `BOOKING_CONFIRMED` | Cita confirmada per l'agent (tag `[CONFIRMA_CITA]`) | ✅ per defecte ON | ✅ per defecte ON | `ChatSessionService.processBookingTag()` |
| `WIDGET_BOOKING` | Cita confirmada via chat widget | ✅ per defecte ON | ✅ per defecte ON | *(futur, quan widget suporti agenda)* |

### 3.2 Format dels missatges (Telegram)

Missatges curts i clars, format HTML Telegram:

**CONTACT_FORM:**
```
📬 <b>Nou contacte</b> · {landing_title}
👤 {nom} · {email} · {telèfon}
💬 {missatge truncat a 120 caràcters}
🔗 <a href="https://amgdl.com/portal/leads">Veure leads →</a>
```

**CHAT_WIDGET_NEW:**
```
💬 <b>Nou xat</b> · {nom_landing_o_web}
🌐 Visita des del widget de la web
```

**WHATSAPP_NEW:**
```
📱 <b>Nou WhatsApp</b> · {phone}
💬 {primer_missatge truncat a 100 caràcters}
```

**EMAIL_NEW:**
```
✉️ <b>Nou email</b> · {from_address}
📋 {subject}
💬 {preview truncat a 100 caràcters}
```

**BOOKING_CONFIRMED:**
```
📅 <b>Cita confirmada</b>
👤 {nom_client} · {data} {hora} ({durada} min)
📝 {notes}
```

**LEAD_CREATED:**
```
✅ <b>Nou lead creat</b>
👤 {nom} · {email_o_telèfon}
📌 Etapa: {stage}
```

---

## 4. Model de dades

### 4.1 Nova entitat `tenant_notification_configs`

```sql
CREATE TABLE tenant_notification_configs (
    tenant_id       UUID PRIMARY KEY,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Per event: Telegram
    tg_contact_form     BOOLEAN NOT NULL DEFAULT TRUE,
    tg_chat_widget_new  BOOLEAN NOT NULL DEFAULT TRUE,
    tg_whatsapp_new     BOOLEAN NOT NULL DEFAULT TRUE,
    tg_email_new        BOOLEAN NOT NULL DEFAULT TRUE,
    tg_lead_created     BOOLEAN NOT NULL DEFAULT FALSE,
    tg_booking          BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Per event: Email
    em_contact_form     BOOLEAN NOT NULL DEFAULT TRUE,
    em_booking          BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Horari silenci (local, 24h)
    quiet_start         SMALLINT,       -- hora inici silenci, p.ex. 22
    quiet_end           SMALLINT,       -- hora fi silenci, p.ex. 8
    timezone            VARCHAR(50) NOT NULL DEFAULT 'Europe/Madrid',
    
    -- Cooldown entre notificacions del mateix event (minuts)
    cooldown_minutes    SMALLINT NOT NULL DEFAULT 0,
    
    updated_at          TIMESTAMPTZ
);
```

> **Nota producció:** Taula nova → cal crear-la manualment a la BD de producció i afegir migration Flyway `V11__tenant_notification_configs.sql`.

### 4.2 Camps existents reutilitzats

- `TenantChatLink.telegramChatId` → ID del xat Telegram del tenant (on rep missatges de l'agent)
- `Tenant.email` → Email de contacte per notificacions

---

## 5. Arquitectura backend

### 5.1 Nou servei `TenantNotificationService`

Paquet: `com.amg.digitalitzacio.shared.notification`

```java
@Service
public class TenantNotificationService {
    
    // Envia notificació Telegram al tenant (best-effort, silenciós si no configurat)
    public void notify(UUID tenantId, NotificationEvent event, Map<String, String> params);
    
    // Comprova si l'event ha de generar notificació (config + quiet hours + cooldown)
    private boolean shouldNotify(TenantNotificationConfig config, NotificationEvent event, NotificationChannel channel);
}
```

**`NotificationEvent` enum:**
```java
public enum NotificationEvent {
    CONTACT_FORM, CHAT_WIDGET_NEW, WHATSAPP_NEW, EMAIL_NEW,
    LEAD_CREATED, BOOKING_CONFIRMED
}
```

**`NotificationChannel` enum:**
```java
public enum NotificationChannel { TELEGRAM, EMAIL }
```

### 5.2 Integració amb Telegram

Usa el bot global (`TELEGRAM_BOT_TOKEN` de `SystemConfigService`) i el `telegramChatId` del `TenantChatLink`. **No requereix el bot propi del tenant** — és el bot de la plataforma AMG qui envia.

```java
// Enviar a Telegram del tenant
botToken = sysConfig.get("TELEGRAM_BOT_TOKEN");
chatId   = chatLinkRepository.findByTenantId(tenantId)
                              .map(TenantChatLink::getTelegramChatId)
                              .orElse(null);
if (chatId == null) return; // no configurat, silenci
telegramApi.post("/bot{token}/sendMessage", {chat_id: chatId, text: ..., parse_mode: "HTML"});
```

### 5.3 Integració amb Email

Usa `EmailService.sendEmail()` existent (Brevo). Destinatari: `tenant.email`.

### 5.4 Cooldown anti-spam

Redis key: `notif:cooldown:{tenantId}:{event}` amb TTL = `cooldown_minutes * 60`.  
Si la key existeix → no envia. Si no existeix → envia i crea la key.

### 5.5 Quiet hours

Si `quiet_start` i `quiet_end` estan configurats, comprova l'hora local del tenant (`timezone`). Si és hora de silenci → no envia per Telegram (però sí per Email si estava activat).

---

## 6. Punts d'integració

### 6.1 `EngineOrchestrator.submitContact()` → event `CONTACT_FORM`

Params: `landing_title`, `nom`, `email`, `telèfon`, `missatge`.

```java
// Ja existeix: notifyTenantContactForm() 
// → substituir/complementar amb TenantNotificationService.notify()
notificationService.notify(tenantId, CONTACT_FORM, Map.of(
    "landing_title", landing.getTitle(),
    "nom", request.name(),
    "email", request.email(),
    "phone", request.phone() != null ? request.phone() : "—",
    "message", request.message() != null ? request.message() : ""
));
```

> **Nota:** `notifyTenantContactForm()` actual usa `emailService` directament. Amb el nou servei, el canal email passa per `TenantNotificationService` que comprova la config. Mantenir compatibilitat: si no hi ha config, usa valors per defecte (email ON per CONTACT_FORM).

### 6.2 `WidgetChatService.createSession()` → event `CHAT_WIDGET_NEW`

Params: `site_id`, `business_name`.

Nota: notifica NOMÉS en la primera sessió del dia del widget (cooldown recomanat: 60 min).

### 6.3 `ConversationalAgentService.handleIncoming()` → events `WHATSAPP_NEW` / `EMAIL_NEW`

Condició: **primer missatge** del contacte (no en respostes posteriors). Comprovar si el Contact existia abans de `contactService.findOrCreate()`.

Params: `identifier` (phone/email), `primer_missatge`.

### 6.4 `ChatSessionService.processBookingTag()` → event `BOOKING_CONFIRMED`

Params: `nom_client`, `data`, `hora`, `duracio`, `notes`.

### 6.5 `LeadService.create()` (endpoint manual) → event `LEAD_CREATED`

Params: `nom`, `email_o_telefon`, `stage`.

---

## 7. API REST

### 7.1 Endpoints

| Mètode | Ruta | Accés | Descripció |
|--------|------|-------|------------|
| `GET` | `/api/v1/notifications/tenants/{tenantId}/config` | ADMIN, CLIENT propi | Llegeix configuració |
| `PUT` | `/api/v1/notifications/tenants/{tenantId}/config` | ADMIN, CLIENT propi | Actualitza configuració |

### 7.2 DTO `NotificationConfigRequest` / `NotificationConfigResponse`

```json
{
  "enabled": true,
  "telegram": {
    "contactForm": true,
    "chatWidgetNew": true,
    "whatsappNew": true,
    "emailNew": true,
    "leadCreated": false,
    "booking": true
  },
  "email": {
    "contactForm": true,
    "booking": true
  },
  "quietHours": {
    "start": 22,
    "end": 8,
    "timezone": "Europe/Madrid"
  },
  "cooldownMinutes": 0
}
```

---

## 8. Frontend

### 8.1 Pàgina de configuració

Ruta: `/portal/admin/tenants/[id]/notifications` o `/portal/settings/notifications`

**Seccions:**
1. **Activació global** — toggle ON/OFF
2. **Notificacions Telegram** — per event, amb indicador si Telegram està vinculat
3. **Notificacions per Email** — per event
4. **Horari de silenci** — `De les HH:00 a les HH:00` + selector timezone
5. **Cooldown** — interval mínim entre notificacions iguals (minuts)

**Estat de connexió Telegram:** si `telegramChatId` és null → mostrar banner "Vincula el teu Telegram per rebre notificacions" amb link a `/portal/settings/telegram`.

### 8.2 Translations (keys noves)

```json
{
  "notifications": {
    "title": "Notificacions",
    "enabled": "Activar notificacions",
    "telegram": "Telegram",
    "email": "Correu electrònic",
    "events": {
      "contactForm": "Nou contacte via formulari",
      "chatWidgetNew": "Nova sessió de xat widget",
      "whatsappNew": "Nou WhatsApp rebut",
      "emailNew": "Nou correu rebut",
      "leadCreated": "Nou lead creat",
      "booking": "Cita confirmada"
    },
    "quietHours": "Horari de silenci",
    "cooldown": "Temps mínim entre notificacions (min)",
    "noTelegram": "Telegram no vinculat"
  }
}
```

---

## 9. Migració Flyway

```sql
-- V11__tenant_notification_configs.sql
CREATE TABLE IF NOT EXISTS tenant_notification_configs (
    tenant_id           UUID PRIMARY KEY,
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    tg_contact_form     BOOLEAN NOT NULL DEFAULT TRUE,
    tg_chat_widget_new  BOOLEAN NOT NULL DEFAULT TRUE,
    tg_whatsapp_new     BOOLEAN NOT NULL DEFAULT TRUE,
    tg_email_new        BOOLEAN NOT NULL DEFAULT TRUE,
    tg_lead_created     BOOLEAN NOT NULL DEFAULT FALSE,
    tg_booking          BOOLEAN NOT NULL DEFAULT TRUE,
    em_contact_form     BOOLEAN NOT NULL DEFAULT TRUE,
    em_booking          BOOLEAN NOT NULL DEFAULT TRUE,
    quiet_start         SMALLINT,
    quiet_end           SMALLINT,
    timezone            VARCHAR(50) NOT NULL DEFAULT 'Europe/Madrid',
    cooldown_minutes    SMALLINT NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ
);
```

---

## 10. Comportament per defecte (sense config)

Si el tenant no té registre a `tenant_notification_configs`:
- Telegram `CONTACT_FORM` → **ON** (usa el notifyTenantContactForm() actual com a fallback)
- Telegram `BOOKING_CONFIRMED` → **ON**
- Email `CONTACT_FORM` → **ON** (ja implementat)
- Resta → **OFF**

Això garanteix compatibilitat amb el comportament actual sense trencar res.

---

## 11. Fases d'implementació

### Fase A — Core (mínim viable)
1. Flyway V11 + entitat `TenantNotificationConfig`
2. `TenantNotificationService` — Telegram + cooldown Redis
3. Integrar `CONTACT_FORM` (substitueix `notifyTenantContactForm()`)
4. Integrar `BOOKING_CONFIRMED` (a `ChatSessionService`)
5. API GET/PUT config + frontend settings bàsic

### Fase B — Tots els events
6. Integrar `WHATSAPP_NEW` / `EMAIL_NEW` (primer missatge)
7. Integrar `CHAT_WIDGET_NEW`
8. Integrar `LEAD_CREATED`

### Fase C — Funcionalitats avançades
9. Quiet hours amb timezone correcte
10. Email notifications (complementa Telegram)
11. Frontend complet amb totes les opcions
12. Test QA complet

---

## 12. Casos QA

| # | Escenari | Resultat esperat |
|---|----------|-----------------|
| QA-01 | Submit formulari landing amb Telegram vinculat | Missatge Telegram al tenant en < 5 s |
| QA-02 | Submit formulari sense Telegram vinculat | Silenci, no error |
| QA-03 | Submit formulari dins quiet hours | No envia Telegram |
| QA-04 | Dos submits en menys del cooldown | Només envia el primer |
| QA-05 | Cita confirmada per agent | Missatge Telegram + email al tenant |
| QA-06 | Primer WA d'un nou contacte | Missatge Telegram al tenant |
| QA-07 | Segon WA del mateix contacte | No envia (no és primer missatge) |
| QA-08 | `enabled = false` | Cap notificació per cap event |
| QA-09 | PUT config → GET config | Configuració persistida correctament |
| QA-10 | Tenant sense registre config | Comportament per defecte (CONTACT_FORM ON) |
