# Mòdul 35 — Meta Ads Analytics (Cost per Lead)

## Objectiu

Connectar el compte de Meta Ads de cada tenant per obtenir dades de despesa per campanya i calcular el **cost per lead (CPL)** directament des del portal analític.

---

## Arquitectura

```
Meta Marketing API
    │
    │  GET /v19.0/act_{AD_ACCOUNT_ID}/insights
    ▼
MetaAdsSyncJob (@Scheduled 06:00 UTC)
    │
    ├── Per cada tenant amb Meta Ads activat
    ├── Obté despesa de "yesterday" per campanya
    └── Guarda a campaign_spend (dedup per tenant+campanyaId+data)
            │
            ▼
    MetaAdsService.getStats(tenantId)
            │
            ├── Sum spend per campaignName (últims 30 dies)
            ├── Count leads per utm_campaign (leads.utm_campaign)
            └── CPL = spend / leads (per campanya)
```

---

## Configuració pas a pas

### 1. Obtenir el System User Access Token

1. Meta Business Manager → Configuració del negoci → Usuaris → Usuaris del sistema
2. Crea un usuari del sistema (o usa el que existeix)
3. Assigna el compte d'anuncis amb permís `ads_read`
4. Genera un token d'accés → **copia el token** (és de llarga durada)

O bé usa un Page Token amb permís `ads_read` des de l'Explorador de Meta Graph.

### 2. Trobar l'Ad Account ID

Meta Ads Manager → URL → `https://business.facebook.com/adsmanager/manage/.../?act=**123456789**`

L'ID pot tenir format `123456789` o `act_123456789` — el sistema accepta ambdós.

### 3. Configurar al portal AMG

A la fitxa del tenant, secció **Meta Ads Analytics**:

| Camp | Valor |
|------|-------|
| Ad Account ID | L'ID numèric o `act_XXXXXX` |
| Access Token | System User Token amb `ads_read` |
| Activat | Toggle ON per habilitar la sincronització |

### 4. Configuració de la campanya a Meta Ads

Per a que els leads es correlacionin amb la despesa, les URLs de destinació dels anuncis han de tenir:

```
?utm_source=facebook&utm_medium=cpc&utm_campaign=NOM_CAMPANYA
```

El `NOM_CAMPANYA` ha de coincidir **exactament** (sense distinció de majúscules) amb el nom de la campanya a Meta Ads Manager.

---

## SQL de producció

```sql
CREATE TABLE IF NOT EXISTS meta_ads_configs (
    tenant_id UUID PRIMARY KEY,
    ad_account_id VARCHAR(50),
    access_token TEXT,
    enabled BOOLEAN NOT NULL DEFAULT false,
    last_sync_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS campaign_spend (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    campaign_id VARCHAR(50) NOT NULL,
    campaign_name VARCHAR(200) NOT NULL,
    spend NUMERIC(10,2),
    impressions BIGINT,
    clicks BIGINT,
    spend_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_campaign_spend UNIQUE (tenant_id, campaign_id, spend_date)
);

CREATE INDEX IF NOT EXISTS idx_campaign_spend_tenant_date ON campaign_spend (tenant_id, spend_date);
```

---

## API REST

| Mètode | URL | Descripció |
|--------|-----|------------|
| `GET` | `/api/v1/meta-ads/tenants/{tenantId}/config` | Obté la configuració |
| `PUT` | `/api/v1/meta-ads/tenants/{tenantId}/config` | Desa la configuració |
| `GET` | `/api/v1/meta-ads/tenants/{tenantId}/stats` | Estadístiques + CPL per campanya |
| `POST` | `/api/v1/meta-ads/tenants/{tenantId}/sync` | Força una sincronització manual |

### Exemple de resposta `GET /stats`

```json
{
  "campaigns": [
    {
      "campaignId": "23851234567890",
      "campaignName": "Juliol 2026",
      "spend": 45.67,
      "impressions": 12345,
      "clicks": 234,
      "leads": 8,
      "cpl": 5.71
    }
  ],
  "totalSpend": 45.67,
  "totalLeadsFromAds": 8,
  "avgCpl": 5.71,
  "period": "last_30d"
}
```

---

## Sincronització automàtica

- **Freqüència:** cada dia a les 06:00 UTC
- **Dades obtingudes:** despesa del dia anterior per campanya
- **Dedup:** no s'insereix si ja existeix `(tenantId, campaignId, spendDate)`
- **Primera sync:** s'executa automàticament el primer dia. Per obtenir dades històriques, cal un sync manual des del portal.

---

## Correlació lead ↔ campanya

La correlació es fa per **nom de campanya** (case-insensitive):
- `campaign_spend.campaign_name` ≈ `leads.utm_campaign`

**Limitació:** els leads captats via Meta Lead Ads natius (formulari dins de l'app) tenen `utm_campaign = "form_{FORM_ID}"` que **no coincidirà** amb el nom de la campanya de Meta Ads.

Per a una correlació perfecta, usa anuncis amb URL de destinació (click-through) i posa el nom de la campanya com a `utm_campaign`.

---

## Permisos necessaris

El token d'accés necessita el permís:
- `ads_read` — per llegir estadístiques de campanyes

**NO** cal `ads_management` — és de només lectura.
