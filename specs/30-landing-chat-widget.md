# Mòdul 30: Landing Chat Widget — Bot IA incrustat a les landing pages

> **Versió:** 1.1
> **Data:** 2026-06-03
> **Dependències:** Mòdul 04 (Engine), Mòdul 20 (Agents IA), Spec 28 (NexeLocal Service Configs), Spec 31 (Agent Feature Toggles)

---

## 1. Objectiu

Incrustar un widget de chat IA directament a les landing pages perquè els visitants
puguin iniciar una conversa amb el bot del negoci sense sortir de la pàgina.

Quan el visitant polsa "Reserva la teva cita" (o qualsevol CTA configurat com a
`action: chat`), s'obre un panell lateral de xat que connecta amb l'agent IA del sector.

**Canal web de F1:** el xat widget és el canal d'entrada natiu de la landing. Requereix:
- **Landing Pro activa** — el chat widget NO està disponible a la Micro-landing
- **F1 contractada** — fase d'automació conversacional

Les funcionalitats addicionals (cites, pressupostos, fidelització, equip) s'activen
progressivament segons les fases F2-F5 contractades, igual que per WhatsApp i Email
(veure Spec 31). El domini és un tercer servei opcional, independent de tots dos.

---

## 2. Abast

### Inclòs
- Widget flotant de chat incrustat al HTML generat per l'Engine
- API pública de sessions de chat (sense autenticació, rate-limited per IP)
- Respostes IA via Claude (model configurable, context del sector)
- Historial de missatges dins la sessió (localStorage)
- Bloc nou de tipus `chat-cta` per al Factory
- Modificació del bloc `hero` per suportar `ctaAction: "chat"`
- **Pre-chat form:** nom + telèfon obligatoris abans d'iniciar la conversa
- **Creació automàtica de Lead** (deduplicat per telèfon) en primera contacte
- **Integració de fases NexeLocal:** F2 (AGENDA), F3 (PRESSUPOSTOS), F4 (FIDELITZACIO), F5 (EQUIP) actives si `enabled: true` a `nexe_service_configs`

### Exclòs
- Persistència de converses a la BD (sessions efímeres, Redis)
- Integració amb Omnichannel Inbox (Mòdul 25) — futur
- Notificació al propietari en temps real quan hi ha una conversa activa — futur

---

## 3. Arquitectura

```
Visitant → Landing HTML → Widget JS (inline)
                                ↓
                    POST /api/v1/chat/sessions          (crear sessió)
                    POST /api/v1/chat/sessions/{id}/messages  (enviar missatge)
                                ↓
                         ChatSessionService
                                ↓
                         Claude API (context del sector)
                                ↓
                         Resposta en streaming o síncron
```

Sessions guardades a **Redis** amb TTL de 2h. Clau: `chat:session:{uuid}`.

---

## 4. Model de dades (Redis)

### ChatSession
```json
{
  "id": "uuid",
  "landingSlug": "estetica-mireia",
  "landingId": "uuid",
  "tenantId": "uuid",
  "contactName": "Maria García",
  "contactPhone": "654123456",
  "leadId": "uuid",
  "agendaEnabled": true,
  "messageCount": 3,
  "messages": [
    { "role": "user", "content": "Hola, teniu hora per dimarts?" },
    { "role": "assistant", "content": "Hola! Sí, tenim disponibilitat..." }
  ],
  "createdAt": "2026-06-02T10:00:00Z",
  "lastActivityAt": "2026-06-02T10:05:00Z"
}
```

- `tenantId` — UUID del tenant propietari de la landing
- `contactName` / `contactPhone` — recollits al pre-chat form
- `leadId` — UUID del Lead creat (o existent) a la BD
- `agendaEnabled` — cache del flag `AGENDA.enabled` per evitar consultes repetides a BD
- TTL Redis: **2 hores** des de `lastActivityAt`

---

## 5. API REST pública

Prefix: `/api/v1/chat` — **tots els endpoints són públics (sense JWT)**

### `POST /api/v1/chat/sessions`
Crea una nova sessió de chat. Requereix nom i telèfon (recollits al pre-chat form).

**Request:**
```json
{
  "landingSlug": "estetica-mireia",
  "contactName": "Maria García",
  "contactPhone": "654123456"
}
```

**Response 201:**
```json
{
  "sessionId": "uuid",
  "greeting": "Hola Maria! Sóc l'assistent virtual d'Estètica Mireia. En què puc ajudar-te?"
}
```

- El `greeting` és el primer missatge de l'agent generat automàticament
- Si el telèfon ja existeix com a Lead del tenant, s'associa a la sessió sense crear duplicat
- Si és un contacte nou, es crea un Lead amb `source: CHAT_WIDGET`

**Rate limiting:** màx. 10 sessions per IP per hora.

---

### `POST /api/v1/chat/sessions/{sessionId}/messages`
Envia un missatge i obté la resposta de l'agent.

**Request:**
```json
{
  "message": "Teniu hora per dijous al matí per a la depilació làser?"
}
```

**Response 200:**
```json
{
  "reply": "Sí! Tenim disponibilitat dijous a les 10h i a les 11h30. Quin et va millor?",
  "sessionId": "uuid"
}
```

**Rate limiting:** màx. 20 missatges per sessió, màx. 60 missatges per IP per hora.

---

## 6. Servei IA (ChatSessionService)

**Model:** `claude-haiku-4-5-20251001` (ràpid i barat per a converses de xat).
**Tokens màxims resposta:** 300 (respostes curtes, estil WhatsApp).

### Construcció del system prompt

L'ordre d'assemblatge segueix el mateix patró que `PromptBuilder` (Spec 31):

```
[1] System prompt base del tenant (TenantChatLink.agentSystemPrompt)
[2] Bloc AGENDA    (si AGENDA.enabled = true a nexe_service_configs)
[3] Bloc PRESSUPOSTOS (si PRESSUPOSTOS.enabled = true)
[4] Bloc FIDELITZACIO (si FIDELITZACIO.enabled = true)
[5] Bloc EQUIP     (si EQUIP.enabled = true)
```

El flag `agendaEnabled` es comprova una sola vegada en crear la sessió i es
guarda a `ChatSession` per evitar consultes repetides a BD per cada missatge.

### Integració Google Calendar (F2)

Quan `agendaEnabled = true`, el bot pot emetre el tag especial:
```
[CONFIRMA_CITA:{"date":"YYYY-MM-DD","time":"HH:MM","duration":60,"name":"NOM","notes":"..."}]
```
`ChatSessionService.processBookingTag()` detecta el tag, crea l'event al Google Calendar
del tenant i elimina el tag de la resposta visible al client.

**Detecció de profanitat:** si el missatge de l'usuari conté paraules malsonants (llista
interna CA/ES/EN), la sessió s'elimina de Redis i es retorna `terminated: true` amb
un missatge d'avís. El widget tanca el panell automàticament als 3 segons.

---

## 7. Bloc `chat-cta` (nou tipus al Factory/Engine)

```json
{
  "id": "blk_chat",
  "type": "chat-cta",
  "props": {
    "title": "Reserva la teva cita",
    "subtitle": "Respon en menys d'1 minut",
    "buttonText": "Xateja amb nosaltres",
    "avatarUrl": "",
    "accentColor": "#c2185b"
  }
}
```

El render HTML del bloc genera:
- Una secció de CTA amb botó
- El botó té `onclick="openChatWidget()"` en lloc d'un enllaç

---

## 8. Widget JS (inline al HTML de la landing)

El `buildHtmlPage()` de l'Engine incrusta un `<script>` i `<div id="amg-chat-widget">` 
quan la landing té algun bloc `chat-cta` o quan `styles.chatEnabled = true`.

**Funcionalitat del widget:**
- Botó flotant baix-dreta (cercle amb icona de xat)
- Panell lateral que apareix en clicar (340px ample, altura 80vh)
- Capçalera: avatar + nom del negoci + "En línia"
- Àrea de missatges amb scroll
- Input + botó d'enviar
- Primera càrrega: `POST /chat/sessions` → mostra el salut inicial
- Cada missatge: `POST /chat/sessions/{id}/messages`
- Estat de "escrivint..." mentre espera resposta

**Disseny:** segueix la paleta de colors de la landing (`primaryColor`).
CSS inline, cap dependència externa.

---

## 9. Configuració per landing

### 9a. Styles de `LandingVersion` (activen el widget)

| Camp | Tipus | Descripció |
|------|-------|------------|
| `chatEnabled` | boolean | Activa el widget flotant globalment |
| `chatBusinessName` | string | Nom que apareix a la capçalera del widget |

### 9b. Taula `landing_chat_contexts` (context del bot)

```sql
CREATE TABLE landing_chat_contexts (
    landing_id       UUID PRIMARY KEY REFERENCES landings(id) ON DELETE CASCADE,
    business_name    VARCHAR(200) NOT NULL,
    sector           VARCHAR(50),
    system_prompt    TEXT NOT NULL,
    profanity_action VARCHAR(20) NOT NULL DEFAULT 'CLOSE',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Endpoints de gestió (autenticats):
- `GET    /api/v1/engine/landings/{id}/chat` — llegir context
- `PUT    /api/v1/engine/landings/{id}/chat` — crear/actualitzar context
- `DELETE /api/v1/engine/landings/{id}/chat` — eliminar context

Si no existeix fila, el backend usa un context genèric basat en el títol de la landing.

---

## 10. Modificació bloc `hero`

Afegir `ctaAction` a les props del bloc `hero`:

| Valor | Comportament |
|-------|-------------|
| `link` (default) | `<a href="ctaUrl">` — comportament actual |
| `chat` | `<button onclick="openChatWidget()">` — obre el widget |

---

## 11. Seguretat

- Tots els endpoints `/api/v1/chat/**` sense JWT però amb **rate limiting** per IP (Redis)
- Missatges sanititzats (màx. 500 chars per missatge d'entrada)
- El system prompt i les configs del tenant no s'exposen mai al client — és server-side
- El `sessionId` és un UUID aleatori no predictible
- Nom i telèfon del pre-chat form validats: mínim 2 caràcters (nom), format telèfon bàsic

---

## 12. Casos QA

| # | Cas | Resultat esperat |
|---|-----|-----------------|
| 1 | Visitant obre landing amb `chatEnabled: true` | Botó flotant visible baix-dreta |
| 2 | Visitant clica el botó de xat | Panell s'obre, salut automàtic en 1-2s |
| 3 | Visitant envia missatge | Resposta en <3s, màx. 2-3 frases |
| 4 | Sessió caducada (>2h) | Nou missatge crea sessió nova automàticament |
| 5 | 20 missatges enviats | Error amigable: "Conversa completada. Contacta'ns per telèfon." |
| 6 | Rate limit IP superat | HTTP 429, missatge amigable |
| 7 | Landing sense `chatEnabled` | Cap widget visible |
| 8 | Visitant envia paraula malsonant | Sessió tancada, missatge d'avís, widget es tanca als 3s |

---

## 13. Modificacions necessàries a landings existents

Les 3 demos de Salut/Bellesa s'han actualitzat als JSONs de demo:

**estetica-mireia:** `ctaAction: "chat"` al bloc `hero`, bloc `chat-cta` afegit,
`chatEnabled: true` + `chatBusinessName: "Estètica Mireia"` als `styles`.

**fisio-llevant** i **nutricio-sa-salut**: mateixa transformació.

Després del desplegament cal crear el chat context via API:
```
PUT /api/v1/engine/landings/{id}/chat
{ "businessName": "Estètica Mireia", "sector": "ESTETICA",
  "systemPrompt": "..." }
```

---

## 14. Pla d'implementació

1. **Backend:** `ChatSession` (model Redis), `ChatSessionService`, `ChatController`
2. **Backend:** `AnthropicClient` (si no existeix) o reutilitzar el del Mòdul 20
3. **Backend:** Rate limiter per IP (Redis + `@RateLimiter` o filter manual)
4. **Backend:** `EngineOrchestrator.buildHtmlPage()` → incrusta el JS del widget
5. **Frontend Factory:** nou bloc `chat-cta` al catàleg de blocs
6. **Demos:** actualitzar els 3 JSONs + re-desplegar

