# Mòdul 19: InfraOps — Monitorització de Servidor i Recomanacions d'Escalat

> **Versió:** 1.0
> **Data:** 2026-05-16
> **Dependències:** Mòdul 11 (Ops & Health), Mòdul 01 (Auth)

---

## 1. Objectius

- Recollir mètriques del servidor (CPU, RAM, disc, connexions DB) cada 5 minuts
- Emmagatzemar historial de 7 dies per detectar tendències
- Generar **recomanacions d'escalat intel·ligents** via Telegram quan els thresholds se superen de forma sostinguda
- Evitar spam d'alertes amb cooldown configurable (24h per defecte)
- Exposar les mètriques i recomanacions al dashboard admin

---

## 2. Abast

### 2.1 Inclòs

- Recol·lecció de mètriques del sistema via JVM MXBeans (CPU, RAM física, disc)
- Mètriques de connexions DB via HikariCP
- Comptador de tenants actius per recomanació de separació n8n/DB
- Notificació via Telegram Bot amb missatge formatat
- API REST per al dashboard admin
- Job de neteja automàtic (elimina snapshots >7 dies)

### 2.2 Exclòs

- Integració directa amb Netdata API (Netdata gestiona les seves pròpies alertes)
- Monitorització de múltiples servidors (multiserver — futur Mòdul 19b)
- Auto-escalat automàtic sense confirmació humana

---

## 3. Model de dades

### 3.1 InfraMetricSnapshot

| Camp | Tipus | Descripció |
|------|-------|-----------|
| id | UUID | PK |
| collectedAt | Instant | Moment de la recol·lecció |
| cpuPercent | Double | % ús CPU del sistema |
| ramPercent | Double | % ús RAM física |
| diskPercent | Double | % ús disc arrel (/) |
| ramUsedMb | Long | RAM usada en MB |
| ramTotalMb | Long | RAM total en MB |
| diskUsedGb | Long | Disc usat en GB |
| diskTotalGb | Long | Disc total en GB |
| dbActiveConnections | Integer | Connexions actives HikariCP |
| dbMaxConnections | Integer | Pool màxim HikariCP |
| activeTenants | Integer | Tenants actius al sistema |

### 3.2 ScalingRecommendation

| Camp | Tipus | Descripció |
|------|-------|-----------|
| id | UUID | PK |
| type | Enum | `UPGRADE_CPU`, `UPGRADE_RAM`, `UPGRADE_DISK`, `SEPARATE_DB`, `SEPARATE_N8N`, `MIGRATE_HETZNER_CLOUD` |
| severity | Enum | `WARNING`, `CRITICAL` |
| message | String(500) | Missatge de recomanació detallat |
| triggerValue | Double | Valor que ha disparat la recomanació |
| threshold | Double | Threshold configurat |
| sentAt | Instant | Quan s'ha enviat la notificació |
| resolvedAt | Instant | Quan el valor ha tornat a ser normal |
| isActive | Boolean | Si la recomanació segueix vigent |
| createdAt | Instant | Auditing |

---

## 4. Thresholds i Regles

| Mètrica | Warning | Critical | Durada sostinguda | Recomanació |
|---------|---------|----------|-------------------|-------------|
| CPU | >80% | >95% | 30 min (6 snapshots) | `UPGRADE_CPU` → ampliar a CX31 |
| RAM | >85% | >95% | 15 min (3 snapshots) | `UPGRADE_RAM` → ampliar RAM o separar DB |
| Disc | >75% | >90% | Immediat | `UPGRADE_DISK` → alliberar o ampliar volum |
| DB connexions | >80% del max | >95% | 15 min (3 snapshots) | `SEPARATE_DB` → DB a VPS dedicat |
| Tenants actius | >30 | >50 | Immediat | `SEPARATE_N8N` (>30), `MIGRATE_HETZNER_CLOUD` (>50) |

**Cooldown:** 24 hores entre recomanacions del mateix `type`.

---

## 5. API REST

Prefix: `/api/v1/infraops`

| Mètode | Ruta | Rols | Descripció |
|--------|------|------|-----------|
| GET | /status | SUPER_ADMIN | Mètriques actuals del servidor |
| GET | /metrics | SUPER_ADMIN | Historial de snapshots (paginat, últimes 24h per defecte) |
| GET | /recommendations | SUPER_ADMIN | Recomanacions actives |
| GET | /recommendations/history | SUPER_ADMIN | Historial complet |
| POST | /recommendations/{id}/resolve | SUPER_ADMIN | Marcar recomanació com resolta |
| POST | /collect | SUPER_ADMIN | Forçar recol·lecció manual |

### Resposta `GET /status`:
```json
{
  "collectedAt": "2026-05-16T08:00:00Z",
  "cpu": { "percent": 42.3, "status": "OK" },
  "ram": { "usedMb": 3200, "totalMb": 8192, "percent": 39.1, "status": "OK" },
  "disk": { "usedGb": 28, "totalGb": 80, "percent": 35.0, "status": "OK" },
  "database": { "activeConnections": 4, "maxConnections": 10, "percent": 40.0, "status": "OK" },
  "tenants": { "active": 8, "recommendation": null },
  "overallStatus": "OK",
  "activeRecommendations": 0
}
```

---

## 6. Missatge Telegram de recomanació

```
🔴 AMG InfraOps — Recomanació d'escalat

Servidor: portal.amg.cat
Mètrica: RAM
Valor actual: 87.3% (durant 15 min)
Threshold: 85%

💡 Recomanació: UPGRADE_RAM
Considera ampliar la RAM del servidor a 16 GB (Hetzner CX31 → CX41, +10€/mes)
o separar la base de dades a un VPS dedicat (CX11, ~5€/mes)

📊 Context: 12 tenants actius, 4/10 connexions DB
⏰ Cooldown: pròxima alerta en 24h
```

---

## 7. Configuració

```yaml
app:
  infraops:
    enabled: true
    collect-interval-minutes: 5
    retention-days: 7
    notification-cooldown-hours: 24
    thresholds:
      cpu-warn: 80
      cpu-crit: 95
      ram-warn: 85
      ram-crit: 95
      disk-warn: 75
      disk-crit: 90
      db-connections-warn: 80
      tenants-n8n-warn: 30
      tenants-cloud-warn: 50
    telegram:
      bot-token: ${TELEGRAM_BOT_TOKEN:}
      chat-id: ${TELEGRAM_CHAT_ID:}
```

---

## 8. Tests

| # | Cas | Resultat |
|---|-----|---------|
| 1 | GET /status | 200, tots els camps presents |
| 2 | GET /metrics?hours=24 | 200, llista de snapshots |
| 3 | GET /recommendations | 200 |
| 4 | POST /collect | 200, snapshot creat |
| 5 | POST /recommendations/{id}/resolve | 200, resolvedAt != null |
| 6 | Sense JWT → 401 | 401 |
| 7 | Rol ADMIN → 403 | 403 |

---

## 9. Pendents

- [ ] Frontend: pestanya "Servidor" al Dashboard Admin amb gràfics de tendència
- [ ] Integració Netdata API per a dades més precises (alternativa als MXBeans)
- [ ] Mòdul 19b: gestió multi-servidor (quan hi hagi >1 VPS)
