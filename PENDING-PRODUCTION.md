# Tasques pendents de producció

**Actualitzat:** 2026-06-21
**Entorn:** `ssh root@65.108.148.62`

---

## 1. Configurar secrets de webhook al System Config

Tots 5 apareixen com `MISSING` a `/portal/admin/system-config`. Cal configurar-los amb els valors reals:

### 🔴 Crític — sense configurar, els webhooks retornen 503/406

| Clau | Categoria | Com obtenir-la |
|------|-----------|----------------|
| `STRIPE_WEBHOOK_SECRET` | PAYMENTS | Stripe Dashboard → Developers → Webhooks → Signing secret (`whsec_...`) |
| `GOCARDLESS_WEBHOOK_SECRET` | PAYMENTS | GoCardless Dashboard → Developers → Webhooks → Secret |
| `TELEGRAM_WEBHOOK_SECRET` | AGENTS | Token arbitrari (mínim 8 car.) que tu decideixes. **Veure pas 1b.** |
| `HOLDED_WEBHOOK_SECRET` | FINOPS | Holded → Configuració → Webhooks → Token |
| `MAILGUN_WEBHOOK_SIGNING_KEY` | EMAIL_INBOUND | Mailgun Dashboard → Sending → Webhooks → HTTP Webhook Signing Key |

**Pas 1b — Registrar TELEGRAM_WEBHOOK_SECRET a Telegram:**
Després de configurar el valor al System Config, cal registrar-lo al bot de Telegram:
```
curl "https://api.telegram.org/bot<TELEGRAM_BOT_TOKEN>/setWebhook?url=https://api.amgdl.com/api/v1/agents/telegram/webhook&secret_token=<TELEGRAM_WEBHOOK_SECRET>"
```

---

## 2. Deploy del codi de l'auditoria de seguretat (juny 2026)

La sessió del 2026-06-21 va corregir **31 vulnerabilitats** al codebase. Cal fer deploy:

```bash
# Al servidor de producció
cd /opt/amg && git pull && docker compose up -d --build backend
```

### Resum de les correccions aplicades

| Severitat | Àrea | Fix |
|-----------|------|-----|
| 🔴 Crític | Stripe webhook | 503 si `STRIPE_WEBHOOK_SECRET` no configurat |
| 🔴 Crític | Double-accept pressupost | Pessimistic lock a `acceptBudget()` |
| 🔴 Crític | Stripe fallback | Elimina `setupPaid=true` sense pagament real |
| 🟠 Alt | WhatsApp Meta webhook | Verificació HMAC-SHA256 (`X-Hub-Signature-256`) |
| 🟠 Alt | Twilio webhook | Verificació HMAC-SHA1 de Twilio |
| 🟠 Alt | Telegram webhook | Verificació `X-Telegram-Bot-Api-Secret-Token` |
| 🟠 Alt | Mailgun webhook | Verificació HMAC-SHA256 (timestamp + token) |
| 🟠 Alt | Holded webhook | Verificació `X-Webhook-Token` |
| 🟠 Alt | GoCardless webhook | 503 si secret no configurat |
| 🟠 Alt | IDOR users | ADMIN força `tenantId` propi a `listUsers` |
| 🟠 Alt | IDOR billing | Budget get comprova `tenantId` del path |
| 🟠 Alt | Swagger | Accés restringit a `SUPER_ADMIN`/`ADMIN` |
| 🟠 Alt | Password mínims | `@Size(min=8)` a login/create/reset |
| 🟡 Mitjà | GDPR retenció | `findByCreatedAtBefore` (no updatedAt) |
| 🟡 Mitjà | Budget número | `MAX` query en lloc de `count+1` (race condition) |
| 🟡 Mitjà | Follow-up URL | URL directa d'acceptació al recordatori |
| 🟡 Mitjà | Redis SCAN | Substitueix `KEYS*` bloquejant per SCAN iteratiu |
| 🟡 Mitjà | Forgot password | Invalida token anterior abans de crear-ne un de nou |
| 🟡 Mitjà | n8n injection | Valida `workflowId` amb regex `[a-zA-Z0-9_-]+` |
| 🟡 Mitjà | Descomptes | Comprova `maxApplications` i incrementa `appliedCount` |
| 🟡 Mitjà | SVG XSS | `Content-Disposition: attachment` per a SVGs |
| 🟡 Mitjà | OAuth CSRF | State token en Redis (TTL 10 min) per Google Calendar |
| 🟡 Mitjà | Prompt injection | Trunca missatges entrants a 4.000 caràcters |
| 🟡 Mitjà | Booking rate limit | 20 req/min per IP al `POST /{token}/confirm` |
| 🟡 Mitjà | Pipeline transicions | Bloqueja retrocessos WON/LOST i salts de >2 etapes |
| 🟡 Mitjà | Booking token dedup | Invalida tokens anteriors del mateix document |
| 🟡 Mitjà | Scheduler ordre | FollowupLog a BD abans de la clau Redis |
| 🟡 Mitjà | IP resolution | `X-Real-IP` → `X-Forwarded-For` → `getRemoteAddr()` |
| 🟡 Mitjà | @Transactional HTTP | Elimina `@Transactional(readOnly=true)` de `sendBudgetViaChannel` |
| 🟡 Mitjà | ensureIntake race | Re-check `findByBudgetId` abans de guardar |
| 🟢 Misc | PublicContact size | Límits `name≤100`, `email≤200`, `message≤2000` |

---

## 3. Sector MARE_DE_DIA — migració SQL

La migració `V70__mare_de_dia_sector_phases.sql` ja està al codebase. Flyway l'aplicarà automàticament al proper deploy. Si cal aplicar-la manualment:

```bash
docker exec amg-postgres psql -U amg -d amg -f /tmp/V70__mare_de_dia_sector_phases.sql
```

---

## 4. SystemConfig keys nous — registre a system_settings

Les 5 claus de webhook s'han afegit a `KNOWN_KEYS` al codi. Apareixeran automàticament a la UI de System Config després del deploy. **No cal cap migració SQL** — les definicions de claus són en codi, no a BD.

---

## Notes

- Totes les correccions de seguretat estan a la branca `main`.
- Compilació verificada neta després de cada fix.
- La configuració dels secrets és **independent del deploy** — cal fer-la des de la UI després que el servidor estigui en marxa.
