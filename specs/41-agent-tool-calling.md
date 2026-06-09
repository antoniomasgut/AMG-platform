# Spec 41 — Agent Tool Calling + Auto-Quota

## Objectiu

Donar a l'agent conversacional accés a eines de negoci (function calling) i gestionar el pressupost de tokens/missatges amb auto-increment silenciós per evitar talls de servei.

---

## Eines disponibles

| Nom | Descripció |
|-----|------------|
| `search_customer` | Cerca leads al CRM per nom, email o telèfon |
| `get_last_quote`  | Obté el darrer pressupost del tenant (filtrable per estat) |
| `get_invoice_link`| Genera l'enllaç d'acceptació segur per a un pressupost SENT |
| `create_calendar_event` | Crea un event a Google Calendar via AGENDA config |

---

## Auto-Quota (tokens IA)

- Camp nou: `overage_token_increment` (default 50.000) a `tenant_ai_configs`
- Quan `canCallAI()` detecta que `used >= budget`:
  - Si `overageTokenIncrement > 0`: incrementa el budget, guarda, notifica admin via Telegram i retorna `true`
  - Si `overageTokenIncrement = 0`: comportament antic (bloqueja)

## Auto-Quota (missatges WhatsApp)

- Camps nous: `monthly_message_budget`, `overage_message_increment` (default 100)
- `ChannelUsageService.record()` crida `TokenBudgetService.checkAndAutoIncrementMessages()` per WHATSAPP i WHATSAPP_META
- Mateixa lògica: incrementa el límit i notifica en lloc de bloquejar

---

## Arquitectura

```
ConversationalAgentService
  └─ provider.chatWithTools(systemPrompt, history, userMessage, tools, executor)
       └─ AnthropicAIProvider (tool_use loop, màx 3 iteracions)
            ├─ tool_use → AgentToolRegistry.execute(tenantId, toolName, input)
            └─ end_turn → retorna text final
```

### `AgentTool` interface

```java
String name();
String description();
Map<String, Object> inputSchema();   // JSON Schema
String execute(UUID tenantId, Map<String, Object> input);
```

### Flux Anthropic tool_use

1. POST `/v1/messages` amb `tools` array
2. Si `stop_reason == "tool_use"`: executa eines, afegeix `tool_result`, torna a POST
3. Si `stop_reason == "end_turn"`: retorna text
4. Màxim 3 iteracions; si s'arriba, fallback a `chat()` simple

---

## Migració

**V32__agent_tools_quota.sql**

```sql
ALTER TABLE tenant_ai_configs
    ADD COLUMN IF NOT EXISTS overage_token_increment   INTEGER DEFAULT 50000,
    ADD COLUMN IF NOT EXISTS monthly_message_budget    INTEGER,
    ADD COLUMN IF NOT EXISTS overage_message_increment INTEGER DEFAULT 100;
```

---

## Fitxers modificats / creats

| Fitxer | Canvi |
|--------|-------|
| `V32__agent_tools_quota.sql` | Nova migració |
| `TenantAIConfig.java` | 3 nous camps |
| `TokenBudgetService.java` | Auto-increment tokens + missatges |
| `ChannelUsageService.java` | Crida checkAndAutoIncrementMessages |
| `AIProvider.java` | Nou default `chatWithTools()` |
| `AnthropicAIProvider.java` | Implementació tool_use loop |
| `AgentTool.java` | Interfície nova |
| `SearchCustomerTool.java` | Cerca leads |
| `GetLastQuoteTool.java` | Darrer pressupost |
| `GetInvoiceLinkTool.java` | Enllaç d'acceptació |
| `CreateCalendarEventTool.java` | Crea event calendari |
| `AgentToolRegistry.java` | Registre de totes les eines |
| `ConversationalAgentService.java` | Usa chatWithTools en lloc de chat |
| `ToolDefinition.java`, `ToolCall.java` | Records compartits |
