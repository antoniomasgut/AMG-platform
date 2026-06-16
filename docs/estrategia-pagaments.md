# Estratègia de pagaments NexeLocal

## Resum de decisions

**Un sol proveïdor: Stripe**

Eliminem GoCardless i centralitzem tot en Stripe per simplicitat operacional. Stripe suporta Bizum, targeta i SEPA Direct Debit des del mateix dashboard.

---

## Tipus de pagament

### 1. Setup (pagament únic)
- **Mètode:** Stripe Checkout (targeta + Bizum)
- **Moment:** Quan el client accepta el pressupost
- **Flux:** Client accepta → resposta `PAYMENT_REQUIRED` + URL Stripe → client paga → webhook → fases contractades
- **Bizum:** Disponible automàticament a Espanya si activat al Stripe Dashboard (no requereix codi extra)

### 2. Mensual recurrent
- **Mètode:** Stripe SEPA Direct Debit (Subscription)
- **Moment:** Quan SUPER_ADMIN fa "Posar en marxa" (`POST /tenants/{id}/go-live`)
- **Flux:** Go-live → crear Stripe Subscription → Stripe cobra automàticament cada mes → webhooks gestionen incidències
- **Client:** Signa mandat SEPA una vegada; Stripe gestiona reintentos i emails de dunning

---

## Comparativa de costos (imports habituals NexeLocal)

| Mètode | Cost per 50€ | Cost per 100€ | Cost per 150€ |
|--------|-------------|--------------|--------------|
| **Stripe SEPA** | 0.18€ | 0.35€ | 0.53€ |
| GoCardless SEPA | 0.35€ | 0.50€ | 0.65€ |
| Stripe targeta (EU) | 0.95€ | 1.65€ | 2.35€ |
| Stripe Bizum | 0.95€ | 1.65€ | 2.35€ |

Stripe SEPA: 0.35% sense fee fix, cap a 6€ per transacció.

---

## Gestió d'impagament mensual

| Event Stripe | Acció backend |
|-------------|--------------|
| `invoice.payment_succeeded` | Confirmar pagament, crear factura Holded |
| `invoice.payment_failed` | `POST /tenants/{id}/suspend` → agent deixa de respondre |
| `customer.subscription.deleted` | Suspendre + notificar AMG via Telegram |

Stripe fa 3 intents automàtics amb intervals creixents (dunning). Si tots fallen, s'activa el webhook de fallada final.

---

## Cicle de vida d'un tenant

```
1. Pressupost acceptat → Stripe Checkout (setup únic, targeta/Bizum)
2. Webhook payment_succeeded → contractedPhases actualitzat
3. AMG implementa el servei (fitxa intake + configuració)
4. SUPER_ADMIN → POST /tenants/{id}/go-live → activePhases = contractedPhases
   → Agent comença a respondre
   → Crea Stripe Subscription (mensual SEPA)
5. Cada mes → Stripe cobra automàticament
   → Si falla → agent suspès fins regularitzar
6. Client regularitza → go-live de nou
```

---

## Lliurament post-activació

Depenent de la complexitat del projecte, AMG determina el tipus d'acompanyament:

| Complexitat | Acció recomanada |
|-------------|-----------------|
| Micro-landing + F1 bàsic | Email amb instruccions d'ús |
| F2–F3 (agenda/pressupostos) | Trucada de 30 min |
| F4–F5 o projecte complex | Sessió de formació presencial/Zoom |

La modalitat de lliurament es pot capturar a la fitxa d'intake (`deliveryMode`) i, si cal formació, agendar-la com a cita al sistema. Es pot preguntar a l'entrevista de qualificació per preparar-ho amb antelació.

---

## Pendent d'implementar

- [ ] Stripe SEPA Subscription: `createSubscription()` a `StripeClient` + `StripeRealClient`
- [ ] Camp `stripeSubscriptionId` a `StripeConfig`
- [ ] Cridar `createSubscription()` des de `POST /tenants/{id}/go-live`
- [ ] Webhooks: `invoice.payment_failed` → suspend, `invoice.payment_succeeded` → go-live
- [ ] Camp `deliveryMode` a `BudgetSetupIntake` (EMAIL / CALL / TRAINING)
- [ ] Camp `trainingScheduledAt` per sessions de formació
