# Guia de Posada en Marxa — Primer VPS i Domini

> **Versió:** 1.0 — Maig 2026
> **Destinatari:** Antoni — primer desplegament real

---

## Resum de costos inicials

| Concepte | Cost |
|----------|------|
| VPS Hetzner CX22 (primer mes) | ~4,5€ |
| Domini .cat (primer any) | ~15€ |
| **Total inicial** | **~20€** |

A partir del segon mes: **~4,5€/mes** (VPS) + **~1,25€/mes** (domini prorratejat).

---

## Pas 1 — Compra el domini

### On comprar
**Gandi.net** — recomanat per `.cat`

1. Ves a [gandi.net](https://www.gandi.net)
2. Busca el domini que vols (exemples: `amgdigitalitzacio.cat`, `amg-digital.cat`)
3. Afegeix al carret → paga (~15€/any)
4. **Important:** activa la protecció WHOIS (gratuïta a Gandi) per no exposar el teu email

> El `.cat` triga fins a 24h en activar-se. Compra'l primer i mentre esperes, configura el VPS.

### Alternatives si `.cat` no està disponible
| Domini | Preu Gandi | Notes |
|--------|-----------|-------|
| `.es` | ~8€/any | Més barat, neutre |
| `.com` | ~12€/any | Si vols escalar fora |

---

## Pas 2 — Crea el VPS a Hetzner

### Registre
1. Ves a [hetzner.com/cloud](https://www.hetzner.com/cloud) → **Get started**
2. Crea compte amb el teu email → verifica
3. Afegeix mètode de pagament (targeta o PayPal)

### Crea el servidor
1. **Cloud** → **+ Create Server**
2. Configuració:
   - **Location:** Nuremberg (`nbg1`) — Europa central, bona latència des de Mallorca
   - **Image:** Ubuntu 24.04 LTS
   - **Type:** Shared vCPU → **CX22** (2 vCPU / 4 GB / 40 GB SSD) → **~4,51€/mes**
   - **SSH Key:** afegeix la teva clau pública (`~/.ssh/id_rsa.pub`)
   - **Name:** `amg-app`
3. Clica **Create & Buy Now**

> En 30 segons tens el servidor amb IP pública.

### Quan escalar
| Clients | Servidor | Cost |
|---------|----------|------|
| 0–15 | CX22 (2 vCPU / 4 GB) | ~4,5€/mes |
| 15–30 | CX32 (4 vCPU / 8 GB) | ~8,5€/mes |
| 30–50 | CX42 (8 vCPU / 16 GB) | ~17€/mes |
| 50+ | CX42 + DB dedicada | ~22€/mes |

Escalar és un clic a Hetzner Console (o amb l'script `upgrade-server.sh`). ~2 min de downtime.

---

## Pas 3 — Apunta el DNS al VPS

A Gandi (o el teu registrador), crea aquests registres DNS:

| Tipus | Nom | Valor | TTL |
|-------|-----|-------|-----|
| A | `@` | IP_DEL_VPS | 300 |
| A | `api` | IP_DEL_VPS | 300 |
| A | `monitor` | IP_DEL_VPS | 300 |
| A | `n8n` | IP_DEL_VPS | 300 |
| A | `storage` | IP_DEL_VPS | 300 |
| A | `minio` | IP_DEL_VPS | 300 |
| CNAME | `www` | `@` | 300 |

> DNS pot trigar entre 5 minuts i 2 hores en propagar-se (TTL 300 = 5 min).

---

## Pas 4 — Configura el servidor

Connecta via SSH:
```bash
ssh root@IP_DEL_VPS
```

Instal·la Docker:
```bash
apt-get update && apt-get install -y docker.io docker-compose-plugin git curl
systemctl enable docker && systemctl start docker
```

Clona el repositori (o puja els fitxers):
```bash
git clone https://github.com/el-teu-repo/portalAMGv3.git /opt/amg
cd /opt/amg/infra
```

---

## Pas 5 — Configura les variables d'entorn

Crea el fitxer `.env` a partir de l'exemple:
```bash
cp .env .env.production
nano .env.production
```

Variables **obligatòries** per arrencar:

```env
APP_DOMAIN=el-teu-domini.cat

POSTGRES_DB=amg_platform
POSTGRES_USER=amg
POSTGRES_PASSWORD=contrasenya-segura-aqui      # min 20 caràcters

REDIS_PASSWORD=contrasenya-redis-segura

MINIO_ROOT_USER=amg_admin
MINIO_ROOT_PASSWORD=contrasenya-minio-segura

JWT_SECRET=clau-jwt-min-64-caracters-aleatoria-aqui-generada-amb-openssl

TRAEFIK_AUTH=admin:hash-generat               # veure nota baix

JASYPT_ENCRYPTOR_PASSWORD=clau-xifrat-vault
```

**Generar contrasenyes segures:**
```bash
# JWT Secret (64+ caràcters)
openssl rand -base64 64

# Contrasenya BD
openssl rand -base64 24

# Traefik Basic Auth hash
apt-get install -y apache2-utils
htpasswd -nb admin la-teva-contrasenya
# → copia el resultat a TRAEFIK_AUTH (els $ doblen-se: $$ a l'env)
```

Variables **opcionals** (per a funcionalitats específiques):
```env
# Telegram (alertes InfraOps + Netdata)
TELEGRAM_BOT_TOKEN=
TELEGRAM_CHAT_ID=

# Holded (FinOps)
FINOP_PROVIDER=mock          # canvia a "live" quan tinguis API Key

# Stripe (Payments)
PAYMENTS_PROVIDER=mock       # canvia a "live" quan tinguis API Key

# GoCardless (SEPA)
GOCARDLESS_PROVIDER=mock     # canvia a "live" quan tinguis compte

# Google Cloud Storage (Backups)
GCS_BUCKET_NAME=
GCS_PROJECT_ID=
```

---

## Pas 6 — Arrenca l'aplicació

```bash
cd /opt/amg/infra
docker compose --env-file .env.production up -d

# Comprova que tots els contenidors estan UP
docker compose ps
```

Hauries de veure tots en estat `running`:
- `amg_traefik`
- `amg_postgres`
- `amg_redis`
- `amg_minio`
- `amg_backend`
- `amg_frontend`
- `amg_netdata`

---

## Pas 6b — Actualitzar backend/frontend (desplegament incremental)

Quan hagis construït noves imatges (`amg-backend:latest`, `infra-frontend:latest`),
usa el compose de desplegament per a un reinici net sense processos orfes:

```bash
# Primer de tot: assegura't que el JWT_SECRET està guardat al servidor
mkdir -p /opt/amg/secrets
openssl rand -base64 48 > /opt/amg/secrets/jwt_secret
chmod 600 /opt/amg/secrets/jwt_secret

# Per desplegar:
export JWT_SECRET=$(cat /opt/amg/secrets/jwt_secret)

docker compose -f infra/docker-compose.deploy.yml down
docker compose -f infra/docker-compose.deploy.yml up -d
```

**Avantatges:**
- `docker compose down` → atura i elimina backend + frontend (zero orfes)
- `docker compose up -d` → crea contenidors nous amb les darreres imatges
- Usa les xarxes `coolify` i `amg-network` (externes) per no tocar Postgres/Redis/Traefik
- El `JWT_SECRET` es llegeix del fitxer `/opt/amg/secrets/jwt_secret` via variable d'entorn

> ⚠️ **Nota:** Si canvies d'IP o recrees el servidor, hauràs de tornar a generar
> el `jwt_secret` i reiniciar el backend. Tots els tokens JWT existents quedaran
> invalidats.

---

## Pas 7 — Configura Telegram per a alertes

### Crear el Bot
1. Telegram → busca `@BotFather` → `/newbot`
2. Posa nom: `AMG Ops Bot`
3. Posa username: `amg_ops_bot` (ha de ser únic, afegeix números si cal)
4. Copia el **TOKEN** que et dóna

### Obtenir el Chat ID
1. Crea un grup Telegram "AMG Gestió" (o usa un existent)
2. Afegeix el bot al grup
3. Envia qualsevol missatge al grup
4. Executa:
   ```bash
   curl "https://api.telegram.org/bot{TOKEN}/getUpdates"
   ```
5. Busca `"chat":{"id":-XXXXXXXXX}` → aquest és el **CHAT_ID** (negatiu per a grups)

### Aplica la configuració
```bash
# Edita .env.production
TELEGRAM_BOT_TOKEN=el-token-del-bot
TELEGRAM_CHAT_ID=-el-chat-id-del-grup

# Edita infra/netdata/health_alarm_notify.conf
# TELEGRAM_BOT_TOKEN="el-token-del-bot"
# TELEGRAM_CHAT_ID="-el-chat-id-del-grup"

# Reinicia
docker compose --env-file .env.production up -d netdata backend
```

---

## Pas 8 — Verifica que tot funciona

```bash
# 1. Frontend accessible
curl -I https://el-teu-domini.cat

# 2. Backend health
curl https://api.el-teu-domini.cat/api/v1/ops/health

# 3. InfraOps (requereix JWT SUPER_ADMIN)
curl -H "Authorization: Bearer TOKEN" https://api.el-teu-domini.cat/api/v1/infraops/status

# 4. Netdata dashboard
# Obre al navegador: https://monitor.el-teu-domini.cat
# User: admin / Password: la que has configurat a TRAEFIK_AUTH
```

---

## Checklist final pre-live

- [ ] Domini actiu i DNS propagat (comprova amb `nslookup el-teu-domini.cat`)
- [ ] SSL actiu (Traefik + Let's Encrypt automàtic)
- [ ] Tots els contenidors Docker en estat `running`
- [ ] Backend health retorna `{"status":"UP"}`
- [ ] Frontend carrega sense errors
- [ ] Login com a SUPER_ADMIN funciona
- [ ] Swagger UI accessible a `https://api.el-teu-domini.cat/swagger-ui.html`
- [ ] Sentry DSN configurat (backend `SENTRY_DSN`, frontend `NEXT_PUBLIC_SENTRY_DSN`)
- [ ] Telegram bot envia missatge de prova
- [ ] Netdata accessible a `monitor.el-teu-domini.cat`
- [ ] Backup manual executat i verificat (`POST /api/v1/ops/backups`)

---

## Problemes habituals

| Problema | Causa probable | Solució |
|----------|---------------|---------|
| SSL no funciona | DNS no propagat | Espera 10-15 min, comprova amb `nslookup` |
| Backend no arrenca | Variables `.env` incorrectes | `docker logs amg_backend` |
| Frontend 502 | Backend no llest | `docker compose restart frontend` |
| Postgres error autenticació | Password amb caràcters especials | Posa el password entre cometes al `.env` |
| Netdata no apareix | Traefik no detecta el contenidor | `docker compose restart traefik` |
