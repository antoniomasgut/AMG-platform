# Spec 59 — Inbound Assist (esborrany IA + aprovació humana, per tenant)

> **Versió:** 2.1 (validat per tenant: enum AUTO/HYBRID/MANUAL; `hasAwait` abans del catch-all admin (crític per a AMG=tenant#1); xat va per ChatSessionService no handleIncoming; polling per índex + appendAssistantMessage + frontend AgencyChatWidget; EmailChannel per multi-tenant; AUTO sense flag pending)
> **Data:** 2026-07-14
> **Estat:** Validat — llest per implementar
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
| **MANUAL** | Sense bot | Només lead/avís; cap resposta automàtica. |

> ⚠️ L'enum real és **`AUTO` / `HYBRID` / `MANUAL`** (no "OFF").
> ⚠️ **AUTO i el flag pending:** `generateDmDraft` sempre persisteix el missatge amb `pending=true`. En AUTO no ha de quedar marcat pending → cal un persist **no-pending** per a AUTO (o reutilitzar `handleIncoming` en AUTO evitant el doble `notifyOperator`).

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

### 5.2 Botons i free-text (routing Telegram)
- **Callbacks** (`iaok:/iarf:/iawr:`): s'afegeixen al costat de `dmok:/dmwr:` a `TelegramWebhookController` (línies ~220-266), que **retornen ABANS** del check admin → cap problema. Gate: `findByTelegramChatId(cbChatId)` + `isActive`, com els DMs.
- **Free-text pendent** (per a 🔄 i ✍️): el bloc `findByTelegramChatId` on viuen els `hasPending` (reviews/comments/DMs) està **DESPRÉS** del catch-all admin (`isAdminChat` → `AmgAdminCommandService.handle` → `return`, línia ~332).
  - ⚠️ **REQUISIT v1 (crític):** com que el Telegram del tenant AMG serà segurament el `AMG_SALES_CHAT_ID` (que **és** admin chat), el text de 🔄/✍️ l'engoliria l'assistent admin. Per tant cal inserir, **ABANS** de la línia 332:
    ```java
    if (!text.startsWith("/") && inboundAssist.hasAwait(chatId)) {
        var reply = inboundAssist.submitAwaitText(chatId, text);
        if (reply != null) return ResponseEntity.ok(okTgReply(chatId, reply));
    }
    ```
- **🔄 Demana canvis** (`iarf:`) → `await REFINE` → el següent text es passa a la IA com a **instrucció de reescriptura** → torna a §5.1. Repetible.
- **✍️ L'escric jo** (`iawr:`) → `await MANUAL` → el següent text és la resposta final.

### 5.3 Enviament (channel-aware)
| Canal | En ✅ / en enviar el manual |
|-------|-----------------------------|
| **EMAIL** | v1 (AMG): `EmailService.sendEmail(destinatari, assumpte, cos)` (Brevo). *Multi-tenant: usar `EmailChannel.sendMessage(to,subject,text,fromEmail,fromName,replyTo)` per preservar la identitat del tenant.* |
| **WHATSAPP_META** | `WhatsAppMetaChannel.sendMessage(tenant.whatsappMetaPhoneNumberId, toPhone, text)` |
| **WIDGET** | nou mètode públic `ChatSessionService.appendAssistantMessage(sessionId, text)` → el visitant ho rep per polling (§6) |

---

## 6. Xat: polling (HYBRID) / síncron (AUTO)

⚠️ **Premissa corregida:** el xat de la landing **NO passa per `handleIncoming`**, sinó per `ChatController → ChatSessionService.sendMessage`, que té el seu **propi `callAI` síncron** (sense tools, sense `agentMode`) i retorna la resposta a la mateixa petició. Per tant la intercepció HYBRID s'ha de fer **dins `ChatSessionService.sendMessage`**.

Per a **HYBRID** cal fer el xat **asíncron amb polling**:
1. Dins `ChatSessionService.sendMessage`, si el tenant és HYBRID → **no** cridar `callAI`; retornar `{status:"PENDING", index:<n>}` (n = nombre de missatges actual) i mostrar «Ara mateix et responem, un momentet 👋».
2. `intake(WIDGET,...)` → esborrany al Telegram del tenant.
3. Nou mètode públic `appendAssistantMessage(sessionId, text)` (avui `saveSession`/`loadSession` són privats).
4. Nou endpoint `GET /api/v1/chat/sessions/{id}/poll?after=<index>` (permitAll) → missatges de la sessió a partir de l'**índex** (⚠️ `ChatMessage` **no té id ni timestamp** → cursor per **posició**, no per msgId).
5. El widget fa **polling** (~3 s) fins que apareix la resposta aprovada.

⚠️ **Frontend:** cal modificar `frontend/src/components/landing/AgencyChatWidget.tsx` (avui espera reply síncron) → afegir el missatge d'espera, el polling i el timeout.

**UX:** el visitant espera l'aprovació → **timeout de fallback** (60-90 s) → el widget demana el contacte («et responem de seguida per WhatsApp/email»), així cap lead es perd. En **AUTO**, el xat torna a ser **immediat** (sense polling, `callAI` com avui).

---

## 7. Prerequisits (per tenant)

- **Correu (inbound = Mailgun):** `EmailWebhookController` processa `*@inbound.amgdl.com` (verifica `MAILGUN_WEBHOOK_SIGNING_KEY`). Per a AMG: encaminar `info@amgdl.com` a l'inbound + `TenantChatLink.emailAddress = info@amgdl.com` lligat al tenant AMG. Resposta sortint = Brevo. Veure `docs/config-correu-cloudflare.html`.
- **Agent IA actiu** per al tenant (`generateDmDraft` retorna null si no ho està).
- **WhatsApp** connectat i lligat al tenant (Mòdul 51).
- **Convivència amb `agentMode` (per punt d'entrada, ⚠️ no és igual als 4):**
  - **Correu i WhatsApp** van per `handleIncoming` → l'intake el **substitueix** (evita el `notifyOperator` sense botó en HYBRID).
  - **Formulari** avui **no** crida `handleIncoming` (només lead+avís) → l'intake és **additiu**.
  - **Xat** va per `ChatSessionService.sendMessage` (no `handleIncoming`) → la intercepció es fa **allà** (§6).
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
9. **MANUAL** → només lead/avís, cap resposta automàtica.
10. **Aïllament:** el tenant A no rep ni pot aprovar els contactes del tenant B.
11. Canvi d'`agentMode` (HYBRID↔AUTO) → canvia el comportament dels 4 canals, sense doble notificació.

---

## 10. Dins/fora d'abast (v1)
- **Dins:** els 4 canals per al **tenant AMG** (tenant #1), amb els modes; refine loop; **polling del xat** (backend: endpoint `poll` + `appendAssistantMessage` + cursor per índex; **frontend: `AgencyChatWidget.tsx`** amb polling/espera/timeout); el **check `hasAwait` abans del catch-all admin** (§5.2).
- **Fora:** extensió del **formulari/xat a les landings dels altres tenants** (natural després); WebSocket (n'hi ha prou amb polling); taula d'auditoria (v1 Redis); bústia visual (Omnichannel Inbox, Spec 25).
