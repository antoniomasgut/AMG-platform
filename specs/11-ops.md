# Mòdul 11: Ops & Health — Monitorització i Alertes

> **Versió:** 1.0
> **Data:** 2026-05-13
> **Dependències:** Mòdul 01 (Auth), Mòdul 10 (Automations), Mòdul 18 (Backup — veure 2.2)

---

## 1. Objectius

- Monitoritzar l'estat de tots els serveis de la plataforma (backend, frontend, BD, Redis, n8n, MinIO, llocs web dels clients)
- Enviar alertes automàtiques via Telegram quan un servei cau
- Mantenir un historial d'incidències i recuperacions
- Dashboard d'operacions per a SUPER_ADMIN
- Facilitar la gestió de logs

---

## 2. Abast

### 2.1 Funcionalitats incloses

- **Health checks periòdics**: Cada 30s per a cada servei monitoritzat
- **Alertes Telegram**: Notificacions automàtiques amb cooldown de 30 min
- **Historial d'incidències**: Registre de caigudes i recuperacions
- **Dashboard Ops**: Visió global de l'estat de tots els serveis
- **Log viewer**: Accés als logs recents dels serveis (backend, n8n, Traefik)

### 2.2 Funcionalitats excloses

- **Gestió de backups**: delegada al Mòdul 18 (Backup Operacional) — backup de BD, dades operatives, i configuració a GCS
- Monitorització avançada d'APM (elastic APM, Datadog — es pot afegir després)
- Pàginament telefònic (SMS/trucada — Telegram és suficient)
- Auto-scaling (Hetzner + Coolify ho gestionen)
- Anàlisi de logs avançat (només visualització recent)

### 2.3 Actors

| Actor | Descripció | Permisos |
|-------|-----------|----------|
| SUPER_ADMIN | Opera tota la plataforma | Dashboard Ops, configurar alertes, backups, veure logs |
| ADMIN | Operador tècnic | Veure dashboard i estat de serveis |

---

## 3. Model de dades

### 3.1 Entitats (PostgreSQL)

#### ServiceHealth

Registre periòdic de l'estat d'un servei.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| serviceName | String(50) | @Column(nullable=false) | `backend`, `frontend`, `postgres`, `redis`, `n8n`, `minio`, `engine`, `traefik` |
| tenantId | UUID | @Column | Null per serveis globals, UUID per llocs web de clients |
| status | Enum(STRING) | @Enumerated | `UP`, `DOWN`, `DEGRADED` |
| responseTimeMs | Long | @Column | Temps de resposta en ms |
| errorMessage | String(500) | @Column | Missatge d'error si està DOWN |
| checkedAt | Instant | @Column(nullable=false) | Moment del check |

**Índex:** (serviceName, checkedAt DESC), (tenantId, checkedAt DESC)

#### Incident

Incidència detectada (caiguda o degradació).

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| serviceName | String(50) | @Column(nullable=false) | Servei afectat |
| tenantId | UUID | @Column | Opcional (si és d'un client) |
| severity | Enum(STRING) | @Enumerated | `CRITICAL`, `WARNING`, `INFO` |
| status | Enum(STRING) | @Enumerated | `OPEN`, `ACKNOWLEDGED`, `RESOLVED` |
| title | String(200) | @Column(nullable=false) | Títol de la incidència |
| description | String(1000) | @Column | Descripció detallada |
| startedAt | Instant | @Column(nullable=false) | Inici de la incidència |
| resolvedAt | Instant | @Column | Fi de la incidència |
| durationSeconds | Long | @Column | Durada en segons (calculada en resoldre) |
| alertSent | Boolean | @Builder.Default | Si s'ha enviat alerta Telegram |
| alertRecovered | Boolean | @Builder.Default | Si s'ha enviat recuperació |
| createdAt | Instant | @CreatedDate | |

**Índex:** (status, startedAt), (serviceName, status)

### 3.2 Enums

```java
public enum ServiceStatus {
    UP, DOWN, DEGRADED
}

public enum IncidentSeverity {
    CRITICAL, WARNING, INFO
}

public enum IncidentStatus {
    OPEN, ACKNOWLEDGED, RESOLVED
}
```

---

## 4. Endpoints API

Prefix base: `/api/v1/ops`

| Mètode | Ruta | @PreAuthorize | Descripció |
|--------|------|--------------|-----------|
| GET | /api/v1/ops/health | Cap (públic) | Health check bàsic de l'API |
| GET | /api/v1/ops/health/all | SUPER_ADMIN | Estat de tots els serveis |
| GET | /api/v1/ops/incidents | SUPER_ADMIN | Llistar incidències (paginat, filtre per estat/servei) |
| PUT | /api/v1/ops/incidents/{id}/acknowledge | SUPER_ADMIN | Acceptar una incidència |
| PUT | /api/v1/ops/incidents/{id}/resolve | SUPER_ADMIN | Resoldre incidència |
| GET | /api/v1/ops/dashboard | SUPER_ADMIN | Dashboard d'operacions |
| GET | /api/v1/ops/logs/{serviceName} | SUPER_ADMIN | Veure logs recents d'un servei |

### Detall d'endpoints

#### `GET /api/v1/ops/health` — Health check públic

Sense autenticació. Retorna l'estat bàsic de l'API.

Response 200:
```json
{
  "status": "UP",
  "version": "0.0.1",
  "timestamp": "2026-05-13T10:00:00Z",
  "uptime": "72h 30m"
}
```

#### `GET /api/v1/ops/health/all` — Estat complet de tots els serveis

**Rol:** SUPER_ADMIN

Response 200:
```json
{
  "services": [
    { "name": "backend", "status": "UP", "responseTimeMs": 45, "lastCheck": "..." },
    { "name": "postgres", "status": "UP", "responseTimeMs": 12, "lastCheck": "..." },
    { "name": "redis", "status": "UP", "responseTimeMs": 3, "lastCheck": "..." },
    { "name": "n8n", "status": "UP", "responseTimeMs": 120, "lastCheck": "..." },
    { "name": "minio", "status": "DEGRADED", "responseTimeMs": 1500, "lastCheck": "..." }
  ],
  "summary": {
    "total": 7,
    "up": 6,
    "down": 0,
    "degraded": 1
  },
  "timestamp": "..."
}
```

#### `GET /api/v1/ops/dashboard` — Dashboard d'operacions

---

## 5. Serveis

### 5.1 HealthChecker

`@Scheduled` service que corre cada 30 segons:

1. Obté la llista de serveis a monitoritzar (configuració + dinàmica: llocs web de clients)
2. Per a cada servei, fa una petició HTTP/TCPSocket
3. Registra el resultat a `ServiceHealth`
4. Si un servei falla 6+ vegades consecutives (3 minuts):
   - Crea un `Incident` amb severity `CRITICAL`
   - Envia alerta Telegram si no s'ha enviat ja (cooldown 30 min)
5. Si un servei es recupera després d'un incident:
   - Tanca l'incident (status RESOLVED)
   - Envia missatge de recuperació a Telegram

### 5.2 AlertService

Gestiona l'enviament d'alertes via Telegram:

- Manté un cooldown de 30 min per servei (no repetir la mateixa alerta)
- Formata el missatge segons l'especificació del CLAUDE.md
- Envia via Bot API de Telegram
- Registra si l'alerta s'ha enviat a l'Incident

### 5.3 LogService

Permet consultar logs recents:

- Backend logs: `journalctl` o fitxer `backend.log`
- n8n logs: API de n8n o docker logs
- Traefik logs: docker logs
- Limit: últims 1000 events, filtrats per nivell (ERROR, WARN, INFO)

---

## 6. Alertes Telegram

### 6.1 Format d'alerta (CAIGUDA)

```
🔴 [PRODUCCIÓ] · [backend] · [ERROR] · fa 3 min
Servei: Backend API
Error: Connection refused (port 8080)
Inici: 2026-05-13 09:45:00
Durada: 3 min 12 s
```

### 6.2 Format de recuperació

```
✅ [PRODUCCIÓ] · [backend] recuperat
Temps caiguda: 4 min 30 s
Inici: 2026-05-13 09:45:00
Fi: 2026-05-13 09:49:30
```

### 6.3 Cooldown

- Mínim 30 minuts entre alertes del mateix servei
- Es mesura des de l'última alerta enviada (no des de l'inici de la caiguda)
- Si el servei es recupera i torna a caure dins del cooldown, no es re-alerta

---

## 7. Configuració

### application.yml

```yaml
app:
  ops:
    health:
      check-interval-ms: 30000
      failure-threshold: 6         # 6 fails consecutius = alerta
    alerts:
      telegram:
        bot-token: ${TELEGRAM_BOT_TOKEN}
        chat-id: ${TELEGRAM_CHAT_ID}
      cooldown-minutes: 30
```

### Serveis monitoritzats per defecte

| Servei | Tipus de check | URL/PORT |
|--------|---------------|----------|
| backend | HTTP GET | https://api.amg.cat/api/v1/ops/health |
| frontend | HTTP GET | https://portal.amg.cat |
| postgres | TCP connect | localhost:5432 |
| redis | TCP/Redis PING | localhost:6379 |
| n8n | HTTP GET | https://n8n.amg.cat/healthz |
| minio | HTTP GET | https://minio.amg.cat/minio/health/live |
| traefik | HTTP GET | https://traefik.amg.cat/ping |

---

## 8. Seguretat

- `/api/v1/ops/health` és públic (només retorna UP/DOWN + versió)
- Tota la resta requereix JWT amb rol SUPER_ADMIN
- Les alertes de Telegram no contenen secrets ni tokens
- Els backups es gestionen al Mòdul 18 (Backup Operacional) amb xifratge AES-256 a GCS

---

## 9. Tests d'integració

10 tests mínims (patró: `OpsControllerTest.java`):

| # | Test | Esperat |
|---|------|---------|
| 1 | Health check públic | 200, status = UP |
| 2 | Health all (SUPER_ADMIN) | 200, llista de serveis |
| 3 | Health all sense auth | 401 |
| 4 | Llistar incidències buit | 200 |
| 5 | Crear incidència (via scheduler mock) | 201, alerta enviada |
| 6 | Acceptar incidència | 200, status = ACKNOWLEDGED |
| 7 | Resoldre incidència | 200, status = RESOLVED, resolvedAt no null |
| 8 | Dashboard | 200, mètriques |
| 9 | Logs d'un servei | 200, llista d'entrades de log |
| 10 | ADMIN no pot accedir a Ops | 403 |

---

## 10. QA / Casos de prova

| # | Escenari | Esperat |
|---|----------|---------|
| OPS-01 | Backend cau 3 min → alerta Telegram | Alerta enviada, Incident OPEN |
| OPS-02 | Backend es recupera → missatge recuperació | Incident RESOLVED, durada calculada |
| OPS-03 | Backend cau 1 minut només (menys de 6 checks) | NO alerta, NO incident |
| OPS-04 | Múltiples serveis cauen → alertes individuals | Cada servei rep la seva alerta |
| OPS-05 | Cooldown actiu → nova caiguda no alerta | No es duplica l'alerta |
| OPS-06 | Dashboard amb incidències obertes | openIncidents > 0 |

---

## 11. Resum d'entitats

| Entitat | Repositori | Endpoints |
|---------|-----------|-----------|
| ServiceHealth | ServiceHealthRepository | health/all |
| Incident | IncidentRepository | llistar, acknowledge, resolve, dashboard |

---

## 12. Obert / Pendents

- [ ] Decidir si els health checks de llocs web de clients es fan des d'aquest mòdul o des d'Engine
- [ ] Implementar el LogService real (accés a logs de Docker/journald)
- [ ] Mètriques d'uptime per client (SLAs)
- [ ] Notificacions addicionals: email a client si el seu web cau > 1h
