# Spec 59 — Agency Inbound Assist (esborrany IA + aprovació humana)

> **Versió:** 1.0
> **Data:** 2026-07-14
> **Estat:** Proposat
> **Depèn de:** Spec 56 F2 (DMs amb aprovació híbrida — patró reutilitzat), Spec 20 (Agents Telegram), Spec 30 (Landing Chat Widget), Spec 51 (Agency Multichannel), Spec 25 (Omnichannel Inbox)

---

## 1. Objectiu

Que **tots els contactes entrants propis d'AMG** (no dels tenants) passin per l'agent IA, que **redacta un esborrany de resposta**, i que **abans d'enviar res AMG l'hagi d'aprovar** des del Telegram (✅ Enviar / ✍️ Escriure). Cap resposta surt sense el vistiplau humà.

**Principi:** la IA estalvia el temps de redactar; la persona manté el control total. És el mateix patró que ja funciona per als DMs d'Instagram/Messenger dels tenants (Spec 56 F2), aplicat als **canals propis d'AMG**.

**Canals coberts:** correu `info@amgdl.com`, formulari web d'`amgdl.com`, WhatsApp d'AMG, i el xat de la landing d'AMG.

---

## 2. Model reutilitzat (ja existent)

`SocialDmService` (Spec 56 F2) fa exactament el flux objectiu:
1. Missatge entrant → `ConversationalAgentService.generateDmDraft(tenantId, senderId, channel, text)` → esborrany.
2. `telegramBotClient.sendMessageWithButtons(chatId, esborrany, [✅ Enviar `dmok:id`, ✍️ Escriure `dmwr:id`])`.
3. `approveDraft(chatId, id)` envia · `startManualReply` + `submitManualReply` per escriure'l a mà. Estat a **Redis**.

Aquest mòdul **generalitza** aquest patró a més canals, amb el tenant fixat al d'AMG.

---

## 3. Identitat "AMG"

- Tenant propietari = **`PLATFORM_TENANT_ID`** (config existent).
- Xat d'aprovació = **`AMG_SALES_CHAT_ID`** (config existent, ja rep les notificacions del formulari web).
- Els fluxos d'aquest mòdul **només** apliquen quan el destinatari és AMG (tenant = PLATFORM_TENANT_ID). Els contactes dels tenants segueixen el seu camí actual.

---

## 4. Model de dades

### 4.1 `agency_pending_replies` (o Redis + auditoria mínima)
| Camp | Tipus | Nota |
|------|-------|------|
| `id` | UUID/String | id curt per als callbacks |
| `channel` | VARCHAR(20) | `EMAIL` / `WEB_FORM` / `WHATSAPP` / `CHAT` |
| `from_ref` | VARCHAR(255) | email, telèfon o session id del remitent |
| `from_name` | VARCHAR(255) | si es coneix |
| `inbound_text` | TEXT | missatge original |
| `draft` | TEXT | esborrany generat per la IA |
| `status` | VARCHAR(20) | `PENDING` / `SENT` / `MANUAL` / `DISCARDED` |
| `created_at` / `resolved_at` | TIMESTAMPTZ | |

> L'estat efímer (esperant text manual) va a **Redis** com al patró DM. La taula és per a **auditoria/historial** i per resoldre el callback després d'un reinici. (Es pot començar només amb Redis i afegir la taula si cal.)

---

## 5. Flux (per canal)

### 5.1 Entrada → esborrany
| Canal | Punt d'entrada | Canvi |
|-------|----------------|-------|
| **Correu** | `EmailWebhookController.handleAsync(tenantId, from, text)` | Si `tenantId == PLATFORM_TENANT_ID` → en comptes d'auto-respondre, crea `PendingReply(EMAIL)` i notifica per aprovar |
| **Formulari web** | `PublicContactController.submit()` | A més del lead + avís actual, genera esborrany IA i el passa a aprovar |
| **WhatsApp** | webhook de WhatsApp d'AMG | Si el número és el d'AMG → esborrany + aprovar |
| **Xat landing** | `ChatSessionService` (Spec 30) | Si és la landing d'AMG → esborrany + aprovar (veure §6, latència) |

Notificació d'aprovació (a `AMG_SALES_CHAT_ID`):
```
📨 Nova consulta [CANAL] de <from>
«<inbound_text>»

Resposta proposada:
«<draft>»
[✅ Enviar]  [✍️ Escriure]
```
Callbacks: `arok:<id>` (aprovar) · `arwr:<id>` (escriure a mà) — nous prefixos al `TelegramWebhookController`.

### 5.2 Aprovació → enviament (per canal)
| Canal | En ✅ Enviar |
|-------|-------------|
| **Correu** | `EmailService.sendEmail(from, assumpte, draft)` (Brevo) |
| **Formulari web** | email a l'adreça del remitent |
| **WhatsApp** | enviar pel canal de WhatsApp al remitent |
| **Xat landing** | publicar el missatge a la `ChatSession` (el visitant el veu) |

En **✍️ Escriure**: AMG escriu el text i s'envia igual pel canal corresponent (`submitManualReply` generalitzat).

---

## 6. El cas del xat en directe (latència)

El xat de la landing és **síncron**: el visitant espera. Amb aprovació humana hi ha retard (AMG ha d'aprovar al Telegram). Solucions (config per canal):
- **Mode aprovació** (per defecte per a la resta): el visitant rep un missatge de cortesia («Ara mateix et responem 👋») i la resposta apareix quan AMG aprova. Adequat si AMG està atent.
- **Mode auto amb còpia** (opcional per al xat): el xat respon a l'instant amb la IA i AMG en rep **còpia** al Telegram per intervenir si cal.

Per defecte: **aprovació** a correu/formulari/WhatsApp; per al **xat**, configurable (comença en mode auto+còpia per no fer esperar el visitant).

---

## 7. Prerequisits d'infraestructura

- **Correu:** encaminar `info@amgdl.com` al webhook (MX de `amgdl.com` → Brevo inbound) i crear l'enllaç `info@amgdl.com → PLATFORM_TENANT_ID` (`TenantChatLink.emailAddress`). Veure `docs/config-correu-cloudflare.html`.
- **WhatsApp d'AMG:** número d'AMG connectat (Spec 51).
- Config existent: `PLATFORM_TENANT_ID`, `AMG_SALES_CHAT_ID`, `ANTHROPIC_API_KEY`, `TELEGRAM_BOT_TOKEN`.

---

## 8. Seguretat / aïllament

- Els fluxos només s'activen per al tenant d'AMG (`PLATFORM_TENANT_ID`). No afecten els contactes dels tenants.
- Els callbacks `arok:`/`arwr:` només s'accepten si el xat és `AMG_SALES_CHAT_ID` (o un admin d'AMG), no des de qualsevol xat.
- Validació de mida/format del text entrant (com el formulari actual).

---

## 9. Casos QA
1. Correu a info@ (un cop encaminat) → arriba esborrany al Telegram d'AMG amb ✅/✍️.
2. ✅ Enviar → el remitent rep la resposta per email; estat `SENT`.
3. ✍️ Escriure → AMG escriu; s'envia el seu text; estat `MANUAL`.
4. Formulari web → a més del lead, arriba esborrany a aprovar.
5. WhatsApp d'AMG → esborrany + aprovació → resposta pel mateix WhatsApp.
6. Xat landing en mode auto+còpia → el visitant rep resposta immediata i AMG en rep còpia.
7. Callback `arok:` des d'un xat que NO és el d'AMG → ignorat.
8. Contacte d'un **tenant** (no AMG) → segueix el flux actual, sense aprovació d'AMG.

---

## 10. Fora d'abast (v1)
- Bústia unificada visual d'AMG (ja hi ha l'Omnichannel Inbox, Spec 25; aquí n'hi ha prou amb Telegram).
- Respostes multi-idioma automàtiques (la IA respon en l'idioma del missatge; sense selector).
