# Changelog de canvis funcionals

Registre de millores aplicades al codi que poden requerir actualització de les specs corresponents.

---

## 2026-06-09 — Revisió i millora del portal (sessió de QA)

### CI/CD (fora d'spec)
- Instal·lat runner self-hosted de GitHub Actions al servidor (`/opt/actions-runner/`)
- Workflow `.github/workflows/deploy.yml` migrat de `appleboy/ssh-action` a `runs-on: self-hosted`
- Motiu: firewall Hetzner bloquejava les IPs de GitHub Actions

---

### Tenants — Tab "Activitat de canals"
**Fitxers:** `frontend/src/app/[locale]/portal/admin/tenants/page.tsx`

**Canvis:**
- Afegida pestanya "Activitat de canals (30d)" a la pàgina `/portal/admin/tenants`
- Carrega en paral·lel (`Promise.all`) les stats dels tenants visibles via `getChannelUsageStats(tenantId)`
- Mostra taula amb: WA Twilio, WA Meta, Telegram, Email, Xat web, Tokens IA
- La pestanya s'activa via query param `?tab=canals`

**Spec afectada:** `specs/14-admin-frontend.md`, `specs/25-omnichannel-inbox.md`

---

### Dashboard — Link "Per tenant" corregit
**Fitxers:** `frontend/src/app/[locale]/portal/page.tsx`

**Canvis:**
- El link "Per tenant →" del bloc d'activitat de canals ara apunta a `/portal/admin/tenants?tab=canals`
- Abans anava a la llista genèrica de tenants

**Spec afectada:** `specs/21-dashboards.md`

---

### Pàgina Procés — Redisseny complet (8 → 6 passos)
**Fitxers:** `frontend/src/app/[locale]/portal/process/page.tsx`

**Canvis:**
- Redissenyada de 8 passos a 6 que reflecteixen el flux real de negoci:
  1. Configuració del sistema (API keys, sectors, plantilles)
  2. Captació de clients (prospecció + Meta Ads)
  3. Qualificació del lead (CRM + demo per sector + reunió)
  4. Pressupost i tancament (per fases NexeLocal)
  5. Implementació (tenant + wizard fases + agents IA)
  6. Monitoratge i backup
- Eliminats passos redundants: "Crear Tenant" i "Agents & Automatitzacions" com a passos independents
- Totes les dades ara són reals (cap text estàtic fals)

**Spec afectada:** `specs/15-demo.md` (flux de captació), `specs/17-service-setup-wizard.md`

---

### Nous endpoints globals al backend
**Fitxers backend:**
- `engine/api/EngineController.java` + `EngineService.java` + `EngineOrchestrator.java`
- `engine/domain/LandingRepository.java`
- `metaads/api/MetaAdsController.java`

**Endpoints nous:**
- `GET /api/v1/engine/admin/landings/summary` → `{total: long, published: long}` (SUPER_ADMIN)
- `GET /api/v1/meta-ads/global/summary` → `{configured: long, enabled: long}` (SUPER_ADMIN)

**Fitxers frontend:**
- `frontend/src/services/factory.ts` → `getLandingsGlobalSummary()`
- `frontend/src/services/meta-ads.ts` → `getMetaAdsGlobalSummary()`

**Spec afectada:** `specs/04-engine.md`, `specs/35-meta-ads-analytics.md`

---

### Prospecció — Paginació + filtres + selector font
**Fitxers backend:**
- `prospecting/api/ProspectingController.java`
- `prospecting/application/ProspectingService.java` + `ProspectingOrchestrator.java`
- `prospecting/domain/ProspectCampaignRepository.java`

**Fitxers frontend:**
- `frontend/src/services/prospecting.ts`
- `frontend/src/app/[locale]/portal/prospecting/page.tsx`

**Canvis backend:**
- `GET /prospecting/campaigns` ara retorna `Page<CampaignResponse>` (Spring Data)
- Paràmetres nous: `page` (def. 0), `size` (def. 20, màx 100), `status`
- Ordenació per `createdAt DESC`
- Filtre per `sector`, `location`, `status` via JPQL

**Canvis frontend:**
- Paginació amb controls anterior/següent i comptador total
- Filtre per estat: Totes / Pendents / Programades / En curs / Completades / Error
- Formulari de creació: selector de **Font** (Google Maps, Pàgines Grogues, Instagram, Manual) — abans hardcoded a Google Maps
- Botó "Programar" ara obre un dropdown amb opcions: cada dia / setmana / 2 setmanes / mes — abans hardcoded a 7 dies
- Columna "Font" afegida a la taula de campanyes

**Spec afectada:** `specs/12-prospecting.md`

---

## Pendent de revisió

Els canvis marcats com "Spec afectada" s'haurien d'incorporar a les specs corresponents si es vol mantenir la documentació alineada amb el codi real.

Specs candidates a actualitzar:
- `specs/12-prospecting.md` — paginació i nous filtres (canvi trencador del contracte de l'API)
- `specs/14-admin-frontend.md` — nova pestanya d'activitat de canals a tenants
- `specs/04-engine.md` — nou endpoint `/admin/landings/summary`
- `specs/21-dashboards.md` — link "Per tenant" corregit
