# Mòdul 30: Landing Chat Widget — Bot IA incrustat a les landing pages

> **Versió:** 1.0
> **Data:** 2026-06-02
> **Dependències:** Mòdul 04 (Engine), Mòdul 20 (Agents IA)

---

## 1. Objectiu

Incrustar un widget de chat IA directament a les landing pages perquè els visitants
puguin iniciar una conversa amb el bot del negoci sense sortir de la pàgina.

Quan el visitant polsa "Reserva la teva cita" (o qualsevol CTA configurat com a
`action: chat`), s'obre un panell lateral de xat que connecta amb l'agent IA del sector.

**Cas d'ús principal:** demos de venda — el prospect veu la landing i immediatament
pot provar el bot que tindria al seu negoci.

---

## 2. Abast

### Inclòs
- Widget flotant de chat incrustat al HTML generat per l'Engine
- API pública de sessions de chat (sense autenticació, rate-limited per IP)
- Respostes IA via Claude (model configurable, context del sector)
- Historial de missatges dins la sessió (localStorage)
- Bloc nou de tipus `chat-cta` per al Factory
- Modificació del bloc `hero` per suportar `ctaAction: "chat"`

### Exclòs
- Persistència de converses a la BD (sessions efímeres, RAM/Redis)
- Integració amb Omnichannel Inbox (Mòdul 25) — futur
- Identificació del visitant (no demana email/nom per iniciar)
- Notificació al propietari quan hi ha una conversa activa — futur

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
  "sectorContext": "Ets l'assistent virtual d'Estètica Mireia...",
  "messages": [
    { "role": "user", "content": "Hola, teniu hora per dimarts?" },
    { "role": "assistant", "content": "Hola! Sí, tenim disponibilitat..." }
  ],
  "createdAt": "2026-06-02T10:00:00Z",
  "lastActivityAt": "2026-06-02T10:05:00Z"
}
```

TTL Redis: **2 hores** des de `lastActivityAt`.

---

## 5. API REST pública

Prefix: `/api/v1/chat` — **tots els endpoints són públics (sense JWT)**

### `POST /api/v1/chat/sessions`
Crea una nova sessió de chat.

**Request:**
```json
{
  "landingSlug": "estetica-mireia"
}
```

**Response 201:**
```json
{
  "sessionId": "uuid",
  "greeting": "Hola! Sóc l'assistent virtual d'Estètica Mireia. En què puc ajudar-te?"
}
```

El `greeting` és el primer missatge de l'agent generat automàticament.

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

```java
// Construeix el context complet de la conversa i crida Claude
private String callClaude(ChatSession session, String userMessage) {
    // 1. System prompt = sectorContext de la landing
    // 2. Historial de missatges anteriors (màx. últims 10)
    // 3. Nou missatge de l'usuari
    // → Crida a AnthropicClient.chat(messages)
    // → Retorna resposta com a String
}
```

**Model:** `claude-haiku-4-5` (ràpid i barat per a converses de xat).
**Tokens màxims resposta:** 300 (respostes curtes, estil WhatsApp).
**Context del sector:** llegit de `LandingVersion.styles["chatContext"]` o,
si no existeix, del `systemPrompt` de `SECTOR_CONTEXTS` (frontend) → el backend
necessita la seva pròpia taula/config de contextos per sector.

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

Al `styles` de la `LandingVersion`:

| Camp | Tipus | Descripció |
|------|-------|------------|
| `chatEnabled` | boolean | Activa el widget flotant globalment |
| `chatContext` | string | System prompt del bot per a aquesta landing específica |
| `chatBusinessName` | string | Nom que apareix a la capçalera del widget |
| `chatAvatarUrl` | string | Avatar del bot (URL) |

Si `chatContext` és buit, el backend usa un context genèric basat en el títol de la landing.

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
- El `sectorContext` no s'exposa mai al client — és server-side
- Sessions no vinculades a usuaris ni tenants (privacitat)
- El `sessionId` és un UUID aleatori no predictible

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

---

## 13. Modificacions necessàries a landings existents

Les 3 demos de Salut/Bellesa cal actualitzar-les:

**estetica-mireia:** canviar `ctaUrl: "#contacte"` → `ctaAction: "chat"` al bloc `hero`.
Afegir bloc `chat-cta` just abans del `contact-form`. Afegir a `styles`:
```json
"chatEnabled": true,
"chatContext": "[system prompt ESTETICA adaptat a Mireia]",
"chatBusinessName": "Estètica Mireia"
```

**fisio-llevant** i **nutricio-sa-salut**: mateixa transformació.

---

## 14. Pla d'implementació

1. **Backend:** `ChatSession` (model Redis), `ChatSessionService`, `ChatController`
2. **Backend:** `AnthropicClient` (si no existeix) o reutilitzar el del Mòdul 20
3. **Backend:** Rate limiter per IP (Redis + `@RateLimiter` o filter manual)
4. **Backend:** `EngineOrchestrator.buildHtmlPage()` → incrusta el JS del widget
5. **Frontend Factory:** nou bloc `chat-cta` al catàleg de blocs
6. **Demos:** actualitzar els 3 JSONs + re-desplegar

