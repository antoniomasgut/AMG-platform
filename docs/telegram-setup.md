# Guia: Configuració de Telegram per a AMG Digitalitzacions

Aquesta guia cobreix dos escenaris:

- **Part A** — Configurar el bot d'administració intern d'AMG (xat de vendes/infraops)
- **Part B** — Connectar un bot de Telegram al negoci d'un client

---

## Part A — Bot d'administració AMG

El bot admin permet al equip d'AMG consultar estadístiques, pipeline de vendes, leads i pressupostos directament des de Telegram, i fer preguntes en llenguatge natural a l'agent IA.

### A.1 — Crear el bot amb BotFather

1. Obre Telegram (mòbil o escriptori) i busca **@BotFather**
2. Envia `/newbot`
3. Posa-li un nom visible (ex: `AMG Admin`)
4. Posa-li un username que acabi en `bot` (ex: `amgdl_admin_bot`)
5. BotFather et donarà un **token** amb el format `123456789:ABCdef...` — guarda'l

### A.2 — Obtenir el teu Telegram Chat ID

1. Busca **@userinfobot** a Telegram
2. Envia-li qualsevol missatge
3. Et respondrà amb el teu **Id** numèric (ex: `123456789`) — apunta'l

### A.3 — Configurar les variables al portal

Entra al portal com a SUPER_ADMIN → **Configuració del sistema** i omple:

| Clau | Valor | Descripció |
|------|-------|------------|
| `TELEGRAM_BOT_TOKEN` | `123456789:ABCdef...` | Token de BotFather |
| `TELEGRAM_CHAT_ID` | el teu chat ID | Xat d'InfraOps/alertes |
| `AMG_SALES_CHAT_ID` | el teu chat ID | Xat de vendes (pot ser el mateix) |
| `TELEGRAM_WEBHOOK_SECRET` | string aleatori segur | Ex: `amg2026xsecret` |

> Si el xat d'alertes de sistema i el xat de vendes és el mateix, posa el mateix ID als dos camps.

### A.4 — Registrar el webhook

Des d'un terminal, executa:

```bash
curl "https://api.telegram.org/bot<TOKEN>/setWebhook" \
  -d "url=https://api.amgdl.com/api/v1/agents/telegram/webhook" \
  -d "secret_token=<TELEGRAM_WEBHOOK_SECRET>"
```

Resposta esperada: `{"ok":true,"description":"Webhook was set"}`

> **Problema comú:** Si reps `409 Conflict: can't use getUpdates while webhook is active`, vol dir que el webhook ja estava registrat — és correcte, no cal fer res més.

### A.5 — Verificar que funciona

Obre el bot a Telegram i envia:

```
/ajuda
```

Hauries de rebre la llista de comandes disponibles. Si respon, tot funciona.

---

### Comandes disponibles

| Comanda | Funció |
|---------|--------|
| `/stats` | Resum general: leads, pressupostos, clients actius |
| `/pipeline` | Kanban de vendes per etapes |
| `/nous` | Leads en estat Nou i Contactat |
| `/leads` | Últims 10 leads |
| `/pressupostos` | Pressupostos enviats pendents de resposta |
| `/tenants` | Clients actius amb les seves fases |
| `/ajuda` | Llista de comandes |
| *(text lliure)* | Respon l'agent IA en llenguatge natural |

---

## Part B — Bot de Telegram per a un client

Cada client pot tenir el seu propi bot de Telegram connectat al seu agent d'IA. El bot respon als missatges del client (cites, pressupostos, preguntes, etc.) usant la base de coneixement del negoci.

### B.1 — El client crea el seu bot

El client (o AMG en nom seu) fa els mateixos passos que A.1:

1. Obre Telegram → busca **@BotFather** → `/newbot`
2. Nom visible: el nom del negoci (ex: `Perruqueria Maria`)
3. Username: ex: `perruqueria_maria_bot`
4. Guarda el **token** que dóna BotFather

### B.2 — Connectar el bot al portal

Dues opcions:

**Opció 1 — Des del portal (recomanat)**

Entra com a SUPER_ADMIN → **Tenants** → selecciona el client → **Telegram** → **Connectar bot** → entra el token.

El sistema automàticament:
- Valida que el token és correcte
- Registra el webhook (`/api/v1/agents/telegram/webhook/{tenantId}`)
- Guarda el token xifrat amb AES-256

**Opció 2 — Via API**

```bash
curl -X POST "https://api.amgdl.com/api/v1/telegram/tenants/{tenantId}/connect" \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -H "Content-Type: application/json" \
  -d '{"botToken": "123456789:ABCdef..."}'
```

### B.3 — Vincular el mòbil del client al bot

Per a que el client pugui rebre notificacions i enviar missatges al seu agent:

1. El client busca el seu bot a Telegram per l'username
2. Envia `/start <codi>` — el codi el genera el portal a la secció de configuració de Telegram del tenant
3. El sistema vincula automàticament el chat ID del client al seu tenant

A partir d'aquí, el client pot:
- Rebre notificacions de cites, leads nous i pressupostos
- Enviar `/absencia [data]` per gestionar absències
- Enviar `/pressupost email nom` per generar un pressupost
- Escriure text lliure i l'agent IA li respondrà

### B.4 — Verificar la connexió

Des del portal → **Tenants** → client → **Telegram**: ha de mostrar l'estat **CONNECTED** i el username del bot (ex: `@perruqueria_maria_bot`).

Si l'estat és ERROR:
1. Verifica que el token és correcte (regenera'l a BotFather si cal: `/revoke`)
2. Fes clic a **Verificar** al portal — torna a intentar registrar el webhook
3. Assegura't que el domini `api.amgdl.com` és accessible públicament (HTTPS)

---

## Resolució de problemes

| Problema | Causa probable | Solució |
|----------|---------------|---------|
| Bot no respon | Webhook no registrat | Torna a fer el pas A.4 o B.2 |
| `409 Conflict` a getUpdates | Webhook actiu | Normal — usa `@userinfobot` per al chat ID |
| `401 Unauthorized` al webhook | Secret incorrecte | Comprova `TELEGRAM_WEBHOOK_SECRET` al portal i al curl |
| Estat ERROR al portal | Token invàlid | Regenera el token a BotFather amb `/revoke` |
| Bot respon però no reconeix l'usuari | Chat ID no configurat | Revisa `AMG_SALES_CHAT_ID` / `TELEGRAM_CHAT_ID` |
| Missatges de clients no arriben | Webhook de tenant incorrecte | Fes **Verificar** des del portal del tenant |

---

## Arquitectura resumida

```
Telegram ──► POST /api/v1/agents/telegram/webhook
                │
                ├─ isAdminChat? ──► AmgAdminCommandService (comandes + IA)
                │
                └─ tenant vinculat? ──► ConversationalAgentService (agent client)

Telegram ──► POST /api/v1/agents/telegram/webhook/{tenantId}
                │
                └─ ConversationalAgentService (missatges directes al tenant)
```

---

*Darrera actualització: juny 2026*
