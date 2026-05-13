# Mòdul 10: Automations — n8n Workflows

> **Versió:** 1.0
> **Data:** 2026-05-13
> **Autor:** [per determinar]
> **Dependències:** Mòdul 01 (Auth), Mòdul 02 (Vault), Mòdul 03 (Leads), Mòdul 04 (Engine)

---

## 1. Objectius

- Integrar la plataforma AMG amb n8n self-hosted per executar automatitzacions
- Exposar webhooks públics i autenticats perquè n8n pugui rebre/esdeveniments i enviar resultats
- Gestionar el cicle de vida dels workflows: template → desplegament → monitorització
- Cobrir els 3 tipus d'automatització del catàleg: Automatització bàsica, Automatització avançada, Bot IA

---

## 2. Abast

### 2.1 Funcionalitats incloses

- Webhook públic per rebre resultats de workflows n8n (POST /api/v1/automations/webhook)
- Webhook autenticat per triggerar workflows des de la plataforma
- Registre d'execucions de workflows (request/response/timestamp/status)
- Catàleg de templates de workflow (bàsic/avançat/bot IA/SMTP/WhatsApp)
- Desplegament de workflows via API de n8n
- Estat de connexió amb n8n (health check)
- Integració amb Engine: trigger post-formulari (quan un ContactLead es crea)
- Integració amb Leads: trigger post-creació (quan un Lead canvia d'etapa)
- Integració amb Vault: enviament de comunicacions via n8n (WhatsApp, Telegram, Email)

### 2.2 Funcionalitats excloses

- Editor visual de workflows (es fa a la UI de n8n directament)
- Market de templates públic
- Auto-retry avançat amb backoff exponencial (n8n ja ho fa)
- Cua d'execucions (n8n ja té el seu propi scheduler)

### 2.3 Actors

| Actor | Descripció | Permisos |
|-------|-----------|----------|
| SUPER_ADMIN | Configura i gestiona workflows | Desplegar, activar/desactivar, veure logs |
| ADMIN | Selecciona templates per als seus clients | Veure estat, activar/desactivar workflows assignats |
| CLIENT | Usuari final | Veure estat dels seus workflows |
| n8n | Sistema extern (autenticat via API Key) | Enviar resultats de webhook, rebre triggers |

---

## 3. Model de dades

### 3.1 Entitats (PostgreSQL)

#### WorkflowTemplate (Plantilla de workflow)

Defineix un workflow n8n predefinit que es pot desplegar.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| name | String(100) | @Column(nullable=false) | Nom humà (ex: "Formulari → WhatsApp") |
| key | String(50) | @Column(unique, nullable=false) | Clau única (ex: "form-to-whatsapp") |
| description | String(300) | @Column | Descripció curta |
| category | Enum(STRING) | @Enumerated | `BASIC`, `ADVANCED`, `BOT_IA`, `SMTP`, `WHATSAPP` |
| activationType | Enum(STRING) | @Enumerated | `AUTOMATIC` (sense configuració), `MANUAL` (requereix setup) |
| n8nWorkflowJson | TEXT | @Column(columnDefinition="TEXT") | JSON del workflow n8n (exportat) |
| setupGuide | TEXT | @Column(columnDefinition="TEXT") | Instruccions de configuració |
| isActive | Boolean | @Builder.Default | true |
| createdAt | Instant | @CreatedDate | |

#### TenantWorkflow (Workflow per tenant)

Workflow desplegat per a un tenant específic.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | Tenant propietari |
| templateId | UUID | @Column(nullable=false) | FK a WorkflowTemplate |
| n8nWorkflowId | String(50) | @Column | ID del workflow a n8n (null si no desplegat) |
| n8nWebhookUrl | String(500) | @Column | URL del webhook a n8n (per triggers) |
| status | Enum(STRING) | @Enumerated | `PENDING`, `DEPLOYED`, `ACTIVE`, `ERROR`, `DISABLED` |
| config | TEXT(JSON) | @Column(columnDefinition="TEXT") | Configuració específica del tenant (JSON) |
| lastRunAt | Instant | @Column | Última execució |
| lastRunStatus | Enum(STRING) | @Enumerated | `SUCCESS`, `FAILED`, `PENDING` |
| errorMessage | String(500) | @Column | Últim error |
| isActive | Boolean | @Builder.Default | true |
| createdAt | Instant | @CreatedDate | |
| updatedAt | Instant | @LastModifiedDate | |

**Índex:** (tenantId, status), (tenantId, templateId, isActive)

#### WorkflowExecution (Registre d'execució)

Cada vegada que la plataforma envia un event a n8n o n8n retorna un resultat.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantWorkflowId | UUID | @Column(nullable=false) | FK a TenantWorkflow |
| triggerType | Enum(STRING) | @Enumerated | `FORM_SUBMIT`, `LEAD_UPDATE`, `COMMUNICATION`, `SCHEDULED`, `WEBHOOK` |
| sourceId | String(100) | @Column | ID de l'entitat origen (leadId, landingId, etc.) |
| requestPayload | TEXT(JSON) | @Column(columnDefinition="TEXT") | Payload enviat a n8n |
| responsePayload | TEXT(JSON) | @Column(columnDefinition="TEXT") | Resposta de n8n |
| n8nExecutionId | String(50) | @Column | ID d'execució a n8n |
| status | Enum(STRING) | @Enumerated | `PENDING`, `SUCCESS`, `FAILED`, `TIMEOUT` |
| errorMessage | String(500) | @Column | Missatge d'error |
| executedAt | Instant | @CreatedDate | |
| completedAt | Instant | @Column | |

---

## 4. API REST

Prefix base: `/api/v1/automations`

### 4.1 Webhook públic (n8n → AMG)

#### `POST /api/v1/automations/webhook` — Rebre resultats de n8n

**Autenticació:** API Key (configurada a l'entorn, compartida amb n8n)

n8n envia el resultat d'una execució. La plataforma l'enllaça amb el `WorkflowExecution` corresponent.

Request:
```json
{
  "workflowId": "n8n-workflow-uuid",
  "executionId": "n8n-exec-123",
  "status": "success",
  "output": { "message": "Missatge enviat correctament", "whatsappMsgId": "wamid.xxx" },
  "error": null,
  "executionStart": "2026-05-13T10:00:00Z",
  "executionEnd": "2026-05-13T10:00:05Z"
}
```

Response 200:
```json
{ "received": true }
```

**Errors:** 401 si API Key invàlida

#### `GET /api/v1/automations/webhook/health` — Health check de n8n

**Autenticació:** Cap (públic, per configurar health check a n8n)

Response 200: `{ "status": "ok", "timestamp": "..." }`

### 4.2 Gestió de workflows (autenticat)

#### `POST /api/v1/automations/tenants/{tenantId}/workflows` — Assignar workflow a un tenant

**Rols:** SUPER_ADMIN, ADMIN

Request:
```json
{
  "templateKey": "form-to-whatsapp",
  "config": {
    "webhookUrl": "https://hooks.n8n.amg.cat/webhook/xxx",
    "targetPhone": "+34600123456"
  }
}
```

Response 201: TenantWorkflow creat amb status `PENDING`

Si el workflow és `AUTOMATIC`, es desplega automàticament a n8n.

#### `POST /api/v1/automations/workflows/{workflowId}/deploy` — Desplegar workflow a n8n

**Rols:** SUPER_ADMIN, ADMIN

Envia el `n8nWorkflowJson` del template a l'API de n8n, substituint variables de configuració.

Response 200: `{ "status": "DEPLOYED", "n8nWorkflowId": "xxx", "n8nWebhookUrl": "..." }`

#### `POST /api/v1/automations/workflows/{workflowId}/activate` — Activar workflow

**Rols:** SUPER_ADMIN, ADMIN

Activa el workflow a n8n (via API n8n) i canvia status a `ACTIVE`.

#### `POST /api/v1/automations/workflows/{workflowId}/deactivate` — Desactivar workflow

**Rols:** SUPER_ADMIN, ADMIN

Desactiva el workflow a n8n i canvia status a `DISABLED`.

#### `GET /api/v1/automations/tenants/{tenantId}/workflows` — Llistar workflows d'un tenant

**Rols:** SUPER_ADMIN, ADMIN, CLIENT (propi)

**Query:** `page`, `size`, `status`

#### `GET /api/v1/automations/workflows/{workflowId}/executions` — Llistar execucions

**Rols:** SUPER_ADMIN, ADMIN, CLIENT (propi)

**Query:** `page`, `size`, `status`

#### `DELETE /api/v1/automations/workflows/{workflowId}` — Eliminar workflow del tenant

**Rols:** SUPER_ADMIN, ADMIN

Desactiva a n8n + elimina el registre. No esborra el template.

#### `GET /api/v1/automations/templates` — Llistar templates disponibles

**Rols:** SUPER_ADMIN, ADMIN

#### `GET /api/v1/automations/health` — Estat de connexió amb n8n

**Rols:** SUPER_ADMIN, ADMIN

Response 200:
```json
{
  "n8nConnected": true,
  "n8nVersion": "1.80.0",
  "uptime": "72h",
  "activeWorkflows": 5,
  "pendingExecutions": 2
}
```

### 4.3 Triggers interns (entre mòduls)

Aquestes connexions no són endpoints REST sinó crides internes del backend.

#### Trigger: Formulari de contacte (des d'Engine)

Quan Engine rep un `POST /api/v1/engine/render/{slug}/contact` i el guarda com a `ContactLead`:

1. Engine busca workflows actius del tenant amb `activationType = FORM_SUBMIT`
2. Per a cada workflow, envia un event a l'AutomationsService
3. AutomationsService guarda `WorkflowExecution` (status=PENDING)
4. Crida al webhook de n8n (si configurat) o desa per a polling

Payload enviat a n8n:
```json
{
  "type": "form_submit",
  "tenantId": "uuid",
  "data": {
    "name": "Joan Servera",
    "email": "joan@example.com",
    "phone": "+34600123456",
    "message": "Vull pressupost",
    "landingSlug": "restaurant-can-pedro",
    "landingTitle": "Restaurant Can Pedro",
    "submittedAt": "2026-05-13T10:00:00Z"
  }
}
```

#### Trigger: Canvi d'etapa de Lead (des de Leads)

Quan un Lead canvia d'etapa (PATCH /leads/{id}/stage):

1. Leads busca workflows actius del tenant amb `activationType = LEAD_UPDATE`
2. Envia event a AutomationsService
3. AutomationsService crida n8n

Payload:
```json
{
  "type": "lead_update",
  "tenantId": "uuid",
  "data": {
    "leadId": "uuid",
    "name": "Joan Servera",
    "email": "joan@example.com",
    "stage": "qualified",
    "previousStage": "new",
    "tags": ["restaurant", "alta prioritat"],
    "updatedAt": "2026-05-13T10:00:00Z"
  }
}
```

#### Trigger: Comunicació (des de Vault)

Quan Vault ha d'enviar una comunicació (requestInfo, confirmPhase):

1. Vault busca el TenantWorkflow del tenant amb `templateKey` corresponent al canal
2. Si existeix i està ACTIVE, crida AutomationsService per enviar el missatge
3. AutomationsService envia a n8n i registra l'execució

Payload:
```json
{
  "type": "communication",
  "tenantId": "uuid",
  "channel": "whatsapp",
  "data": {
    "to": "+34600123456",
    "templateName": "request_info",
    "parameters": {
      "clientName": "Joan",
      "serviceName": "Landing Pro"
    },
    "replyWebhook": "https://api.amg.cat/api/v1/automations/webhook"
  }
}
```

### 4.4 Mapa complet d'endpoints

| Mètode | Ruta | Descripció | Auth | Rols |
|--------|------|-----------|------|------|
| POST | /api/v1/automations/webhook | Rebre resultats n8n | API Key | Intern |
| GET | /api/v1/automations/webhook/health | Health check | Pública | — |
| GET | /api/v1/automations/tenants/{tId}/workflows | Llistar workflows | JWT | Tots |
| POST | /api/v1/automations/tenants/{tId}/workflows | Assignar workflow | JWT | SUPER_ADMIN, ADMIN |
| POST | /api/v1/automations/workflows/{wId}/deploy | Desplegar a n8n | JWT | SUPER_ADMIN, ADMIN |
| POST | /api/v1/automations/workflows/{wId}/activate | Activar | JWT | SUPER_ADMIN, ADMIN |
| POST | /api/v1/automations/workflows/{wId}/deactivate | Desactivar | JWT | SUPER_ADMIN, ADMIN |
| DELETE | /api/v1/automations/workflows/{wId} | Eliminar | JWT | SUPER_ADMIN, ADMIN |
| GET | /api/v1/automations/workflows/{wId}/executions | Llistar execucions | JWT | Tots |
| GET | /api/v1/automations/templates | Llistar templates | JWT | SUPER_ADMIN, ADMIN |
| GET | /api/v1/automations/health | Estat n8n | JWT | SUPER_ADMIN, ADMIN |

---

## 5. Seguretat

### 5.1 Autenticació
- Endpoints de gestió: JWT (Mòdul 01 Auth)
- Webhook de n8n: API Key (configurable via `N8N_WEBHOOK_API_KEY`)
- Health check: públic (només retorna ok/timestamp)

### 5.2 Autorització
- SUPER_ADMIN: gestionar tots els workflows i templates
- ADMIN: gestionar workflows dels seus tenants
- CLIENT: veure estat dels seus workflows i execucions

### 5.3 Protecció
- Rate limiting al webhook públic (100 req/min per IP)
- Validació de payloads JSON (mida max 1MB)
- API Key rotable via variable d'entorn

---

## 6. Configuració n8n

### 6.1 Variables d'entorn

| Variable | Descripció |
|----------|-----------|
| `N8N_API_URL` | URL de l'API interna de n8n (ex: http://n8n:5678/api/v1) |
| `N8N_API_KEY` | API Key de n8n (user management) |
| `N8N_WEBHOOK_BASE_URL` | URL base per webhooks (ex: https://hooks.amg.cat) |
| `N8N_WEBHOOK_API_KEY` | API Key que n8n envia al webhook de la plataforma |

### 6.2 docker-compose

Afegir al `infra/docker-compose.yml`:

```yaml
n8n:
  image: n8nio/n8n:latest
  restart: unless-stopped
  ports:
    - "5678:5678"
  environment:
    - N8N_HOST=n8n.amg.cat
    - N8N_PORT=5678
    - N8N_PROTOCOL=https
    - N8N_EDITOR_BASE_URL=https://n8n.amg.cat
    - WEBHOOK_URL=https://hooks.amg.cat
    - DB_TYPE=postgresdb
    - DB_POSTGRESDB_HOST=postgres
    - DB_POSTGRESDB_DATABASE=n8n
    - DB_POSTGRESDB_USER=n8n
    - DB_POSTGRESDB_PASSWORD=${N8N_DB_PASSWORD}
    - N8N_METRICS=false
    - N8N_SKIP_WEBHOOK_DEREGISTRATION_ON_SHUTDOWN=true
  volumes:
    - n8n_data:/home/node/.n8n
  labels:
    - "traefik.enable=true"
    - "traefik.http.routers.n8n.rule=Host(`n8n.amg.cat`)"
    - "traefik.http.routers.n8n.tls=true"
```

---

## 7. Integració amb el Vault

El Mòdul 02 Vault gestiona la configuració de canals de comunicació. Per a cada canal, el Vault pot:

1. Guardar les credencials del canal (API keys de WhatsApp, Telegram, SMTP) al Vault AES-256
2. Quan ha d'enviar un missatge, crida AutomationsService amb el payload
3. AutomationsService busca el workflow actiu del tenant per al canal corresponent
4. Envia el payload al webhook de n8n
5. n8n executa el workflow (ex: crida API de WhatsApp Business)
6. n8n retorna el resultat al webhook de la plataforma
7. AutomationsService registra l'execució i notifica al Vault

### Templates de workflow per canal

| Canal | Template Key | Descripció |
|-------|-------------|-----------|
| WhatsApp | `whatsapp-notify` | Enviar notificació via WhatsApp Business API |
| WhatsApp | `whatsapp-conversation` | Conversa bidireccional amb botons |
| Telegram | `telegram-notify` | Enviar missatge via Bot API |
| Telegram | `telegram-conversation` | Conversa amb botons inline |
| SMTP | `smtp-send` | Enviar email via SMTP corporatiu |
| SMTP | `smtp-html` | Enviar email HTML amb plantilla |

---

## 8. Tests (QA)

### 8.1 Funcionals

| # | Cas | Resultat |
|---|-----|---------|
| 1 | Llistar templates | 200, llista de templates |
| 2 | Assignar workflow bàsic a un tenant | 201, status PENDING |
| 3 | Desplegar workflow a n8n (mock) | 200, n8nWorkflowId retornat |
| 4 | Activar workflow | 200, status ACTIVE |
| 5 | Desactivar workflow | 200, status DISABLED |
| 6 | Llistar workflows del tenant | 200, paginat |
| 7 | Llistar execucions d'un workflow | 200, buit o amb dades |
| 8 | Eliminar workflow del tenant | 204 |
| 9 | Webhook públic rebut correctament | 200, received: true |
| 10 | Webhook amb API Key invàlida | 401 |
| 11 | Trigger post-formulari (Engine) | WorkflowExecution creat, n8n cridat |
| 12 | Trigger post-lead (Leads) | WorkflowExecution creat, n8n cridat |
| 13 | Trigger comunicació (Vault) | WorkflowExecution creat, n8n cridat |
| 14 | Health check n8n | 200, estat de connexió |
| 15 | CLIENT no pot desplegar/activar | 403 |

### 8.2 Límits

| # | Cas | Resultat |
|---|-----|---------|
| 1 | Payload massa gran (>1MB) | 400 |
| 2 | n8n no accessible | Status ERROR, errorMessage |
| 3 | Workflow ja desplegat | 409 Conflict |

---

## 9. Dependències

| Mòdul | Dependència | Tipus |
|-------|-----------|-------|
| Mòdul 01 (Auth) | Autenticació JWT | Forta |
| Mòdul 02 (Vault) | Configuració de canals i credencials | Forta |
| Mòdul 03 (Leads) | Trigger post-creació/canvi de lead | Mitjana |
| Mòdul 04 (Engine) | Trigger post-formulari | Mitjana |
| Mòdul 13 (i18n) | Traduccions (interfície d'admin) | Dèbil |

---

## 10. Obert / Pendents

- [ ] n8n self-hosted: decidir si va al mateix VPS o a un de dedicat
- [ ] Llista completa de templates de workflow a implementar
- [ ] Sistema de reintents si n8n no respon en X segons
- [ ] Rate limiting per tenant a les crides a n8n
- [ ] UI d'admin per gestionar workflows (llistat + estat)
- [ ] Alertes si un workflow falla repetidament
