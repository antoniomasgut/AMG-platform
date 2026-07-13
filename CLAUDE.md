# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

# AMG Digitalització · Workspace Principal

**Projecte:** Plataforma SaaS multi-tenant per gestionar webs i automatitzacions de negocis locals a Mallorca
**Idioma del codi:** Anglès (variables, mètodes, classes)
**Idioma dels comentaris:** Català

---

## Contingut actual del repositori

Aquest repositori és un monorepo que conté dos nivells:

### 1. Kits operatius (ja funcionals)

Cada `kit-*/` és un **espai de treball autònom de Claude Code** amb el seu propi `CLAUDE.md`, `INSTRUCCIONES.md` i skills. Quan obres una carpeta de kit, Claude llegeix el CLAUDE.md local i segueix el seu comportament específic.

| Carpeta | Funció |
|---------|--------|
| `kit-web-scrolling/` | Genera webs premium amb animacions de scroll |
| `kit-instagram-web/` | Converteix perfil d'Instagram en web de marca personal |
| `kit-automatizaciones-n8n/` | Crea i gestiona workflows de n8n (amb MCP opcional) |
| `kit-auditoria-seo/` | Auditoria SEO completa → informe HTML |
| `kit-auditoria-meta-ads/` | Audita anuncis Meta Ads + landing pages |
| `kit-auditoria-negocio/` | Auditoria general de negoci |
| `kit-dashboard-facturas/` | Llegeix PDFs de factures i genera dashboard financer |
| `kit-prospeccion/` | Cerca clients potencials per sector/ciutat |
| `kit-extension-chrome/` | Crea extensions Chrome (Manifest V3) des de zero |
| `kit-skill-creator/` | Genera noves skills per a Claude Code |

### 2. Plataforma AMG (en desenvolupament)

El codi del backend, frontend i infra ja està creat i en evolució constant:

| Carpeta | Contingut |
|---------|-----------|
| `specs/` | Especificacions dels mòduls (01-auth, 02-vault, 07-billing, perfils-cataleg) |
| `backend/` | Spring Boot 3 + Java 21 amb arquitectura hexagonal |
| `frontend/` | Next.js 14 App Router + Tailwind + next-intl (4 idiomes) |
| `infra/` | Docker, Coolify, Traefik |

---

## Estructura de cada kit

```
kit-nom/
├── CLAUDE.md           ← comportament d'inici i regles del kit
├── INSTRUCCIONES.md    ← guia d'usuari final
└── .claude/
    └── skills/
        └── NN-nom.md   ← skill que s'activa automàticament
```

Quan treballis dins un kit, llegeix primer el seu `CLAUDE.md` local — substitueix qualsevol instrucció genèrica.

---

## Stack tècnic planificat (plataforma AMG)

### Backend
- Spring Boot 3 + Java 21
- Spring Data JPA + Hibernate, Spring Security + JWT
- AES-256 (Jasypt) per a credencials de clients
- PostgreSQL (schema per tenant), Redis (JWT + rate limiting), MinIO (assets)
- Build: Maven
- **Email transaccional**: Brevo EU REST API (`POST https://api.brevo.com/v3/smtp/email`, header `api-key:`). Clau: `BREVO_API_KEY` a `SystemConfigService`. **NO** usar `JavaMailSender` ni Resend.
- **`jakarta.mail`** (standalone `org.eclipse.angus:angus-mail`) — ONLY per al verificador SMTP del Vault; no és per a email transaccional
- **OpenAPI/Swagger**: `springdoc-openapi-starter-webmvc-ui` — accés a `/swagger-ui.html` i `/v3/api-docs`
- **Sentry**: `sentry-spring-boot-starter-jakarta` (backend) + `@sentry/nextjs` (frontend) — captura d'errors no controlats

### Frontend
- Next.js 14 App Router, Tailwind CSS
- next-intl — 4 idiomes: `ca`, `es`, `en`, `de`
- Zod (validació), Zustand (estat client), React Query (servidor)
- Problema habitual d'hidratació: usar `useTranslations` al server component i `next-intl/client` al client component

### Infra
- Hetzner VPS, Coolify + Docker, Traefik (SSL via Let's Encrypt)
- n8n self-hosted, alertes via Telegram Bot

### Stack de comunicació (canals d'agents)
| Canal | Proveïdor | Clau(s) necessàries |
|-------|-----------|---------------------|
| WhatsApp Business | Twilio REST API | `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_WHATSAPP_FROM` |
| WhatsApp Business (Meta Cloud) | Meta Graph API | `whatsappMetaPhoneNumberId` per tenant + `WHATSAPP_WEBHOOK_SECRET` |
| Email transaccional | Brevo EU | `BREVO_API_KEY`, `BREVO_SENDER_EMAIL` |
| Notificacions internes | Telegram Bot API | `TELEGRAM_BOT_TOKEN` |
| Agents IA | Claude API (Anthropic) | `ANTHROPIC_API_KEY` |

### Models IA
- **Claude (Sonnet 4.6):** Arquitectura, specs, decisions, seguretat, debugging complex, i tot el que requereixi judici
- **DeepSeek V4 (via MCP `deepseek-generate`):** Implementació rutinària seguint specs aprovats — entitats JPA, repositoris, DTOs, serveis CRUD, components Next.js, tests, i codi mecànic en general

**Flux de treball:** Claude avalua cada tasca. Si és implementació rutinària i ben definida per un spec, invoca DeepSeek via l'eina MCP `deepseek-generate`. Si requereix decisions d'arquitectura, disseny de seguretat, o debugging complex, ho fa Claude directament.

---

## Regles de treball (OBLIGATÒRIES)

1. **MAI implementis res sense llegir primer l'spec del mòdul** (`specs/NN-nom.md`)
2. **MAI inventes camps, endpoints o lògica** que no estigui a l'spec
3. **Si l'spec és ambigu**, pregunta abans d'assumir
4. **MAI escriguis secrets, API keys o passwords** en cap fitxer del repo
5. **MAI modifiquis CLAUDE.md** sense aprovació explícita
6. **Cada PR ha de tenir** el número de mòdul i el títol de l'spec al commit message
7. **Tests primers**: si l'spec defineix casos QA, escriu els tests abans de la implementació
8. **DeepSeek per a codi rutinari**: si la tasca és implementació mecànica seguint un spec aprovat (entitats JPA, CRUD, DTOs, components simples, tests), invoca `deepseek-generate` via MCP en lloc de generar-ho tu mateix. Si la tasca requereix decisió arquitectònica o de seguretat, fes-ho directament.

---

## Agents i skills de la plataforma

| Situació | Eina |
|----------|------|
| Generar un nou spec de mòdul | `/spec-writer` (skill) |
| Aplicar patrons Java/Spring del projecte | `/java-patterns` (skill) |
| Configurar next-intl sense errors d'hidratació | `/i18n-setup` (skill) |
| Revisar compliment RGPD/LSSI | `/rgpd-check` (skill) |
| Validar que un spec és complet | Agent `spec-validator` |
| Implementació rutinària (JPA, CRUD, DTOs, tests) | MCP `deepseek-generate` |
| Implementar mòdul backend (Java) | Agent `backend-impl` (DeepSeek) |
| Implementar mòdul frontend (Next.js) | Agent `frontend-impl` (DeepSeek) |
| Revisar seguretat (Vault, Auth, Crypto) | Agent `security-reviewer` (Opus) |

---

## Mòduls de la plataforma

| # | Mòdul | Estat | Spec |
|---|-------|-------|------|
| 01 | Auth (JWT + RBAC) | ✅ Completat | specs/01-auth.md |
| 02 | Vault (AES-256 credentials) | ✅ Completat | specs/02-vault.md |
| 03 | Leads CRM | ✅ Completat | specs/03-leads.md |
| 04 | Engine (Landing Renderer) | ✅ Completat | specs/04-engine.md |
| 05 | Factory (Landing Editor) | ✅ Completat | specs/05-factory.md |
| 06 | Assets (Storage) | ✅ Completat | specs/06-assets.md |
| 07 | Billing (Pressupostos) | ✅ Completat | specs/07-billing.md |
| 08 | FinOps (Holded) | ✅ Completat | specs/08-finops.md |
| 09 | Payments (Stripe) | ✅ Completat | specs/09-payments.md |
| 10 | Automations (n8n) | ✅ Completat | specs/10-automations.md |
| 11 | Ops & Health | ✅ Completat | specs/11-ops.md |
| 12 | Prospecting | ✅ Completat | specs/12-prospecting.md |
| 13 | i18n + SEO + RGPD | ✅ Completat | specs/13-i18n-seo-rgpd.md |
| 14 | Admin Frontend (CRUD usuaris/tenants) | ✅ Completat | specs/14-admin-frontend.md |
| 15 | Demo (tenant demo automàtic) | ✅ Completat | specs/15-demo.md |
| 16 | Onboarding (guia ràpida post-registre) | ✅ Completat | specs/16-onboarding.md |
| 17 | Service Setup Wizard (assistent implementació) | ✅ Completat | specs/17-service-setup-wizard.md |
| 18 | Backup Operacional (còpia seguretat dades a GCS) | ✅ Completat | specs/18-backup-operacional.md |
| 09b | GoCardless (domiciliació SEPA automàtica) | ✅ Completat | specs/09b-gocardless.md |
| 19 | InfraOps (monitorització servidor + escalat) | ✅ Completat | specs/19-infraops.md |
| 20 | Agents Autònoms (Telegram, substitueix n8n) | ✅ Completat | specs/20-agents.md |
| 21 | Dashboards per rol (SUPER_ADMIN / ADMIN / CLIENT) | ✅ Completat | specs/21-dashboards.md |
| 22 | Sector Pricing (model NexeLocal F1-F5) | ✅ Completat | specs/22-sector-pricing.md |
| 23 | Domain Reseller (registre i gestió de dominis) | ✅ Completat | specs/23-domain-reseller.md |
| 24 | Agent Activation Flows (activació + canals + mode) | ✅ Completat | specs/24-agent-activation-flows.md |
| 25 | Omnichannel Inbox (converses unificades) | ✅ Completat | specs/25-omnichannel-inbox.md |
| 26 | RAG Knowledge Base (base de coneixement IA) | ✅ Completat (backend + frontend) | specs/26-rag-knowledge-base.md |
| 27 | WhatsApp Business API (Meta Cloud, per tenant) | ✅ Completat | specs/27-whatsapp-business.md |
| 28 | NexeLocal Service Configs (config per fase i sector) | ✅ Completat | — |
| 29 | Web Hosting & Import (Bàsic + Pro) | ✅ Completat | specs/29-web-hosting-import.md |
| 30 | Landing Chat Widget | ✅ Completat | specs/30-landing-chat-widget.md |
| 31 | Agent Feature Toggles | ✅ Completat | specs/31-agent-feature-toggles.md |
| 32 | Tenant Notifications (Telegram + Email) | ✅ Completat | specs/32-tenant-notifications.md |
| 33 | Absence & Reschedule Cascade | ✅ Completat | specs/33-absence-reschedule.md |
| 34 | Meta Lead Ads | ✅ Completat | specs/34-meta-lead-ads.md |
| 35 | Meta Ads Analytics | ✅ Completat | specs/35-meta-ads-analytics.md |
| 36 | Meta Ads Management | ✅ Completat | specs/36-meta-ads-management.md |
| 37 | Tenant Document Builder | ✅ Completat (backend) · ✅ Frontend (nou) | specs/37-tenant-document-builder.md |
| 38 | Tenant Storage (MinIO + S3) | ✅ Completat | specs/38-tenant-storage.md |
| 39 | System Config | ✅ Completat | specs/39-system-config.md |
| 40 | Google Workspace (Drive, Gmail, Calendar, Sheets) | ✅ Completat | specs/40-google-workspace.md |
| 41 | Agent Tool Calling (function calling + auto-quota) | ✅ Completat | specs/41-agent-tool-calling.md |
| 42 | Meta Cloud API OAuth Onboarding (per-tenant WABA connect) | ✅ Completat | specs/42-meta-oauth-onboarding.md |
| 43 | Communication Templates (WhatsApp + Email per acció+idioma) | ✅ Completat | specs/43-communication-templates.md |
| 44 | Secure Document Delivery (lliurament segur, RGPD Art. 9) | ✅ Completat | specs/44-secure-document-delivery.md |
| 45 | Post-Budget Booking (F3 → F2, reserva automàtica post-acceptació) | ✅ Completat | specs/45-post-budget-booking.md |
| 46 | Phase Integration Matrix (fases independents + integrades per sector) | ✅ Completat | specs/46-phase-integration-matrix.md |
| 49 | Landing SEO Vertical (pàgines pilar + Schema + fix localhost) | ✅ Completat | specs/49-landing-seo-vertical.md |
| 50 | Sales & Implementation Pipeline (prospecció → activació amb etapes, SLAs i onboarding) | ✅ Completat | specs/50-sales-pipeline.md |
| 51 | Agency Multichannel Contact (Widget IA + WhatsApp Business per a la landing d'AMG) | 🔧 Parcial (widget+WA personal actius; WABA pendent config) | specs/51-agency-multichannel-contact.md |
| 52 | Social Publisher (publicació multi-xarxa via Telegram + IA) | ✅ Completat | specs/52-social-publisher.md |
| 53 | F3 Cobrament Online (Stripe del tenant a l'acceptació de documents) | ✅ Completat | specs/53-f3-cobrament-online.md |

---

## Monitoring & Alertes

- Health checks cada 30s per a webs, Docker i n8n
- Threshold d'alerta: 3 minuts de fallada consecutiva (6 checks)
- Canal: Telegram — un sol grup per tot, cooldown 30 min entre alertes
- Format: `🔴 [CLIENT] · [SERVEI] · [ERROR] · fa X min`
- Recuperació: `✅ [CLIENT] · [SERVEI] recuperat · temps caiguda: X min`

---

## Model de preus (referència per Billing)

Cada fase té un cost de **setup individual** (varia per `businessSize`: AUTONOMO/PETIT/MITJA) i una quota mensual. Les fases (F1–F5) **no són acumulatives**: el client pot contractar qualsevol combinació. El mensual total = suma dels tiers en ordre (1a fase = priceF1, 2a = priceF2...) independentment de quines fases específiques. El setup total = suma de `setupFn[ordinal]` per cada fase seleccionada (F1=`setupPrice`, F2..F5=`setupF2..F5` de `sector_pricing`). La tarifa d'enginyeria/hora: **50 €/h** (`ENGINEER_HOURLY_RATE`). Veure `specs/serveis-cataleg.md` i `specs/22-sector-pricing.md`.

**Landings i dominis (serveis independents de les fases):**

| Servei | Setup | Mensual |
|--------|-------|---------|
| Micro-landing | 30€ | 9€/mes | Blocs bàsics + formulari de contacte (email) |
| Landing Pro | 80€ | 15€/mes | Tots els blocs + formulari + botó WhatsApp + chat widget IA (requereix F1) |
| Web Professional (sense IA) Estàndard | 390€ | 15€/mes | Web a mida en català, multi-secció, contingut redactat, SEO, form + WhatsApp. Sense agent IA |
| Web Professional (sense IA) Plus | 590€ | 15€/mes | Multi-pàgina (5-8), més contingut, galeria, SEO treballat. Sense agent IA |
| Domini autogestionat | 30€ | — (client gestiona renovació) |
| Domini gestionat | 60€ (inclou 1r any) | 15€/any (renovació) |

**Serveis addicionals (setup únic):**

| Servei | Setup |
|--------|-------|
| WhatsApp Business API | 50€ |
| Automatització bàsica (n8n) | 20€ |
| Automatització avançada (n8n) | 60€ |
| Bot IA bàsic | 40€ |
| Bot IA avançat (RAG) | 100€ |
| SMTP Corporatiu | 25€ |
| Google Analytics | 25€ |
| Social Publisher (Instagram + Facebook + Google Business + galeria via Telegram IA) | 50€ |
| Content Planner (Mòdul 58 · planning mensual + brief automation) | 0€ (inclòs en F1, requereix Social Publisher) |

**Gestió mensual de xarxes socials (servei gestionat, en català — no autoservei):**

| Nivell | Mensual | Inclou |
|--------|---------|--------|
| Bàsic | 69€/mes | 4 publicacions/mes, 1-2 xarxes, contingut IA en català |
| Gestió | 139€/mes | 8 publicacions/mes, multi-xarxa (IG+FB+Google), gestió de comentaris/DMs, informe |
| Premium | 199€/mes | 12 publicacions/mes, totes les xarxes + campanyes estacionals |

*Alternativa autoservei (Via D · autonomia): el client ho gestiona amb el Content Planner; només agent F1 (59€/mes), sense quota de gestió.*

---

## Notes de producció (taules creades manualment)

Hibernate a producció usa `ddl-auto: validate` — les taules noves **cal crear-les manualment** a la BD de producció (`ssh root@65.108.148.62 'docker exec amg-postgres psql -U amg -d amg -c "..."'`):

| Taula | Creat | SQL resumit |
|-------|-------|-------------|
| `whatsapp_waba_configs` | 2026-05-24 | Entitat Mòdul 27 |
| `sector_phases` | 2026-05-26 | Catàleg de fases per sector (105 files via SQL) |
| `nexe_service_configs` | 2026-05-28 | `(tenant_id UUID, service_key VARCHAR(30), config_json TEXT, updated_at TIMESTAMPTZ, PK(tenant_id, service_key))` |
| `websites` | V23 | `(id UUID PK, tenant_id UUID, type VARCHAR(20), status VARCHAR(20), domain VARCHAR(255), container_name VARCHAR(100), storage_bytes BIGINT, review_notes TEXT, reviewed_by UUID, reviewed_at TIMESTAMPTZ, deployed_at TIMESTAMPTZ, created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ)` |
| `storage_provider_configs` | V21 | `(tenant_id UUID, provider_key VARCHAR(30), config_json TEXT, is_active BOOLEAN, updated_at TIMESTAMPTZ, PK(tenant_id, provider_key))` |
| `google_connections` | V22 | `(id UUID PK, tenant_id UUID, google_account_email VARCHAR, google_user_id VARCHAR, encrypted_access_token TEXT, encrypted_refresh_token TEXT, token_expires_at TIMESTAMPTZ, is_active BOOLEAN)` |
| `google_module_configs` | V22 | `(tenant_id UUID PK, drive_enabled BOOLEAN, gmail_enabled BOOLEAN, calendar_enabled BOOLEAN, sheets_enabled BOOLEAN, drive_folder_id VARCHAR)` |
| `oauth_states` | V22 | `(id UUID PK, tenant_id UUID, state_token VARCHAR(64) UNIQUE, redirect_uri VARCHAR(500), requested_scopes TEXT, expires_at TIMESTAMPTZ)` |
| `stripe_configs` (ALTER) | V44 Flyway | `ADD COLUMN stripe_customer_id, stripe_payment_method_id, pm_last_four, pm_brand, pm_exp_month, pm_exp_year` |
| `model_pricing` | V49 Flyway | `(model_name VARCHAR(100) PK, provider VARCHAR(50), input_cost_micros BIGINT, output_cost_micros BIGINT, markup_percent INTEGER DEFAULT 20, client_input_micros BIGINT, client_output_micros BIGINT, updated_at TIMESTAMPTZ)` |
| `booking_tokens` (ALTER) | V66 Flyway | `lead_id` nullable + `ADD COLUMN source_document_id UUID, recipient_phone VARCHAR(30), recipient_name VARCHAR(255), booking_label VARCHAR(100)` |
| `tenant_document_preferences` (ALTER) | V66 Flyway | `ADD COLUMN auto_booking_on_accept BOOLEAN NOT NULL DEFAULT true` |

**NexeLocal Service Configs** (`nexe_service_configs`):
- Service keys: `AGENDA`, `PRESSUPOSTOS`, `FIDELITZACIO`, `EQUIP`
- Endpoint: `GET/PUT /api/v1/nexe/tenants/{tenantId}/configs/{serviceKey}`
- Frontend: `/portal/admin/tenants/[id]/nexe/[service]` (agenda / pressupostos / fidelitzacio / equip)
- F2 Agenda: 4 modes per sector (`appointment` / `inspection` / `vehicle` / `meeting`)
- F3 Pressupostos: 2 modes per sector (`formal` / `pricelist`)
- F4 Seguiment: `FIDELITZACIO` (service key intern sense canvi)
- F5 Alertes & Equip: usa grup de Telegram compartit (`telegram_group_id`), no Telegram per-membre
