# Mòdul 12: Prospecting — Cerca i Gestió de Clients Potencials

> **Versió:** 2.2
> **Data:** 2026-07-02
> **Dependències:** Mòdul 01 (Auth), Mòdul 03 (Leads), Mòdul 15 (Demo), Mòdul 30 (Chat Widget), Mòdul 41 (Agent Tool Calling)
> **Canvis v2:** Scoring ampliat (0-100), web scraping profund, detecció de xarxes socials, informe IA, demo automàtica, generació de widget JS, missatge personalitzat IA, classificació per rang, dashboard

---

## 1. Objectius

- Identificar negocis locals amb potencial de conversió i puntuar-los automàticament (0–100)
- Generar demos personalitzades automàticament quan el score supera el llindar
- Analitzar la presència digital dels prospects via IA i detectar oportunitats concretes
- Generar codi de widget (chat + WhatsApp) llest per copiar per a negocis amb web existent
- Enviar missatges de prospecció hiper-personalitzats (mai genèrics)
- Gestionar el seguiment fins a la conversió

---

## 2. Flux general

```
Google Places API
      │
      ▼
Scraping bàsic (nom, categoria, adreça, telèfon, web, rating, reviews, fotos, horaris)
      │
      ▼
Enriquiment (web scraping + xarxes socials + tech stack)
      │
      ▼
Scoring 0–100
      │
      ├─ 0–30  → Descartar automàticament
      ├─ 31–60 → Guardar per revisió manual
      ├─ 61–80 → Generar demo automàtica
      └─ 81–100 → Demo prioritària + contacte immediat
                      │
                      ▼
              Informe IA estructurat
                      │
                      ▼
              Generació de missatge personalitzat
                      │
                      ▼
              Enviament pel canal disponible
                      │
                      ▼
              Seguiment (obertura demo, converses, reunions)
```

---

## 3. Fonts de dades

El sistema és modular (`ProspectScraper` interface). Cada font és una implementació independent.

### Prioritat 1 — Google Places API (✅ implementat)

Dades obtingudes:
- Nom, categoria, adreça, telèfon, web, valoració, nombre de ressenyes
- Horaris, fotos, descripció, coordenades, `placeId`, múltiples seus

### Prioritat 2 — Web scraping (⚠️ parcialment implementat — ampliar)

`WebAnalyzerService` analitza la web del negoci i extreu:

**Contingut:**
- Títol, meta description, serveis, FAQ, formularis, emails, telèfons, WhatsApp, CTA, horaris, logo

**Presència digital:**
- Xarxes socials detectades (Facebook, Instagram, LinkedIn, TikTok, YouTube)

**Anàlisi tècnica:**
| Senyal | Com es detecta |
|--------|---------------|
| SSL | Schema `https://` + certificat vàlid |
| Responsive | Viewport meta tag + media queries |
| Velocitat | Time-to-first-byte via HTTP HEAD |
| Framework/CMS | Headers HTTP, classes HTML, meta generators |
| Google Analytics | `gtag.js` o `analytics.js` a la font |
| Google Tag Manager | `gtm.js` a la font |
| Meta Pixel | `fbq('init'` a la font |
| Chat widget | Intercom, Crisp, Tidio, LiveChat, Drift |
| WhatsApp widget | `wa.me/` links o widgets de tercers |
| Sistema de reserves | Calendly, SimplyBook, Acuity, Booksy, formulari propi |
| Formulari de contacte | `<form>` amb camps de contacte |
| Política de cookies | Banner de cookies / avís RGPD |
| Accessibilitat bàsica | `alt` en imatges, contrast colors |

### Prioritat 3 — Google Business Profile (⚠️ nou)

Via Google My Business API (reutilitza connexió Spec 52 §7.2):
- Preguntes i respostes públiques
- Ressenyes: text, valoració, freqüència de noves ressenyes
- Fotografies publicades
- Popularitat (pics d'activitat)

### Prioritat 4 — Xarxes socials (⚠️ nou — estén enriquiment existent)

Detecció automàtica per nom del negoci + URL del web:

| Xarxa | Dades a extreure |
|-------|-----------------|
| Facebook | Existència de pàgina, última publicació, seguidors aproximats |
| Instagram | Usuari, última publicació, seguidors |
| LinkedIn | Pàgina d'empresa, activitat recent |
| TikTok | Compte, última publicació |
| YouTube | Canal, vídeos recents |

### Prioritat 5 — Tech stack (⚠️ nou — part de `WebAnalyzerService`)

Detectar automàticament via capçaleres HTTP + anàlisi font:
- Google Analytics / GA4 / GTM
- Meta Pixel
- Cloudflare
- Framework (React, Vue, Next.js, WordPress, PrestaShop, Shopify, Wix, Squarespace)
- Hosting (OVH, Ionos, Hostinger, etc.)

---

## 4. Model de dades

### 4.1 Entitat `Prospect` — camps nous (migració Flyway V73)

Camps existents: `id`, `campaignId`, `name`, `description`, `sector`, `address`, `city`, `postalCode`, `phone`, `email`, `website`, `instagram`, `googleRating`, `googleReviews`, `googlePlaceId`, `hasWebsite`, `hasInstagram`, `hasWhatsapp`, `status`, `source`, `externalId`, `leadId`, `notes`, `score`, `createdAt`, `updatedAt`

**Nous camps (ALTER TABLE prospects):**

```sql
-- Web analysis
ALTER TABLE prospects ADD COLUMN has_ssl BOOLEAN;
ALTER TABLE prospects ADD COLUMN is_responsive BOOLEAN;
ALTER TABLE prospects ADD COLUMN has_analytics BOOLEAN;
ALTER TABLE prospects ADD COLUMN has_pixel BOOLEAN;
ALTER TABLE prospects ADD COLUMN has_gtm BOOLEAN;
ALTER TABLE prospects ADD COLUMN has_chat_widget BOOLEAN;
ALTER TABLE prospects ADD COLUMN has_booking_system BOOLEAN;
ALTER TABLE prospects ADD COLUMN has_contact_form BOOLEAN;
ALTER TABLE prospects ADD COLUMN has_faq BOOLEAN;
ALTER TABLE prospects ADD COLUMN has_clear_cta BOOLEAN;
ALTER TABLE prospects ADD COLUMN tech_stack VARCHAR(500);     -- JSON array de techs detectades
ALTER TABLE prospects ADD COLUMN web_load_ms INTEGER;         -- time-to-first-byte en ms
ALTER TABLE prospects ADD COLUMN cms_detected VARCHAR(50);    -- WordPress, Shopify, etc.

-- Social networks
ALTER TABLE prospects ADD COLUMN facebook_url VARCHAR(300);
ALTER TABLE prospects ADD COLUMN linkedin_url VARCHAR(300);
ALTER TABLE prospects ADD COLUMN tiktok_url VARCHAR(300);
ALTER TABLE prospects ADD COLUMN youtube_url VARCHAR(300);
ALTER TABLE prospects ADD COLUMN has_facebook BOOLEAN;
ALTER TABLE prospects ADD COLUMN has_linkedin BOOLEAN;
ALTER TABLE prospects ADD COLUMN has_tiktok BOOLEAN;

-- Scoring & classification
ALTER TABLE prospects ADD COLUMN score_breakdown TEXT;        -- JSON amb detall de cada senyal
ALTER TABLE prospects ADD COLUMN prospect_tier VARCHAR(20);   -- DISCARD, REVIEW, DEMO, PRIORITY

-- AI analysis
ALTER TABLE prospects ADD COLUMN ai_report TEXT;              -- informe estructurat generat per IA
ALTER TABLE prospects ADD COLUMN ai_pitch TEXT;               -- missatge personalitzat per enviar
ALTER TABLE prospects ADD COLUMN demo_url VARCHAR(500);       -- URL de la demo generada (si n'hi ha)
ALTER TABLE prospects ADD COLUMN widget_code TEXT;            -- codi JS generat (si té web)
ALTER TABLE prospects ADD COLUMN web_analyzed_at TIMESTAMPTZ;
ALTER TABLE prospects ADD COLUMN ai_analyzed_at TIMESTAMPTZ;
```

### 4.2 Entitat `ProspectCampaign` — camps nous

```sql
ALTER TABLE prospect_campaigns ADD COLUMN auto_demo_threshold INTEGER DEFAULT 61;
ALTER TABLE prospect_campaigns ADD COLUMN auto_contact_channel VARCHAR(20);  -- EMAIL, WHATSAPP, FORM
ALTER TABLE prospect_campaigns ADD COLUMN total_demos_generated INTEGER DEFAULT 0;
ALTER TABLE prospect_campaigns ADD COLUMN total_discarded INTEGER DEFAULT 0;
```

---

## 5. Sistema de scoring (0–100)

El scoring és **configurable sense modificar codi**: els pesos es llegeixen de `SystemConfig` (Spec 39) i es poden ajustar des del portal.

### 5.1 Potencial comercial (+50 pts màxim)

| Senyal | Condició | Punts |
|--------|---------|-------|
| `PROFITABLE_SECTOR` | Sector d'alta conversió (fisio, perruqueria, restaurant, etc.) | +20 |
| `HIGH_REVIEWS` | >100 ressenyes Google | +10 |
| `GOOD_RATING` | Rating ≥ 4.4★ | +10 |
| `HAS_MOBILE` | Telèfon mòbil detectat | +5 |
| `MULTIPLE_LOCATIONS` | Múltiples seus (>1 al Places API) | +10 |

### 5.2 Necessitat de digitalització (+85 pts màxim)

| Senyal | Condició | Punts |
|--------|---------|-------|
| `NO_CHAT` | Sense chat widget a la web | +10 |
| `NO_WHATSAPP_WIDGET` | Sense WhatsApp Business / widget WA | +10 |
| `NO_BOOKING` | Sense sistema de reserves online | +10 |
| `NO_AUTOMATIONS` | Sense GTM ni Pixel (no fa tracking) | +15 |
| `OLD_WEB` | Web lenta (>3s TTFB) o sense responsive | +15 |
| `NO_CTA` | Sense CTA clara a la web | +10 |
| `NO_FAQ` | Sense secció FAQ | +5 |
| `NO_FORM` | Sense formulari de contacte | +10 |

### 5.3 Penalitzacions

| Senyal | Condició | Punts |
|--------|---------|-------|
| `DOMAIN_DOWN` | Domini caigut o timeout | -50 |
| `CLOSED` | Negoci marcat com a tancat a Google | -100 |
| `NOT_ACCEPTING` | "No accepta nous clients" detectat | -50 |
| `UNDER_CONSTRUCTION` | Web en construcció | -40 |

### 5.4 Classificació per rang

| Rang | Tier | Acció automàtica |
|------|------|-----------------|
| 0–30 | `DISCARD` | Marcar com a descartat, no contactar |
| 31–60 | `REVIEW` | Guardar per revisió manual |
| 61–80 | `DEMO` | Generar demo automàtica |
| 81–100 | `PRIORITY` | Demo prioritària + notificació Telegram al SUPER_ADMIN |

---

## 6. Informe IA estructurat

`ProspectAnalysisService` usa Claude (Sonnet) per generar un informe estructurat per a cada prospect que supera 31 punts.

**Inputs:** totes les dades del `Prospect` (web analysis, scoring, xarxes, tech stack)

**Output JSON (desat a `ai_report`):**
```json
{
  "digitalProblems": ["Sense sistema de reserves", "Web no responsive", "..."],
  "automationOpportunities": ["Agent IA per WhatsApp → gestió de cites", "..."],
  "manualProcesses": ["Reserva per telèfon", "Respostes a WhatsApp manuals", "..."],
  "channelsUsed": ["Instagram actiu", "Facebook inactiu", "Sense WhatsApp Business"],
  "commercialOpportunities": ["F1+F2 → agent + agenda → estalvi 3h/dia", "..."],
  "recommendedDemo": "FISIOTERAPEUTA amb agent de cites + recordatoris",
  "estimatedPhases": ["F1", "F2"],
  "pitchAngle": "Tens 4.8★ però perds clients per no respondre fora d'horari"
}
```

---

## 7. Generació automàtica de demo

Quan `score >= auto_demo_threshold` (defecte: 61):

`ProspectDemoGeneratorService` orquestra:
1. Crear tenant demo via `DemoService` (Mòdul 15)
2. Configurar landing personalitzada amb:
   - Colors extrets de la web del negoci (palette detection)
   - Logo (si es troba a la web)
   - Serveis detectats
   - FAQ detectat
   - Informació pública (nom, adreça, horaris)
   - Formulari de contacte + botó WhatsApp
3. Entrenar agent IA amb la informació pública recollida (context + KB)
4. Desar `demo_url` al `Prospect`
5. Si `PRIORITY`: notificar SUPER_ADMIN per Telegram

**Restriccions:**
- L'agent IA de la demo respon **únicament amb informació pública** del negoci
- La landing inclou avís visible: "Aquesta és una demostració creada per AMG Digitalitzacions"
- TTL de la demo: 7 dies (ampliat respecte els 24h de demos internes)

---

## 8. Generació de widget JS

Per a prospects que ja tenen web (`hasWebsite = true`), `WidgetCodeGeneratorService` genera codi llest per copiar.

### 8.1 Widget de chat

```javascript
<!-- AMG Chat Widget -->
<script>
  window.AMGChat = { tenantId: 'DEMO_ID', position: 'bottom-right', theme: 'light' };
</script>
<script src="https://cdn.amgdl.com/widget.js" async></script>
```

### 8.2 Widget WhatsApp

```javascript
<!-- WhatsApp Button by AMG -->
<a href="https://wa.me/34XXXXXXXXX?text=Hola%2C%20m%27agradaria%20m%C3%A9s%20informaci%C3%B3"
   style="position:fixed;bottom:24px;right:24px;z-index:9999">
  <img src="https://cdn.amgdl.com/wa-btn.svg" width="60" alt="WhatsApp">
</a>
```

### 8.3 Instruccions per framework

El codi s'acompanya d'instruccions específiques per al CMS/framework detectat:

| CMS/Framework | Instruccions |
|--------------|-------------|
| WordPress | Plugin Header Footer Code Manager o functions.php |
| Elementor | Custom Code widget |
| PrestaShop | Mòdul CustomJS |
| Shopify | theme.liquid → `</body>` |
| Wix | Velo → Page Code |
| Squarespace | Code Injection → Footer |
| HTML estàtic | Abans de `</body>` |
| React/Next.js | `_document.tsx` o `layout.tsx` |
| Vue | `index.html` → `</body>` |

---

## 9. Missatge personalitzat IA ✅

`ProspectMessageGeneratorService` usa Claude (via `ProspectAnalysisService.generatePitch()`) per generar un missatge d'aproximació que:

- **Mai és genèric** — fa referència a dades concretes del negoci
- Menciona oportunitats específiques detectades
- Adapta el to al sector i la mida del negoci
- Inclou un CTA clar (demo, reunió, trucada)

**Exemple output:**
> "Hola, he vist que [Nom Negoci] té 4.8★ a Google amb 127 ressenyes — excel·lent reputació. He notat que no teniu sistema de reserves online ni WhatsApp Business: molts dels vostres clients potencials probablement us escriuen fora d'horari i no reben resposta. He preparat una demostració de com quedaria un agent IA gestionant les vostres cites automàticament: [demo_url]"

**Desat a `ai_pitch`.** Endpoint: `POST /prospects/{id}/generate-pitch`.

> Quan s'envia per email, s'ha d'incloure el pixel de tracking (§20) per registrar `pitchOpenedAt`.

---

## 10. Detecció de canal de contacte

El sistema detecta automàticament el millor canal per contactar (ordre de prioritat configurable):

1. **Email** — detectat a la web o via enriquiment
2. **WhatsApp** — si `hasWhatsapp = true`
3. **Formulari web** — si `hasContactForm = true` + URL del formulari
4. **Telèfon** — com a darrer recurs (manual)
5. **Facebook Messenger** — si `hasFacebook = true`
6. **Instagram DM** — si `hasInstagram = true`
7. **LinkedIn** — si `hasLinkedin = true`

El canal seleccionat s'usa per a l'enviament automàtic del missatge (integra amb Spec 20/43).

---

## 11. Endpoints API

Prefix base: `/api/v1/prospecting`

### Existents (✅)

| Mètode | Ruta | Descripció |
|--------|------|-----------|
| POST | `/campaigns` | Crear nova campanya |
| GET | `/campaigns` | Llistar campanyes |
| GET | `/campaigns/{id}` | Detall de campanya |
| POST | `/campaigns/{id}/run` | Executar cerca |
| PUT | `/campaigns/{id}` | Actualitzar campanya |
| DELETE | `/campaigns/{id}` | Eliminar campanya |
| GET | `/campaigns/{id}/prospects` | Llistar prospects |
| PUT | `/prospects/{id}` | Actualitzar estat/notes |
| POST | `/prospects/{id}/enrich` | Enriquir prospect |
| POST | `/prospects/{id}/export` | Exportar a Leads |
| POST | `/campaigns/{id}/export-all` | Exportar tots a Leads |

### v2 — implementats (✅)

| Mètode | Ruta | Descripció |
|--------|------|-----------|
| POST | `/prospects/{id}/analyze-web` | Llança `WebAnalyzerService` per al prospect |
| POST | `/campaigns/{id}/analyze-all` | Analitza web de tots els prospects d'una campanya |
| POST | `/prospects/{id}/ai-report` | Genera informe IA estructurat + pitch |
| POST | `/prospects/{id}/generate-demo` | Genera demo personalitzada (síncron, qualsevol tier) |
| POST | `/prospects/{id}/generate-pitch` | Genera missatge pitch via IA i el desa |
| GET | `/prospects/{id}/widget-code` | Retorna codi JS del widget |
| GET | `/campaigns/{id}/dashboard` | Stats de la campanya (score mig, tiers, conversió) |
| GET | `/dashboard` | Dashboard global de prospecció |
| POST | `/campaigns/{id}/import-csv` | Importa prospects des de CSV (màx 500 files) |
| GET | `/duplicates` | Llista grups de prospects duplicats cross-campanya |
| GET | `/track/{prospectId}/open` | Pixel 1x1 tracking obertura de pitch (públic) |

---

## 12. Serveis nous

| Servei | Estat | Responsabilitat |
|--------|-------|----------------|
| `WebAnalyzerService` | ✅ | Scraping profund de la web: SSL, responsive, tech stack, social links, chat, reserves, formularis |
| `ProspectAnalysisService` | ✅ | Genera informe IA estructurat + pitch via Claude |
| `ProspectDemoGeneratorService` | ✅ | Crea demo personalitzada (delega a `DemoService` — Spec 15) |
| `WidgetCodeGeneratorService` | ✅ | Genera snippets JS per chat + WhatsApp, adaptat per CMS |
| `ProspectMessageGeneratorService` | ✅ | Wrapper que persisteix el pitch generat per `ProspectAnalysisService` |
| `ProspectChannelDetectorService` | ✅ | Determina el millor canal de contacte disponible |
| `ProspectCsvImportService` | ✅ | Importa prospects des de CSV; deduplicació per phone/email |
| `ProspectFollowUpScheduler` | ✅ | Scheduler diari (8:00) de follow-ups automatitzats |

---

## 13. Dashboard

`GET /api/v1/prospecting/dashboard` retorna:

```json
{
  "totalAnalyzed": 342,
  "discarded": 89,
  "review": 121,
  "demo": 98,
  "priority": 34,
  "avgScore": 54.2,
  "demosGenerated": 132,
  "exported": 67,
  "conversionRate": 19.6,
  "bySector": [
    { "sector": "FISIOTERAPEUTA", "count": 45, "avgScore": 71.3, "conversion": 28.9 }
  ],
  "byChannel": [
    { "channel": "EMAIL", "sent": 89, "responded": 23, "rate": 25.8 }
  ],
  "avgTimeToResponse": "2.4 dies"
}
```

---

## 14. Configuració (SystemConfig — Spec 39)

Claus configurables sense modificar codi:

| Clau | Valor per defecte | Descripció |
|------|------------------|-----------|
| `PROSPECTING_AUTO_DEMO_THRESHOLD` | `61` | Score mínim per generar demo |
| `PROSPECTING_DISCARD_THRESHOLD` | `30` | Score màxim per descartar |
| `PROSPECTING_DEMO_TTL_DAYS` | `7` | Dies de vida de les demos de prospecció |
| `PROSPECTING_WEB_TIMEOUT_MS` | `10000` | Timeout scraping web |
| `PROSPECTING_MAX_CONCURRENT_ENRICH` | `3` | Enriquiments en paral·lel |
| `PROSPECTING_CONTACT_PRIORITY` | `EMAIL,WHATSAPP,FORM,PHONE` | Ordre de canals |

---

## 15. Seguretat i límits

- Google Places API: 10 req/s màxim
- Web scraping: 1 req/3s per domini, `User-Agent` honest (`AMGBot/1.0`)
- Només dades públiques — mai autenticació a tercers
- `CLIENT` sense accés al mòdul de prospecció
- API Keys al Vault (Mòdul 02)

---

## 16. Ordre d'implementació

### v2 (✅ completat — V72)
1. Migració V72 — nous camps a `prospects` i `prospect_campaigns`
2. `WebAnalyzerService`, scoring ampliat, classificació per tiers
3. `ProspectAnalysisService` + `ProspectMessageGeneratorService`
4. `ProspectDemoGeneratorService` + `WidgetCodeGeneratorService`
5. `ProspectChannelDetectorService` + nous endpoints + dashboard

### v2.1 (✅ completat — V75)
6. **Migració V75** — camps de tracking (`pitch_sent_at`, `pitch_opened_at`, `demo_opened_at`, `followup_count`, `last_followup_at`) i configuració de seqüència per campanya
7. **`ProspectCsvImportService`** — importació CSV amb deduplicació
8. **Pixel de tracking** — endpoint públic `/track/{id}/open`
9. **Deduplicació cross-campanya** — `/duplicates`
10. **`ProspectFollowUpScheduler`** — scheduler diari follow-ups automàtics
11. **`POST /prospects/{id}/generate-demo`** — endpoint manual de generació de demo

---

## 17. QA

| Cas | Resultat esperat |
|-----|-----------------|
| Prospect amb score 75 | Tier DEMO, demo generada automàticament |
| Prospect amb score 85 | Tier PRIORITY, notificació Telegram SUPER_ADMIN |
| Prospect amb score 20 | Tier DISCARD, no es genera demo ni missatge |
| Web amb WordPress detectat | `cmsDetected='WordPress'`, instruccions WP al widget code |
| Negoci sense web | `WebAnalyzerService` retorna null graciosament, score reflecteix NO_WEBSITE |
| Missatge IA generat | Conté referència concreta (rating, carència detectada), no és genèric |
| Demo generada | Landing pública accessible, agent respon amb info del negoci, avís "demostració" visible |
| Domini caigut | Score -50, tier DISCARD, `DOMAIN_DOWN` als signals |
| CSV importat (400 files) | 400 importats, duplicats omesos, retorna `{imported, skipped}` |
| CSV amb >500 files | Error 400 |
| Pixel tracking cridat | `pitchOpenedAt` actualitzat a BD, imatge PNG 1x1 retornada |
| Duplicat cross-campanya | `/duplicates` retorna el grup amb `matchType=PHONE` o `PLACE_ID` |
| Campanya amb `autoSequenceEnabled=true` | Scheduler detecta prospects CONTACTED > N dies i envia notificació Telegram |

---

## 18. Seqüència automàtica de follow-up

### 18.1 Descripció
`ProspectFollowUpScheduler` s'executa cada dia a les 8:00. Comprova tots els prospects amb:
- `status = CONTACTED`
- `pitchSentAt IS NOT NULL`
- `followupCount < 3`
- Darrer contacte (o pitch) fa més de `followupDaysN` dies

### 18.2 Configuració per campanya
```
ProspectCampaign.autoSequenceEnabled = true
ProspectCampaign.followupDays1 = 3   (defecte)
ProspectCampaign.followupDays2 = 7   (defecte)
ProspectCampaign.followupDays3 = 14  (defecte)
```

### 18.3 Acció del scheduler
1. Incrementa `followupCount`
2. Actualitza `lastFollowupAt = now()`
3. Envia notificació Telegram al canal de la plataforma (`TELEGRAM_CHAT_ID`)
4. Format: `🔁 Follow-up #N — [Nom negoci]\nPorta X dies sense resposta...`

### 18.4 Límit
Màxim 3 follow-ups per prospect (`followupCount >= 3` → para).

---

## 19. Importació CSV

### 19.1 Endpoint
```
POST /api/v1/prospecting/campaigns/{id}/import-csv
Content-Type: multipart/form-data
Param: file (CSV)
```

### 19.2 Format CSV
```csv
name,phone,email,website,address,city
Forn Ca n'Antònia,+34971234567,info@forncantonia.com,https://forncantonia.com,Carrer Major 5,Felanitx
```
- Primera fila: capçaleres (es detecten automàticament i s'ometen)
- Màxim **500 files** per importació (error 400 si supera)
- Columnes opcionals: les buides s'ignoren

### 19.3 Deduplicació
- Dins la campanya: `(campaignId, phone)` i `(campaignId, email)`
- Resposta: `{"imported": N, "skipped": M}`

### 19.4 Prospect creat
- `source = MANUAL`
- `status = NEW`
- `hasWebsite = true` si `website` no és buit

---

## 20. Tracking d'obertures

### 20.1 Camps nous a `prospects`
| Camp | Tipus | Descripció |
|------|-------|-----------|
| `pitchSentAt` | TIMESTAMPTZ | Quan es va enviar el pitch |
| `pitchOpenedAt` | TIMESTAMPTZ | Primera vegada que s'obre l'email (via pixel) |
| `demoOpenedAt` | TIMESTAMPTZ | Primera vegada que s'obre la demo |
| `followupCount` | INTEGER | Nombre de follow-ups enviats (0–3) |
| `lastFollowupAt` | TIMESTAMPTZ | Data del darrer follow-up |

### 20.2 Pixel d'obertura
```
GET /api/v1/prospecting/track/{prospectId}/open
```
- Endpoint públic (sense autenticació)
- Actualitza `pitchOpenedAt` si és null
- Retorna PNG 1x1 transparent (`Cache-Control: no-store`)
- S'incrusta a l'email del pitch: `<img src="https://api.amgdl.com/api/v1/prospecting/track/{id}/open" width="1" height="1">`

---

## 21. Deduplicació cross-campanya

```
GET /api/v1/prospecting/duplicates
```

Retorna grups de prospects duplicats entre campanyes del sistema:

```json
[
  {
    "matchType": "PLACE_ID",
    "matchValue": "ChIJN1t_tDeuEmsRUsoyG83frY4",
    "prospects": [
      {"id": "...", "campaignId": "...", "name": "Forn Ca n'Antònia", "status": "NEW"},
      {"id": "...", "campaignId": "...", "name": "Forn Ca n'Antònia", "status": "CONTACTED"}
    ]
  }
]
```

**Criteris de duplicat:**
- Mateix `googlePlaceId` (no nul) en més d'un prospect
- Mateix `phone` (no nul) en més d'un prospect

---

## 22. Indicadors visuals a la llista de prospects

### 22.1 Badge "ja digitalitzat"

A la llista de prospects, quan `has_chat_widget = true` (el negoci ja té un chat widget detectat a la seva web), es mostra un badge visual **"ja digitalitzat"**.

- **Propòsit**: identificar ràpidament prospects que ja han pres mesures de digitalització i que, per tant, podrien ser menys receptius a la proposta de chat widget, però potencialment receptius a un servei millor (agent IA vs. widget genèric)
- **Implementació**: camp `hasChatWidget` del DTO `ProspectResponse`; renderitzat al component de llista del frontend
- **Scoring relacionat**: el senyal `NO_CHAT` (§5.2) no aplica si `has_chat_widget = true`

### 22.2 URL de la demo del prospect

El camp `demo_url` d'un `Prospect` apunta a la **landing renderitzada** pel motor:
```
https://api.amgdl.com/api/v1/engine/render/demo-{token}
```
No apunta mai a `/demo/inbox/{token}` (ruta legacy interna). La URL pública de la landing és l'única que es comparteix amb el prospect.

---

## 23. Pendents (⚠️)

- **GBP API** (Google Business Profile): ressenyes, Q&A, popularitat — pendent d'integrar V71 amb el pipeline de prospecció
- **Enviament automàtic de pitch**: el canal és detectat però l'enviament real (email/WhatsApp) no s'executa automàticament — pendent d'integrar amb Spec 20/43
- **Tracking obertura demo**: `demoOpenedAt` disponible a la BD però el widget de demo no envia l'event encara
