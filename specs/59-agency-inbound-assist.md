# Spec 59 — Inbound Assist (esborrany IA + aprovació humana, per tenant)

> **Versió:** 2.0 (reenfocat: capacitat **per tenant** — no específica d'AMG. AMG Digitalitzacions és el **tenant #1** (dogfooding). Enruta al Telegram del propi tenant, com els DMs → evita el gate d'admin chat)
> **Data:** 2026-07-14
> **Estat:** Proposat (cal re-validar el reenfocament per tenant)
> **Depèn de:** Spec 56 F2 (patró d'aprovació DM — es replica), Spec 20 (Agents Telegram), Spec 30 (Landing Chat Widget), Spec 51 (WhatsApp), Spec 25 (Omnichannel Inbox)

---

## 1. Objectiu

Que **qualsevol tenant** pugui rebre els seus contactes entrants (correu, formulari, WhatsApp, xat de la landing) i que **l'agent IA redacti un esborrany**, que el tenant aprova des del **seu Telegram** abans d'enviar. Tres accions:
- **✅ Enviar** — surt tal qual.
- **🔄 Demana canvis** — dóna una **indicació** ("més curta", "més formal", "menciona el preu", "en castellà") → la IA **regenera** mantenint el context → torna a aprovar (repetible).
- **✍️ L'escric jo** — l'escriu sencera.

**Igual als 4 canals.** El comportament el decideix l'**`agentMode` del tenant** (§3). **AMG Digitalitzacions = tenant #1** (el propietari fa servir la mateixa infraestructura); quan entrin clients, ho tenen igual.

---

## 2. Servei propi (replica el patró DM, no el modifica)

`SocialDmService` (Spec 56 F2) és el **model**, però el seu `send()` està cablejat a Meta. → **Es crea `InboundAssistService`** que:
- Reïx l'estat pendent a **Redis** (TTL 24h; sobreviu reinicis → **v1 sense taula**).
- **`send()` channel-aware** (§5.3).
- Reutilitza `ConversationalAgentService.generateDmDraft(tenantId, identifier, ConversationChannel, text)` (genèric; `ConversationChannel` ja té `EMAIL`, `WHATSAPP_META`, `WIDGET`).
- **Enruta l'aprovació al Telegram del tenant** (`TenantChatLink.telegramChatId`), **exactament com els DMs** → reutilitza el bloc de routing existent (callbacks + free-text al xat del tenant). **NO** es modifica `SocialDmService`.

---

## 3. Modes (l'`agentMode` del tenant)

Es reutilitza l'`agentMode` existent. **Igual als 4 canals**, canviable des de la fitxa del tenant:

| `agentMode` | Comportament | Detall |
|-------------|--------------|--------|
| **HYBRID** (per defecte) | **Supervisat** | Esborrany → Telegram del tenant amb ✅/🔄/✍️ → només surt en aprovar. |
| **AUTO** | **Autònom** | La IA respon **directament** pel canal + **còpia** informativa al Telegram del tenant. |
| **OFF** | Sense bot | Només lead/avís; cap resposta automàtica. |

**Efecte al xat:** en **AUTO** el xat respon **síncron i immediat** (com avui, sense polling). El polling (§6) només cal en **HYBRID**.

Recomanació: començar en HYBRID (superviso tot) i passar a AUTO quan la IA sigui prou bona.

---

## 4. Dades (Redis)

- `ia:pending:<id>` → JSON {tenantId, channel, fromRef, fromName, inboundText, draft} (TTL 24h).
- `ia:await:<chatId>` → {id, mode: REFINE|MANUAL} per capturar el següent text lliure al xat del tenant.

Mapatge canal → `ConversationChannel`: correu i formulari → `EMAIL`; WhatsApp → `WHATSAPP_META`; xat → `WIDGET`.

---

## 5. Flux

### 5.1 Entrada → esborrany (el tenant es resol pels camins actuals)
| Canal | Entrada · resolució de tenant | Canvi |
|-------|-------------------------------|-------|
| **Correu** | `EmailWebhookController` → `findByEmailAddressIgnoreCase(to)` → tenant | En comptes d'auto-respondre, `InboundAssistService.intake(tenant, EMAIL, from, text)` |
| **Formulari** | v1: formulari d'`amgdl.com` (`PublicContactController`, tenant = AMG) | A més del lead + avís, `intake(...)`. *(Extensió: formularis de les landings dels tenants.)* |
| **WhatsApp** | `WhatsAppMetaWebhookController` → `phone_number_id` → tenant | `intake(tenant, WHATSAPP_META, telèfon, text)` |
| **Xat** | v1: xat de la landing d'`amgdl.com`; tenant del `ChatSession` | `intake(tenant, WIDGET, sessionId, text)` |

`intake()` → `generateDmDraft(...)` → Redis → envia al **Telegram del tenant** (`TenantChatLink.telegramChatId`):
```
📨 Nova consulta [CANAL] de <fromRef>
«<inboundText>»
Resposta proposada: «<draft>»
[✅ Enviar]  [🔄 Demana canvis]  [✍️ L'escric jo]
```
Callbacks: `iaok:<id>` · `iarf:<id>` · `iawr:<id>`.

### 5.2 Botons i free-text (routing Telegram — reutilitza el camí dels DMs)
- Els callbacks i el free-text pendent es gestionen al **mateix bloc del xat del tenant** que els DMs (`findByTelegramChatId(cbChatId)` amb `isActive`), on ja viuen `hasPending` de reviews/comments/DMs → s'hi afegeix `inboundAssist.hasAwait(chatId)`. **No cal tocar la branca admin.**
- **🔄 Demana canvis** (`iarf:`) → `await REFINE` → el següent text es passa a la IA com a **instrucció de reescriptura** → torna a §5.1. Repetible.
- **✍️ L'escric jo** (`iawr:`) → `await MANUAL` → el següent text és la resposta final.
- ⚠️ *Cas límit AMG:* si el tenant té el Telegram lligat a un **xat que també és admin** (`AMG_SALES_CHAT_ID`), cal que el check `hasAwait` s'avaluï **abans** del catch-all admin. Per a tenants normals no aplica.

### 5.3 Enviament (channel-aware)
| Canal | En ✅ / en enviar el manual |
|-------|-----------------------------|
| **EMAIL** | `EmailService.sendEmail(destinatari, assumpte, cos)` (Brevo, sortint) |
| **WHATSAPP_META** | `WhatsAppMetaChannel.sendMessage(...)` al remitent |
| **WIDGET** | escriure a la `ChatSession` → el visitant ho rep per polling (§6) |

---

## 6. Xat: polling (HYBRID) / síncron (AUTO)

El widget és avui **síncron** i sense push. Per a **HYBRID** cal fer-lo **asíncron amb polling**:
1. El visitant envia → backend retorna `{status:"PENDING", messageId}` i el widget mostra «Ara mateix et responem, un momentet 👋».
2. `intake(WIDGET,...)` → esborrany al Telegram del tenant.
3. Nou endpoint `GET /api/v1/chat/sessions/{id}/poll?after=<msgId>` (permitAll) → missatges nous de la sessió.
4. El widget fa **polling** (~3 s) fins que apareix la resposta aprovada.

**UX:** el visitant espera l'aprovació → **timeout de fallback** (60-90 s) → el widget demana el contacte («et responem de seguida per WhatsApp/email»), així cap lead es perd. En **AUTO**, el xat torna a ser **immediat** (sense polling).

---

## 7. Prerequisits (per tenant)

- **Correu (inbound = Mailgun):** `EmailWebhookController` processa `*@inbound.amgdl.com` (verifica `MAILGUN_WEBHOOK_SIGNING_KEY`). Per a AMG: encaminar `info@amgdl.com` a l'inbound + `TenantChatLink.emailAddress = info@amgdl.com` lligat al tenant AMG. Resposta sortint = Brevo. Veure `docs/config-correu-cloudflare.html`.
- **Agent IA actiu** per al tenant (`generateDmDraft` retorna null si no ho està).
- **WhatsApp** connectat i lligat al tenant (Mòdul 51).
- **Convivència amb `agentMode`:** l'intake d'aquest mòdul **substitueix** el camí genèric de `handleIncoming` per als 4 canals (perquè en HYBRID no es dispari també `notifyOperator` sense botó). El comportament el mana el mateix `agentMode` del tenant.
- Config existent: `PLATFORM_TENANT_ID` (identifica el tenant AMG), `ANTHROPIC_API_KEY`, `TELEGRAM_BOT_TOKEN`.

---

## 8. Seguretat / aïllament

- **Per tenant:** cada tenant només veu i aprova els **seus** contactes; l'aprovació va al **seu** Telegram; els callbacks es gategen pel seu `TenantChatLink` (com els DMs). Cap fuga cross-tenant.
- Validació de mida/format del text entrant.

---

## 9. Casos QA
1. Correu al tenant → esborrany al **seu** Telegram amb els 3 botons.
2. ✅ Enviar → el remitent rep la resposta (email/WhatsApp/xat segons canal); pending esborrat.
3. 🔄 Demana canvis → indicació → la IA regenera → torna amb els 3 botons (repetible).
4. ✍️ L'escric jo → el text del tenant s'envia pel canal.
5. Formulari (AMG v1) → a més del lead, esborrany a aprovar.
6. WhatsApp → esborrany → aprovació → resposta pel mateix WhatsApp.
7. Xat en **HYBRID** → «un momentet» + polling → apareix en aprovar; timeout → demana contacte.
8. **AUTO** (qualsevol canal) → resposta directa + còpia informativa; xat immediat sense polling.
9. **OFF** → només lead/avís.
10. **Aïllament:** el tenant A no rep ni pot aprovar els contactes del tenant B.
11. Canvi d'`agentMode` (HYBRID↔AUTO) → canvia el comportament dels 4 canals, sense doble notificació.

---

## 10. Dins/fora d'abast (v1)
- **Dins:** els 4 canals per al **tenant AMG** (tenant #1), amb els modes; polling del widget del xat; refine loop.
- **Fora:** extensió del **formulari/xat a les landings dels altres tenants** (natural després); WebSocket (n'hi ha prou amb polling); taula d'auditoria (v1 Redis); bústia visual (Omnichannel Inbox, Spec 25).
