# Mòdul 34 — Meta Lead Ads + UTM Tracking

## Objectiu

Capturar leads generats des de Facebook Ads i Instagram Ads directament al CRM, i identificar l'origen de tots els leads via paràmetres UTM.

---

## Part 1 — UTM Tracking (formularis de la landing)

### Com funciona

Quan un visitant arriba a una landing des d'un anunci, la URL porta paràmetres UTM:

```
https://estetica-mireia.webs.amgdl.com/?utm_source=facebook&utm_medium=cpc&utm_campaign=juliol26
```

El formulari de contacte llegeix aquests paràmetres i els guarda al lead del CRM:
- `utm_source` → `leads.utm_source` (ex: `facebook`, `instagram`, `google`)
- `utm_medium` → `leads.utm_medium` (ex: `cpc`, `social`, `email`)
- `utm_campaign` → `leads.utm_campaign` (ex: `juliol26`, `promocio-estiu`)

Si l'usuari navega per la pàgina abans d'omplir el formulari, els UTMs es preserven a `sessionStorage`.

### Configuració a Meta Ads Manager

Per a cada anunci, afegeix els paràmetres UTM a la URL de destinació:

| Paràmetre | Valor recomanat |
|-----------|-----------------|
| `utm_source` | `facebook` o `instagram` |
| `utm_medium` | `cpc` (cost per clic) o `lead_ads` |
| `utm_campaign` | Nom de la campanya (ex: `juliol26`) |

**A Meta Ads Manager:** Edita l'anunci → URL de destinació → "Paràmetres d'URL" → afegeix els paràmetres manualment o usa el constructor d'URLs de Meta.

### Detecció automàtica de la font

Quan `utm_source=facebook` → el lead es crea amb `source=FACEBOOK`  
Quan `utm_source=instagram` → el lead es crea amb `source=INSTAGRAM`  
Altres valors → `source=LANDING_FORM` (comportament anterior)

---

## Part 2 — Meta Lead Ads (formularis natius de Facebook/Instagram)

### Què és Meta Lead Ads

Anuncis amb formulari natiu integrat a Facebook/Instagram — l'usuari omple les dades **sense sortir de l'app**. AMG rep els leads via webhook.

### Arquitectura

```
Meta Lead Ads (Facebook/Instagram)
    │
    │  POST (nou lead)
    ▼
GET /api/v1/leads/meta-webhook  ← verificació inicial
POST /api/v1/leads/meta-webhook ← events de nous leads
    │
    ▼
MetaLeadWebhookService
    │
    ├── busca tenant per metaPageId
    ├── crida Graph API → obté dades del lead
    └── crea Lead al CRM (source=FACEBOOK|INSTAGRAM)
```

### Configuració pas a pas

#### 1. Crear una Facebook App (si no en tens)

1. Ves a [developers.facebook.com](https://developers.facebook.com)
2. Crea una nova app → tipus "Business"
3. Afegeix el producte "Webhooks"
4. A "Subscripcions de la pàgina" → subscriu-te a `leadgen`

#### 2. Configurar el webhook a la Facebook App

- **URL del callback:** `https://api.amgdl.com/api/v1/leads/meta-webhook`
- **Token de verificació:** el valor que guardis a `META_WEBHOOK_VERIFY_TOKEN` (veure pas 3)
- **Camps a subscriure:** `leadgen`

#### 3. Afegir claus al SystemConfig (portal admin → Configuració del sistema)

| Clau | Valor | Notes |
|------|-------|-------|
| `META_WEBHOOK_VERIFY_TOKEN` | Token secret (ex: `amg-meta-2026`) | Ha de coincidir amb el que poses a Facebook |
| `META_PAGE_ACCESS_TOKEN` | Token d'accés de la pàgina | Un sol token si tots els tenants usen la mateixa app |
| `META_PAGE_ACCESS_TOKEN_{PAGE_ID}` | Token per pàgina específica | Per a configuració multi-tenant |

**Com obtenir el Page Access Token:**
1. A Meta Business Suite → Configuració → Comptes de la pàgina → selecciona la pàgina
2. Tab "Tokens d'accés" → genera un token de llarga durada (Long-lived token)
3. O usa l'explorador d'APIs de Meta Graph: [developers.facebook.com/tools/explorer](https://developers.facebook.com/tools/explorer)

#### 4. Configurar el tenant al portal AMG

A la fitxa del tenant, pestanya "Agent / Canals":

- Camp **Meta Page ID**: l'ID numèric de la pàgina de Facebook del client (ex: `123456789012345`)
  - Com trobar-lo: ves a la pàgina de Facebook → "Sobre" → "Informació de la pàgina" → ID de la pàgina
  - O: URL de la pàgina → `facebook.com/NomPagina` → eines de desenvolupador → ID de la pàgina

Tècnicament: s'emmagatzema al camp `meta_page_id` de `tenant_chat_links`.

#### 5. Crear el formulari de Lead Ads a Meta

1. Meta Ads Manager → Crear campanya → Objectiu: "Generació de leads"
2. A nivell d'anunci → "Formulari de lead instantani" → Crear nou
3. Camp "Nom complet" → mapeja a `full_name`
4. Camp "Correu electrònic" → mapeja a `email`
5. Camp "Telèfon" (opcional) → mapeja a `phone_number`
6. Publica l'anunci

#### 6. Afegir columna a la base de dades de producció

```sql
-- Camps UTM als leads
ALTER TABLE leads ADD COLUMN IF NOT EXISTS utm_source   VARCHAR(100);
ALTER TABLE leads ADD COLUMN IF NOT EXISTS utm_medium   VARCHAR(100);
ALTER TABLE leads ADD COLUMN IF NOT EXISTS utm_campaign VARCHAR(150);
ALTER TABLE leads ADD COLUMN IF NOT EXISTS meta_lead_id VARCHAR(50);

-- Meta Page ID al chat link
ALTER TABLE tenant_chat_links ADD COLUMN IF NOT EXISTS meta_page_id VARCHAR(30);
```

---

## Flux complet d'un lead de Meta Lead Ads

```
1. Usuari veu anunci a Instagram
2. Omple el formulari natiu (nom, email, telèfon)
3. Meta processa el lead → genera un leadgen_id
4. Meta envia POST a https://api.amgdl.com/api/v1/leads/meta-webhook
5. AMG rep: { "object":"page", "entry":[{ "id":"PAGE_ID", "changes":[{ "field":"leadgen", "value":{ "leadgen_id":"xxx", "page_id":"PAGE_ID" } }] }] }
6. AMG busca tenant per PAGE_ID → trobar tenant
7. AMG crida Graph API: GET /v19.0/{leadgen_id}?fields=field_data,created_time,platform&access_token=TOKEN
8. Graph API retorna: { "field_data":[{ "name":"full_name","values":["Maria"] }, { "name":"email","values":["maria@gmail.com"] }], "platform":"instagram" }
9. AMG crea Lead: source=INSTAGRAM, utmSource="instagram", utmMedium="lead_ads"
10. AMG envia notificació Telegram al tenant: "Nou lead de Instagram Ads: Maria (maria@gmail.com)"
```

---

## Dades que es guarden al CRM

| Camp | Valor | Notes |
|------|-------|-------|
| `source` | `FACEBOOK` / `INSTAGRAM` | Detecció automàtica pel camp `platform` |
| `utm_source` | `facebook` / `instagram` | Per anàlisi de campanyes |
| `utm_medium` | `lead_ads` | Identificador del tipus d'anunci |
| `utm_campaign` | `form_{FORM_ID}` | ID del formulari de Meta |
| `meta_lead_id` | `{leadgen_id}` | Per evitar duplicats |

---

## Proves i depuració

### Simular un lead de Meta (testing)

```bash
# Envia un event de prova al webhook
curl -X POST https://api.amgdl.com/api/v1/leads/meta-webhook \
  -H "Content-Type: application/json" \
  -d '{
    "object": "page",
    "entry": [{
      "id": "TU_PAGE_ID",
      "changes": [{
        "field": "leadgen",
        "value": {
          "leadgen_id": "TEST_LEAD_ID_123",
          "page_id": "TU_PAGE_ID",
          "form_id": "TEST_FORM_ID"
        }
      }]
    }]
  }'
```

### Verificar el webhook

Meta enviarà una petició GET de verificació quan configuris el webhook. Comprova als logs del backend:
```
INFO MetaLeadWebhookController: Meta webhook verificat correctament
```

### Eines de diagnòstic de Meta

- **Meta Webhooks Testing Tool:** Facebook App → Webhooks → Envia un missatge de prova
- **Meta Lead Ads Testing Tool:** [developers.facebook.com/tools/lead-ads-testing](https://developers.facebook.com/tools/lead-ads-testing)

---

## Seguretat

- El `META_WEBHOOK_VERIFY_TOKEN` ha de ser un secret aleatori (mínim 20 caràcters)
- El `META_PAGE_ACCESS_TOKEN` és un token d'accés sensible — NO el poseu al codi font
- Meta signa els payloads amb l'`App Secret` — pendent implementar verificació de signatura HMAC per a producció (TODO)
