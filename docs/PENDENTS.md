# Tasques pendents — AMG Digitalització

**Estat del servidor:** http://65.108.148.62:3000 (frontend) · http://65.108.148.62:8081 (backend)  
**Última sessió:** 2026-05-18

---

## 🔴 Prioritat alta — fer quan tinguis el domini

### 1. Domini + SSL
- Comprar domini (opcions: `amg.es`, `digitalitzam.com`, `amgdigital.es`)
- Als DNS del registrador, afegir registre **A** → `65.108.148.62`
- Configurar Traefik al servidor per servir el portal amb SSL automàtic (Let's Encrypt)
- Actualitzar la variable `APP_CORS_ALLOWED_ORIGINS` al contenidor backend amb el nou domini

```bash
# Exemple de com recrear el backend amb el domini definitiu:
docker stop amg-backend && docker rm amg-backend
docker run -d --name amg-backend ... \
  -e APP_CORS_ALLOWED_ORIGINS=https://amgdigital.com \
  ...
```

### 2. Crear primer usuari SUPER_ADMIN
La base de dades és buida. Cal crear el primer compte per poder entrar al portal.

```bash
ssh -i ~/.ssh/id_ed25519 root@65.108.148.62
docker exec -it amg-postgres psql -U amg -d amg

-- Inserir SUPER_ADMIN (password: canviar després del primer login)
INSERT INTO tenants (id, name, email, status, created_at, updated_at)
VALUES (gen_random_uuid(), 'AMG Admin', 'admin@amgdigital.com', 'ACTIVE', now(), now());

INSERT INTO users (id, tenant_id, name, email, password_hash, role, active, created_at, updated_at)
VALUES (gen_random_uuid(), <tenant_id>, 'Admin', 'admin@amgdigital.com',
  '$2a$12$...', -- bcrypt del password
  'SUPER_ADMIN', true, now(), now());
```

O més fàcil: demanar a Claude que ho faci via l'endpoint de registre o directament a la BD.

---

## 🟡 Prioritat mitjana

### 3. Verificar CSS del portal
L'usuari va reportar que els estils no es veien correctament. Cal obrir el portal al navegador i revisar:
- Landing pública (`/ca`)
- Portal d'usuari (`/ca/portal`)
- Editor de landings (`/ca/portal/landings/[id]/edit`)

### 4. Verificar editor de landings (Mòdul 05 Factory)
Provar el flux complet:
1. Crear nova landing (`/portal/landings/new`) → seleccionar plantilla
2. Editor: afegir blocs, reordenar (drag & drop), canviar estils
3. Guardar esborrany → Publicar
4. Verificar que la landing pública es renderitza correctament

### 5. Mòdul 16 Onboarding
Guia ràpida post-registre per a nous tenants. Spec a `specs/16-onboarding.md`.
- Backend: no cal (és un flux frontend)
- Frontend: wizard de 3-4 passos després del primer login

### 6. GoCardless frontend
Backend complet a `/api/v1/gocardless`. Falta pàgina UI al portal per:
- Configurar mandat SEPA
- Veure estat de la domiciliació

### 7. Agents Telegram — configuració
```bash
# Afegir TELEGRAM_BOT_TOKEN al backend:
docker stop amg-backend && docker rm amg-backend
docker run -d --name amg-backend ... \
  -e TELEGRAM_BOT_TOKEN=<token_del_bot> \
  ...

# Registrar webhook a Telegram:
curl "https://api.telegram.org/bot<TOKEN>/setWebhook?url=https://amgdigital.com/api/v1/agents/telegram/webhook"
```
Falta també una pàgina UI per veure els agents actius i els vincles tenant-chat.

---

## 🔵 Millores futures

### 8. Flyway migrations
Substituir `SPRING_JPA_HIBERNATE_DDL_AUTO=update` per migracions Flyway versionades.
Fitxers SQL ja existeixen a `docs/migration-006-agents.sql` i `infra/postgres/`.

### 9. CI/CD automàtic
Ara el deploy és manual (git pull + docker build + docker restart).
Configurar Coolify per fer-ho automàticament en cada push a `main`.
- Coolify: http://65.108.148.62:8000 (antoniomasgut@gmail.com / desidia_10A2026)

### 10. Tests e2e nous mòduls
Els tests Playwright existents no cobreixen:
- Leads CRM
- Payments
- Prospecting
- Backup / InfraOps
- Editor de landings

---

## Estat del servidor (referència)

| Contenidor | Port | Credencials |
|------------|------|-------------|
| `amg-frontend` | 3000 | — |
| `amg-backend` | 8081 | JWT auth |
| `amg-postgres` | 5433 | amg / amg_db_2026 / db: amg |
| `amg-redis` | 6380 | pass: amg_redis_2026 |
| `coolify` | 8000 | antoniomasgut@gmail.com / desidia_10A2026 |

**SSH:** `ssh -i ~/.ssh/id_ed25519 root@65.108.148.62`  
**Codi al servidor:** `/opt/amg` (branca `main`)

---

## Mòduls completats ✅

01 Auth · 02 Vault · 03 Leads CRM · 04 Engine · 05 Factory · 06 Assets · 07 Billing · 08 FinOps · 09 Payments · 09b GoCardless (backend) · 10 Automations · 11 Ops & Health · 12 Prospecting · 13 i18n+SEO+RGPD · 14 Admin Frontend · 15 Demo · 17 Service Wizard · 18 Backup · 19 InfraOps · 20 Agents (backend)

## Millores de plataforma completades ✅

- **OpenAPI/Swagger:** Documentació interactiva d'API a `/swagger-ui.html` i `/v3/api-docs`
- **Sentry:** Monitoratge d'errors backend (Spring Boot) + frontend (Next.js)
- **Cobertura de tests:** 273 tests backend (53 de nous: Auth 9, Vault 6, Knowledge Base 25, Conversation 10, Scheduler 3)
