# Spec 54 · Google Business Reviews → Telegram (notificació + resposta)

**Estat:** 📝 Proposta (pendent aprovació)
**Depèn de:** Mòdul 40 (Google Workspace), Mòdul 32 (Tenant Notifications), Mòdul 20 (Agents Telegram)
**Fase comercial:** F4 (Fidelització) — reutilitza `businessEnabled` + `businessLocationId`

---

## 1. Objectiu

Quan algú publica una ressenya al perfil de Google Business del tenant:

1. El tenant rep un **missatge de Telegram** amb les dades principals (autor, estrelles, comentari, data).
2. El tenant pot **respondre la ressenya des del mateix Telegram**, i la resposta es publica al perfil de Google.

---

## 2. Què ja existeix (no s'ha de reconstruir)

| Peça | Ubicació | Estat |
|------|----------|-------|
| Polling de ressenyes GBP v4.9 + cache | `GoogleBusinessReviewSyncService.sync()` (diari 3am) | ✅ |
| Entitat + repo | `GoogleBusinessReview`, `google_business_reviews` | ✅ |
| Comanda `/reviews` a Telegram (mostra 5 darreres) | `TenantTelegramCommandService.handleReviews()` | ✅ |
| Enviar Telegram al tenant vinculat | `TenantNotificationService.notify()` + `TenantChatLink` | ✅ |
| Botons inline + `answerCallbackQuery` | `TelegramBotClient.sendMessageWithButtons()` | ✅ |
| Dispatch de callbacks al webhook + patró d'estat pendent | `TelegramWebhookController` (p.ex. `budgetWorkflowService.hasPendingApproval`) | ✅ |

## 3. Què falta (a construir)

### 3.1 Detecció de ressenya nova
`upsertReview()` actualment fa upsert silenciós. Cal distingir **nova** (no existia el `reviewId`) i marcar-la per notificar. Afegir columna `notified_at TIMESTAMPTZ NULL` a `google_business_reviews`:
- Ressenya nova → `notified_at = NULL` → candidata a notificar.
- Després d'enviar el Telegram → `notified_at = now()`.

### 3.2 Cadència de polling ⚠️ DECISIÓ
El `@Scheduled` actual és **diari (3am)** — massa lent per a "quan algú escriu". La Google Business Profile API **no té webhook push** de ressenya nova (les notificacions via Pub/Sub requereixen allowlist de Google, procés separat).
**Recomanació:** afegir un `@Scheduled` addicional cada **15 min** que només itera tenants F4 amb `businessEnabled`, crida `sync()` i tot seguit notifica les ressenyes amb `notified_at IS NULL`. Manté el sync 3am com a full-resync.

### 3.3 Notificació Telegram
Nou event `GOOGLE_REVIEW_NEW` a `NotificationEvent`. Missatge:
```
⭐ Nova ressenya a Google · ★★★★☆ (4/5)
👤 Marta P.
"Molt bon tracte, tornaré segur"
🕒 fa 5 min
```
+ botó inline **✍️ Respondre** amb `callback_data = grev:<reviewId>`.

### 3.4 Flux de resposta des de Telegram
1. Callback `grev:<reviewId>` → `answerCallbackQuery` + desa estat pendent `(chatId → reviewId)` a Redis (TTL 15 min), i respon "Escriu la resposta per a aquesta ressenya".
2. El següent missatge de text d'aquest chat (detectat al webhook, prioritat abans del routing d'agents, com `hasPendingApproval`) es pren com a text de resposta.
3. `GoogleBusinessReviewSyncService.replyToReview(tenantId, reviewId, text)`:
   `PUT https://mybusiness.googleapis.com/v4/{reviewName}/reply` amb body `{"comment": text}` i Bearer token del tenant.
4. Actualitza `GoogleBusinessReview.reply` local + confirma al Telegram ("✅ Resposta publicada").

Alternativa/complement: comanda directa `/respondre <text>` responent la darrera ressenya sense contestar.

---

## 4. ⚠️ Blocador crític: scope OAuth

`GoogleAuthService.SCOPES` **només** conté `drive`, `gmail`, `calendar`, `sheets`. **No inclou** `https://www.googleapis.com/auth/business.manage`, que és obligatori tant per **llegir** com per **respondre** ressenyes GBP. Conseqüències:

- El polling de ressenyes actual (Mòdul 40) probablement **encara no funciona en producció** per manca d'scope.
- Cal afegir la clau de mòdul `"business"` → `business.manage` al mapa `SCOPES` i al flux d'activació Google.
- **Els tenants ja connectats hauran de tornar a autoritzar Google** (re-consent) per concedir el nou scope. Impacte operatiu real a comunicar.

---

## 5. Canvis en producció (ddl-auto: validate)
```sql
ALTER TABLE google_business_reviews ADD COLUMN notified_at TIMESTAMPTZ NULL;
```
+ migració Flyway equivalent.

---

## 6. Fitxers afectats (previsió)
- `GoogleBusinessReviewSyncService` — `syncAndNotify()` @Scheduled 15min, `replyToReview()`, flag nova a `upsertReview`.
- `GoogleBusinessReview` + repo — camp `notifiedAt`, `findByTenantIdAndNotifiedAtIsNull`.
- `NotificationEvent` — `GOOGLE_REVIEW_NEW`.
- `TelegramWebhookController` — callback `grev:` + estat pendent de resposta.
- `GoogleReviewReplyService` (nou, estat pendent Redis) o reutilitzar patró existent.
- `GoogleAuthService.SCOPES` — afegir `business` → `business.manage`.

---

## 7. Decisions (aprovades 2026-07-06)
| Decisió | Valor |
|---------|-------|
| Cadència detecció | Polling cada **60 min** (F4 + businessEnabled) |
| UX resposta | **Botó inline "✍️ Respondre"** + estat pendent (com aprovació de pressupost) |
| Scope re-consent | Afegir `business.manage` i **re-autoritzar** els tenants ja connectats |
| Notificar només ≥ cert rating? | No — notificar totes; el tenant decideix si respon |

**Backfill:** al primer sync d'un tenant (0 ressenyes prèvies a la BD) les ressenyes existents es marquen `notified_at = now()` sense notificar, per evitar spam de l'històric.
