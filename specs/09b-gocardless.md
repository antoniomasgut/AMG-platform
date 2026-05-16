# Mòdul 09b: GoCardless — Domiciliació SEPA Automàtica

> **Versió:** 1.0
> **Data:** 2026-05-16
> **Dependències:** Mòdul 01 (Auth), Mòdul 08 (FinOps — facturació mensual), Mòdul 09 (Payments — Stripe per setup)

---

## 1. Objectius

- Automatitzar el cobrament mensual de les quotes recurrents via **GoCardless** (SEPA Direct Debit)
- Eliminar la gestió manual del fitxer pain.008 per als tenants que usen GoCardless
- Permetre que cada tenant tingui configurat un proveïdor de pagament recurrent des de l'admin:
  - `TRANSFER` — transferència bancària manual (per defecte)
  - `SEPA_MANUAL` — fitxer pain.008 generat per nosaltres i pujat al banc
  - `GOCARDLESS` — cobrament automàtic via API GoCardless

---

## 2. Abast

### 2.1 Funcionalitats incloses

- **GoCardlessClient** (Interface + Mock + Real): abstracció per cridar l'API REST de GoCardless
- **Redirect Flow**: flux per autoritzar el mandat SEPA del client (redireccionar a pàgina GoCardless)
- **GoCardlessMandate**: emmagatzemar el mandate ID i estat per tenant
- **Cobrament mensual automàtic**: el `MonthlyBillingJob` detecta el proveïdor actiu per cada tenant i carrega via GoCardless si és el cas
- **Webhooks GoCardless**: rebre notificacions de pagament completat/fallit/mandat cancel·lat
- **Dashboard de proveïdors**: endpoint `GET /api/v1/payments/tenants/{id}/providers` que mostra quins proveïdors té actius cada tenant (Stripe per setup + proveïdor recurrent)

### 2.2 Funcionalitats excloses

- Pagaments instantanis (GoCardless és exclusivament per a recurrents/domiciliació)
- Targetes de crèdit (és Stripe)
- Reemborsaments via GoCardless (es gestionen manualment amb el banc)

### 2.3 Selecció de proveïdor des de l'admin

```
Admin Panel → Tenant Detail → Pestanya "Pagaments"

Setup (únic):
  ○ Transferència bancària (per defecte)
  ○ Stripe  [Configurar API Key]  ← isActive toggle

Mensual recurrent:
  ○ Transferència bancària (per defecte)
  ○ SEPA Manual (pain.008)  ← requereix SepaMandate registrat
  ○ GoCardless (automàtic)  [Configurar API Key]  ← isActive toggle
```

Regla de prioritat per recurrent: `GOCARDLESS > SEPA_MANUAL > TRANSFER`

---

## 3. Model de dades

### 3.1 GoCardlessConfig (Configuració per tenant)

| Camp | Tipus | Descripció |
|------|-------|-----------|
| id | UUID | PK |
| tenantId | UUID | FK Tenant (unique) |
| apiKeyRef | String(100) | Referència al Vault (API Key xifrada) |
| environment | Enum | `SANDBOX`, `LIVE` |
| creditorId | String(50) | Creditor ID de GoCardless |
| webhookSecret | String(100) | Secret per verificar webhooks |
| isActive | Boolean | Si el proveïdor està actiu per a aquest tenant |
| createdAt / updatedAt | Instant | Auditing |

### 3.2 GoCardlessMandate (Mandat SEPA via GoCardless)

| Camp | Tipus | Descripció |
|------|-------|-----------|
| id | UUID | PK |
| tenantId | UUID | FK Tenant (unique) |
| gcMandateId | String(50) | ID del mandat a GoCardless (ex: `MD001`) |
| gcRedirectFlowId | String(50) | ID del redirect flow (temporal, fins completar) |
| status | Enum | `PENDING_SUBMISSION`, `SUBMITTED`, `ACTIVE`, `FAILED`, `CANCELLED`, `EXPIRED` |
| accountHolderName | String(100) | Titular del compte (retornat per GoCardless) |
| bankName | String(100) | Nom del banc (retornat per GoCardless) |
| lastFourDigits | String(4) | Últims 4 dígits de l'IBAN |
| createdAt / updatedAt | Instant | Auditing |

### 3.3 GoCardlessPayment (Pagament mensual via GoCardless)

| Camp | Tipus | Descripció |
|------|-------|-----------|
| id | UUID | PK |
| tenantId | UUID | FK Tenant |
| monthlyInvoiceId | UUID | FK MonthlyInvoice |
| gcPaymentId | String(50) | ID del pagament a GoCardless |
| amount | BigDecimal | Import en €  |
| status | Enum | `PENDING_SUBMISSION`, `SUBMITTED`, `CONFIRMED`, `PAID_OUT`, `FAILED`, `CANCELLED` |
| chargeDate | LocalDate | Data prevista de càrrec |
| paidOutAt | Instant | Quan s'ha cobrat efectivament |
| failureReason | String(255) | Motiu del fallada (si FAILED) |
| createdAt / updatedAt | Instant | Auditing |

---

## 4. API REST

Prefix: `/api/v1/gocardless`

| Mètode | Ruta | Rols | Descripció |
|--------|------|------|-----------|
| POST | /configure/{tenantId} | SUPER_ADMIN | Configurar GoCardless (API Key + creditor ID + environment) |
| GET | /configure/{tenantId} | SUPER_ADMIN, ADMIN | Veure configuració GoCardless del tenant |
| POST | /tenants/{tenantId}/mandate/initiate | SUPER_ADMIN, ADMIN | Iniciar redirect flow → retorna URL d'autorització |
| GET | /tenants/{tenantId}/mandate/complete | Públic | Completar redirect flow (callback de GoCardless) |
| GET | /tenants/{tenantId}/mandate | SUPER_ADMIN, ADMIN | Veure estat del mandat |
| DELETE | /tenants/{tenantId}/mandate | SUPER_ADMIN | Cancel·lar mandat |
| GET | /tenants/{tenantId}/payments | SUPER_ADMIN, ADMIN | Llistar pagaments GoCardless del tenant |
| POST | /webhook | Públic (signat) | Webhook GoCardless |

### Endpoint addicional a `/api/v1/payments`:

| Mètode | Ruta | Rols | Descripció |
|--------|------|------|-----------|
| GET | /api/v1/payments/tenants/{tenantId}/providers | SUPER_ADMIN, ADMIN | Resum de proveïdors actius (setup + recurrent) |

### Resposta `GET /providers`:
```json
{
  "tenantId": "uuid",
  "setup": {
    "activeProvider": "STRIPE",
    "stripeConfigured": true,
    "stripeActive": true
  },
  "recurring": {
    "activeProvider": "GOCARDLESS",
    "sepaMandateActive": false,
    "goCardlessMandateActive": true,
    "goCardlessMandateStatus": "ACTIVE"
  }
}
```

---

## 5. Flux d'autorització del mandat (Redirect Flow)

```
ADMIN inicia: POST /gocardless/tenants/{id}/mandate/initiate
  → GoCardlessClient.createRedirectFlow(successReturnUrl)
  → Retorna { redirectUrl: "https://pay.gocardless.com/obauth/..." }

ADMIN envia l'URL al client (email, WhatsApp)

Client autoritza el seu IBAN a GoCardless

GoCardless redirigeix a: GET /gocardless/tenants/{id}/mandate/complete?redirect_flow_id=RE123
  → GoCardlessClient.completeRedirectFlow(redirectFlowId)
  → Retorna mandate ID, compte bancari, nom del titular
  → Guarda GoCardlessMandate amb status=ACTIVE

A partir d'aquí, el MonthlyBillingJob usa GoCardless per cobrar aquest tenant
```

---

## 6. Flux de cobrament mensual (MonthlyBillingJob actualitzat)

```
Per cada tenant amb MonthlyInvoice generada:

1. Comprovar proveïdor actiu:
   - Si GoCardlessMandate.status = ACTIVE → usar GoCardless
   - Sinó si SepaMandate.isActive = true → marcar per SEPA manual (pain.008)
   - Sinó → deixar com PENDING (factura per transferència)

2. Si GoCardless:
   GoCardlessClient.createPayment(mandateId, amount, chargeDate, description)
   → GoCardlessPayment creat (status=PENDING_SUBMISSION)
   → MonthlyInvoice.sepaCollected = false (s'actualitza via webhook)

3. Si SEPA manual:
   → MonthlyInvoice.sepaCollectionDate = dia 5 del mes
   → S'inclou al fitxer pain.008 generat manualment
```

---

## 7. Webhooks GoCardless

Events a escoltar:

| Event | Acció |
|-------|-------|
| `payments.paid_out` | GoCardlessPayment → PAID_OUT; MonthlyInvoice → PAID |
| `payments.failed` | GoCardlessPayment → FAILED; notificar SUPER_ADMIN |
| `payments.cancelled` | GoCardlessPayment → CANCELLED |
| `mandates.cancelled` | GoCardlessMandate → CANCELLED; notificar SUPER_ADMIN |
| `mandates.expired` | GoCardlessMandate → EXPIRED |

Verificació: HMAC-SHA256 amb `webhookSecret`.

---

## 8. Configuració

```yaml
app:
  gocardless:
    provider: mock        # mock | live
    api-url: https://api.gocardless.com
    sandbox-url: https://api-sandbox.gocardless.com
    webhook-secret: ${GOCARDLESS_WEBHOOK_SECRET}
```

---

## 9. Tests

| # | Cas | Resultat |
|---|-----|---------|
| 1 | Configurar GoCardless | 201 |
| 2 | Iniciar redirect flow | 200, redirectUrl no null |
| 3 | Completar redirect flow (mock) | 200, mandate ACTIVE |
| 4 | Veure mandat actiu | 200 |
| 5 | Cancel·lar mandat | 204 |
| 6 | Llistar pagaments | 200 |
| 7 | Webhook paid_out | 200, invoice PAID |
| 8 | Webhook failed | 200, payment FAILED |
| 9 | GET /providers amb tot configurat | 200, activeProvider correcte |
| 10 | Sense JWT → 401 | 401 |

---

## 10. Pendents

- [ ] Implementar `GoCardlessRealClient` complet (stubs ara)
- [ ] Frontend: pestanya "Pagaments" al Tenant Detail amb selector de proveïdor
- [ ] Notificació SUPER_ADMIN quan un pagament GoCardless falla (Telegram bot)
