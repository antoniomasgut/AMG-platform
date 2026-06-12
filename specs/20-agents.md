# Mòdul 20 — Agents Autònoms i Conversacionals

**Versió:** 2.1  
**Estat:** v1 ✅ Implementat · v2 ⏳ Pendent · v2.1 (costos/pressupostos) ✅ Implementat  
**Branca:** main (commit `ae694d6`) + feat/modul-20-v2  
**Substitució de:** n8n per a automatitzacions bàsiques

> **v1 (existent):** Agents d'automatització interna — envien missatges programats al tenant via Telegram.  
> **v2 (nou):** Agents conversacionals — responen als clients finals del tenant via WhatsApp, Telegram i Email.

---

## 1. Visió general

Sistema modular d'agents autònoms integrats amb Telegram que substitueix n8n per a automatitzacions senzilles. Cada agent és un `@Component` Spring que implementa la interfície `Agent` i s'auto-registra al `AgentRegistry`. L'activació és event-driven: quan un tenant verifica un servei al portal, el sistema activa l'agent corresponent.

**Principi:** un agent = un servei del catàleg. El `slug` de l'agent ha de coincidir exactament amb el `slug` del `CatalogService`.

---

## 2. Arquitectura

```
Telegram Webhook
       ↓
TelegramWebhookController
  ├─ /start {linkCode}  → vincula tenant ↔ chatId
  └─ missatge normal    → AgentRegistry → agent.handleMessage()

ServiceVerifiedEvent (Spring)
       ↓
AgentLifecycleManager
       └─ agent.onActivate(tenantId)
              └─ crea ScheduledAgentTask

AgentTaskScheduler (cada 60s)
       └─ agent.executeTask(tenantId, taskType, payload)
```

---

## 3. Entitats de domini

### TenantChatLink
Vincle 1:1 entre un tenant i un chat de Telegram.

| Camp | Tipus | Notes |
|------|-------|-------|
| `id` | UUID | PK |
| `tenantId` | UUID | unique — un tenant = un chat |
| `telegramChatId` | Long | null fins que s'activi el vincle |
| `linkCode` | String(20) | codi temporal (`/start linkCode`) |
| `linkCodeExpiresAt` | Instant | caducitat del codi |
| `isActive` | Boolean | default true |
| `createdAt` / `updatedAt` | Instant | auditoría |

### ScheduledAgentTask
Cua de tasques que els agents han d'executar.

| Camp | Tipus | Notes |
|------|-------|-------|
| `id` | UUID | PK |
| `tenantId` | UUID | propietari |
| `agentSlug` | String(50) | agent que ha d'executar-la |
| `taskType` | String(50) | ex. `SEND_REMINDER` |
| `payload` | TEXT | JSON lliure amb dades |
| `scheduledAt` | Instant | quan executar |
| `status` | Enum | `PENDING` / `EXECUTED` / `FAILED` |
| `executedAt` | Instant | null fins que s'executa |
| `errorMessage` | TEXT | missatge d'error si `FAILED` |

**Índex:** `(status, scheduled_at)` per a eficiència del scheduler.

---

## 4. Interfície Agent

```java
public interface Agent {
    String getServiceSlug();          // ha de coincidir amb CatalogService.slug
    String getDisplayName();          // nom llegible
    void onActivate(UUID tenantId);   // s'activa quan el servei es verifica
    void onDeactivate(UUID tenantId); // s'activa quan el servei es desactiva
    String handleMessage(UUID tenantId, String text, Long chatId); // missatge TG entrant
    void executeTask(UUID tenantId, String taskType, String payloadJson); // tasca scheduled
}
```

`handleMessage` retorna `null` si l'agent no sap gestionar el missatge (el sistema prova el següent agent).

---

## 5. Components del sistema

### TelegramWebhookController
`POST /api/v1/agents/telegram/webhook` — `permitAll`

- Detecta `/start {linkCode}` → valida codi, vincula `chatId` al tenant, envia benvinguda
- Missatge normal → busca tenant per `chatId` → enruta a cada agent per ordre → primer que retorna resposta no-null l'envia
- Sempre retorna HTTP 200 amb JSON de resposta directa de Telegram

### AgentRegistry
- `@PostConstruct` indexa tots els `Agent` beans per slug
- Thread-safe (populate únic a l'arrencada)
- Mètodes: `findBySlug()`, `hasAgent()`, `getAllAgents()`

### AgentLifecycleManager
- `@TransactionalEventListener` de `ServiceVerifiedEvent`
- Quan es publica l'event: busca agent per `serviceSlug` → crida `onActivate(tenantId)`
- Si no hi ha agent per aquell slug: log debug, continua sense error

### TelegramBotClient
- Wrapper REST per a l'API de Telegram
- `sendMessage(chatId, text)` → `POST https://api.telegram.org/bot{token}/sendMessage`
- Parse mode HTML, retorna `boolean` (èxit/error)
- Token configurat via `app.agents.telegram.bot-token`

### AgentTaskScheduler
- `@Scheduled(fixedRate = 60_000)` — s'executa cada 60 segons
- Selecciona tasques `PENDING` amb `scheduledAt <= now()` ordenades per data
- Per cada tasca: invoca `agent.executeTask()` → marca `EXECUTED` o `FAILED`
- Transaccional per garantir consistència

---

## 6. Agents implementats

### ReminderAgent (`slug: "recordatori-24h"`)
Envia recordatoris periòdics via Telegram substituint la workflow de n8n.

**Credencials encriptades al Vault:**
- `hours_before` — hores d'antelació del recordatori
- `reminder_template` — text del missatge a enviar

**onActivate:**
1. Llegeix `hours_before` del Vault (desencriptat)
2. Valida que el tenant té `TenantChatLink` actiu
3. Crea `ScheduledAgentTask(scheduledAt = now + hours_before, taskType = "SEND_REMINDER")`
4. Envia missatge de benvinguda via Telegram

**handleMessage:** detecta "recordatori"/"hora" → mostra plantilla | "ajuda"/"help" → instruccions | altres → `null`

**executeTask (`SEND_REMINDER`):** llegeix `reminder_template` del Vault → envia `"🐶 RECORDATORI\n\n{plantilla}"` → marca `EXECUTED`

---

## 7. Flux d'activació complet

```
1. Usuari verifica servei "recordatori-24h" al portal
   ↓ TenantVaultService publica ServiceVerifiedEvent
   
2. AgentLifecycleManager rep l'event
   ↓ busca ReminderAgent per slug
   ↓ crida onActivate(tenantId)

3. ReminderAgent.onActivate()
   ↓ crea ScheduledAgentTask (PENDING, scheduledAt = now + 24h)
   ↓ envia welcome via Telegram

4. AgentTaskScheduler (cada 60s)
   ↓ troba la tasca quan scheduledAt <= now
   ↓ executeTask("SEND_REMINDER")
   ↓ envia recordatori → marca EXECUTED

5. Usuari rep missatge Telegram
   ↓ pot respondre "recordatori" per veure plantilla
```

---

## 8. Configuració (`application.yml`)

```yaml
app:
  agents:
    telegram:
      bot-token: ${TELEGRAM_BOT_TOKEN:}
```

Variables d'entorn necessàries:
- `TELEGRAM_BOT_TOKEN` — token del bot de Telegram

---

## 9. Migració de base de dades

`docs/migration-006-agents.sql` — crea:
- `tenant_chat_links`
- `scheduled_agent_tasks`
- Índex `(status, scheduled_at)` a `scheduled_agent_tasks`

---

## 10. Com afegir un nou agent

1. Crear `@Component` que implementi `Agent`
2. Assegurar que `getServiceSlug()` retorna el `slug` d'un `CatalogService` existent
3. Implementar `onActivate`, `handleMessage`, `executeTask`
4. L'`AgentRegistry` l'auto-descobreix a l'arrencada
5. L'`AgentLifecycleManager` l'activa quan el servei es verifica

No cal cap registre manual — l'arquitectura és completament plug-in.

---

## 11. Casos QA

| # | Cas | Resultat esperat |
|---|-----|-----------------|
| TC-01 | `/start {linkCode}` vàlid → Telegram | Vincula chatId, envia benvinguda |
| TC-02 | `/start {linkCode}` caducat | Retorna error "Codi caducat" |
| TC-03 | Servei verificat → `ServiceVerifiedEvent` | `onActivate` crida, tasca creada |
| TC-04 | Scheduler processa tasca PENDING vençuda | Marca EXECUTED, envia missatge |
| TC-05 | Agent no trobat per slug | Tasca marcada FAILED |
| TC-06 | Missatge "recordatori" a Telegram | Retorna plantilla encriptada |
| TC-07 | Missatge desconegut | Retorna null, cap resposta |
| TC-08 | Error a executeTask | Captura excepció, marca FAILED amb errorMessage |

---

## 12. Estat i pendent

| Ítem | Estat |
|------|-------|
| Interfície Agent | ✅ |
| AgentRegistry | ✅ |
| AgentLifecycleManager | ✅ |
| TelegramWebhookController | ✅ |
| TelegramBotClient | ✅ |
| AgentTaskScheduler | ✅ |
| ReminderAgent | ✅ |
| Tests d'integració | ⏳ Pendents |
| Més agents (ex. InvoiceAgent, ReviewAgent) | ⏳ Pendents |
| Configurar Telegram webhook al servidor | ⏳ Pendent (`TELEGRAM_BOT_TOKEN`) |

---

# Part 2 — Agents Conversacionals (v2)

> Agents orientats al client final del tenant. Reben missatges de clients del negoci via WhatsApp, Telegram o Email, els processen amb IA (Claude) i responen automàticament o en mode híbrid.

---

## 13. Visió general v2

```
Client final del negoci
  └─→ WhatsApp / Telegram / Email
           ↓
    WebhookController (per canal)
           ↓
    ConversationalAgentService
      ├─ Carrega config tenant (Vault)
      ├─ Carrega historial (ConversationService)
      ├─ PromptBuilder → system prompt
      ├─ Crida Claude API
      └─ AgentMode check:
           AUTO   → respon immediatament
           HYBRID → guarda pendent, notifica tenant per Telegram intern
           MANUAL → notifica tenant, no envia res
```

**Principi:** el ConversationalAgent és un `@Service` únic (no implementa la interfície `Agent` d'automatització). Coexisteix amb els agents de la Part 1 sense interferència.

---

## 14. Entitats noves

### Conversation

| Camp | Tipus | Descripció |
|------|-------|-----------|
| `id` | BIGSERIAL | PK |
| `tenantId` | UUID | Tenant propietari |
| `customerIdentifier` | String(100) | Telèfon, email o chatId del client final |
| `channel` | Enum(STRING) | `WHATSAPP` \| `TELEGRAM` \| `EMAIL` |
| `role` | Enum(STRING) | `USER` \| `ASSISTANT` |
| `content` | TEXT | Contingut del missatge |
| `pendingApproval` | Boolean | true si mode HYBRID i pendent d'aprovació |
| `approvedAt` | Instant | Quan el tenant ha aprovat/enviat |
| `createdAt` | Instant | |

**Índexs:** `(tenant_id, customer_identifier, channel)`, `(tenant_id, pending_approval) WHERE pending_approval = true`

**Persistència:** tots els missatges es guarden a PostgreSQL (font de veritat). Redis és caché de lectura ràpida (TTL 10 min), no és la font principal. Veure Spec 26 secció 5 per a la gestió de l'historial llarg (resum automàtic).

### AgentMode (extensió a TenantChatLink)

Nou camp a `TenantChatLink`:

| Camp | Tipus | Descripció |
|------|-------|-----------|
| `agentMode` | Enum(STRING) | `AUTO` \| `HYBRID` \| `MANUAL` · default `AUTO` |
| `whatsappPhoneNumber` | String(20) | Número Twilio assignat al tenant |
| `emailAddress` | String(100) | Email Resend del negoci |

**AgentMode enum:** `AUTO` (respon sol), `HYBRID` (suggereix, tenant aprova), `MANUAL` (tenant respon, agent en silenci)

---

## 15. ConversationalAgentService

```
handleIncoming(tenantId, customerIdentifier, channel, text):
  1. Carrega TenantChatLink → agentMode, canals actius
  2. ConversationService.loadHistory(tenantId, customerIdentifier, channel)
        → últims 30 missatges de PostgreSQL
        → Contact.conversationSummary (resum de missatges anteriors, si n > 30)
  3. PromptBuilder.build(tenantId, customerContext) → system prompt
        → inclou KnowledgeBase del tenant (Spec 26)
        → inclou historial del client (resum + missatges recents)
  4. Claude API (claude-haiku-4-5) → resposta
  5. switch(agentMode):
       AUTO   → sendViaChannel() → saveConversation(pendingApproval=false)
       HYBRID → saveConversation(pendingApproval=true) → notifyTenantViaTelegram()
       MANUAL → notifyTenantViaTelegram() [només avís, no guarda resposta IA]
  6. Guarda missatge del client a Conversation
```

**Model IA:** `claude-haiku-4-5-20251001` (balanç cost/velocitat per a respostes conversacionals)

---

## 16. PromptBuilder

Construeix el system prompt llegint dades del Vault del tenant (ja configurat via Mòdul 17 Service Wizard):

```
Ets l'assistent virtual de {businessName}.
El teu to és: {tone} (llegit del Vault o default "professional i proper").
Respons en català de forma concisa i natural.

HORARI: {schedule del Vault}
SERVEIS: {llista de serveis del TenantService}
PREUS: {preus configurats al Vault}
INSTRUCCIONS ESPECIALS: {customInstructions del Vault, si existeix}

REGLES:
- Si no saps alguna cosa, pregunta en lloc d'inventar
- Confirma les dades abans de qualsevol compromís
- En cas d'urgència o queixa greu, indica que contactin directament
```

**Font de dades:** `TenantVaultService.getDecryptedCredentials(tenantId)` per als camps configurats al Wizard.

---

## 17. Canals

### Canal WhatsApp (Twilio)

**Credencials al Vault** (per tenant): `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_WHATSAPP_NUMBER`

**Webhook:** `POST /api/v1/agents/whatsapp/webhook/{tenantId}` · `permitAll`
- Valida signatura Twilio (`X-Twilio-Signature`)
- Extreu `From` (telèfon client) i `Body` (text)
- Delega a `ConversationalAgentService`

**Enviament:** `WhatsAppChannel.send(phone, text)` via Twilio REST API

**Costos estimats per tenant:** ~€1/mes número + €0.009/missatge sortint (recordatoris) · missatges entrants gratuïts (finestra 24h)

### Canal Telegram (client-facing, diferent del Telegram intern)

El Telegram intern (Part 1) notifica el **tenant**. El Telegram client-facing respon als **clients finals del negoci**.

Cada tenant que activa aquest canal crea el seu propi bot de Telegram (token al Vault).

**Webhook:** `POST /api/v1/agents/telegram/customer/webhook/{tenantId}` · `permitAll`

### Canal Email (Resend)

**Credencials al Vault** (global o per tenant): `RESEND_API_KEY`, `RESEND_FROM_DOMAIN`

**Webhook entrant:** `POST /api/v1/agents/email/webhook/{tenantId}` · `permitAll`
- Extreu `from` (email client) i `text`
- Delega a `ConversationalAgentService`

**Enviament:** `EmailChannel.send(toEmail, subject, text)` via Resend REST API

---

## 18. API REST — Agents conversacionals

Prefix: `/api/v1/agents/conversational`

| Mètode | Ruta | Descripció | Rols |
|--------|------|-----------|------|
| GET | /{tenantId}/conversations | Llistar converses recents (paginat) | Autenticat (propi tenant) |
| GET | /{tenantId}/conversations/{customerId} | Historial amb un client | Autenticat |
| GET | /{tenantId}/pending | Respostes pendents d'aprovació | Autenticat |
| POST | /{tenantId}/pending/{conversationId}/approve | Aprovar i enviar resposta | Autenticat |
| POST | /{tenantId}/pending/{conversationId}/edit | Editar i enviar | Autenticat |
| DELETE | /{tenantId}/pending/{conversationId} | Descartar resposta | Autenticat |
| PUT | /{tenantId}/mode | Canviar AgentMode | Autenticat |
| GET | /{tenantId}/status | Estat dels canals actius | Autenticat |

#### `GET /{tenantId}/pending` — Respostes pendents

Response:
```json
[
  {
    "id": 123,
    "customerIdentifier": "+34612345678",
    "channel": "WHATSAPP",
    "customerMessage": "Vull una cita per dijous",
    "suggestedResponse": "Hola! Per dijous tenim disponible a les 10h i les 16h...",
    "createdAt": "2026-05-18T10:30:00Z"
  }
]
```

#### `PUT /{tenantId}/mode` — Canviar mode

Request: `{ "mode": "HYBRID" }`
Response 200: `{ "mode": "HYBRID", "updatedAt": "..." }`

---

## 19. Frontend — `/portal/agents`

Accessible per a tots els tenants (no només SUPER_ADMIN). Mostra l'estat de l'agent conversacional del tenant.

### Pestanya "Agent"
- Toggle mode: AUTO / HYBRID / MANUAL (amb explicació breu de cada un)
- Canals actius: Telegram ✓ · WhatsApp ✓/pendent · Email ✓/pendent
- Estadístiques: converses aquest mes, temps de resposta mig

### Pestanya "Pendents" (visible si HYBRID i hi ha pendents)
- Llista de missatges pendents d'aprovació
- Per cada un: missatge del client + resposta suggerida per l'agent
- Accions: `[Enviar]` `[Editar i enviar]` `[Descartar]`
- Badge al menú lateral quan hi ha pendents

### Pestanya "Converses"
- Llista de converses recents agrupades per client
- Vista de la conversa completa (usuari / agent alternats)
- Filtre per canal

---

## 20. Configuració

```yaml
app:
  agents:
    telegram:
      bot-token: ${TELEGRAM_BOT_TOKEN:}      # existent (intern)
    conversational:
      claude-model: claude-haiku-4-5-20251001
      max-history-messages: 20
      conversation-ttl-hours: 48
```

Variables Twilio (per tenant, al Vault):
- `TWILIO_ACCOUNT_SID`
- `TWILIO_AUTH_TOKEN`
- `TWILIO_WHATSAPP_NUMBER`

Variables Resend (globals o per tenant):
- `RESEND_API_KEY`
- `RESEND_FROM_DOMAIN`

---

## 21. Casos QA — ConversationalAgent

| # | Cas | Resultat esperat |
|---|-----|-----------------|
| CA-01 | Missatge WhatsApp en mode AUTO | Resposta enviada immediatament, conversa guardada |
| CA-02 | Missatge WhatsApp en mode HYBRID | Guardat com a pendent, notificació al Telegram intern del tenant |
| CA-03 | Missatge WhatsApp en mode MANUAL | Notificació al tenant, cap resposta enviada |
| CA-04 | Tenant aprova resposta pendent | Missatge enviat al client, `pendingApproval = false` |
| CA-05 | Tenant edita i envia resposta | Missatge modificat enviat, conversa actualitzada |
| CA-06 | Tenant descarta resposta | `pendingApproval = false`, sense enviament |
| CA-07 | Canvi de mode AUTO → HYBRID | Mode actualitzat, pendents futurs queden a la cua |
| CA-08 | PromptBuilder sense dades Vault | Usa prompt genèric de fallback |
| CA-09 | Historial carregat des de Redis | Últims 20 missatges inclosos al context |
| CA-10 | Signatura Twilio invàlida | 403 Forbidden |
| CA-11 | CLIENT veu les seves pròpies converses | 200, no veu converses d'altres tenants |

---

## 22. Estat v2

| Ítem | Estat |
|------|-------|
| Entitat Conversation | ⏳ Pendent |
| AgentMode a TenantChatLink | ⏳ Pendent |
| ConversationalAgentService | ⏳ Pendent |
| PromptBuilder | ⏳ Pendent |
| WhatsAppChannel (Twilio) | ⏳ Pendent |
| EmailChannel (Resend) | ⏳ Pendent |
| Telegram client-facing webhook | ⏳ Pendent |
| API REST conversacional | ⏳ Pendent |
| Frontend /portal/agents | ⏳ Pendent |
| Migració BD (conversation, agentMode) | ⏳ Pendent |

---

# Part 3 — Sistema de costos i pressupostos IA (v2.1)

> Control de costos per tenant: preus de models IA amb markup, pressupostos mensuals de consum, i tracking del cost real per missatge/token.

---

## 23. Entitat ModelPricing

Taula `model_pricing` (V49). Preu de cost i preu client per model IA, amb markup configurable.

| Camp | Tipus | Descripció |
|------|-------|-----------|
| `modelName` | VARCHAR(100) PK | Identificador del model (ex. `claude-haiku-4-5-20251001`) |
| `provider` | VARCHAR(50) | Proveïdor (`anthropic`, `deepseek`, `other`) |
| `inputCostMicros` | BIGINT | Cost real entrada en micros d'€ per token |
| `outputCostMicros` | BIGINT | Cost real sortida en micros d'€ per token |
| `markupPercent` | INTEGER | Markup aplicat al client (default 20%) |
| `clientInputMicros` | BIGINT | Preu client entrada = `inputCostMicros × (100 + markup) / 100` |
| `clientOutputMicros` | BIGINT | Preu client sortida = `outputCostMicros × (100 + markup) / 100` |
| `updatedAt` | TIMESTAMPTZ | Última actualització |

**Models suportats:** `claude-haiku-4-5-20251001`, `claude-haiku-4-5`, `claude-sonnet-4-6`, `claude-opus-4-7`, `deepseek-chat`, `deepseek-reasoner`

**Recàlcul automàtic:** `ModelPricingService` recalcula el primer dia de cada mes a les 00:05 UTC aplicant el markup actual. Es pot recalcular manualment via `recalculate(markupPercent)`.

---

## 24. Extensions a TenantAIConfig

Nous camps afegits en migracions successives:

| Camp | Migració | Tipus | Descripció |
|------|----------|-------|-----------|
| `responseLanguage` | V45 | VARCHAR(10) | Idioma de les respostes de l'agent (ex. `ca`, `es`) |
| `chatModel` | V46 | VARCHAR(100) | Model a usar pel canal chat widget |
| `whatsappModel` | V46 | VARCHAR(100) | Model a usar pel canal WhatsApp |
| `emailModel` | V46 | VARCHAR(100) | Model a usar pel canal email |
| `fallbackModel` | V47 | VARCHAR(100) | Model de fallback si el principal falla |
| `monthlyCostBudgetEurCents` | V48 | INTEGER | Pressupost mensual IA en cèntims (500 = €5) |
| `monthlyMessageBudget` | V46 | INTEGER | Nombre màxim de missatges WhatsApp/mes |
| `monthlyWhatsappBudgetEurCents` | V51 | INTEGER | Pressupost mensual WhatsApp en cèntims |

---

## 25. Extensions a sector_phases (V50)

Nous camps per calcular pressupostos recomanats per fase i sector:

| Camp | Tipus | Descripció |
|------|-------|-----------|
| `aiBudgetEurCents` | INTEGER | Pressupost base IA en c€/mes per a mida AUTÒNOM (default per fase: F1=500, F2=200, F3=150, F4=50, F5+=50) |
| `whatsappMessageBudget` | INTEGER | Missatges WA base/mes per a mida AUTÒNOM (F1=200, F2=150, F3=50, F4=30, F5+=0) |

---

## 26. Extensions a channel_usage_logs i token_usage_logs

**`token_usage_logs`** (V48): nou camp `cost_eur_micros BIGINT` — cost calculat en micros d'€ en el moment de l'enviament.

**`channel_usage_logs`** (V51): nou camp `cost_eur_micros BIGINT` — cost del missatge WhatsApp en micros d'€ (tarifa conservadora per defecte: €0.055/missatge = 55.000 micros).

---

## 27. TenantBudgetDefaultsService

Calcula i aplica pressupostos mensuals recomanats per a un tenant basant-se en:
1. Les fases contractades (`tenant.contractedPhases` — format `F1,F2,F3`)
2. La mida d'empresa (`businessSize`): AUTÒNOM ×1 · PETIT ×2.5 · MITJÀ ×5
3. Els valors base de `sector_phases.ai_budget_eur_cents` i `whatsapp_message_budget`

**Mètodes públics:**

| Mètode | Descripció |
|--------|-----------|
| `getDefaults(tenantId)` | Calcula els pressupostos recomanats sense persistir |
| `applyDefaults(tenantId)` | Calcula i persisteix a `TenantAIConfig` (sobreescriu sempre) |
| `compute(size, contractedPhases)` | Càlcul genèric sense accedir a BD de tenant |

**BudgetDefaults record:**
```
costBudgetEurCents     — pressupost IA en c€/mes
messageBudget          — missatges WhatsApp/mes
whatsappBudgetEurCents — pressupost WA en c€/mes (estimació tarifa €0.055/msg)
breakdown              — text llegible amb el desglossament per fase
```
