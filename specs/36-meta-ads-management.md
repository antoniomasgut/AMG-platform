# Mòdul 36 — Meta Ads Management (Gestió de Campanyes)

## Objectiu

Permetre als tenants crear, gestionar i optimitzar campanyes de Facebook/Instagram Ads directament des del portal AMG, sense necessitat de sortir a Meta Ads Manager. Cobreix el cicle complet: campanya → conjunt d'anuncis (ad set) → anunci (ad creatives).

**Prerequisit:** Mòdul 35 (Meta Ads Analytics) — reutilitza la taula `meta_ads_configs` i el token d'accés.

---

## Diferència entre Mòdul 35 i 36

| Mòdul 35 | Mòdul 36 |
|----------|----------|
| Lectura: sync de despesa | Escriptura: crear/modificar campanyes |
| Token `ads_read` | Token `ads_management` (més permisos) |
| System User Token global | OAuth per tenant (o System User amb permisos delegats) |
| Ja implementat | Pendent |

---

## Flux complet

```
Portal AMG (tenant)
    │
    ├── 1. Autoritzar Meta (OAuth o System User Token)
    ├── 2. Crear Campanya (objectiu, pressupost, dates)
    ├── 3. Crear Ad Set (públic, ubicació, oferta)
    ├── 4. Crear Ad (creatiu: imatge/vídeo, text, CTA, URL)
    └── 5. Publicar → Meta valida i activa
            │
            ▼
    Meta Marketing API
    POST /v19.0/act_{AD_ACCOUNT_ID}/campaigns
    POST /v19.0/act_{AD_ACCOUNT_ID}/adsets
    POST /v19.0/act_{AD_ACCOUNT_ID}/adcreatives
    POST /v19.0/act_{AD_ACCOUNT_ID}/ads
```

---

## Arquitectura backend

### Paquets nous

```
metaads/ (ampliar el paquet existent del Mòdul 35)
  domain/
    AdCampaign.java          ← entitat local (mirall de Meta)
    AdCampaignRepository.java
    AdSet.java
    AdSetRepository.java
    Ad.java
    AdRepository.java
    AdCreative.java
    AdCreativeRepository.java
    MetaOAuthToken.java      ← token OAuth per tenant (si s'usa OAuth)
    MetaOAuthTokenRepository.java
  application/
    MetaAdsCampaignService.java    ← CRUD de campanyes
    MetaAdsAdSetService.java       ← CRUD d'ad sets
    MetaAdsCreativeService.java    ← upload d'imatges + creació de creatius
    MetaAdsPublishService.java     ← orquestrador de publicació
    MetaAdsStatusSyncJob.java      ← sync d'estats (ACTIVE/PAUSED/etc.)
  api/
    MetaAdsCampaignController.java
    MetaAdsAdSetController.java
    MetaAdsAdController.java
    dto/
      CreateCampaignRequest.java
      CreateAdSetRequest.java
      CreateAdRequest.java
      CampaignResponse.java
      AdSetResponse.java
      AdResponse.java
```

---

## Model de dades

### Taula `ad_campaigns` (mirall local de Meta)

```sql
CREATE TABLE ad_campaigns (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID NOT NULL,
    meta_campaign_id VARCHAR(50),          -- ID a Meta un cop publicat
    name             VARCHAR(200) NOT NULL,
    objective        VARCHAR(50) NOT NULL, -- LEAD_GENERATION, CONVERSIONS, REACH, etc.
    status           VARCHAR(30) NOT NULL, -- DRAFT, ACTIVE, PAUSED, ARCHIVED, ERROR
    daily_budget     NUMERIC(10,2),        -- en cèntims a Meta, en euros aquí
    lifetime_budget  NUMERIC(10,2),        -- alternatiu a daily_budget
    start_time       TIMESTAMPTZ,
    stop_time        TIMESTAMPTZ,
    meta_error       TEXT,                 -- missatge d'error de Meta si falla
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ad_campaigns_tenant ON ad_campaigns (tenant_id);
```

### Taula `ad_sets`

```sql
CREATE TABLE ad_sets (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id         UUID NOT NULL REFERENCES ad_campaigns(id) ON DELETE CASCADE,
    tenant_id           UUID NOT NULL,
    meta_adset_id       VARCHAR(50),
    name                VARCHAR(200) NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    -- Pressupost (hereda de la campanya o en defineix un de propi)
    daily_budget        NUMERIC(10,2),
    -- Optimització
    optimization_goal   VARCHAR(50),    -- LEAD_GENERATION, LINK_CLICKS, REACH, etc.
    billing_event       VARCHAR(50),    -- IMPRESSIONS, LINK_CLICKS, etc.
    bid_amount          NUMERIC(10,2),  -- puja manual (opcional)
    -- Públic
    targeting_json      TEXT,           -- JSON complet de targeting de Meta
    age_min             INT,
    age_max             INT,
    genders             VARCHAR(20),    -- ALL, MALE, FEMALE
    geo_locations_json  TEXT,           -- JSON de localitzacions
    interests_json      TEXT,           -- JSON d'interessos (flex targeting)
    -- Ubicació
    publisher_platforms TEXT,           -- facebook,instagram
    placements_json     TEXT,
    -- Dates
    start_time          TIMESTAMPTZ,
    stop_time           TIMESTAMPTZ,
    meta_error          TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ad_sets_campaign ON ad_sets (campaign_id);
CREATE INDEX idx_ad_sets_tenant ON ad_sets (tenant_id);
```

### Taula `ad_creatives`

```sql
CREATE TABLE ad_creatives (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID NOT NULL,
    meta_creative_id     VARCHAR(50),
    name                 VARCHAR(200),
    -- Contingut
    headline             VARCHAR(255),   -- títol de l'anunci
    body                 VARCHAR(600),   -- text principal
    description          VARCHAR(255),   -- descripció sota el link
    call_to_action       VARCHAR(50),    -- LEARN_MORE, SIGN_UP, GET_QUOTE, etc.
    link_url             VARCHAR(500),   -- URL de destinació
    -- Imatge (pujada a MinIO, enviada a Meta per hash)
    image_asset_id       UUID,           -- referència a assets (Mòdul 6)
    meta_image_hash      VARCHAR(100),   -- hash que retorna Meta en pujar la imatge
    -- Vídeo (opcional)
    meta_video_id        VARCHAR(50),
    -- Formulari de Lead Ads natiu (opcional, alternatiu a link_url)
    meta_lead_form_id    VARCHAR(50),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Taula `ads`

```sql
CREATE TABLE ads (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    adset_id         UUID NOT NULL REFERENCES ad_sets(id) ON DELETE CASCADE,
    tenant_id        UUID NOT NULL,
    meta_ad_id       VARCHAR(50),
    creative_id      UUID REFERENCES ad_creatives(id),
    name             VARCHAR(200),
    status           VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    meta_error       TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Taula `meta_oauth_tokens` (si s'usa OAuth per tenant)

```sql
CREATE TABLE meta_oauth_tokens (
    tenant_id         UUID PRIMARY KEY,
    access_token      TEXT NOT NULL,          -- xifrat AES-256
    token_type        VARCHAR(20),
    expires_at        TIMESTAMPTZ,
    scope             TEXT,                    -- permisos concedits
    meta_user_id      VARCHAR(50),
    ad_account_id     VARCHAR(50),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

---

## Autorització: dues opcions

### Opció A — System User Token (recomanada per AMG)

AMG crea un System User al Meta Business Manager amb accés als comptes d'anuncis dels clients. Un únic token global amb `ads_management`.

**Pros:**
- Sense flux OAuth per tenant
- Token de llarga durada (no caduca si es renova periòdicament)
- Control centralitzat des de Meta Business Manager

**Contres:**
- AMG necessita ser administrador del Business Manager del client
- Menys aïllament (un token per a tots)

**Configuració a SystemConfig:**
- `META_ADS_MANAGEMENT_TOKEN` — System User token amb `ads_management`
- `META_ADS_APP_ID` — App ID de la Facebook App
- `META_ADS_APP_SECRET` — App Secret (per verificar signatures)

### Opció B — OAuth per tenant

Cada tenant autoritza amb el seu propi compte de Meta.

**Flux OAuth:**
```
1. Tenant clica "Connectar Meta"
2. Portal redirigeix → Meta OAuth (scope: ads_management, ads_read, pages_read_engagement)
3. Meta redirigeix de tornada → /api/v1/meta-ads/oauth/callback?code=XXX&state=tenantId
4. Backend intercanvia code per access_token (short-lived)
5. Intercanvia per long-lived token (GET /oauth/access_token?grant_type=fb_exchange_token)
6. Guarda xifrat a meta_oauth_tokens
```

**Endpoints OAuth:**
```
GET /api/v1/meta-ads/oauth/authorize?tenantId={tenantId}
GET /api/v1/meta-ads/oauth/callback?code=XXX&state={tenantId}
DELETE /api/v1/meta-ads/oauth/revoke/{tenantId}
```

**Recomanació:** Comença amb l'Opció A (més simple). Migra a Opció B si es necessita aïllament per tenant o si els clients gestionen els seus propis Business Managers.

---

## API REST

### Campanyes

| Mètode | URL | Descripció |
|--------|-----|------------|
| `GET` | `/api/v1/meta-ads/tenants/{tid}/campaigns` | Llista campanyes |
| `POST` | `/api/v1/meta-ads/tenants/{tid}/campaigns` | Crea campanya (estat DRAFT) |
| `GET` | `/api/v1/meta-ads/tenants/{tid}/campaigns/{id}` | Detall campanya |
| `PATCH` | `/api/v1/meta-ads/tenants/{tid}/campaigns/{id}` | Edita (nom, pressupost, dates) |
| `POST` | `/api/v1/meta-ads/tenants/{tid}/campaigns/{id}/publish` | Publica a Meta |
| `POST` | `/api/v1/meta-ads/tenants/{tid}/campaigns/{id}/pause` | Pausa a Meta |
| `POST` | `/api/v1/meta-ads/tenants/{tid}/campaigns/{id}/resume` | Repren a Meta |
| `DELETE` | `/api/v1/meta-ads/tenants/{tid}/campaigns/{id}` | Arxiva a Meta |

### Ad Sets

| Mètode | URL | Descripció |
|--------|-----|------------|
| `GET` | `/api/v1/meta-ads/tenants/{tid}/campaigns/{cid}/adsets` | Llista ad sets |
| `POST` | `/api/v1/meta-ads/tenants/{tid}/campaigns/{cid}/adsets` | Crea ad set |
| `PATCH` | `/api/v1/meta-ads/tenants/{tid}/campaigns/{cid}/adsets/{id}` | Edita |
| `DELETE` | `/api/v1/meta-ads/tenants/{tid}/campaigns/{cid}/adsets/{id}` | Arxiva |

### Creatius i Anuncis

| Mètode | URL | Descripció |
|--------|-----|------------|
| `POST` | `/api/v1/meta-ads/tenants/{tid}/creatives` | Upload imatge + crea creatiu |
| `GET` | `/api/v1/meta-ads/tenants/{tid}/creatives` | Llista creatius |
| `POST` | `/api/v1/meta-ads/tenants/{tid}/adsets/{sid}/ads` | Crea anunci (associa creatiu a ad set) |
| `PATCH` | `/api/v1/meta-ads/tenants/{tid}/adsets/{sid}/ads/{id}` | Edita |

### Eines

| Mètode | URL | Descripció |
|--------|-----|------------|
| `GET` | `/api/v1/meta-ads/targeting/interests?q={query}` | Cerca interessos de Meta |
| `GET` | `/api/v1/meta-ads/targeting/locations?q={query}` | Cerca localitzacions |
| `GET` | `/api/v1/meta-ads/tenants/{tid}/pages` | Pàgines de Facebook del tenant |

---

## Meta Marketing API — cridades principals

### 1. Crear campanya

```
POST /v19.0/act_{AD_ACCOUNT_ID}/campaigns
{
  "name": "Juliol 2026 - Estetica Mireia",
  "objective": "OUTCOME_LEADS",
  "status": "PAUSED",
  "special_ad_categories": []
}
→ { "id": "23851234567890" }
```

**Objectius suportats (selecció per AMG):**
| Valor Meta | Descripció |
|-----------|------------|
| `OUTCOME_LEADS` | Generació de leads (formulari natiu o landing) |
| `OUTCOME_TRAFFIC` | Tràfic a la web |
| `OUTCOME_AWARENESS` | Reconeixement de marca |
| `OUTCOME_ENGAGEMENT` | Interaccions (m'agrada, comentaris) |

### 2. Crear Ad Set

```
POST /v19.0/act_{AD_ACCOUNT_ID}/adsets
{
  "name": "Dones 25-45 Palma",
  "campaign_id": "23851234567890",
  "daily_budget": 1000,          ← en cèntims (1000 = 10€)
  "optimization_goal": "LEAD_GENERATION",
  "billing_event": "IMPRESSIONS",
  "targeting": {
    "age_min": 25,
    "age_max": 45,
    "genders": [2],              ← 1=homes, 2=dones
    "geo_locations": {
      "cities": [{ "key": "710347", "name": "Palma de Mallorca", "radius": 25, "distance_unit": "kilometer" }]
    },
    "flexible_spec": [
      { "interests": [{ "id": "6003349442621", "name": "Beauty" }] }
    ]
  },
  "publisher_platforms": ["facebook", "instagram"],
  "status": "PAUSED",
  "start_time": "2026-07-01T00:00:00+0200"
}
```

### 3. Pujar imatge i crear creatiu

```
# Pas 1: Upload imatge (multipart)
POST /v19.0/act_{AD_ACCOUNT_ID}/adimages
→ { "images": { "filename.jpg": { "hash": "abc123..." } } }

# Pas 2: Crear creatiu
POST /v19.0/act_{AD_ACCOUNT_ID}/adcreatives
{
  "name": "Creative - Estiu 2026",
  "object_story_spec": {
    "page_id": "123456789012345",
    "link_data": {
      "image_hash": "abc123...",
      "link": "https://estetica-mireia.webs.amgdl.com/?utm_source=facebook&utm_medium=cpc&utm_campaign=juliol2026",
      "message": "Descobreix els nostres tractaments d'estiu. Reserva ara amb descompte!",
      "name": "Tracta't bé aquest estiu",
      "description": "Primera visita amb 20% de descompte",
      "call_to_action": { "type": "LEARN_MORE" }
    }
  }
}
→ { "id": "23851234567891" }
```

### 4. Crear Ad (associar creatiu a ad set)

```
POST /v19.0/act_{AD_ACCOUNT_ID}/ads
{
  "name": "Ad - Estiu 2026",
  "adset_id": "23851234567892",
  "creative": { "creative_id": "23851234567891" },
  "status": "PAUSED"
}
→ { "id": "23851234567893" }
```

### 5. Activar (campanya → ad set → ad)

```
POST /v19.0/{campaign_id}   → { "status": "ACTIVE" }
POST /v19.0/{adset_id}      → { "status": "ACTIVE" }
POST /v19.0/{ad_id}         → { "status": "ACTIVE" }
```

---

## Estat de les campanyes (màquina d'estats)

```
DRAFT ──publish──► PENDING_REVIEW
                        │
               Meta approva/rebutja
                   │         │
                ACTIVE     REJECTED
                   │
              pause/resume
                   │
                PAUSED
                   │
                archive
                   │
               ARCHIVED
```

**Nota:** `PENDING_REVIEW` pot ser instantani (Meta rarament revisa anuncis de low-risk) o trigar fins a 24h. El `MetaAdsStatusSyncJob` comprova l'estat cada hora durant les primeres 24h post-publicació.

---

## Targeting: assistent simplificat per a AMG

Per evitar mostrar la complexitat nativa de Meta, l'UI ofereix un formulari simplificat:

### Camps de l'assistent

```
Públic
├── Edats: [25] a [55]
├── Gènere: ○ Tots  ○ Homes  ○ Dones
├── Localització: [Palma de Mallorca ×] [Inca ×] [+ Afegir]
│   └── Radi (km): [25]
└── Interessos: [Bellesa ×] [Benestar ×] [+ Afegir]

Ubicació de l'anunci
├── ○ Automàtic (recomanat)
└── ○ Manual:
    ☑ Facebook Feed
    ☑ Instagram Feed
    ☑ Instagram Stories
    ☐ Audience Network

Pressupost
├── ● Pressupost diari:    [10.00] €/dia
│   ○ Pressupost total:   [      ] € total
└── Oferta: ○ Automàtic (recomanat)  ○ Manual [   ] €
```

### Cerca d'interessos (API de Meta)

```
GET /v19.0/search?type=adinterest&q=bellesa&access_token=TOKEN
→ [
    { "id": "6003349442621", "name": "Beauty", "audience_size": 1200000 },
    { "id": "6003395291321", "name": "Skin care", "audience_size": 850000 }
  ]
```

L'endpoint backend `/api/v1/meta-ads/targeting/interests?q=bellesa` fa proxy a Meta amb el token global.

### Cerca de localitzacions

```
GET /v19.0/search?type=adgeolocation&q=palma&location_types=["city"]&access_token=TOKEN
→ [
    { "key": "710347", "name": "Palma de Mallorca", "region": "Balearic Islands", "country_code": "ES" }
  ]
```

---

## Frontend

### Estructura de rutes

```
/portal/admin/tenants/[id]/meta-ads
  ├── page.tsx                  ← llistat de campanyes + botó "Nova campanya"
  ├── new/
  │   └── page.tsx              ← wizard de creació (4 passos)
  └── [campaignId]/
      ├── page.tsx              ← detall campanya (ad sets, ads, stats)
      ├── edit/
      │   └── page.tsx          ← editar campanya
      └── adsets/
          ├── new/
          │   └── page.tsx      ← crear ad set
          └── [adsetId]/
              └── ads/
                  └── new/
                      └── page.tsx  ← crear anunci
```

---

### Pantalla 1 — Llistat de campanyes

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ← Estetica Mireia                                                       │
│  IA & Canals  /  Pressupostos  /  Analítica  /  Meta Ads  ◄             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  META ADS                                          [+ Nova campanya]    │
│                                                                          │
│  ┌─ Resum del mes ──────────────────────────────────────────────────┐   │
│  │  Despesa total    Impressions    Clics    Leads    CPL mitjà      │   │
│  │    124.50 €         58,230       1,847      27      4.61 €        │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  ┌─ Campanyes ──────────────────────────────────────────────────────┐   │
│  │                                                                   │   │
│  │  ● ACTIVA   Juliol 2026 · Tractaments estiu           10€/dia    │   │
│  │             2 ad sets · 4 anuncis · CPL: 4.61€        →          │   │
│  │             ████████████░░░░░  12 dies restants                  │   │
│  │             [Pausar]  [Editar]  [Veure stats]                     │   │
│  │  ─────────────────────────────────────────────────────────────   │   │
│  │  ⏸ PAUSADA  Juny 2026 · Promoció tardor                5€/dia    │   │
│  │             1 ad set · 2 anuncis · CPL: 6.20€          →          │   │
│  │             [Reprendre]  [Editar]  [Duplicar]                     │   │
│  │  ─────────────────────────────────────────────────────────────   │   │
│  │  ✎ ESBORRANY  Nova campanya sense publicar                  —    │   │
│  │               1 ad set · 0 anuncis                      →          │   │
│  │               [Continuar]  [Eliminar]                             │   │
│  │  ─────────────────────────────────────────────────────────────   │   │
│  │  ✕ REBUTJADA  Prova agost                               0€/dia   │   │
│  │               Motiu: Imatge conté text excessiu          →          │   │
│  │               [Corregir]  [Arxivar]                               │   │
│  │                                                                   │   │
│  └───────────────────────────────────────────────────────────────────   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### Pantalla 2 — Wizard de creació (4 passos)

El wizard és lineal i no permet avançar sense completar el pas actual. El progrés es mostra a la barra superior. Es pot desar com a esborrany en qualsevol moment.

#### Pas 1: Objectiu

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ← Campanyes  /  Nova campanya                                           │
│                                                                          │
│  ①  Objectiu    ○ Públic    ○ Creatiu    ○ Revisió                      │
│  ─────────────────────────────────────────────────────────────────────  │
│                                                                          │
│  QUIN ÉS L'OBJECTIU?                                                    │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │  ◉  Generació de leads                       RECOMANAT           │   │
│  │     L'usuari omple un formulari o va a la teva landing.          │   │
│  │     Ideal per a: estètica, clíniques, serveis locals             │   │
│  ├──────────────────────────────────────────────────────────────────┤   │
│  │  ○  Tràfic a la web                                              │   │
│  │     Porta gent a la teva pàgina. Bé per a restaurants,          │   │
│  │     botigues o pàgines amb informació detallada                  │   │
│  ├──────────────────────────────────────────────────────────────────┤   │
│  │  ○  Reconeixement de marca                                       │   │
│  │     Fes que la gent et conegui al teu municipi.                  │   │
│  │     Bé per a negocis nous o temporades especials                 │   │
│  ├──────────────────────────────────────────────────────────────────┤   │
│  │  ○  Interaccions                                                 │   │
│  │     M'agrades, comentaris, comparticions.                        │   │
│  │     Per a contingut viral o sortejaments                         │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│                                                  [Continuar →]          │
└─────────────────────────────────────────────────────────────────────────┘
```

#### Pas 2: Públic i pressupost

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ← Campanyes  /  Nova campanya                                           │
│                                                                          │
│  ✓ Objectiu    ②  Públic    ○ Creatiu    ○ Revisió                      │
│  ─────────────────────────────────────────────────────────────────────  │
│                                                                          │
│  CAMPANYA                                                                │
│  Nom  [Juliol 2026 · Tractaments estiu_____________________________]    │
│  Dates  Inici [01/07/2026]  Fi [31/07/2026] ○ Sense data de fi          │
│                                                                          │
│  PRESSUPOST                                                              │
│  ◉ Pressupost diari    [10.00] €/dia                                    │
│  ○ Pressupost total    [     ] € per a tot el període                   │
│                                                                          │
│  Estimació:  ~310€ durant 31 dies                                        │
│  ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░  Pressupost baix (< 5€) per a        │
│  ████████████░░░░░░░░░░░░░░░░░░░░░  resultats òptims, mínim 5-15€       │
│                                                                          │
│  PÚBLIC ─────────────────────────────────────────────────────────────   │
│                                                                          │
│  Edats      [25]  fins a  [55]                                           │
│  Gènere     ◉ Tots   ○ Homes   ○ Dones                                  │
│                                                                          │
│  Localització                                                            │
│  [Palma de Mallorca ×]  [Inca ×]  [+ Afegir localització]              │
│  Radi:  ○ 10km  ◉ 25km  ○ 40km  ○ Tot Mallorca                         │
│                                                                          │
│  Interessos  (opcional — deixa buit per arribar a tothom)               │
│  [Bellesa ×]  [Benestar ×]  [Estètica ×]  [+ Afegir interès]           │
│  Cerca: [                                  ]                            │
│          Suggerits: Spa · Massatge · Cosmètica · Perruqueria            │
│                                                                          │
│  Ubicació de l'anunci                                                    │
│  ◉ Automàtic (Meta tria les millors ubicacions)                         │
│  ○ Manual:  ☑ Facebook Feed  ☑ Instagram Feed                           │
│             ☑ Instagram Stories  ☐ Audience Network                     │
│                                                                          │
│  Mida estimada del públic                                                │
│  ░░░░░████████████░░░░░░░░░  Àmpli                                      │
│  ~180.000 – 320.000 persones  ← actualitza en temps real                │
│                                                                          │
│              [← Enrere]                        [Continuar →]            │
└─────────────────────────────────────────────────────────────────────────┘
```

**Cerca d'interessos en temps real** (autocomplete contra l'API de Meta):
```
  Cerca: [belle                              ]
         ┌──────────────────────────────────────┐
         │ Bellesa                  1.2M persones│
         │ Bellesa natural          890K persones│
         │ Bellesa i cura personal  2.1M persones│
         └──────────────────────────────────────┘
```

#### Pas 3: Creatiu

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ← Campanyes  /  Nova campanya                                           │
│                                                                          │
│  ✓ Objectiu    ✓ Públic    ③  Creatiu    ○ Revisió                      │
│  ─────────────────────────────────────────────────────────────────────  │
│                                                                          │
│  ┌─────────────────────────────────┐  ┌──────────────────────────────┐  │
│  │ EDITOR                          │  │ VISTA PRÈVIA                 │  │
│  │                                 │  │                              │  │
│  │ Imatge *                        │  │ ┌──────────────────────────┐ │  │
│  │ ┌───────────────────────────┐   │  │ │                          │ │  │
│  │ │                           │   │  │ │    [imatge 1:1]          │ │  │
│  │ │  [ + Pujar imatge ]       │   │  │ │                          │ │  │
│  │ │  1080×1080px recomanat    │   │  │ │                          │ │  │
│  │ │  JPG/PNG · màx 30MB       │   │  │ └──────────────────────────┘ │  │
│  │ └───────────────────────────┘   │  │ Estetica Mireia · Patrocinat  │  │
│  │ O des de galeria: [···] [···]   │  │                              │  │
│  │                                 │  │ Tracta't bé aquest estiu     │  │
│  │ Text principal *  (125 car rec) │  │ Primera visita 20% descompte │  │
│  │ ┌───────────────────────────┐   │  │ estetica-mireia.webs.amgdl…  │  │
│  │ │Descobreix els nostres     │   │  │ ┌──────────────────────────┐ │  │
│  │ │tractaments d'estiu.       │   │  │ │       Saber més          │ │  │
│  │ │Reserva ara amb descompte! │   │  │ └──────────────────────────┘ │  │
│  │ └───────────────────────────┘   │  │                              │  │
│  │ 89/600 caràcters                │  │ ── Instagram Feed ──         │  │
│  │                                 │  │ ┌──────────────────────────┐ │  │
│  │ Títol *  (40 car rec)           │  │ │ ○ Estetica Mireia        │ │  │
│  │ [Tracta't bé aquest estiu___]   │  │ │                          │ │  │
│  │                                 │  │ │    [imatge 4:5]          │ │  │
│  │ Descripció  (opcional)          │  │ │                          │ │  │
│  │ [Primera visita amb 20% desc.]  │  │ │                          │ │  │
│  │                                 │  │ │ Tracta't bé aquest estiu │ │  │
│  │ URL de destinació *             │  │ │ [Saber més]              │ │  │
│  │ [https://estetica-mireia.webs…] │  │ └──────────────────────────┘ │  │
│  │ ✓ Afegir UTMs automàticament    │  │                              │  │
│  │  ?utm_source=facebook           │  │  ○ Feed  ○ Stories  ○ Reels  │  │
│  │  &utm_medium=cpc                │  │                              │  │
│  │  &utm_campaign=juliol-2026      │  └──────────────────────────────┘  │
│  │                                 │                                     │
│  │ Botó d'acció                    │                                     │
│  │ [Saber més               ▼]     │                                     │
│  │  Saber més · Reservar ara       │                                     │
│  │  Contactar · Obtenir pressupost │                                     │
│  │  Registrar-se · Descarregar     │                                     │
│  │                                 │                                     │
│  └─────────────────────────────────┘                                     │
│                                                                          │
│              [← Enrere]                        [Continuar →]            │
└─────────────────────────────────────────────────────────────────────────┘
```

La vista prèvia s'actualitza en temps real mentre s'escriu. Es pot alternar entre Facebook Feed, Instagram Feed, Instagram Stories i Instagram Reels.

#### Pas 4: Revisió i publicació

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ← Campanyes  /  Nova campanya                                           │
│                                                                          │
│  ✓ Objectiu    ✓ Públic    ✓ Creatiu    ④  Revisió                      │
│  ─────────────────────────────────────────────────────────────────────  │
│                                                                          │
│  RESUM DE LA CAMPANYA                                                    │
│                                                                          │
│  ┌─ Configuració ─────────────────────────────────────────────────┐     │
│  │  Nom          Juliol 2026 · Tractaments estiu                  │     │
│  │  Objectiu     Generació de leads                               │     │
│  │  Dates        1 jul 2026 → 31 jul 2026  (31 dies)             │     │
│  │  Pressupost   10,00 €/dia  ·  ~310 € total estimat            │     │
│  └────────────────────────────────────────────────────────────────┘     │
│                                                                          │
│  ┌─ Públic ───────────────────────────────────────────────────────┐     │
│  │  Edats        25 – 55 anys  ·  Tots els gèneres               │     │
│  │  Localització Palma + 25km  ·  Inca + 25km                    │     │
│  │  Interessos   Bellesa · Benestar · Estètica                    │     │
│  │  Ubicació     Automàtica                                       │     │
│  │  Estimació    ~180.000 – 320.000 persones                     │     │
│  └────────────────────────────────────────────────────────────────┘     │
│                                                                          │
│  ┌─ Anunci ───────────────────────────────────────────────────────┐     │
│  │  ┌──────┐  "Tracta't bé aquest estiu"                          │     │
│  │  │[img] │  Descobreix els nostres tractaments d'estiu...       │     │
│  │  │      │  estetica-mireia.webs.amgdl.com  ·  Saber més        │     │
│  │  └──────┘                                                      │     │
│  └────────────────────────────────────────────────────────────────┘     │
│                                                                          │
│  ┌─ ⚠ Avís important ─────────────────────────────────────────────┐     │
│  │  Meta revisa tots els anuncis. El temps habitual és de          │     │
│  │  pocs minuts fins a 24h. Rebràs una notificació Telegram       │     │
│  │  quan s'activi o si hi ha algun problema.                      │     │
│  │                                                                 │     │
│  │  Consulta les polítiques de publicitat de Meta abans de        │     │
│  │  publicar. [Veure polítiques →]                                │     │
│  └────────────────────────────────────────────────────────────────┘     │
│                                                                          │
│     [← Enrere]   [Desar com a esborrany]   [Publicar a Meta ►]         │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

**Estat durant la publicació** (substitueix el botó per un feedback progressiu):

```
  ── Publicant... ──────────────────────────────────────────────────
  ✓ Campanya creada a Meta
  ✓ Ad Set creat
  ✓ Imatge pujada
  ✓ Creatiu creat
  ✓ Anunci creat
  ⏳ Pendent de revisió per Meta...
  ─────────────────────────────────────────────────────────────────
  Rebràs una notificació Telegram quan s'activi.    [Veure campanya]
```

En lloc d'un spinner opac, es mostren les etapes una a una per transmetre que el procés avança i on pot fallar si hi ha un error.

---

### Pantalla 3 — Detall d'una campanya activa

```
┌─────────────────────────────────────────────────────────────────────────┐
│  ← Campanyes  /  Juliol 2026 · Tractaments estiu                        │
│                                                               ● ACTIVA  │
│  [Pausar]  [Editar]  [Duplicar]  [Arxivar]                              │
│  ─────────────────────────────────────────────────────────────────────  │
│                                                                          │
│  ┌─ Rendiment ─────────────┬──────────────┬───────────┬───────────┐    │
│  │  Ahir                   │  7 dies       │  30 dies  │  Total    │    │
│  ├─────────────────────────┴──────────────┴───────────┴───────────┤    │
│  │  Despesa        Impressions      Clics       Leads       CPL   │    │
│  │  12.40 €          4,230           134           3        4.13€  │    │
│  │  ████████         ██████████      ████         ●●●              │    │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  ┌─ Ad Sets (2) ────────────────────────────────  [+ Nou Ad Set]  ─┐   │
│  │                                                                  │   │
│  │  ● Dones 25-45 · Palma                         10 €/dia         │   │
│  │    CPL: 4.13€ · 3 leads avui · 2 anuncis actius      [Gestionar]│   │
│  │                                                                  │   │
│  │  ⏸ Homes 30-55 · Mallorca                       0 €/dia         │   │
│  │    CPL: — · 0 leads · PAUSAT                         [Gestionar]│   │
│  │                                                                  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  ┌─ Detall Ad Set: Dones 25-45 · Palma ────────────────────────────┐   │
│  │  Públic: Dones 25-45 · Palma 25km · Bellesa/Benestar           │   │
│  │  Estimació: 120.000 – 180.000 · Oferta: AUTOMÀTICA             │   │
│  │                                                                  │   │
│  │  Anuncis (2)                            [+ Nou anunci]          │   │
│  │  ┌──────────────────────────────────────────────────────────┐   │   │
│  │  │  [img] ● "Tracta't bé aquest estiu"    CPL: 4.13€  Editar│   │   │
│  │  │        Juliol 2026 · 3 leads           [Pausar] [Duplicar]│   │   │
│  │  ├──────────────────────────────────────────────────────────┤   │   │
│  │  │  [img] ⏸ "Estiu de canvis"             CPL: —     Editar │   │   │
│  │  │        PAUSAT manualment               [Reprendre]        │   │   │
│  │  └──────────────────────────────────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

### Notes de disseny UX

**Wizard lineal, no tabs.** L'usuari no pot saltar passos. Cada pas valida el contingut abans de permetre avançar (camp nom buit → error inline, no toast).

**Vista prèvia en temps real (pas 3).** El panell dret s'actualitza a mesura que s'escriu el text o es puja la imatge. Es pot canviar entre formats (Feed 1:1, Stories 9:16, Reels) per veure com queda a cada ubicació.

**UTMs automàtics.** Es generen a partir del nom de la campanya (espais → guions, minúscules, caràcters especials eliminats) sense que l'usuari hagi d'escriure res. Es mostren per transparència però no cal editar-los.

**Feedback de publicació progressiu.** En lloc d'un spinner opac, es mostren les etapes seqüencials (campanya → ad set → imatge → creatiu → ad). Si alguna falla, es mostra l'error exacte de Meta i quina etapa ha fallat.

**Notificació Telegram.** Quan Meta aprova o rebutja, el tenant rep un missatge Telegram directament. No cal que quedi pendent a la pantalla.

**Estimació del públic en temps real (pas 2).** Cada cop que l'usuari afegeix o treu una localització, interès o canvia l'edat, es fa una crida a `/api/v1/meta-ads/targeting/estimate` que retorna el rang de persones. Ajuda l'usuari a entendre si el públic és massa petit o massa gran.

---

## Serveis backend — detall

### `MetaAdsCampaignService`

```java
// Crea campanya local (estat DRAFT)
CampaignResponse createCampaign(UUID tenantId, CreateCampaignRequest req, UserPrincipal principal);

// Publica a Meta
CampaignResponse publish(UUID tenantId, UUID campaignId);
// Seqüència:
// 1. POST /campaigns → obté metaCampaignId
// 2. Per cada adSet: POST /adsets → obté metaAdSetId
// 3. Per cada ad: POST /adcreatives, POST /ads
// 4. POST /{campaignId} → status=ACTIVE
// 5. Actualitza BD local

// Pausa/repren a Meta
CampaignResponse pause(UUID tenantId, UUID campaignId);
CampaignResponse resume(UUID tenantId, UUID campaignId);
```

### `MetaAdsCreativeService`

```java
// Puja imatge a Meta (via MinIO URL o multipart)
String uploadImage(UUID tenantId, MultipartFile file);
// 1. Desa imatge a MinIO (reutilitza Assets, Mòdul 6)
// 2. POST /adimages → obté hash
// 3. Desa hash a ad_creatives

// Crea creatiu a Meta
String createCreative(UUID tenantId, AdCreative creative);
```

### `MetaAdsStatusSyncJob`

```java
// Cada hora comprova l'estat de campanyes en PENDING_REVIEW
// POST /v19.0/{id}?fields=status,review_feedback
// Si status=ACTIVE → actualitza BD
// Si status=DISAPPROVED → actualitza BD + envia notificació Telegram al tenant
@Scheduled(cron = "0 0 * * * *")
void syncPendingReview();

// Cada dia a les 07:00 UTC sincronitza tots els estats
@Scheduled(cron = "0 0 7 * * *")
void syncAllStatuses();
```

### `MetaAdsTargetingService`

```java
// Proxy per a cerca d'interessos i localitzacions
List<InterestResult> searchInterests(String query);
List<LocationResult> searchLocations(String query);
List<FacebookPage> getTenantPages(UUID tenantId);
```

---

## Notificacions automàtiques (via Telegram al tenant)

| Event | Missatge |
|-------|---------|
| Campanya aprovada | ✅ Meta Ads: "Juliol 2026" activada. Pressupost: 10€/dia |
| Campanya rebutjada | ❌ Meta Ads: "Juliol 2026" rebutjada. Motiu: [motiu de Meta] |
| Pressupost esgotat | ⚠️ Meta Ads: "Juliol 2026" ha esgotat el pressupost diari |
| CPL > llindar (configurable) | 📊 Meta Ads: CPL de "Juliol 2026" supera 8€ (ara: 9.20€) |

---

## Seguretat

### Validacions obligatòries

1. **Verificació de propietat**: el `tenantId` de la campanya local ha de coincidir amb l'`adAccountId` del `meta_ads_configs` del tenant. Mai es pot cridar a un compte d'anuncis que no pertany al tenant autenticat.

2. **Rate limiting**: Meta limita a 200 cridades/hora per compte d'anuncis. Implementar rate limiter amb Redis per evitar bloquejos.

3. **Token segur**: l'`access_token` de `meta_oauth_tokens` es xifra amb AES-256 (via `VaultEncryption`) abans de guardar-lo.

4. **HMAC de webhooks**: Meta envia un header `X-Hub-Signature-256` a cada webhook. Cal verificar-lo amb l'`App Secret`.

5. **Prevenció de despesa no autoritzada**: 
   - Establir `spending_limit` a nivell d'Ad Account via API
   - Camp `max_daily_budget_eur` a `meta_ads_configs` per limitar el que el tenant pot configurar des del portal

### Permisos necessaris

| Permís Meta | Per a |
|------------|-------|
| `ads_management` | Crear/modificar campanyes, ad sets, ads |
| `ads_read` | Llegir estadístiques (ja al Mòdul 35) |
| `pages_read_engagement` | Llegir les pàgines de Facebook per als creatius |
| `pages_manage_ads` | Associar pàgina als creatius |
| `business_management` | Accés al Business Manager (si s'usa System User) |

---

## Limitacions i casos especials

### Moderació de contingut de Meta

Meta pot rebutjar un anunci per:
- Imatges que mostren "abans/després" (prohibit en salut/bellesa)
- Text que inclou promeses de resultats garantits
- Imatges amb més del 20% de text (deprecated però segueix afectant)
- Contingut relacionat amb temes especials (crèdits, habitatge, feina, salut)

**Recomanació:** Mostrar les [políticies de publicitat de Meta](https://www.facebook.com/policies/ads/) al pas 3 del wizard.

### Duplicació de campanyes

Funcionalitat útil per a "copiar campanya":
```
POST /api/v1/meta-ads/tenants/{tid}/campaigns/{id}/duplicate
```
Crea una còpia en estat DRAFT amb tots els ad sets i ads.

### Campanyes de temporada

Suport per a "plantilles de campanya" pre-definides per sector:
- Estètica: "Promoció estival", "Tornada a la rutina", "Nadal"
- Restaurant: "Menú de setmana", "Divendres nit", "Events especials"
- Clínica: "Revisió anual", "Vacunes grip"

Emmagatzemar a `campaign_templates` (tenant_id NULL = global, tenant_id = personalitzada).

---

## Integració amb Mòdul 35 (Analytics)

Un cop implementat el Mòdul 36, el `MetaAdsSyncJob` del Mòdul 35 pot:
- Enriquir les dades de `campaign_spend` amb el `campaignId` local
- Correlacionar directament leads ↔ campanya per `metaCampaignId` en comptes de per nom
- Eliminar el problema de matching per nom (veure limitació del Mòdul 35)

---

## Fases d'implementació recomanades

### Fase 1 (MVP) — Campanyes bàsiques
- [ ] System User Token (Opció A) — sense OAuth
- [ ] Crear/Editar/Pausar/Arxivar campanyes
- [ ] Crear Ad Sets amb assistent simplificat
- [ ] Upload d'imatge + creatiu bàsic (link ad)
- [ ] Publicació i sync d'estat
- [ ] Notificacions Telegram (aprovada/rebutjada)

### Fase 2 — Millores
- [ ] OAuth per tenant (Opció B)
- [ ] Cerca d'interessos en temps real
- [ ] Vista prèvia de l'anunci
- [ ] Plantilles de campanya per sector
- [ ] Duplicació de campanyes
- [ ] Llindar d'alerta CPL configurable

### Fase 3 — Advanced
- [ ] Vídeo ads
- [ ] Lead Ads natius (formulari dins de Meta) integrat amb el webhook del Mòdul 34
- [ ] A/B testing (múltiples creatius per ad set)
- [ ] Regles automàtiques (pausar si CPL > X€)
- [ ] Audiències personalitzades (Custom Audiences)

---

## Dependències

| Mòdul | Necessitat |
|-------|-----------|
| Mòdul 6 (Assets) | Upload d'imatges a MinIO |
| Mòdul 32 (Notifications) | Notificacions Telegram quan s'aprova/rebutja |
| Mòdul 34 (Meta Lead Ads) | Webhook de leads natius (Fase 3) |
| Mòdul 35 (Analytics) | Correlació directa spend ↔ leads |
| `VaultEncryption` | Xifrar tokens OAuth |
| `SystemConfigService` | `META_ADS_MANAGEMENT_TOKEN`, `META_ADS_APP_SECRET` |
