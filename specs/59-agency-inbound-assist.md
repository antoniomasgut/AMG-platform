# Spec 59 — Agency Inbound Assist (esborrany IA + aprovació humana)

> **Versió:** 1.3 (modes commutables via `agentMode`: HYBRID=supervisat / AUTO=autònom no supervisat / OFF; en AUTO el xat torna a ser síncron sense polling)
> **Data:** 2026-07-14
> **Estat:** Validat — llest per implementar
> **Depèn de:** Spec 56 F2 (patró d'aprovació — es REPLICA, no es modifica), Spec 20 (Agents Telegram), Spec 30 (Landing Chat Widget), Spec 51 (Agency Multichannel/WABA), Spec 25 (Omnichannel Inbox)

---

## 1. Objectiu

Que **tots els contactes entrants propis d'AMG** (no dels tenants) passin per l'agent IA, que **redacta un esborrany de resposta**, i que **abans d'enviar res AMG l'aprovi** des del Telegram. Tres accions:
- **✅ Enviar** — surt tal qual.
- **🔄 Demana canvis** — AMG dóna una **indicació** ("més curta", "més formal", "menciona el preu", "en castellà") → la IA **regenera** l'esborrany mantenint el context → torna a demanar aprovació (repetible).
- **✍️ L'escric jo** — AMG l'escriu sencera.

Canals: correu `info@amgdl.com`, formulari web, WhatsApp d'AMG, xat de la landing d'AMG. **El bot es comporta EXACTAMENT IGUAL als 4.**

**Dos modes commutables** (§3.1), iguals per als 4 canals:
- **Supervisat** (per defecte): esborrany + aprovació (✅/🔄/✍️). Cap resposta surt sense el teu vistiplau.
- **Autònom** (no supervisat): el bot respon **directament**, amb una **còpia** al teu Telegram per si vols intervenir. Per activar quan tinguis confiança.

---

## 2. Servei propi (NO es reutilitza SocialDmService directament)

El patró d'aprovació de `SocialDmService` (Spec 56 F2) és el **model a replicar**, PERÒ el seu `send()` està cablejat a Meta (`messagingChannel.sendMessage`), així que **no serveix per a email/WhatsApp/xat**.

→ **Es crea `AgencyInboundService`** que:
- Reïx l'estat pendent a **Redis** (com SocialDmService), amb TTL 24h (sobreviu reinicis; Redis és extern → **no cal taula** per a la supervivència del callback).
- Té un **`send()` channel-aware** (switch per canal, §5.3).
- **Reutilitza** `ConversationalAgentService.generateDmDraft(tenantId, identifier, ConversationChannel, text)` per generar l'esborrany — és genèric (no lligat a DMs) i `ConversationChannel` ja té `EMAIL`, `WHATSAPP_META`, `WIDGET`.
- **NO es modifica** `SocialDmService`.

---

## 3. Identitat "AMG"

- Tenant propietari = **`PLATFORM_TENANT_ID`** (config existent).
- Xat d'aprovació = **`AMG_SALES_CHAT_ID`** (config existent; ja rep els avisos del formulari web; és un **admin chat** segons `AmgAdminCommandService.isAdminChat`).
- Els fluxos només apliquen quan el destinatari és AMG (tenant == PLATFORM_TENANT_ID). Els contactes dels tenants segueixen el camí actual.

### 3.1 Modes de funcionament (commutable)

Es reutilitza el concepte d'**`agentMode`** que ja existeix (AUTO/HYBRID/OFF) per al tenant d'AMG. El mode s'aplica **igual als 4 canals** i es canvia des del portal (fitxa del tenant d'AMG) o config.

| `agentMode` | Comportament | Detall |
|-------------|--------------|--------|
| **HYBRID** (per defecte) | **Supervisat** | Esborrany → Telegram amb ✅/🔄/✍️ → només surt en aprovar. (Flux §5-§6.) |
| **AUTO** | **Autònom (no supervisat)** | La IA respon **directament** pel canal, sense aprovació. S'envia una **còpia** a `AMG_SALES_CHAT_ID` (informativa, sense botons) per poder intervenir després. |
| **OFF** | Sense bot | Només es registra el lead / avís (comportament actual del formulari); cap resposta automàtica. |

**Efecte en el xat (important):** en **AUTO**, el xat respon **síncronament i a l'instant** (com avui) → **no cal polling**. El polling (§6) només és necessari en **HYBRID** (resposta diferida per l'aprovació). Per tant, el mode autònom també **simplifica** el xat.

Recomanació operativa: començar en **HYBRID** (superviso tot), i passar a **AUTO** quan les respostes de la IA siguin prou bones.

---

## 4. Model de dades

**Redis primer** (imprescindible): `agency:pending:<id>` → JSON {channel, fromRef, fromName, inboundText, draft} (TTL 24h) + `agency:await:<chatId>` → {id, mode: REFINE|MANUAL} per capturar el següent text lliure.

Taula `agency_pending_replies` **opcional** (només si es vol historial/RGPD): id, channel, from_ref, inbound_text, draft, status (`PENDING`/`SENT`/`MANUAL`/`DISCARDED`), created_at, resolved_at. **v1: només Redis.**

Mapatge de canal → `ConversationChannel` per a `generateDmDraft`: correu i formulari → `EMAIL`; WhatsApp → `WHATSAPP_META`; xat → `WIDGET`.

---

## 5. Flux

### 5.1 Entrada → esborrany
| Canal | Punt d'entrada | Canvi |
|-------|----------------|-------|
| **Correu** | `EmailWebhookController.handleAsync(tenantId, from, text)` | Si `tenantId == PLATFORM_TENANT_ID` → **bypassa** `handleIncoming` i crida `AgencyInboundService.intake(EMAIL, from, text)` |
| **Formulari web** | `PublicContactController.submit()` | A més del lead + avís actual, `AgencyInboundService.intake(EMAIL, email, message)` |
| **WhatsApp** | `WhatsAppMetaWebhookController` → `handleIncoming(WHATSAPP_META)` | Si tenant == PLATFORM → intake(WHATSAPP_META, telèfon, text) |
| **Xat landing** | `ChatSessionService.sendMessage` (sessió `agency`) | **Esborrany + aprovació** (igual que la resta) → el visitant rep la resposta aprovada via **polling** (§6) |

`intake()` → `generateDmDraft(PLATFORM, fromRef, channel, text)` → desa a Redis → envia a `AMG_SALES_CHAT_ID`:
```
📨 Nova consulta [CANAL] de <fromRef>
«<inboundText>»

Resposta proposada:
«<draft>»
[✅ Enviar]  [🔄 Demana canvis]  [✍️ L'escric jo]
```
Callbacks: `arok:<id>` · `arrf:<id>` (refine) · `arwr:<id>` (manual).

### 5.2 Botons i free-text (routing a Telegram — ⚠️ punts crítics de la validació)
- **Els callbacks `arok:`/`arrf:`/`arwr:` es gategen per `amgAdminCommandService.isAdminChat(cbChatId)`** (o comparació directa amb `AMG_SALES_CHAT_ID`). **MAI** per `findByTelegramChatId` (el grup de vendes no té `TenantChatLink`).
- **El check de free-text pendent** (`agencyInboundService.hasAwait(chatId)`) s'ha d'inserir **DINS la branca admin** de `TelegramWebhookController`, **abans** de `AmgAdminCommandService.handle()` (línia ~332) — perquè avui el text a un admin chat es consumeix allà i retorna abans dels `hasPending` de reviews/comments/DMs.
- **🔄 Demana canvis:** `arrf:` → marca `await REFINE` → el següent text d'AMG es passa a la IA com a **instrucció de reescriptura** (regenera el draft amb el context original + la instrucció) → torna a §5.1 amb els 3 botons. Repetible.
- **✍️ L'escric jo:** `arwr:` → `await MANUAL` → el següent text d'AMG és la **resposta final** → s'envia pel canal.

### 5.3 Enviament (channel-aware, `AgencyInboundService.send`)
| Canal | Acció en ✅ (o en enviar el manual) |
|-------|-------------------------------------|
| **EMAIL** (correu i formulari) | `EmailService.sendEmail(destinatari, assumpte, cos)` — el 1r arg és el **remitent original** (Brevo, sortint) |
| **WHATSAPP_META** | `WhatsAppMetaChannel.sendMessage(...)` al remitent |
| **WIDGET** (xat) | només auto+còpia (§6) |

Estat → `SENT` (o `MANUAL`); s'esborra el pending de Redis.

---

## 6. Xat en directe — aprovació via polling (comportament igual als altres 3)

**Requisit:** el bot es comporta **igual als 4 canals** → el xat també va amb esborrany + aprovació. Però `ChatController.sendMessage` és avui **100% síncron** (el visitant rep la resposta a la mateixa petició HTTP; el widget no fa polling ni WS). Per tant, cal **fer el xat asíncron amb polling**:

1. El visitant envia el missatge → el backend **no** retorna la resposta de la IA de cop; retorna `{status: "PENDING", messageId}` i mostra un **missatge d'espera** al widget: «Ara mateix et responem, un momentet 👋» + indicador d'escrivint.
2. En paral·lel, `intake(WIDGET, sessionId, text)` genera l'esborrany i el passa a `AMG_SALES_CHAT_ID` amb els 3 botons.
3. **Nou endpoint de polling** `GET /api/v1/chat/sessions/{id}/poll?after=<msgId>` (permitAll, com la resta del widget) → retorna els missatges nous de la `ChatSession` (Redis).
4. El **widget fa polling** (cada ~3 s) fins que apareix la resposta aprovada. Quan AMG fa ✅ (o edita), s'escriu el missatge a la `ChatSession` → el poll el recull → el visitant el veu.

**⚠️ Consideració d'UX (important):** el visitant **espera** que AMG aprovi. Si AMG no és ràpid, pot marxar. Per mitigar-ho:
- Missatge d'espera clar des del primer segon.
- **Timeout de fallback** configurable (p. ex. 60-90 s sense aprovació) → el widget diu «Et responem de seguida per WhatsApp/email, deixa'ns el contacte» i captura les dades. Així cap lead es perd tot i el retard.
- (Opcional, config per si es vol) un **mode auto+còpia** com a alternativa, si AMG prefereix immediatesa al xat en algun moment.

---

## 7. Prerequisits

- **Correu (inbound = Mailgun, ⚠️ no Brevo):** `EmailWebhookController` processa `*@inbound.amgdl.com` via Mailgun (verifica `MAILGUN_WEBHOOK_SIGNING_KEY`). Cal: encaminar `info@amgdl.com` a aquest inbound, i crear `TenantChatLink.emailAddress = info@amgdl.com` lligat a `PLATFORM_TENANT_ID` (perquè `findByEmailAddressIgnoreCase` el resolgui). La **resposta sortint** va per Brevo (`EmailService`). Veure `docs/config-correu-cloudflare.html`.
- **Agent IA actiu per al tenant PLATFORM:** `generateDmDraft` retorna null si l'agent no està configurat/actiu (`prepareIncoming` buit). Cal `TenantAIConfig`/AGENT habilitat per al tenant d'AMG.
- **Convivència amb l'`agentMode` existent (clau):** el mode HYBRID/AUTO/OFF **és** el mateix `agentMode` de la plataforma (§3.1), no un de nou. Per al tenant AMG, l'**intake d'aquest mòdul substitueix** el camí genèric: en HYBRID no s'ha de disparar també `ConversationalAgentService.notifyOperator` (que envia una suggerència sense botó) → **bypassar `handleIncoming`** per als 4 canals quan tenant==PLATFORM i deixar que `AgencyInboundService` apliqui el mode. Els **tenants normals** mantenen el seu `agentMode` i comportament actuals, intactes.
- **WhatsApp d'AMG:** WABA d'AMG connectat i lligat al tenant propietari (Mòdul 51, prerequisit operatiu).
- Config existent: `PLATFORM_TENANT_ID`, `AMG_SALES_CHAT_ID`, `ANTHROPIC_API_KEY`, `TELEGRAM_BOT_TOKEN`.

---

## 8. Seguretat / aïllament

- Els fluxos només s'activen per al tenant d'AMG (`PLATFORM_TENANT_ID`); els tenants no es toquen.
- Callbacks `arok:`/`arrf:`/`arwr:` i free-text pendent: **només** si `isAdminChat(chatId)` / `chatId == AMG_SALES_CHAT_ID`.
- Validació de mida/format del text entrant (com el formulari actual).

---

## 9. Casos QA
1. Correu a info@ (encaminat) amb tenant=PLATFORM → arriba esborrany a `AMG_SALES_CHAT_ID` amb els 3 botons.
2. ✅ Enviar → el remitent rep la resposta per email (Brevo); pending esborrat.
3. 🔄 Demana canvis → AMG escriu "més curta i en castellà" → la IA regenera → torna amb els 3 botons; es pot repetir.
4. ✍️ L'escric jo → AMG escriu la resposta → s'envia pel canal; estat MANUAL.
5. Formulari web → a més del lead, arriba esborrany a aprovar.
6. WhatsApp d'AMG → esborrany → aprovació → resposta pel mateix WhatsApp.
7. Xat landing (sessió agency) → el visitant veu "un momentet 👋" i fa polling; AMG aprova al Telegram → la resposta apareix al widget. Si passa el timeout sense aprovar → el widget demana el contacte (fallback).
8. Free-text a `AMG_SALES_CHAT_ID` mentre hi ha un `await` pendent → es captura pel flux (no cau a l'assistent admin).
9. Callback `arok:` des d'un xat que NO és admin/AMG → ignorat.
10. Contacte d'un **tenant** (no AMG) → flux actual, sense aprovació d'AMG.
11. **Mode AUTO** (autònom): correu/WhatsApp/formulari → la IA respon directament + còpia informativa a `AMG_SALES_CHAT_ID` (sense botons); no espera aprovació.
12. **Mode AUTO al xat** → el visitant rep resposta immediata (síncron, sense polling); AMG en rep còpia.
13. **Mode OFF** → només lead/avís, cap resposta automàtica.
14. Canviar el mode del tenant AMG (HYBRID↔AUTO) → el comportament dels 4 canals canvia en conseqüència, sense doble notificació.

---

## 10. Dins/fora d'abast (v1)
- **Dins:** els 4 canals amb esborrany+aprovació idèntic; **polling al widget** del xat (endpoint + widget) perquè el xat es comporti igual; timeout de fallback al xat.
- **Fora:** WebSocket per al xat (n'hi ha prou amb polling); taula d'auditoria (v1 només Redis); bústia visual unificada (ja hi ha l'Omnichannel Inbox, Spec 25).
