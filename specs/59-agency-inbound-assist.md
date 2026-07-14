# Spec 59 — Agency Inbound Assist (esborrany IA + aprovació humana)

> **Versió:** 1.1 (incorpora la validació: servei propi channel-aware, routing de callbacks/free-text a l'AMG_SALES_CHAT_ID, xat només auto+còpia, inbound Mailgun; + opció "demana canvis a la IA")
> **Data:** 2026-07-14
> **Estat:** Validat — llest per implementar
> **Depèn de:** Spec 56 F2 (patró d'aprovació — es REPLICA, no es modifica), Spec 20 (Agents Telegram), Spec 30 (Landing Chat Widget), Spec 51 (Agency Multichannel/WABA), Spec 25 (Omnichannel Inbox)

---

## 1. Objectiu

Que **tots els contactes entrants propis d'AMG** (no dels tenants) passin per l'agent IA, que **redacta un esborrany de resposta**, i que **abans d'enviar res AMG l'aprovi** des del Telegram. Tres accions:
- **✅ Enviar** — surt tal qual.
- **🔄 Demana canvis** — AMG dóna una **indicació** ("més curta", "més formal", "menciona el preu", "en castellà") → la IA **regenera** l'esborrany mantenint el context → torna a demanar aprovació (repetible).
- **✍️ L'escric jo** — AMG l'escriu sencera.

Cap resposta surt sense el vistiplau humà. Canals: correu `info@amgdl.com`, formulari web, WhatsApp d'AMG, xat de la landing d'AMG.

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
| **Xat landing** | `ChatSessionService.sendMessage` (sessió `agency`) | **Auto + còpia** (§6), no aprovació |

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

## 6. Xat en directe — només auto + còpia

`ChatController.sendMessage` és **100% síncron**: el visitant rep la resposta a la mateixa petició HTTP i el widget **no fa polling ni websocket**. Per tant **el "mode aprovació" NO és viable** al xat (una resposta diferida no arribaria mai al visitant).

→ Per al xat: **auto + còpia**. El widget respon a l'instant amb la IA, i s'envia una **còpia a `AMG_SALES_CHAT_ID`** perquè AMG ho vegi i intervingui si cal (afegint la notificació dins `ChatSessionService.sendMessage` per a sessions `agency`, al costat de `notifyAdminNewChatSession`). El mode aprovació al xat queda **fora d'abast** fins que el widget tingui push (polling/WS).

---

## 7. Prerequisits

- **Correu (inbound = Mailgun, ⚠️ no Brevo):** `EmailWebhookController` processa `*@inbound.amgdl.com` via Mailgun (verifica `MAILGUN_WEBHOOK_SIGNING_KEY`). Cal: encaminar `info@amgdl.com` a aquest inbound, i crear `TenantChatLink.emailAddress = info@amgdl.com` lligat a `PLATFORM_TENANT_ID` (perquè `findByEmailAddressIgnoreCase` el resolgui). La **resposta sortint** va per Brevo (`EmailService`). Veure `docs/config-correu-cloudflare.html`.
- **Agent IA actiu per al tenant PLATFORM:** `generateDmDraft` retorna null si l'agent no està configurat/actiu (`prepareIncoming` buit). Cal `TenantAIConfig`/AGENT habilitat per al tenant d'AMG.
- **Convivència amb `agentMode` HYBRID:** en HYBRID, `ConversationalAgentService.notifyOperator` ja envia una "resposta suggerida" sense botó → per al tenant AMG s'ha de **bypassar `handleIncoming`** (§5.1) i passar pel flux nou, per evitar doble notificació.
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
7. Xat landing (sessió agency) → el visitant rep resposta immediata i AMG en rep còpia (no aprovació).
8. Free-text a `AMG_SALES_CHAT_ID` mentre hi ha un `await` pendent → es captura pel flux (no cau a l'assistent admin).
9. Callback `arok:` des d'un xat que NO és admin/AMG → ignorat.
10. Contacte d'un **tenant** (no AMG) → flux actual, sense aprovació d'AMG.

---

## 10. Fora d'abast (v1)
- Mode aprovació al **xat** (cal push al widget).
- Taula d'auditoria (v1 només Redis).
- Bústia visual unificada (ja hi ha l'Omnichannel Inbox, Spec 25).
