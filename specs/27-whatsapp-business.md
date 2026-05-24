# Spec 27 — WhatsApp Business API (Meta Cloud + Embedded Signup)

> **Versió:** 1.0
> **Data:** 2026-05-24
> **Dependències:** Spec 01 (Auth), Spec 02 (Vault), Spec 20 (Agents), Spec 24 (Agent Activation)

---

## 1. Objectiu

Connectar cada tenant a la seva pròpia **WhatsApp Business Account (WABA)** de Meta, de manera que:

- Cada client usa el seu número de WhatsApp real (no un número d'AMG compartit)
- L'agent IA del Mòdul 20 respon pels canals configurats per a aquell tenant
- El flux de connexió és guiat (Embedded Signup) o manual (Phone Number ID + token)

---

## 2. Dos modes de connexió

### Mode A — Embedded Signup (producció, recomanat)
> Requereix Facebook App aprovada per Meta amb permisos `whatsapp_business_messaging` + `business_management`.

1. Admin clica "Connectar WhatsApp" al portal
2. S'obre el popup de Meta (Facebook Login SDK / Embedded Signup)
3. El client s'autentica amb el seu compte Facebook Business
4. Meta retorna un `code` via JS callback
5. Frontend envia `code` al backend
6. Backend intercanvia `code` → `user_access_token` → `system_user_token` (llarg termini)
7. Backend obté `waba_id` + `phone_number_id` + registra webhook
8. Config desada al Vault (token xifrat)

### Mode B — Manual (MVP, disponible ara)
Admin entra manualment:
- `Phone Number ID` (del Meta Business Manager)
- `Access Token` permanent (system user token)
- `WABA ID` (opcional, per a registre de webhook)

Equivalent a l'actual `whatsappMetaPhoneNumberId` però amb token dedicat per tenant.

---

## 3. Model de dades

### `WhatsAppWabaConfig`

| Camp | Tipus | Descripció |
|------|-------|------------|
| id | UUID | PK |
| tenantId | UUID | FK Tenant (unique) |
| wabaId | String(50) | WhatsApp Business Account ID |
| phoneNumberId | String(50) | Phone Number ID de Meta |
| accessTokenRef | String(100) | Ref al Vault (token xifrat AES-256) |
| displayPhoneNumber | String(20) | Ex: `+34612345678` |
| businessName | String(100) | Nom del negoci (de Meta) |
| status | Enum | `PENDING`, `CONNECTED`, `ERROR`, `DISCONNECTED` |
| webhookRegistered | Boolean | Si el webhook de Meta ja s'ha registrat |
| connectedAt | Instant | Quan es va connectar |
| createdAt | Instant | Auditing |

### Enum `WhatsAppConnectionStatus`
```
PENDING      — config guardada, pendents de verificar
CONNECTED    — webhook actiu, rebent missatges
ERROR        — token invàlid o webhook fallit
DISCONNECTED — desconnectat manualment
```

---

## 4. Endpoints

| Mètode | URL | Rol | Descripció |
|--------|-----|-----|------------|
| GET | `/api/v1/whatsapp/tenants/{id}/config` | ADMIN+ | Obté config WABA |
| POST | `/api/v1/whatsapp/tenants/{id}/connect` | ADMIN+ | Connecta (manual o code OAuth) |
| POST | `/api/v1/whatsapp/tenants/{id}/verify` | ADMIN+ | Verifica token i registra webhook |
| DELETE | `/api/v1/whatsapp/tenants/{id}/config` | ADMIN+ | Desconnecta |
| GET | `/api/v1/whatsapp/webhook` | public | Verificació webhook Meta (challenge) |
| POST | `/api/v1/whatsapp/webhook` | public | Rep missatges/events de Meta |
| POST | `/api/v1/whatsapp/tenants/{id}/test` | ADMIN+ | Envia missatge de prova |

---

## 5. Flux de webhook Meta

Meta envia tots els missatges al mateix endpoint (`POST /api/v1/whatsapp/webhook`). El backend enruta per `phone_number_id`:

```
POST /api/v1/whatsapp/webhook
  → extreure phone_number_id del payload
  → cercar WhatsAppWabaConfig per phoneNumberId
  → obtenir tenantId
  → crear/actualitzar Contact (Spec 25 Inbox)
  → passar missatge a ConversationalAgentService (Spec 20)
```

### Payload Meta (simplificat)
```json
{
  "object": "whatsapp_business_account",
  "entry": [{
    "id": "WABA_ID",
    "changes": [{
      "value": {
        "messaging_product": "whatsapp",
        "metadata": { "phone_number_id": "PHONE_NUMBER_ID" },
        "messages": [{
          "from": "34612345678",
          "id": "wamid.XXX",
          "timestamp": "1234567890",
          "text": { "body": "Hola!" },
          "type": "text"
        }]
      }
    }]
  }]
}
```

---

## 6. Verificació webhook

Meta fa un GET per verificar el webhook:

```
GET /api/v1/whatsapp/webhook
  ?hub.mode=subscribe
  &hub.verify_token=<AMG_WHATSAPP_WEBHOOK_SECRET>
  &hub.challenge=<CHALLENGE_STRING>

→ retorna 200 amb el challenge string
```

`AMG_WHATSAPP_WEBHOOK_SECRET` és una clau configurada a `SystemConfig` (clau: `WHATSAPP_WEBHOOK_SECRET`).

---

## 7. Enviament de missatges (per l'agent)

El `ConversationalAgentService` ha de poder enviar missatges via WhatsApp Meta quan el tenant té WABA connectada:

```java
metaWhatsAppClient.sendTextMessage(
  phoneNumberId,    // del tenant
  accessToken,      // del Vault
  recipientPhone,   // número del client final
  messageText
);
```

### Endpoint Meta Graph API
```
POST https://graph.facebook.com/v18.0/{phone_number_id}/messages
Authorization: Bearer {access_token}
{
  "messaging_product": "whatsapp",
  "to": "34612345678",
  "type": "text",
  "text": { "body": "El text del missatge" }
}
```

---

## 8. Seguretat

- Access tokens mai exposats al frontend — sempre via ref al Vault
- Webhook verificat per `verify_token` + signatura HMAC-SHA256 (header `X-Hub-Signature-256`)
- Operació de connexió restringida a ADMIN i SUPER_ADMIN

---

## 9. Configuració del sistema (SystemConfig keys)

| Clau | Descripció |
|------|------------|
| `WHATSAPP_WEBHOOK_SECRET` | Token de verificació del webhook de Meta |
| `META_APP_ID` | ID de la Facebook App (per a Embedded Signup) |
| `META_APP_SECRET` | Secret de la Facebook App |

---

## 10. Tests QA

| ID | Test |
|----|------|
| WA-01 | POST connect manual → config desada, status PENDING |
| WA-02 | POST verify → token vàlid → status CONNECTED |
| WA-03 | POST verify → token invàlid → status ERROR, 422 |
| WA-04 | GET webhook challenge → retorna challenge |
| WA-05 | POST webhook → missatge enrutat al tenant correcte |
| WA-06 | POST webhook → phone_number_id desconegut → 200 ignorat |
| WA-07 | DELETE config → status DISCONNECTED |
| WA-08 | POST test → envia missatge de prova al número configurat |
| WA-09 | GET config → ADMIN veu la seva config, no la d'altres tenants |

---

## 11. Frontend (Admin Tenant Detail)

`WhatsAppMetaCard` component al detall del tenant (sota GoCardlessCard):

```
┌──────────────────────────────────────────────────────┐
│  WhatsApp Business API        [● CONNECTAT]  [↻ Re-  │
│                                               verificar]│
├──────────────────────────────────────────────────────┤
│  📱 +34 612 345 678 · Nom del negoci                  │
│  Phone Number ID: 123456789012345                      │
│  WABA ID: 987654321                                    │
│  Webhook: ✅ Registrat                                 │
│                                                        │
│  [Embedded Signup — Connectar compte Meta]             │
│     ↓ o bé                                            │
│  [Connectar manualment] → formulari token/phone_id    │
│                                                        │
│  [✉ Enviar missatge de prova]  [✗ Desconnectar]       │
└──────────────────────────────────────────────────────┘
```

---

## 12. Integració amb l'agent (Spec 20)

Quan `ConversationalAgentService` rep un missatge i vol respondre via WhatsApp:

1. Comprova si el tenant té `WhatsAppWabaConfig` amb `status = CONNECTED`
2. Si sí → usa `MetaWhatsAppClient.sendTextMessage(phoneNumberId, token, recipient, text)`
3. Si no → fallback a Twilio (si configurat) o ignora

El `phone_number_id` i el `access_token` (desxifrat del Vault) es passen al client en cada crida. **No es fa cache del token desxifrat en memòria.**
