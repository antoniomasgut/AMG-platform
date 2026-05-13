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

### 2. Plataforma AMG (en planificació)

El codi del backend/frontend/infra **encara no existeix**. Les carpetes `specs/`, `backend/`, `frontend/` i `infra/` estan pendents de crear.

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

### Frontend
- Next.js 14 App Router, Tailwind CSS
- next-intl — 4 idiomes: `ca`, `es`, `en`, `de`
- Zod (validació), Zustand (estat client), React Query (servidor)
- Problema habitual d'hidratació: usar `useTranslations` al server component i `next-intl/client` al client component

### Infra
- Hetzner VPS, Coolify + Docker, Traefik (SSL via Let's Encrypt)
- n8n self-hosted, alertes via Telegram Bot

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

## Mòduls de la plataforma (tots pendents)

| # | Mòdul | Estat | Spec |
|---|-------|-------|------|
| 01 | Auth (JWT + RBAC) | ✅ Completat | specs/01-auth.md |
| 02 | Vault (AES-256 credentials) | ✅ Completat | specs/02-vault.md |
| 03 | Leads CRM | ✅ Completat | specs/03-leads.md |
| 04 | Engine (Landing Renderer) | ⬜ Pendent | specs/04-engine.md |
| 05 | Factory (Landing Editor) | ⬜ Pendent | specs/05-factory.md |
| 06 | Assets (Storage) | ⬜ Pendent | specs/06-assets.md |
| 07 | Billing (Pressupostos) | ✅ Completat | specs/07-billing.md |
| 08 | FinOps (Holded) | ⬜ Pendent | specs/08-finops.md |
| 09 | Payments (Stripe) | ⬜ Pendent | specs/09-payments.md |
| 10 | Automations (n8n) | ⬜ Pendent | specs/10-automations.md |
| 11 | Ops & Health | ⬜ Pendent | specs/11-ops.md |
| 12 | Prospecting | ⬜ Pendent | specs/12-prospecting.md |
| 13 | i18n + SEO + RGPD | ✅ Completat | specs/13-i18n-seo-rgpd.md |
| 14 | Admin Frontend (CRUD usuaris/tenants) | ✅ Completat | specs/14-admin-frontend.md |

---

## Monitoring & Alertes

- Health checks cada 30s per a webs, Docker i n8n
- Threshold d'alerta: 3 minuts de fallada consecutiva (6 checks)
- Canal: Telegram — un sol grup per tot, cooldown 30 min entre alertes
- Format: `🔴 [CLIENT] · [SERVEI] · [ERROR] · fa X min`
- Recuperació: `✅ [CLIENT] · [SERVEI] recuperat · temps caiguda: X min`

---

## Model de preus (referència per Billing)

| Servei | Setup | Mensual |
|--------|-------|---------|
| WhatsApp Business | 50€ | — |
| Micro-landing | 30€ | — |
| Landing Pro | 80€ | — |
| Bot IA bàsic | 40€ | — |
| Bot IA avançat (RAG) | 100€ | — |
| Domini + DNS + SSL | 60€ | — |
| Automatització bàsica | 20€/u | — |
| **Pla Bàsic** | — | 29€ |
| **Pla Intermedi** | — | 49€ |
| **Pla Avançat** | — | 99€ |
