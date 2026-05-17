# Mòdul 20 — Agents Autònoms (Telegram)

**Versió:** 1.0  
**Estat:** ✅ Implementat  
**Branca:** main (commit `ae694d6`)  
**Substitució de:** n8n per a automatitzacions bàsiques

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
