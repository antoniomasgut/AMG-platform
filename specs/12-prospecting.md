# Mòdul 12: Prospecting — Cerca i Gestió de Clients Potencials

> **Versió:** 1.0
> **Data:** 2026-05-13
> **Dependències:** Mòdul 01 (Auth), Mòdul 03 (Leads)

---

## 1. Objectius

- Cercar clients potencials per sector i localitat a Mallorca
- Generar llistats de prospects amb dades de contacte (nom, email, telèfon, web, xarxes socials)
- Enriquir els prospects amb informació pública (Google Maps, Instagram, web)
- Exportar prospects al Mòdul 03 (Leads) per iniciar el CRM
- Gestionar campanyes de prospecció per sector/zona

---

## 2. Abast

### 2.1 Funcionalitats incloses

- **Cerca per sector + localitat**: Buscar negocis a Mallorca per tipus (restaurants, botigues, etc.) i municipi
- **Fonts de dades**: Google Maps (scraping étic), Instagram Business, Páginas Amarillas
- **Enriquiment automàtic**: Extreure email, telèfon, web, xarxes socials, horaris, valoracions
- **Llistat de prospects**: Taula amb dades bàsiques i estat de prospecció
- **Exportació a Leads**: Seleccionar prospects i crear Leads al CRM (Mòdul 03)
- **Campanyes de prospecció**: Agrupar cerques per sector/ubicació amb estat i notes
- **Límit per font**: Control de rate limiting per no ser bloquejat

### 2.2 Funcionalitats excloses

- Scraping massiu / agressiu (només consultes públiques amb rate limiting)
- Compra de bases de dades de tercers
- Enviament automàtic d'emails o missatges (es fa des d'Automations)
- Verificació de contactes (es farà manualment)

### 2.3 Actors

| Actor | Descripció | Permisos |
|-------|-----------|----------|
| SUPER_ADMIN | Gestiona totes les campanyes de prospecció | Crear, executar, exportar |
| ADMIN | Operador comercial | Crear cerques, veure prospects, exportar a Leads |

---

## 3. Model de dades

### 3.1 Entitats (PostgreSQL)

#### ProspectCampaign (Campanya de prospecció)

Agrupa una cerca per sector + localitat.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| name | String(100) | @Column(nullable=false) | Nom humà (ex: "Restaurants Palma") |
| sector | String(50) | @Column(nullable=false) | Sector a cercar (ex: "restaurant", "perruqueria") |
| location | String(100) | @Column(nullable=false) | Localitat (ex: "Palma", "Inca", "Manacor") |
| source | Enum(STRING) | @Enumerated | `GOOGLE_MAPS`, `INSTAGRAM`, `PAGINAS_AMARILLAS`, `MANUAL` |
| status | Enum(STRING) | @Enumerated | `DRAFT`, `IN_PROGRESS`, `COMPLETED`, `FAILED` |
| totalFound | Integer | @Builder.Default | Nombre total de prospects trobats |
| totalExported | Integer | @Builder.Default | Quants s'han exportat a Leads |
| searchParams | TEXT(JSON) | @Column(columnDefinition="TEXT") | Paràmetres de cerca (JSON) |
| notes | String(500) | @Column | Notes internes |
| createdBy | UUID | @Column(nullable=false) | FK a User |
| createdAt | Instant | @CreatedDate | |
| updatedAt | Instant | @LastModifiedDate | |

**Índex:** (sector, location), (status)

#### Prospect (Client potencial)

Un negoci trobat durant la prospecció.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| campaignId | UUID | @Column(nullable=false) | FK a ProspectCampaign |
| name | String(150) | @Column(nullable=false) | Nom del negoci |
| description | String(500) | @Column | Descripció breu |
| sector | String(50) | @Column | Sector detectat |
| address | String(255) | @Column | Adreça completa |
| city | String(100) | @Column | Municipi |
| postalCode | String(10) | @Column | Codi postal |
| phone | String(20) | @Column | Telèfon |
| email | String(100) | @Column | Email (si es troba) |
| website | String(300) | @Column | URL del web |
| instagram | String(100) | @Column | Usuari d'Instagram |
| googleRating | BigDecimal(2,1) | @Column | Valoració (0.0-5.0) |
| googleReviews | Integer | @Column | Nombre de ressenyes |
| googlePlaceId | String(100) | @Column(unique=true) | ID de Google Places |
| hasWebsite | Boolean | @Builder.Default | Si té web pròpia |
| hasInstagram | Boolean | @Builder.Default | Si té Instagram |
| hasWhatsapp | Boolean | @Builder.Default | Si té WhatsApp Business |
| status | Enum(STRING) | @Enumerated | `NEW`, `CONTACTED`, `QUALIFIED`, `EXPORTED`, `DISCARDED` |
| source | Enum(STRING) | @Enumerated | Font d'origen |
| externalId | String(100) | @Column | ID a la font externa |
| leadId | UUID | @Column | FK a Lead (Mòdul 03) si s'exporta |
| notes | String(500) | @Column | Notes internes |
| createdAt | Instant | @CreatedDate | |
| updatedAt | Instant | @LastModifiedDate | |

**Índex:** (campaignId), (status), (sector, city), googlePlaceId (unique)

### 3.2 Enums

```java
public enum ProspectSource {
    GOOGLE_MAPS, INSTAGRAM, PAGINAS_AMARILLAS, MANUAL
}

public enum CampaignStatus {
    DRAFT, IN_PROGRESS, COMPLETED, FAILED
}

public enum ProspectStatus {
    NEW, CONTACTED, QUALIFIED, EXPORTED, DISCARDED
}
```

---

## 4. Endpoints API

Prefix base: `/api/v1/prospecting`

| Mètode | Ruta | @PreAuthorize | Descripció |
|--------|------|--------------|-----------|
| POST | /api/v1/prospecting/campaigns | SUPER_ADMIN, ADMIN | Crear nova campanya |
| GET | /api/v1/prospecting/campaigns | SUPER_ADMIN, ADMIN | Llistar campanyes |
| GET | /api/v1/prospecting/campaigns/{id} | SUPER_ADMIN, ADMIN | Detall de campanya |
| POST | /api/v1/prospecting/campaigns/{id}/run | SUPER_ADMIN, ADMIN | Executar cerca (scraping) |
| PUT | /api/v1/prospecting/campaigns/{id} | SUPER_ADMIN, ADMIN | Actualitzar campanya |
| DELETE | /api/v1/prospecting/campaigns/{id} | SUPER_ADMIN | Eliminar campanya |
| GET | /api/v1/prospecting/campaigns/{id}/prospects | SUPER_ADMIN, ADMIN | Llistar prospects d'una campanya |
| PUT | /api/v1/prospecting/prospects/{id} | SUPER_ADMIN, ADMIN | Actualitzar estat/notes d'un prospect |
| POST | /api/v1/prospecting/prospects/{id}/enrich | SUPER_ADMIN, ADMIN | Enriquir prospect (cercar email, xarxes) |
| POST | /api/v1/prospecting/prospects/{id}/export | SUPER_ADMIN, ADMIN | Exportar prospect a Leads (Mòdul 03) |
| POST | /api/v1/prospecting/campaigns/{id}/export-all | SUPER_ADMIN, ADMIN | Exportar tots els prospects d'una campanya a Leads |

### Detall d'endpoints

#### `POST /api/v1/prospecting/campaigns` — Crear campanya

Request:
```json
{
  "name": "Restaurants Palma",
  "sector": "restaurant",
  "location": "Palma",
  "source": "GOOGLE_MAPS",
  "searchParams": {
    "keywords": ["restaurant", "menjar", "taula"],
    "maxResults": 50,
    "minRating": 3.5
  },
  "notes": "Campanya per trobar restaurants a Palma sense web"
}
```

Response 201: ProspectCampaign

#### `POST /api/v1/prospecting/campaigns/{id}/run` — Executar cerca

Dispara el scraper per a la campanya. Retorna els prospects trobats en temps real o en background.

Response 202:
```json
{
  "campaignId": "uuid",
  "status": "IN_PROGRESS",
  "estimatedDuration": "30s"
}
```

#### `POST /api/v1/prospecting/prospects/{id}/enrich` — Enriquir prospect

Busca informació addicional del negoci:
- Web scraping de la seva web (si en té)
- Cerca d'email via WhoIs o contact page
- Instagram scraping (si és públic)
- WhatsApp Business detection

Response 200: Prospect actualitzat amb noves dades

#### `POST /api/v1/prospecting/prospects/{id}/export` — Exportar a Leads

Crea un Lead al Mòdul 03 amb les dades del prospect.

Response 201:
```json
{
  "prospectId": "uuid",
  "leadId": "uuid",
  "leadUrl": "/api/v1/leads/uuid",
  "status": "EXPORTED"
}
```

---

## 5. Serveis

### 5.1 ProspectScraper (Interface)

```java
public interface ProspectScraper {
    List<ProspectResult> search(String sector, String location, Map<String, Object> params);
    ProspectEnrichment enrich(String externalId, String website);
    boolean isRateLimited();
    String getSourceName();
}
```

### 5.2 Implementacions

- **GoogleMapsScraper**: Cerca negocis a Google Maps (via Places API o scraping)
- **InstagramScraper**: Cerca perfils d'Instagram per sector/ubicació
- **ManualScraper**: Entrada manual de dades (no fa scraping)

Totes les implementacions tenen rate limiting incorporat (màxim X peticions per minut).

### 5.3 ProspectingService

```java
public interface ProspectingService {
    ProspectCampaign createCampaign(CreateCampaignRequest request);
    ProspectCampaign getCampaign(UUID campaignId);
    Page<ProspectCampaign> listCampaigns(String sector, String location, int page, int size);
    ProspectCampaign updateCampaign(UUID campaignId, UpdateCampaignRequest request);
    void deleteCampaign(UUID campaignId);

    ProspectCampaign runCampaign(UUID campaignId);

    Page<Prospect> listProspects(UUID campaignId, String status, int page, int size);
    Prospect updateProspect(UUID prospectId, UpdateProspectRequest request);
    Prospect enrichProspect(UUID prospectId);
    LeadExportResponse exportProspect(UUID prospectId);
    int exportAllProspects(UUID campaignId);
}
```

---

## 6. Integració amb Google Places API

### 6.1 API

- Base URL: `https://maps.googleapis.com/maps/api/place`
- Autenticació: API Key (`GOOGLE_PLACES_API_KEY`)
- Endpoints:
  - `POST /maps/api/place/textsearch/json` — Cerca textual per sector + ubicació
  - `GET /maps/api/place/details/json` — Detall d'un negoci (email, web, horaris)

### 6.2 Rate limiting

- 10 peticions per segon (límits de Google)
- El scraper ha d'esperar entre peticions
- Si es supera el límit, Google retorna `OVER_QUERY_LIMIT`

### 6.3 Flux de cerca

```
1. ADMIN crea campanya: sector = "restaurant", location = "Palma"
2. ADMIN executa campanya: POST /campaigns/{id}/run
3. Sistema:
   a. Crida Google Places Text Search: "restaurants a Palma, Mallorca"
   b. Obté llistat de negocis (nom, adreça, rating, placeId)
   c. Per a cada negoci:
      - Crida Place Details per obtenir telèfon, web, horaris
      - Detecta si té web pròpia (hasWebsite)
      - Guarda com a Prospect (status = NEW)
   d. Quan acaba, marca campanya com COMPLETED
4. ADMIN revisa els prospects
5. ADMIN pot enriquir: cercar email, Instagram
6. ADMIN exporta a Leads els prospects interessants
```

---

## 7. Configuració

### application.yml

```yaml
app:
  prospecting:
    google:
      places-api-key: ${GOOGLE_PLACES_API_KEY}
      rate-limit-per-second: 10
    enrichment:
      timeout-seconds: 10
      max-concurrent: 3
    default-max-results: 50
```

---

## 8. Seguretat

- Les API Keys de Google Places s'emmagatzemen al Vault (Mòdul 02)
- Scraping només de dades públiques (no autenticades)
- Rate limiting per evitar bloqueig de fonts externes
- CLIENT no té accés a prospecció

---

## 9. Tests d'integració

12 tests mínims (patró: `ProspectingControllerTest.java`):

| # | Test | Esperat |
|---|------|---------|
| 1 | Crear campanya | 201 |
| 2 | Llistar campanyes buit | 200 |
| 3 | Executar campanya amb MockScraper | 202, prospects creats |
| 4 | Llistar prospects d'una campanya | 200 |
| 5 | Actualitzar estat d'un prospect | 200 |
| 6 | Enriquir prospect (mock) | 200, dades addicionals |
| 7 | Exportar prospect a Leads | 201, leadId no null |
| 8 | Exportar tots els prospects | 200, count > 0 |
| 9 | CLIENT no pot accedir | 403 |
| 10 | Sense JWT | 401 |
| 11 | Eliminar campanya | 204 |
| 12 | Prospect ja exportat no es pot re-exportar | 409 |

---

## 10. QA / Casos de prova

| # | Escenari | Esperat |
|---|----------|---------|
| PRO-01 | Cercar "restaurants a Palma" → 25 resultats | Campaign COMPLETED, 25 prospects |
| PRO-02 | Enriquir un restaurant amb web | email trobat (o no), Instagram detectat |
| PRO-03 | Exportar 5 prospects a Leads | 5 Leads creats, prospects EXPORTED |
| PRO-04 | Cercar mateix sector+ubicació dos cops | No duplicar prospects (deduplicació per googlePlaceId) |
| PRO-05 | Google API key no configurada | 400 "Google Places not configured" |
| PRO-06 | Rate limit excedit | Esperar i reintentar, o error clar |

---

## 11. Resum d'entitats

| Entitat | Repositori | DTOs | Endpoints |
|---------|-----------|------|-----------|
| ProspectCampaign | ProspectCampaignRepository | CreateCampaignRequest, CampaignResponse | CRUD + run |
| Prospect | ProspectRepository | UpdateProspectRequest, ProspectResponse, LeadExportResponse | llistar, update, enrich, export |

---

## 12. Obert / Pendents

- [ ] Decidir si Google Places API key es posa directament a application.yml o es guarda al Vault
- [ ] MockScraper: implementar per tests (retorna dades falses)
- [ ] Deduplicació: evitar crear el mateix prospect dos cops (per googlePlaceId)
- [ ] Campanyes programades: executar automàticament cada setmana/mes
- [ ] Notificació quan una campanya acaba (Telegram)
- [ ] Export massiu: crear Leads en batch (transacció)
