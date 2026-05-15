# Mòdul 18: Backup Operacional — Còpia de seguretat de dades operatives

> **Versió:** 1.0
> **Data:** 2026-05-15
> **Autor:** [per determinar]
> **Dependències:** Mòdul 01 (Auth), Mòdul 02 (Vault), Mòdul 03 (Leads), Mòdul 07 (Billing), Mòdul 10 (Automations)

---

## 1. Objectius

- Crear un sistema de còpia de seguretat de les dades operatives de cada tenant (credencials, configuracions de serveis, CRM, billing, automatitzacions)
- Poder restaurar un tenant al seu estat anterior en cas de pèrdua de dades o errors humans
- Facilitar l'exportació de dades per portabilitat (RGPD art. 20)
- Tenir traçabilitat de totes les operacions de backup (qui, quan, què)
- Emmagatzemar els backups de forma segura a Google Cloud Storage (GCS)

---

## 2. Abast

### 2.1 Funcionalitats incloses

- **Backup programat automàtic**: execució setmanal completa (tots els tenants)
- **Backup manual**: inici des del panell d'admin per a un tenant específic o tots
- **Exportació per tenant**: descàrrega d'un fitxer JSON amb totes les dades d'un tenant
- **Importació/Restauració**: pujar un fitxer JSON per restaurar un tenant
- **Dashboard de backups**: llistat de backups realitzats, estat, mida, data
- **Notificacions**: alerta en completar-se un backup (exit/error)
- **Retenció automàtica**: purga de backups antics segons política configurable
- **Xifratge**: els fitxers de backup es guarden xifrats a GCS (AES-256)

### 2.2 Dades incloses al backup

Per a cada tenant, s'inclou:

| Àrea | Dades | Mòdul font |
|------|-------|------------|
| **Tenant** | Informació del tenant (nom, slug, email, telèfon, estat) | Auth (01) |
| **Usuaris** | Usuaris/contactes del tenant (nom, email, rol) | Auth (01) |
| **Vault** | Credencials xifrades, credential fields, audit log | Vault (02) |
| **Service Setup** | Perfils assignats, fases, estats d'implementació | Vault (02) |
| **CRM** | Leads, comunicacions, form submissions | Leads (03) |
| **Billing** | Pressupostos, línies de pressupost, factures, pagaments | Billing (07) |
| **Automations** | Configuracions de workflows per tenant | Automations (10) |

### 2.3 Dades excloses

- Landing pages i el seu contingut (ja es gestionen amb Engine + Factory)
- Assets multimèdia (fotos, vídeos, documents — ja hi ha sistema d'assets)
- Templates de landing (són globals, no per tenant)
- Configuració global de la plataforma (no per tenant)
- Health checks i incidents (dades transitòries d'operacions)

### 2.4 Actors

| Actor | Descripció | Permisos |
|-------|-----------|----------|
| SUPER_ADMIN | Opera tot el sistema | Dashboard backups, iniciar/descarregar/restaurar |
| ADMIN | Pot veure backups i descarregar | Veure llistat, descarregar fitxers |

---

## 3. Model de dades

### 3.1 Entitats (PostgreSQL)

#### BackupTask

Representa una operació de backup (programada o manual).

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| type | Enum(STRING) | @Enumerated | `SCHEDULED`, `MANUAL_FULL`, `MANUAL_TENANT` |
| status | Enum(STRING) | @Enumerated | `PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`, `PARTIAL` |
| scope | Enum(STRING) | @Enumerated | `ALL_TENANTS`, `SINGLE_TENANT` |
| tenantId | UUID | @Column | UUID del tenant si scope=SINGLE_TENANT, null si ALL |
| requestedBy | UUID | @Column(nullable=false) | UUID del SUPER_ADMIN que va iniciar (o system si programat) |
| totalTenants | Integer | @Column | Nombre de tenants a processar |
| completedTenants | Integer | @Column | Nombre de tenants processats correctament |
| failedTenants | Integer | @Column | Nombre de tenants amb errors |
| filePath | String(500) | @Column | Ruta al fitxer a GCS (per SINGLE_TENANT) |
| fileSize | Long | @Column | Mida en bytes |
| archivePath | String(500) | @Column | Ruta al fitxer comprimit a GCS (per ALL_TENANTS) |
| archiveSize | Long | @Column | Mida del fitxer comprimit |
| errorMessage | String(1000) | @Column | Missatge d'error global |
| startedAt | Instant | @Column(nullable=false) | |
| completedAt | Instant | @Column | |
| retentionUntil | Instant | @Column | Data fins a la qual es conserva (per purga automàtica) |
| createdAt | Instant | @CreatedDate | |

**Índex:** (status, startedAt DESC), (tenantId, startedAt DESC), (createdAt)

#### BackupExportLog

Registre de l'exportació individual d'un tenant dins d'un BackupTask.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| taskId | UUID | @Column(nullable=false) | FK a BackupTask |
| tenantId | UUID | @Column(nullable=false) | Tenant exportat |
| tenantName | String(100) | @Column | Nom del tenant (per llegibilitat) |
| status | Enum(STRING) | @Enumerated | `SUCCESS`, `FAILED` |
| filePath | String(500) | @Column | Ruta al fitxer JSON a GCS |
| fileSize | Long | @Column | Mida del fitxer |
| sectionsCount | Integer | @Column | Nombre de seccions exportades |
| errorMessage | String(500) | @Column | Error si n'hi ha |
| startedAt | Instant | @Column | |
| completedAt | Instant | @Column | |

**Índex:** (taskId), (tenantId)

#### RestoreTask

Operació de restauració d'un tenant.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | Tenant a restaurar |
| backupTaskId | UUID | @Column | BackupTask d'origen (null si és pujada manual) |
| status | Enum(STRING) | @Enumerated | `PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`, `ROLLED_BACK` |
| sections | JSON | @Column(columnDefinition="jsonb") | Llista de seccions a restaurar |
| requestedBy | UUID | @Column(nullable=false) | SUPER_ADMIN que va iniciar |
| errorMessage | String(1000) | @Column | |
| createdAt | Instant | @CreatedDate | |
| completedAt | Instant | @Column | |

**Índex:** (tenantId, createdAt DESC)

### 3.2 Enums

```java
public enum BackupType {
    SCHEDULED,          // Execució automàtica programada
    MANUAL_FULL,        // Manual per a tots els tenants
    MANUAL_TENANT       // Manual per a un tenant específic
}

public enum BackupStatus {
    PENDING, IN_PROGRESS, COMPLETED, FAILED, PARTIAL
}

public enum BackupScope {
    ALL_TENANTS, SINGLE_TENANT
}

public enum ExportStatus {
    SUCCESS, FAILED
}

public enum RestoreStatus {
    PENDING, IN_PROGRESS, COMPLETED, FAILED, ROLLED_BACK
}
```

---

## 4. Endpoints API

Prefix base: `/api/v1/ops/backups`

### 4.1 Gestió de backups

| Mètode | Ruta | @PreAuthorize | Descripció |
|--------|------|--------------|-----------|
| POST | /api/v1/ops/backups | SUPER_ADMIN | Iniciar backup manual |
| GET | /api/v1/ops/backups | SUPER_ADMIN | Llistar backups (paginat, filtrat) |
| GET | /api/v1/ops/backups/{id} | SUPER_ADMIN | Detall d'un backup |
| GET | /api/v1/ops/backups/{id}/download | SUPER_ADMIN, ADMIN | Descarregar fitxer de backup |
| GET | /api/v1/ops/backups/{id}/logs | SUPER_ADMIN | Log d'exportacions individuals del backup |
| DELETE | /api/v1/ops/backups/{id} | SUPER_ADMIN | Eliminar un backup (fitxer + registre) |

### 4.2 Restauració

| Mètode | Ruta | @PreAuthorize | Descripció |
|--------|------|--------------|-----------|
| POST | /api/v1/ops/backups/restore | SUPER_ADMIN | Iniciar restauració des d'un backup existent |
| POST | /api/v1/ops/backups/restore/upload | SUPER_ADMIN | Pujar fitxer JSON i restaurar |
| GET | /api/v1/ops/backups/restore/{id} | SUPER_ADMIN | Estat d'una restauració |

### 4.3 Exportació

| Mètode | Ruta | @PreAuthorize | Descripció |
|--------|------|--------------|-----------|
| POST | /api/v1/ops/backups/export/{tenantId} | SUPER_ADMIN | Exportar un tenant específic (backup immediat) |
| GET | /api/v1/ops/backups/export/{tenantId}/latest | SUPER_ADMIN, ADMIN | Descarregar últim backup d'un tenant |

### 4.4 Dashboard

| Mètode | Ruta | @PreAuthorize | Descripció |
|--------|------|--------------|-----------|
| GET | /api/v1/ops/backups/dashboard | SUPER_ADMIN | Estadístiques de backups |

### 4.5 Detall d'endpoints

#### `POST /api/v1/ops/backups` — Iniciar backup manual

**Rol:** SUPER_ADMIN

Request:
```json
{
  "type": "MANUAL_FULL",
  "tenantId": "uuid"       // Obligatori si type=MANUAL_TENANT, null si MANUAL_FULL
}
```

Response 202:
```json
{
  "id": "uuid",
  "type": "MANUAL_FULL",
  "status": "IN_PROGRESS",
  "scope": "ALL_TENANTS",
  "totalTenants": 15,
  "startedAt": "2026-05-15T10:00:00Z"
}
```

Errors:
| Codi | Situació |
|------|---------|
| 400 | Tipus invàlid o tenantId faltant |
| 401 | No autenticat |
| 403 | No té rol SUPER_ADMIN |
| 404 | Tenant no trobat (si type=MANUAL_TENANT) |

#### `GET /api/v1/ops/backups` — Llistar backups

**Rol:** SUPER_ADMIN

Query params: `page`, `size`, `status`, `type`, `from`, `to`

Response 200:
```json
{
  "content": [
    {
      "id": "uuid",
      "type": "MANUAL_FULL",
      "status": "COMPLETED",
      "scope": "ALL_TENANTS",
      "totalTenants": 15,
      "completedTenants": 15,
      "failedTenants": 0,
      "archiveSize": 245760,
      "startedAt": "2026-05-15T10:00:00Z",
      "completedAt": "2026-05-15T10:00:45Z",
      "retentionUntil": "2026-06-14T10:00:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

#### `GET /api/v1/ops/backups/{id}/download` — Descarregar fitxer

**Rol:** SUPER_ADMIN, ADMIN

Response 200: Binary file (application/gzip o application/json)
- Content-Disposition: attachment; filename="backup-{id}.json.gz"

#### `POST /api/v1/ops/backups/restore` — Restaurar des d'un backup

**Rol:** SUPER_ADMIN

Request:
```json
{
  "tenantId": "uuid",
  "backupTaskId": "uuid",
  "sections": ["vault", "crm", "billing", "automations", "service-setup"]
}
```

Response 202:
```json
{
  "id": "uuid",
  "tenantId": "uuid",
  "status": "IN_PROGRESS",
  "sections": ["vault", "crm", "billing", "automations", "service-setup"],
  "createdAt": "2026-05-15T11:00:00Z"
}
```

#### `GET /api/v1/ops/backups/dashboard` — Dashboard

**Rol:** SUPER_ADMIN

Response 200:
```json
{
  "totalBackups": 24,
  "lastBackup": "2026-05-15T04:00:00Z",
  "lastBackupStatus": "COMPLETED",
  "backupsByType": {
    "SCHEDULED": 12,
    "MANUAL_FULL": 4,
    "MANUAL_TENANT": 8
  },
  "storageUsedBytes": 52428800,
  "nextScheduledBackup": "2026-05-22T04:00:00Z",
  "retentionDays": 30
}
```

---

## 5. Serveis

### 5.1 BackupOrchestrator

Servei principal que coordina les operacions de backup:

1. **Iniciar backup**: crea un BackupTask amb status PENDING, el passa a IN_PROGRESS
2. **Executar exportacions**: per a cada tenant, crida BackupExporter
3. **Comptabilitzar resultats**: actualiza completedTenants/failedTenants
4. **Comprimir**: si scope=ALL_TENANTS, comprimeix tots els fitxers en un .tar.gz
5. **Pujar a GCS**: guarda el fitxer amb la ruta `amg-backups/operational/{taskId}/{tenantId}.json.gz`
6. **Marcar completat**: status=COMPLETED o PARTIAL (si hi ha errors)
7. **Enviar notificació**: email o Telegram amb resum

### 5.2 BackupExporter

Responsable d'exportar les dades d'un tenant específic:

1. **VaultSection**: recupera credencials (xifrades), credential fields, audit log, assignació de perfils/fases
2. **CrmSection**: recupera leads, comunicacions, form submissions
3. **BillingSection**: recupera pressupostos, línies, factures, pagaments
4. **AutomationsSection**: recupera configuracions de workflows
5. **TenantSection**: recupera dades del tenant i usuaris/contactes
6. **Serialitzar**: genera un JSON amb totes les seccions
7. **Comprimir**: gzip el JSON
8. **Pujar a GCS**: amb la ruta `amg-backups/operational/{taskId}/{tenantId}.json.gz`

### 5.3 BackupRestorer

Responsable de restaurar un tenant:

1. **Validar**: comprova que el tenant existeix i el fitxer és vàlid
2. **Aturar serveis relacionats**: opcional, per evitar races
3. **Restaurar per seccions**: en ordre (Tenant → Vault → Service Setup → Billing → CRM → Automations)
4. **Transaccional per secció**: si una secció falla, es registra l'error però es continua
5. **Rollback**: si el paràmetre `allowRollback` és true, es fa un snapshot previ

### 5.4 BackupScheduler

Servei `@Scheduled` que executa el backup automàtic:

- Execució setmanal (configurable, per defecte diumenge a les 4 AM)
- Crea un BackupTask amb type=SCHEDULED, scope=ALL_TENANTS
- Delega en BackupOrchestrator
- Es salta si ja hi ha un backup en curs

### 5.5 BackupRetentionManager

Servei que purga backups antics:

- S'executa diàriament (configurable)
- Elimina fitxers de GCS amb retentionUntil > ara
- Marca els BackupTask com a eliminats (no s'esborren de la BD per audit trail)
- Log de purga

### 5.6 NotificationService

Envia notificacions de completat de backup:

- Opcions: email (SMTP) o Telegram
- Contingut: resum del backup (total tenants, errors, mida, durada)
- Es configura per tenant global (not a per-tenant)

---

## 6. Format del fitxer de backup

Estructura JSON per tenant:

```json
{
  "version": "1.0",
  "exportedAt": "2026-05-15T10:00:00Z",
  "tenantId": "uuid",
  "tenantSlug": "client-example",
  "sections": {
    "tenant": {
      "name": "Client Example SL",
      "slug": "client-example",
      "email": "info@client.cat",
      "phone": "+34600000000",
      "isActive": true,
      "users": [
        { "email": "admin@client.cat", "name": "Admin", "role": "ADMIN" }
      ]
    },
    "vault": {
      "profiles": [
        { "profileId": "uuid", "profileName": "Professionals cita", "assignedAt": "..." }
      ],
      "credentials": [
        { "serviceId": "uuid", "serviceName": "WhatsApp Business", "fields": [] }
      ]
    },
    "service-setup": {
      "phases": [
        { "phaseId": "uuid", "phaseName": "Configuració bàsica", "status": "COMPLETED" }
      ],
      "services": [
        { "serviceId": "uuid", "serviceName": "WhatsApp", "status": "VERIFIED" }
      ]
    },
    "crm": {
      "leads": [],
      "communications": []
    },
    "billing": {
      "budgets": [],
      "invoices": [],
      "payments": []
    },
    "automations": {
      "workflows": []
    }
  }
}
```

---

## 7. Configuració

### application.yml

```yaml
app:
  ops:
    backups:
      enabled: true
      schedule: "0 4 * * 0"          # Cada diumenge a les 4 AM
      retention-days: 30
      storage:
        provider: gcs
        gcs:
          project-id: ${GCS_PROJECT_ID}
          bucket-name: ${GCS_BACKUP_BUCKET}
          credentials-path: ${GCS_CREDENTIALS_PATH}   # JSON de service account
          path-prefix: "amg-backups/operational"
      encryption:
        enabled: true
        key: ${BACKUP_ENCRYPTION_KEY}
      notifications:
        enabled: true
        channel: telegram             # telegram | email
      export:
        compress: true                # gzip els fitxers JSON
        include-audit-log: false      # L'audit log pot ser molt gran
      restore:
        allow-rollback: true          # Fer snapshot previ a la restauració
        max-file-size: 50MB           # Màxim per pujada manual
```

### GCS bucket

```
gs://{bucket}/
└── amg-backups/
    └── operational/
        ├── scheduled/
        │   ├── 2026-05-15/
        │   │   └── full-20260515-040000.tar.gz
        │   └── 2026-05-22/
        │       └── ...
        └── manual/
            ├── full-20260515-100000.tar.gz
            └── tenant-uuid-20260515-100000.json.gz
```

### Dependència Maven

```xml
<dependency>
    <groupId>com.google.cloud</groupId>
    <artifactId>google-cloud-storage</artifactId>
    <version>2.43.0</version>
</dependency>
```

---

## 8. Seguretat

### 8.1 Autenticació i autorització

- Tots els endpoints requereixen JWT amb rol SUPER_ADMIN (excepte download que permet ADMIN)
- El dashboard i llistats requereixen SUPER_ADMIN
- Les operacions de restauració només SUPER_ADMIN

### 8.2 Xifratge

- Els fitxers JSON es comprimeixen amb gzip abans de pujar a GCS
- Si `encryption.enabled=true`, es xifren amb AES-256 abans de pujar
- La clau de xifratge es configura via `BACKUP_ENCRYPTION_KEY` (environment variable)
- Els fitxers a GCS NO contenen informació desxifrada

### 8.3 Protecció de dades

- Les credencials s'exporten **ja xifrades** (el vault ja les té xifrades a la BD)
- No s'exporten passwords d'usuari, només dades de perfil
- Els fitxers de backup s'eliminen automàticament després del període de retenció
- L'audit trail de backups es conserva a la BD encara que el fitxer s'hagi eliminat

---

## 9. RGPD / LSSI

- **Dades personals:** Emails, noms, telèfons de contactes (dins de tenant/crm/billing)
- **Base legal:** Compliment d'obligació contractual (art. 6.1.b) i interès legítim (art. 6.1.f) per continuïtat del servei
- **Conservació:** Els backups es conserven 30 dies (configurable), després es purguen automàticament
- **Portabilitat:** L'exportació per tenant en JSON compleix el dret de portabilitat (art. 20)
- **Supressió:** En eliminar un tenant, el següent backup programat ja no l'inclourà. Els backups anteriors es purgaran segons retenció
- **Registre d'operacions:** Cada backup i restauració queda registrat a la BD amb timestamp i responsable

---

## 10. Tests d'integració

| # | Test | Esperat |
|---|------|---------|
| 1 | Iniciar backup MANUAL_FULL sense auth | 401 |
| 2 | Iniciar backup MANUAL_FULL amb ADMIN | 403 |
| 3 | Iniciar backup MANUAL_FULL amb SUPER_ADMIN | 202, status=IN_PROGRESS |
| 4 | Iniciar backup MANUAL_TENANT amb tenantId vàlid | 202 |
| 5 | Iniciar backup MANUAL_TENANT amb tenantId inexistent | 404 |
| 6 | Llistar backups buit | 200, llista buida |
| 7 | Llistar backups després d'un backup | 200, 1 element |
| 8 | Descarregar backup existent | 200, Content-Disposition |
| 9 | Descarregar backup inexistent | 404 |
| 10 | Dashboard amb backups | 200, mètriques |
| 11 | Restaurar des d'un backup | 202, restauració completa |
| 12 | Restaurar amb tenantId inexistent | 404 |
| 13 | Eliminar backup | 204 |
| 14 | Backup programat s'executa (mock scheduler) | BackupTask creat |
| 15 | Exportació per tenant amb dades | JSON amb totes les seccions |
| 16 | Exportació de tenant sense dades | JSON amb seccions buides |

---

## 11. Dependències entre mòduls

| Mòdul | Dependència | Tipus |
|-------|-----------|-------|
| 01 (Auth) | Dades de tenant + usuaris | Forta |
| 02 (Vault) | Credencials, perfils, fases | Forta |
| 03 (Leads) | Leads i comunicacions | Forta |
| 07 (Billing) | Pressupostos, factures, pagaments | Forta |
| 10 (Automations) | Workflows per tenant | Forta |

---

## 12. Obert / Pendents

- [ ] Decidir si el backup inclou l'historial de comunicacions complet o només l'últim mes
- [ ] Determinar si l'audit log del vault s'exporta o no (pot ser molt gran)
- [ ] Decidir canal de notificació per defecte (Telegram + email?)
- [ ] Valorar si cal xifratge addicional o el de GCS és suficient
- [ ] Decidir política de retenció per defecte (30 dies?)
- [ ] Cal un mode "read-only" durant la restauració per evitar writes concurrents?
