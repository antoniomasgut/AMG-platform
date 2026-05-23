# Spec 25 — Omnichannel Inbox (Converses Unificades)

**Versió:** 1.0  
**Data:** 2026-05-23  
**Estat:** Aprovat  
**Depèn de:** Spec 20 (Agents), Spec 24 (Fluxos d'activació)

---

## 1. Objectiu

Proporcionar una inbox unificada que consolidi totes les converses del agent conversacional en un sol lloc, independentment del canal (WhatsApp, Telegram, Email). Permet a l'equip AMG (i als clients) respondre manualment, canviar el mode de l'agent i veure l'historial complet.

---

## 2. Model de contactes (Opció B)

### 2.1 Principi

Cada parella `(customerIdentifier, channel)` genera automàticament un **Contact** en el primer missatge. El mateix contacte real pot tenir múltiples identificadors (p.ex. WhatsApp + Telegram) que inicialment apareixen separats. En el futur es podrà fer un merge manual.

### 2.2 Entitats noves

#### `Contact`
```
id          UUID PK
tenant_id   UUID FK (no FK constraint, per rendiment)
display_name VARCHAR — per defecte = customerIdentifier, editable per l'admin
created_at  TIMESTAMP
```

#### `ContactIdentifier`
```
id          UUID PK
contact_id  UUID FK → contacts.id
tenant_id   UUID (per facilitar queries sense JOIN)
channel     ENUM (WHATSAPP, WHATSAPP_META, TELEGRAM, EMAIL)
identifier  VARCHAR — el customerIdentifier del canal
created_at  TIMESTAMP

UNIQUE (tenant_id, channel, identifier)
```

### 2.3 Cicle de vida

```
1r missatge d'un nou (tenant, channel, identifier)
  → findOrCreate: busca ContactIdentifier existent
  → si no existeix: crea Contact (displayName = identifier) + ContactIdentifier
  → processa el missatge normalment
```

---

## 3. API Backend

### 3.1 Endpoints nous

| Mètode | Ruta | Accés | Descripció |
|--------|------|-------|------------|
| GET | `/api/v1/agents/contacts/{tenantId}` | ADMIN, CLIENT propi | Llista de contactes amb resum |
| GET | `/api/v1/agents/contacts/{tenantId}/{contactId}/thread` | ADMIN, CLIENT propi | Tots els missatges d'un contacte (tots els canals) |
| POST | `/api/v1/agents/contacts/{tenantId}/{contactId}/reply` | ADMIN, CLIENT propi | Envia resposta manual pel canal més recent del contacte |
| PATCH | `/api/v1/agents/contacts/{tenantId}/{contactId}/name` | ADMIN, CLIENT propi | Reanomena el contacte |

### 3.2 DTOs

#### `ContactSummaryResponse`
```json
{
  "contactId": "uuid",
  "displayName": "Joan Mestre",
  "channels": [
    { "channel": "WHATSAPP", "identifier": "+34666123456" },
    { "channel": "TELEGRAM", "identifier": "5520713163" }
  ],
  "lastMessage": "Hola, quant costa el servei?",
  "lastMessageRole": "USER",
  "lastMessageAt": "2026-05-23T10:30:00Z",
  "lastChannel": "WHATSAPP",
  "lastIdentifier": "+34666123456",
  "pendingCount": 0
}
```

#### `ContactThreadResponse`
Reutilitza `ConversationResponse` existent, afegint:
```json
{
  "id": 42,
  "customerIdentifier": "+34666123456",
  "channel": "WHATSAPP",
  "role": "USER",
  "content": "Hola, quant costa el servei?",
  "pendingApproval": false,
  "createdAt": "2026-05-23T10:30:00Z"
}
```

#### `SendReplyRequest`
```json
{ "text": "Hola Joan! El servei costa..." }
```

#### `RenameContactRequest`
```json
{ "displayName": "Joan Mestre" }
```

### 3.3 Lògica de `sendReply`

1. Carrega el contact i els seus ContactIdentifiers
2. Troba l'identificador amb el missatge més recent (`findTop1...OrderByCreatedAtDesc`)
3. Envia via el canal corresponent (TelegramBotClient / WhatsAppChannel / WhatsAppMetaChannel / EmailChannel)
4. Guarda el missatge com a `Conversation` amb `role=ASSISTANT, pendingApproval=false`

### 3.4 Modificació a `ConversationalAgentService`

A `handleIncoming()`, just after the `isActive` check:
```java
contactService.findOrCreate(tenantId, channel, customerIdentifier);
```

### 3.5 Nous mètodes a `ConversationRepository`

```java
List<Conversation> findByTenantIdAndCustomerIdentifierAndChannelOrderByCreatedAtAsc(
    UUID tenantId, String customerIdentifier, ConversationChannel channel);

Optional<Conversation> findTop1ByTenantIdAndCustomerIdentifierAndChannelOrderByCreatedAtDesc(
    UUID tenantId, String customerIdentifier, ConversationChannel channel);

long countByTenantIdAndCustomerIdentifierAndChannelAndPendingApprovalTrue(
    UUID tenantId, String customerIdentifier, ConversationChannel channel);
```

---

## 4. Frontend

### 4.1 Ruta

`/portal/agents/inbox` — accessible per ADMIN i CLIENT.

- CLIENT: usa el seu propi `tenantId` de sessió
- ADMIN/SUPER_ADMIN: rep `tenantId` per query param (`?tenantId=xxx`) — accessible des de la fitxa del tenant

### 4.2 Layout

```
┌─────────────────────────────────────────────────────────────┐
│  Inbox · Agent Conversacional          [● AUTO ▼]  [⚙️]    │
├──────────────────────┬──────────────────────────────────────┤
│ 🔍 Cerca contacte    │  Joan Mestre                         │
├──────────────────────┤  📱 WhatsApp · +34666123456         │
│ Tots│WA│TG│Email     ├──────────────────────────────────────┤
├──────────────────────┤                                      │
│ 📱 Joan Mestre       │  [Joan]  Hola, quant costa?  10:28  │
│    "Hola, quant...   │                                      │
│    10:28 ·  WA       │  [Bot]   El servei costa...  10:29  │
├──────────────────────┤                                      │
│ ✈️ maria@mail.com    │  [Joan]  Perfecte, podem...  10:31  │
│    "Gràcies per..."  │                                      │
│    09:15 · Email     │                                      │
├──────────────────────┤  ──────────────────────────────────  │
│ 📟 5520713163        │  ✍️  Escriu una resposta...          │
│    "Ok, demà..."     │                     [Enviar per WA]  │
│    Ahir · TG         │                                      │
└──────────────────────┴──────────────────────────────────────┘
```

### 4.3 Components

| Component | Descripció |
|-----------|------------|
| `InboxPage` | Pàgina principal, gestiona estat selecció + mode |
| `ContactList` | Sidebar amb llista de contactes, cerca i filtres per canal |
| `ContactCard` | Targeta individual: icona canal, nom, preview, hora |
| `ThreadView` | Fil de conversa ordenat cronològicament |
| `MessageBubble` | Bombolla de missatge (USER blau, ASSISTANT gris, indicador canal) |
| `ReplyBox` | Caixa de resposta manual (visible sempre, desactivada en AUTO) |
| `ModeToggle` | Selector AUTO / HYBRID / MANUAL al header |

### 4.4 Comportament del `ReplyBox`

| Mode | Comportament |
|------|-------------|
| AUTO | Visible però desactivat + missatge "El bot respon automàticament" |
| HYBRID | Actiu — les respostes entren com a pendents d'aprovació |
| MANUAL | Actiu — envia directament |

### 4.5 Icones de canal

| Canal | Icona | Color |
|-------|-------|-------|
| WHATSAPP / WHATSAPP_META | 📱 | Verd |
| TELEGRAM | ✈️ | Blau |
| EMAIL | 📧 | Gris |

---

## 5. Serveis frontend nous (`agents-conversational.ts`)

```typescript
export interface ContactSummary {
  contactId: string;
  displayName: string;
  channels: { channel: string; identifier: string }[];
  lastMessage: string | null;
  lastMessageRole: 'USER' | 'ASSISTANT' | null;
  lastMessageAt: string | null;
  lastChannel: string | null;
  lastIdentifier: string | null;
  pendingCount: number;
}

export const listContacts = (tenantId: string) =>
  apiFetch<ContactSummary[]>(`/agents/contacts/${tenantId}`);

export const getContactThread = (tenantId: string, contactId: string) =>
  apiFetch<ConversationResponse[]>(`/agents/contacts/${tenantId}/${contactId}/thread`);

export const sendReply = (tenantId: string, contactId: string, text: string) =>
  apiFetch<void>(`/agents/contacts/${tenantId}/${contactId}/reply`, {
    method: 'POST', body: JSON.stringify({ text }),
  });

export const renameContact = (tenantId: string, contactId: string, displayName: string) =>
  apiFetch<void>(`/agents/contacts/${tenantId}/${contactId}/name`, {
    method: 'PATCH', body: JSON.stringify({ displayName }),
  });
```

---

## 6. Accés des d'altres pàgines

- **Menú lateral portal**: afegir "Inbox" sota "Agents" (amb badge de pendents si `pendingCount > 0`)
- **Fitxa de tenant (admin)**: botó "Obrir Inbox" que va a `/portal/agents/inbox?tenantId={id}`

---

## 7. Pendents futurs (fora d'aquest spec)

- **Merge de contactes**: UI per fusionar dos Contact en un (actualitza `contactId` dels ContactIdentifiers)
- **Notificació en temps real**: WebSocket o polling per refrescar inbox quan arriba un missatge nou
- **Adjunts**: suport d'imatges i documents en el fil de conversa
- **Etiquetes de contacte**: tagging manual (p.ex. "Client actiu", "Lead", "Problema tècnic")

---

## 8. Casos QA

| # | Cas | Resultat esperat |
|---|-----|-----------------|
| 1 | Primer missatge Telegram d'un número nou | Contact creat automàticament amb displayName = chatId |
| 2 | Segon missatge del mateix número | No es crea un segon Contact |
| 3 | Missatge WhatsApp del mateix número que Telegram | Es crea un segon Contact independent |
| 4 | Admin reanomena un contacte | `displayName` actualitzat, chatId/número segueix sent el identifier |
| 5 | Reply manual en mode AUTO | Boton desactivat, no envia |
| 6 | Reply manual en mode MANUAL | Missatge enviat via el canal corresponent, apareix al fil |
| 7 | Fil unificat d'un contacte amb 2 canals | Missatges de tots els canals ordenats per hora |
