# Mòdul 53: F3 Cobrament Online — Stripe del tenant a l'acceptació de documents

> **Versió:** 1.0
> **Data:** 2026-07-03
> **Dependències:** Mòdul 37 (Document Builder), Mòdul 44 (Secure Delivery), Mòdul 09 (Payments — SDK Stripe), Mòdul 28 (NexeServiceConfigs)

---

## 1. Objectiu

Fer que la fase F3 ("Pressupostos i **Cobraments**") pugui cobrar de veritat: quan el client
final d'un tenant accepta un pressupost, opcionalment se'l redirigeix a un Stripe Checkout
**del compte del tenant** per pagar el total o una paga i senyal.

**Principis:**
- **Opcional i per tenant** — un pintor amb pressupostos de 8.000€ no ho vol; un fisio amb bons sí
- **Diners directes al tenant** — s'usa la clau Stripe del tenant (vault); AMG mai toca els fons
- **El pagament mai bloqueja l'acceptació** — si Stripe falla o no està configurat, l'acceptació
  es completa igual i el cobrament queda fora del sistema (comportament actual)
- **Sense webhooks per tenant** — verificació de la sessió al retorn del checkout (redirect),
  consultant l'API de Stripe amb la clau del tenant. Evita configurar webhooks a N comptes.

---

## 2. Configuració per tenant

Clau `PRESSUPOSTOS` de `nexe_service_configs` — camps nous al JSON:

| Camp | Valors | Default |
|------|--------|---------|
| `online_payment_mode` | `OFF` \| `FULL` \| `DEPOSIT` | `OFF` |
| `deposit_percent` | 5–90 | `30` |

Requisit addicional per operar: `stripe_configs` del tenant amb `api_key_ref` (clau secreta
xifrada amb `VaultEncryption`, mateix patró que GoCardless) i `is_active = true`.
El `PhaseHealthService.checkF3` ja ho vigila.

---

## 3. Model de dades — migració V88

```sql
ALTER TABLE secure_document_tokens
  ADD COLUMN payment_status     VARCHAR(20),      -- NULL/NONE | PENDING | PAID
  ADD COLUMN payment_session_id VARCHAR(120),     -- id de la Checkout Session
  ADD COLUMN payment_amount     NUMERIC(10,2),    -- import cobrat (total o paga i senyal)
  ADD COLUMN payment_paid_at    TIMESTAMPTZ;
```

El pagament es traça al **token de lliurament** (no al `GeneratedDocument`): és on viu el
cicle de vida del lliurament i l'acceptació.

---

## 4. Flux

```
Client accepta el document (POST /api/v1/documents/view/{token}/accept)
      │
      ├─ mode OFF o sense Stripe actiu → flux actual (booking + notificació) — response sense paymentUrl
      │
      └─ mode FULL/DEPOSIT + StripeConfig actiu + total > 0:
            1. import = total (FULL) o total × deposit_percent/100 (DEPOSIT), arrodonit a 2 decimals
            2. Checkout Session amb RequestOptions.apiKey = clau del tenant (desxifrada del vault)
               - line item: "Pressupost {number} — {nom del negoci}" / "Paga i senyal (N%) — ..."
               - metadata: docTokenId, tenantId
               - success_url: {API}/api/v1/documents/view/{token}/payment-return?session_id={CHECKOUT_SESSION_ID}
               - cancel_url:  {APP}/d/{token}
            3. token.paymentStatus = PENDING, paymentSessionId, paymentAmount
            4. response.paymentUrl = session.url → el frontend hi redirigeix
      │
      ▼
GET /payment-return (públic): consulta la sessió amb la clau del tenant
      ├─ payment_status == "paid" → token.paymentStatus = PAID + paidAt + notificació Telegram al tenant
      └─ redirect 302 → {APP}/d/{token}?paid=1 (o ?paid=0)
```

Errors de Stripe durant l'acceptació: log warn + acceptació completada sense pagament
(paymentStatus queda NULL). Mai 500 cap al client final.

---

## 5. Serveis

| Servei | Responsabilitat |
|--------|----------------|
| `TenantStripeCheckoutService` (payments.application) | `createCheckout(tenantId, amountEur, concepte, successUrl, cancelUrl)` i `isSessionPaid(tenantId, sessionId)` amb `RequestOptions` per clau de tenant — **mai toca `Stripe.apiKey` global** |
| `DocumentViewService` | Llegeix la config PRESSUPOSTOS, calcula l'import des del JSON `calculated.total` del `GeneratedDocument`, orquestra el checkout i el retorn |

---

## 6. API

| Mètode | Ruta | Auth | Canvi |
|--------|------|------|-------|
| POST | `/api/v1/documents/view/{token}/accept` | públic (token) | La resposta passa de buida a `{ paymentUrl: string \| null }` |
| GET | `/api/v1/documents/view/{token}/payment-return?session_id=` | públic (token) | **Nou** — verifica i redirigeix |

`SecurityConfig`: `/api/v1/documents/view/**` ja és públic (verificar; si no, afegir el retorn).

---

## 7. Frontend

1. **Config** (`/portal/admin/tenants/[id]/nexe/pressupostos`): selector de mode
   (Sense cobrament / Cobrar el total / Paga i senyal amb % configurable) + avís si el tenant
   no té Stripe configurat.
2. **Pàgina pública del document** (`/d/[token]`): si `accept` retorna `paymentUrl` → redirigir;
   al tornar amb `?paid=1` → mostrar confirmació de pagament.

---

## 8. Notificacions

- Pagament confirmat → Telegram del tenant (patró `notifyTenantAcceptance`):
  "💳 Pagament rebut — {import} € del pressupost {number} ({nom client})"
- El missatge d'acceptació existent indica si el pagament ha quedat pendent.

---

## 9. QA

| Cas | Esperat |
|-----|---------|
| Mode OFF | Acceptació idèntica a l'actual, `paymentUrl = null` |
| Mode FULL amb Stripe actiu, total 500€ | Checkout de 500€, PENDING; al retorn pagat → PAID |
| Mode DEPOSIT 30%, total 1.000€ | Checkout de 300€ |
| Stripe del tenant caigut/clau invàlida | Acceptació OK sense pagament, warn al log |
| `calculated.total` absent o 0 | Sense checkout |
| Retorn amb sessió no pagada | `?paid=0`, paymentStatus segueix PENDING |
| Doble acceptació | Bloquejada (comportament existent) |
| Retorn repetit d'una sessió pagada | Idempotent (PAID es manté, cap notificació duplicada) |
