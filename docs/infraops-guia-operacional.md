# Guia Operacional — Sistema d'Alertes i Recomanacions InfraOps

> **Versió:** 1.0 — Maig 2026
> **Audiència:** Antoni (SUPER_ADMIN) — operador del sistema

---

## Què és aquest sistema?

Dos sistemes complementaris vigilen el servidor en tot moment:

| Sistema | Qui l'executa | Què detecta | Com avisa |
|---------|--------------|-------------|-----------|
| **Netdata** | Docker, continu | Problemes immediats (CPU espic, disc ple, contenidor caigut) | Telegram instantani |
| **InfraOps** (Mòdul 19) | Spring Boot, cada 5 min | Tendències sostingudes → recomanacions d'escalat | Telegram + Dashboard admin |

**Netdata** = bombers (apaga incendis)
**InfraOps** = arquitecte (planifica el creixement)

---

## Com funciona InfraOps pas a pas

### 1. Recol·lecció (cada 5 minuts)

El backend Spring Boot mesura automàticament:

```
CPU del sistema        → via JVM OperatingSystemMXBean
RAM física usada       → via JVM OperatingSystemMXBean
Espai de disc (/)      → via java.io.File
Connexions DB actives  → via HikariCP pool stats
Nombre de tenants      → via TenantRepository.count()
```

Cada mesura es guarda a la taula `infra_metric_snapshots`. Els snapshots s'eliminen automàticament als 7 dies.

### 2. Avaluació de thresholds

Cada mesura s'avalua contra thresholds **sostinguts** (no instantanis):

| Mètrica | Warning | Critical | Durada necessària |
|---------|---------|----------|-------------------|
| CPU | >80% | >95% | 30 minuts (6 snapshots consecutius) |
| RAM | >85% | >95% | 15 minuts (3 snapshots consecutius) |
| Disc | >75% | >90% | Immediat |
| Connexions DB | >80% del màxim | >95% | 15 minuts |
| Tenants actius | >30 | >50 | Immediat |

**Per què sostingut?** Un pic de CPU d'1 minut és normal. Una CPU alta durant 30 minuts és un problema real.

### 3. Cooldown (evita spam)

Si ja s'ha enviat una recomanació del mateix tipus en les últimes **24 hores**, no s'envia una altra. La recomanació queda activa al dashboard fins que la marquis com resolta.

### 4. Resolució automàtica

Quan la mètrica torna a estar per sota del threshold, la recomanació es marca com a resolta automàticament.

---

## Missatges que rebràs per Telegram

### CPU alta (>80% durant 30 min)
```
🔴 Recomanació: UPGRADE_CPU

CPU alta de forma sostinguda durant 30 min: 83.2% (límit: 80%)

💡 Considera ampliar a 8 vCPU (Hetzner CX32, ~13€/mes)

📊 Context: 12 tenants actius
⏰ Pròxima alerta en 24h
```
**Acció:** Ves a Hetzner Console → Servers → Resize. O executa `./infra/scripts/upgrade-server.sh --server-name=amg-app --new-type=cx32`

---

### RAM alta (>85% durant 15 min)
```
🔴 Recomanació: UPGRADE_RAM

RAM alta de forma sostinguda durant 15 min: 87.3% (límit: 85%)

💡 Considera ampliar a 16 GB (CX32→CX42, +10€/mes)
   o separar la base de dades a un VPS dedicat (~5€/mes)
```
**Acció (opció econòmica):** Separar DB amb `./infra/scripts/migrate-db-to-vps.sh`
**Acció (opció simple):** Ampliar servidor amb `./infra/scripts/upgrade-server.sh --new-type=cx42`

---

### Disc quasi ple (>75%)
```
🔴 Recomanació: UPGRADE_DISK

Disc quasi ple: 76.4% (límit: 75%)

💡 Allibera espai o amplia el volum a Hetzner
```
**Acció immediata:**
```bash
# Comprova què ocupa espai
ssh root@VPS_IP 'docker system df'
ssh root@VPS_IP 'docker system prune -af'  # elimina imatges no usades

# Si no és suficient → ampliar volum a Hetzner Console
# Volumes → Resize (sense downtime)
```

---

### Connexions DB altes (>80% del pool durant 15 min)
```
🔴 Recomanació: SEPARATE_DB

Connexions DB altes: 82.0% del pool (límit: 80%)

💡 Considera separar PostgreSQL a un VPS dedicat (Hetzner CX22, ~5€/mes)
```
**Acció alternativa a curt termini:** Augmentar el pool HikariCP (`spring.datasource.hikari.maximum-pool-size=20`) a l'`application.yml`.
**Acció estructural:** `./infra/scripts/migrate-db-to-vps.sh`

---

### Tenants >30: Separa n8n
```
💡 Recomanació: SEPARATE_N8N

32 tenants actius (límit recomanat: 30)

Consulta separar n8n a un VPS dedicat per alliberar recursos
```
**Acció:** `./infra/scripts/migrate-n8n-to-vps.sh`

---

### Tenants >50: Migra a Hetzner Cloud
```
💡 Recomanació: MIGRATE_HETZNER_CLOUD

52 tenants actius (límit recomanat: 50)

És hora de migrar a Hetzner Cloud amb Load Balancer i DB gestionada
```
**Acció:** Parla amb Antoni (implementar Terraform complet amb `architecture = "full"`).

---

## Dashboard Admin

Pots veure l'estat en temps real a:
```
GET /api/v1/infraops/status          → estat actual del servidor
GET /api/v1/infraops/metrics?hours=24 → historial últimes 24h
GET /api/v1/infraops/recommendations  → recomanacions actives
POST /api/v1/infraops/collect         → forçar recol·lecció manual
POST /api/v1/infraops/recommendations/{id}/resolve → marcar com resolta
```

**Exemple de resposta `/status`:**
```json
{
  "collectedAt": "2026-05-16T08:00:00Z",
  "cpu":      { "percent": 42.3, "status": "OK" },
  "ram":      { "usedMb": 3200, "totalMb": 8192, "percent": 39.1, "status": "OK" },
  "disk":     { "usedGb": 28, "totalGb": 80, "percent": 35.0, "status": "OK" },
  "database": { "activeConnections": 4, "maxConnections": 10, "percent": 40.0, "status": "OK" },
  "tenants":  { "active": 8, "recommendation": null },
  "overallStatus": "OK",
  "activeRecommendations": 0
}
```

---

## Diferència entre Netdata i InfraOps

### Quan avisa Netdata:
- CPU espic >80% durant **2 minuts** (alerta puntual)
- RAM >85% durant **5 minuts**
- Disc >75% (immediat)
- Contenidor Docker aturat

### Quan avisa InfraOps:
- CPU >80% durant **30 minuts** (problema sostingut)
- RAM >85% durant **15 minuts**
- Tenants >30 o >50 (decisió de creixement)

→ **Pots rebre les dues**: Netdata avisa primer (situació urgent), InfraOps confirma si continua (cal actuar).

---

## Configuració Telegram (Netdata)

Al fitxer `infra/netdata/health_alarm_notify.conf`:
```
SEND_TELEGRAM="YES"
TELEGRAM_BOT_TOKEN="TOKEN_DEL_BOT"
TELEGRAM_CHAT_ID="ID_DEL_GRUP"
```

## Configuració Telegram (InfraOps)

A l'`.env` del servidor:
```
TELEGRAM_BOT_TOKEN=TOKEN_DEL_BOT
TELEGRAM_CHAT_ID=ID_DEL_GRUP
```

### Com obtenir TOKEN i CHAT_ID:
1. Obre Telegram → busca `@BotFather` → `/newbot` → segueix instruccions → copia el TOKEN
2. Afegeix el bot al teu grup de gestió AMG
3. Envia un missatge al grup, llavors:
   ```
   curl "https://api.telegram.org/bot{TOKEN}/getUpdates"
   ```
   Busca `"chat":{"id":...}` → aquest és el CHAT_ID

---

## Roadmap d'escalat recomanat

```
Ara (8 clients) — 1 VPS CX32 (4 vCPU / 8 GB / 80 GB) — ~13€/mes
  ✓ Tot en un servidor
  ✓ Netdata + InfraOps vigilant

15-20 clients — Ampliar a CX42 (8 vCPU / 16 GB) — ~26€/mes
  → upgrade-server.sh

25-30 clients — Separar n8n a CX22 dedicat — +5€/mes
  → migrate-n8n-to-vps.sh

40-50 clients — Separar DB a CX22 dedicat — +5€/mes
  → migrate-db-to-vps.sh

50+ clients — Hetzner Cloud complet + Managed DB — ~60-80€/mes
  → terraform apply -var='architecture=full'
```
