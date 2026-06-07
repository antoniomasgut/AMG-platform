# Resum: 5 grans features completades (Juny 2026)

## 1. Google Workspace Frontend

**Endpoints nous (backend):**
- `GET /api/v1/google/drive/{tenantId}` — llista fitxers de Drive
- `GET /api/v1/google/calendar/events/{tenantId}` — llista esdeveniments del calendari

**Pàgina nova (frontend):**
- `/portal/admin/integrations/` — pàgina unificada d'integracions amb targetes per:
  - Google Workspace (amb estat de connexió en viu)
  - WhatsApp Business
  - Telegram
  - SMTP
  - Meta Ads
  - GoCardless

**Canvis:**
- `GoogleController.java` — +2 endpoints
- `GoogleOrchestrator.java` — +2 mètodes (`listDriveFiles`, `listCalendarEvents`)
- `DriveFileItem.java`, `CalendarEventItem.java` — DTOs nous
- `integrations/page.tsx` — pàgina nova
- `PortalShell.tsx` — enllaç canviat de "Google Workspace" a "Integracions"

---

## 2. GoCardless Real Client

**Fitxer nou (backend):**
- `GoCardlessRealClient.java` — client HTTP real per l'API GoCardless
  - `createRedirectFlow` + `completeRedirectFlow`
  - `createPayment`
  - `cancelMandate`
  - Autenticació Basic Auth amb API Key del tenant (Vault) o global (SystemConfig)
  - Header `GoCardless-Version: 2015-07-06`

**Activat per:** `app.gocardless.provider=live` (default: mock)

**Canvis (frontend):**
- `admin.ts` — `getPaymentProviders()` + `ProviderSummary` interface
- `GoCardlessCard` — mostra resum del proveïdor de pagament

---

## 3. Meta Ads Fase 2/3 (ja existia)

**Estat:** Ja completat abans d'aquesta sessió.

**Inclou:**
- Entities: `AdCampaign`, `AdSet`, `AdCreative`, `Ad`
- CRUD endpoints per a totes les entitats
- Publicació a Meta API real
- Pausa/reactivació/arxiu de campanyes
- Cerca de targeting (interessos, ubicacions)
- Generació d'imatges via DALL-E 3
- Estimació d'audiència
- Sincronització diària de despesa
- Rate limiting amb Redis

---

## 4. Vault Comms Module

**Nous fitxers:**
- `CommunicationSender.java` — interfície SPI per a senders de comunicació
- `BrevoCommunicationSender.java` — implementació per email via Brevo REST API
- `VaultCommunicationService.java` — servei principal (send + respond + retry)
- `CommunicationRetryJob.java` — scheduler horari per reintentar comunicacions fallides

**Canvis:**
- `CommunicationRequest.java` — nous camps: `retryCount`, `maxRetries`
- `CommunicationRequestRepository.java` — `findExpiredForRetry` query
- `TenantVaultService.java` — `requestClientInfo` ara envia email de veritat via `VaultCommunicationService`

**Config necessària (SystemConfig):**
- `BREVO_API_KEY`
- `BREVO_SENDER_EMAIL`

---

## 5. pgvector / Semantic Search per RAG

**Nous fitxers:**
- `EmbeddingService.java` — crida a OpenAI `text-embedding-3-small`
  - `embed(String text) → float[]`
  - `embedToJson(String text) → String` (JSON array)
  - `parseEmbedding(String json) → float[]`
  - `cosineSimilarity(float[] a, float[] b) → double`
  - Fallback a `null` si no hi ha API key
- `VectorSearchService.java` — cerca semàntica
  - `search(knowledgeBaseId, query, limit)` → `List<SearchResult>`
  - Llindar mínim de similitud: 0.3
  - Ordenat per score descendent

**Canvis:**
- `KnowledgeEntry.java` — nous camps: `embedding` (TEXT), `isVectorized` (BOOLEAN)
- `KnowledgeController.java` — endpoints nous:
  - `POST /api/v1/agents/knowledge/{tenantId}/vectorize` — vectoritza totes les entries
  - `POST /api/v1/agents/knowledge/{tenantId}/search` — cerca semàntica (body: `{query, limit}`)

**Nota:** Embeddings guardats com a JSON TEXT (no pgvector nadiu). Es pot migrar a pgvector més endavant.

---

## Decisions tècniques clau

| Decisió | Raó |
|---------|-----|
| Embeddings com a TEXT JSON (no pgvector) | Evita requerir extensió PostgreSQL |
| CommunicationSender com a SPI | Fàcil afegir WhatsApp/Telegram senders després |
| GoCardless real client per property | No canviar codi per canviar de sandbox → producció |
| Integracions en una sola pàgina | UX més neta que rutes separades per servei |
| `ddl-auto: validate` a producció | Migracions manuals obligatòries |

---

## Migracions pendents per producció

```sql
ALTER TABLE knowledge_entries ADD COLUMN embedding TEXT;
ALTER TABLE knowledge_entries ADD COLUMN is_vectorized BOOLEAN DEFAULT FALSE;

ALTER TABLE communication_requests ADD COLUMN retry_count INTEGER DEFAULT 0;
ALTER TABLE communication_requests ADD COLUMN max_retries INTEGER DEFAULT 3;

ALTER TABLE prospect_campaigns ADD COLUMN scheduled_next_run TIMESTAMPTZ;
ALTER TABLE prospect_campaigns ADD COLUMN repeat_interval_days INTEGER;
```
