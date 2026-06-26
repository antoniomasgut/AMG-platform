# Mòdul 51: Agency Multichannel Contact — Widget IA + WhatsApp per a la landing d'AMG

> **Versió:** 1.0
> **Data:** 2026-06-26
> **Dependències:** Mòdul 27 (WhatsApp Business), Mòdul 30 (Landing Chat Widget), Mòdul 42 (Meta OAuth Onboarding)

---

## 1. Objectiu

Gestionar els canals d'entrada de contacte a la **landing pública d'AMG Digitalitzacions**
(`amgdl.com`) de forma unificada:

| Canal | Tecnologia | Estat |
|-------|-----------|-------|
| Widget IA (botó taronja) | `ChatSessionService.createAgencySession()` | ✅ Actiu |
| WhatsApp personal (botó verd) | Link `wa.me/34654048164` | ✅ Temporal |
| WhatsApp Business + IA (botó verd) | Meta Cloud API → `ConversationalAgentService` | ⏳ Pendent configuració WABA |

**Principi clau:** el tenant propietari (`isOwner = true`) és tractat com qualsevol altre
tenant a efectes d'agents IA i WhatsApp — però amb endpoints propis per a la landing pública,
sense autenticació i amb rate-limiting per IP.

---

## 2. Canal 1: Widget IA (botó taronja)

### 2.1 Frontend

Component `AgencyChatWidget.tsx` (landing pública):

- **Verificació d'estat:** `GET /api/v1/chat/agency/status` → `{ enabled: boolean }`
  - `enabled = true` si el tenant propietari té `TenantChatLink.widgetEnabled = true` i `isActive = true`
  - Si `enabled = false`, el widget no es renderitza

- **Pre-chat form:** nom + telèfon obligatoris (igual que el widget de tenants, Spec 30)

- **Creació de sessió:** `POST /api/v1/chat/agency/sessions`
  ```json
  { "contactName": "Maria", "contactPhone": "654123456", "locale": "ca" }
  ```
  - Respon amb `{ sessionId, greeting, history }` — `history` inclou converses prèvies WIDGET + WHATSAPP + WHATSAPP_META del mateix telèfon

- **Enviament de missatges:** `POST /api/v1/chat/sessions/{sessionId}/messages`
  - Idèntic al widget de tenants — reutilitza el mateix endpoint

### 2.2 Backend — `ChatSessionService.createAgencySession()`

1. Rate-limit per IP (igual que els tenants)
2. Obté el tenant propietari via `tenantRepository.findByIsOwnerTrue()`
3. Construeix el system prompt via `PromptBuilder.build(ownerId, null)` — usa la KB i config del tenant propietari
4. Model IA: llegeix `TenantAIConfig.preferredModel`; fallback a `claude-haiku-4-5-20251001`
5. Carrega historial previ per telèfon dels canals WIDGET, WHATSAPP, WHATSAPP_META
6. Genera salutació adaptada a l'`locale` del request body (fallback: `Accept-Language`)
7. Guarda sessió a Redis (TTL 2h) i persiste la salutació a `conversations` via `ConversationService`
8. Crea o reutilitza Lead (deduplicat per telèfon + `tenantId` del propietari)

### 2.3 Activació

El SUPER_ADMIN habilita/deshabilita el widget des de:
`/portal/admin/tenants/{ownerId}/setup` → secció Agent IA → toggle "Widget actiu"

Taula afectada: `tenant_chat_links` (columna `widget_enabled`)

---

## 3. Canal 2: WhatsApp personal (temporal)

Botó verd flotant a la landing. Obre `https://wa.me/34654048164` en una nova pestanya.

**Limitacions:**
- Conversa directa al mòbil personal — fora del sistema
- No es registra cap lead ni conversa a la BD
- L'agent IA no intervé

**Condició de retirada:** quan el WhatsApp Business d'AMG estigui `CONNECTED`
(veure secció 4), el botó verd ha de canviar al número de WABA i la conversa
passa per l'agent IA automàticament.

---

## 4. Canal 3: WhatsApp Business + IA (estat objectiu)

### 4.1 Flux de configuració de WABA per al tenant propietari

El tenant propietari segueix el mateix flux que qualsevol tenant client (Spec 42):

1. SUPER_ADMIN entra a `/portal/admin/tenants/{ownerId}/setup` → WhatsApp
2. Inicia OAuth Meta: `POST /api/v1/whatsapp/meta/oauth/init?tenantId={ownerId}`
3. Completa el flow a Meta Business Suite (compte WABA d'AMG, no dels clients)
4. Confirma: `POST /api/v1/whatsapp/meta/oauth/confirm` → crea entrada a `whatsapp_waba_configs`
5. Registra webhook: `POST /api/v1/whatsapp/meta/{ownerId}/register-webhook`
6. Estat final: `WhatsAppConnectionStatus.CONNECTED` per al tenant propietari

### 4.2 Flux en temps real (un cop CONNECTED)

```
Visitant escriu a WA Business d'AMG
        ↓
Meta Cloud API → POST /api/v1/whatsapp/webhook
        ↓
WhatsAppController → WhatsAppBusinessOrchestrator.handleWebhookMessage(phoneNumberId, from, text)
        ↓
configRepository.findByPhoneNumberId(phoneNumberId) → obté tenantId del propietari
        ↓
ConversationalAgentService.handleIncoming(ownerId, from, WHATSAPP_META, text)
        ↓
IA genera resposta (usa KB + config del propietari)
        ↓
MetaWhatsAppClient.sendTextMessage(phoneNumberId, accessToken, from, reply)
```

### 4.3 Historial unificat

Quan el mateix telèfon ha escrit tant pel widget com per WhatsApp, el context de la IA inclou totes dues converses (WIDGET + WHATSAPP_META). Gestionat a `ChatSessionService.createAgencySession()` via `conversationService.loadHistoryAcrossChannels()`.

### 4.4 Canvi del botó verd quan WABA és actiu

**Comportament esperat** (pendent d'implementar al frontend):

```
GET /api/v1/chat/agency/status
→ { enabled: boolean, wabaPhone: string | null }
```

- Si `wabaPhone` no és null → botó verd obre `https://wa.me/{wabaPhone}` (número WABA)
- Si `wabaPhone` és null → botó verd obre `https://wa.me/34654048164` (número personal, temporal)

**Backend pendent:** ampliar `AgencyStatusResponse` per incloure `wabaPhone`:
```java
record AgencyStatusResponse(boolean enabled, String wabaPhone) {}
```
Obtenir `wabaPhone` de `whatsapp_waba_configs` del tenant propietari (si `CONNECTED`).

---

## 5. Endpoints públics (sense autenticació)

| Mètode | Path | Descripció |
|--------|------|-----------|
| `GET` | `/api/v1/chat/agency/status` | Widget actiu + número WABA si disponible |
| `POST` | `/api/v1/chat/agency/sessions` | Crear sessió de xat IA per a la landing d'AMG |
| `POST` | `/api/v1/chat/sessions/{id}/messages` | Enviar missatge (compartit amb widget de tenants) |

**Seguretat:** rate-limit per IP a Redis. No requereix JWT.

---

## 6. Gaps pendents d'implementar

### P1 — `wabaPhone` a `agency/status` (frontend + backend)

Permet que el botó verd del frontend mostri el número WABA quan estigui configurat,
en lloc del número personal temporal.

**Backend:** `ChatController.getAgencyStatus()` → afegir `wabaPhone` a la resposta.
**Frontend:** `AgencyChatWidget.tsx` → llegir `wabaPhone` de l'status i actualitzar l'href del botó verd.

### P2 — Configurar WABA per al tenant propietari

Tasca operacional (no codi): seguir el flux d'OAuth Meta (Spec 42) per al tenant `isOwner = true`.
Requisits previs: compte Meta Business verificat + número de telèfon dedicat per a AMG.

### P3 — Notificació al SUPER_ADMIN quan arriba un missatge al widget

Quan un visitant inicia una conversa al widget IA de la landing, enviar notificació
a Telegram (`AMG_SALES_CHAT_ID`) amb: nom, telèfon, primer missatge.
Implementar a `ChatSessionService.createAgencySession()` — opció best-effort, no bloquejant.

---

## 7. QA

| Cas | Resultat esperat |
|-----|-----------------|
| Widget desactivat (`widgetEnabled=false`) | Cap widget renderitzat a la landing |
| Widget actiu, usuari envia missatge | IA respon en català (o idioma del locale) |
| Usuari amb historial WA previ inicia widget | L'agent IA "recorda" converses anteriors |
| Missatge a WA Business (quan CONNECTED) | IA respon automàticament per WA |
| `agency/status` amb WABA CONNECTED | `wabaPhone` conté el número del WABA d'AMG |
| Rate limit superat (>10 sessions/IP/h) | 429 Too Many Requests |
