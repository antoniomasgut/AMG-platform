# Spec 29 — Web Hosting & Import (Landing Import Bàsic + Pro)

**Versió**: 1.0  
**Estat**: Esborrany  
**Mòdul**: 29  
**Depèn de**: Mòdul 06 (Assets/MinIO), Mòdul 18 (Backup), Mòdul 04 (Engine/Dominis)

---

## 1. Visió general

Tres serveis diferenciats per a la presència web d'un tenant:

| Servei | Slug | Descripció | Accés |
|--------|------|------------|-------|
| **Landing Factory** | `landing-pro` | Editor visual intern (Factory) | Tots els tenants |
| **Import Bàsic** | `import-basic` | Web estàtica HTML/CSS/JS (carpeta o ZIP) | F3+ o aprovació manual |
| **Import Pro** | `import-pro` | Web amb contenidors Docker (1 o més) | F5 + revisió AMG obligatòria |

---

## 2. Import Bàsic — Web Estàtica

### 2.1 Descripció
El client té una web HTML/CSS/JS ja feta. AMG la puja, la serveix via Nginx i la vincula al domini del tenant.

### 2.2 Format d'entrada
- **ZIP** pujat des del portal (màx 50 MB)
- Ha de contenir un `index.html` a l'arrel
- Pot incloure subcarpetes (`/assets`, `/css`, `/js`, `/images`)
- No s'accepten fitxers PHP, `.htaccess`, scripts de servidor

### 2.3 Format de sortida (exportació)
- Descàrrega del ZIP original tal com va ser pujat
- Disponible des del portal: `Serveis → Import Bàsic → Descarregar`

### 2.4 Desplegament
```
MinIO (amg-assets)
  └── tenants/{tenantId}/static-site/
        ├── index.html
        ├── css/style.css
        └── ...

Docker: nginx:alpine
  - Munta volum des de MinIO (sync via rclone)
  - Nom contenidor: site-{tenantSlug}-static
  - Xarxa: coolify (Traefik)
  - Label Traefik: Host(`{domini}`)
  - RAM limit: 32 MB
  - No ports exposats directament
```

### 2.5 Actualització
- El tenant (o AMG) puja un nou ZIP → reemplaça els fitxers → Nginx recarrega automàticament
- No cal rebuild de contenidor

### 2.6 Backup
- El ZIP s'emmagatzema a MinIO com a asset del tenant
- Inclòs als backups de Mòdul 18 (GCS Nearline EU)
- Retenció: 30 dies de versions anteriors

### 2.7 Requisits d'accés
- Fase F3 o superior **o** aprovació manual SUPER_ADMIN
- Revisió del ZIP per AMG (comprovació manual de seguretat bàsica)
- Setup manual: AMG desplegue el contenidor Nginx un cop aprovat

---

## 3. Import Pro — Web amb Contenidors

### 3.1 Descripció
El client té una web complexa (frontend + backend + BD, o WordPress, etc.) empaquetada en Docker. AMG la revisa, l'aprova i la desplegue al servidor dedicat de clients.

### 3.2 Format d'entrada
**Opció A — Docker Compose** (recomanat):
- Fitxer `docker-compose.yml` + variables d'entorn (`.env.example`)
- Imatges des de Docker Hub o registre privat (credencials necessàries)
- Volums de dades opcionals (BD inicial en SQL dump)

**Opció B — Imatge Docker única**:
- Nom de la imatge + tag (ex: `wordpress:6.5`)
- Variables d'entorn necessàries
- Volums de dades

### 3.3 Format de sortida (exportació)
- `docker-compose.yml` + `.env.example` (sense secrets reals)
- Dump de la base de dades (si n'hi ha) en format SQL
- Descàrrega des del portal: `Serveis → Import Pro → Exportar`
- Exportació disponible en qualsevol moment (no només en cancel·lació)

### 3.4 Revisió obligatòria AMG
Abans de desplegar, SUPER_ADMIN ha de revisar:
- [ ] No usa mode `--privileged`
- [ ] No munta directoris del host fora de `/opt/amg/clients/{tenantId}/`
- [ ] No exposa ports directament (tot via Traefik)
- [ ] Imatges de fonts conegudes (Docker Hub oficial o GitHub)
- [ ] No té dependències de xarxa cap a la xarxa interna `amg-network`
- [ ] Límits de RAM i CPU definits al `docker-compose.yml`

### 3.5 Desplegament
```
Servidor: servidor dedicat a clients (separat del servidor de la plataforma)
  └── /opt/amg/clients/{tenantId}/
        ├── docker-compose.yml
        ├── .env (secrets xifrats per AMG)
        └── volumes/
              ├── db/         (dades BD)
              └── uploads/    (fitxers pujats pel client final)

Xarxa Docker: clients-network (aïllada de amg-network)
Traefik labels: Host(`{domini}`) per al contenidor frontend
RAM limit per tenant: 512 MB per defecte (configurable fins 2 GB)
CPU limit: 0.5 CPU per defecte (configurable fins 2 CPU)
```

### 3.6 Backup
- **Volums Docker**: backup diari via `docker run --rm -v volume:/data alpine tar czf -` → GCS
- **Dump BD**: si el compose inclou MySQL/PostgreSQL, dump automàtic nocturn
- **docker-compose.yml + .env.example**: versionat a Git intern
- Retenció: 7 dies diaris + 4 setmanals
- Restauració: disponible des del portal per SUPER_ADMIN

### 3.7 Requisits d'accés
- **Fase F5 obligatòria**
- Revisió manual AMG (mínim 48h des de la sol·licitud)
- Contracte addicional de responsabilitat (el codi del client és responsabilitat del client)
- Setup fee addicional (auditoria + configuració)

---

## 4. Portal — Flux d'usuari

### 4.1 Import Bàsic
```
Admin/Tenant → Serveis → Import Bàsic → Sol·licitar
  → Pujar ZIP
  → Estat: "Pendent de revisió"
  → AMG revisa i aprova (portal admin)
  → Estat: "Desplegant..."
  → Estat: "Actiu" + URL accessible
  → Botó "Actualitzar web" (pujar nou ZIP)
  → Botó "Exportar ZIP"
```

### 4.2 Import Pro
```
Admin/Tenant → Serveis → Import Pro → Sol·licitar
  → Formulari: docker-compose.yml + .env.example + descripció
  → Estat: "Pendent de revisió AMG (48h)"
  → SUPER_ADMIN fa checklist de seguretat al portal
  → Aprova o rebutja (amb motiu)
  → Si aprovat: desplegament manual per AMG
  → Estat: "Actiu" + URL accessible
  → Botó "Exportar" (compose + DB dump)
```

---

## 5. Entitats de domini

### WebSite (nova entitat)
```java
@Entity @Table("websites")
UUID id
UUID tenantId
WebsiteType type          // STATIC, CONTAINER
WebsiteStatus status      // PENDING_REVIEW, APPROVED, DEPLOYING, ACTIVE, SUSPENDED, REJECTED
String domain
String containerName      // Docker container/stack name
Long storageBytes         // Mida actual en bytes
String reviewNotes        // Notes de revisió AMG
UUID reviewedBy           // SUPER_ADMIN que ha revisat
LocalDateTime reviewedAt
LocalDateTime deployedAt
LocalDateTime createdAt
```

### WebsiteType
```java
STATIC    // HTML/CSS/JS — Nginx
CONTAINER // Docker Compose — un o més contenidors
```

---

## 6. API Endpoints

| Mètode | Ruta | Rol | Descripció |
|--------|------|-----|------------|
| `GET` | `/api/v1/hosting/tenants/{id}/sites` | ADMIN+ | Llista webs del tenant |
| `POST` | `/api/v1/hosting/tenants/{id}/sites/static` | ADMIN+ | Sol·licitar Import Bàsic (multipart ZIP) |
| `POST` | `/api/v1/hosting/tenants/{id}/sites/container` | ADMIN+ | Sol·licitar Import Pro (compose + env) |
| `GET` | `/api/v1/hosting/tenants/{id}/sites/{siteId}` | ADMIN+ | Detall d'una web |
| `PUT` | `/api/v1/hosting/tenants/{id}/sites/{siteId}/static` | ADMIN+ | Actualitzar ZIP (nova versió) |
| `GET` | `/api/v1/hosting/tenants/{id}/sites/{siteId}/export` | ADMIN+ | Descarregar ZIP o compose+dump |
| `DELETE` | `/api/v1/hosting/tenants/{id}/sites/{siteId}` | SUPER_ADMIN | Eliminar web i contenidors |
| `POST` | `/api/v1/hosting/admin/sites/{siteId}/approve` | SUPER_ADMIN | Aprovar sol·licitud |
| `POST` | `/api/v1/hosting/admin/sites/{siteId}/reject` | SUPER_ADMIN | Rebutjar sol·licitud (amb motiu) |
| `GET` | `/api/v1/hosting/admin/sites/pending` | SUPER_ADMIN | Llista sol·licituds pendents |

---

## 7. Catàleg de serveis — canvis

Substituir `landing-extra` pels dos nous serveis:

| Slug | Nom | Setup | Mensual | Tipus |
|------|-----|-------|---------|-------|
| `import-basic` | Landing Import Bàsic | 40 € | 8 €/mes | OTHER |
| `import-pro` | Landing Import Pro | 120 € | 30 €/mes | OTHER |

Eliminar: `landing-extra` (conceptualment reemplaçat)

---

## 8. Backup — integració amb Mòdul 18

### Import Bàsic
- Backup del ZIP a GCS via MinIO sync (ja existent)
- Cap canvi al BackupOrchestrator

### Import Pro
- `BackupOrchestrator` nou cas `WEBSITE_VOLUMES`:
  - Enumera contenidors amb label `amg.tenant={tenantId}`
  - Per cada volum: `docker run --rm -v {vol}:/data alpine tar czf - /data | gzip` → GCS
  - Per cada BD detectada (MySQL/Postgres): dump via `docker exec`
- Backup nocturn automàtic (3:00 AM)
- Restauració: endpoint `POST /api/v1/hosting/admin/sites/{siteId}/restore/{backupId}`

---

## 9. Servidor dedicat a clients

Quan s'arribi a **3 webs Import Pro actives** o **RAM > 70%** al servidor principal:
- Llançar nou Hetzner CX32 (9,75 €/mes) dedicat a clients
- Traefik del servidor principal fa proxy cap al nou servidor
- El portal mostra `nodeId` per saber en quin servidor és cada web

---

## 10. Casos QA

| ID | Cas | Resultat esperat |
|----|-----|-----------------|
| QA-01 | Pujar ZIP vàlid (index.html a l'arrel) | Sol·licitud creada, estat PENDING_REVIEW |
| QA-02 | Pujar ZIP sense index.html | Error 400 "Falta index.html a l'arrel" |
| QA-03 | Pujar ZIP > 50 MB | Error 413 |
| QA-04 | Aprovar Import Bàsic | Estat → ACTIVE, URL accessible |
| QA-05 | Actualitzar ZIP → nova versió | Web actualitzada, versió anterior a backup |
| QA-06 | Exportar ZIP | Descàrrega del ZIP original |
| QA-07 | Sol·licitar Import Pro sense F5 | Error 403 |
| QA-08 | Import Pro: aprovar checklist | Estat → APPROVED, AMG pot desplegar |
| QA-09 | Import Pro: exportar | ZIP amb compose + SQL dump |
| QA-10 | Backup nocturn Import Pro | Volums a GCS, dump BD inclòs |
| QA-11 | Eliminar web Import Pro | Contenidors aturats i eliminats, volums a backup final |
