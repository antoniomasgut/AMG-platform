# Catàleg de Serveis AMG Digitalització

> **Versió:** 2.0
> **Data:** 2026-05-13
> **Propòsit:** Catàleg complet de serveis individuals que oferim, amb costos interns i preus de venda.
> Cada client rep una oferta personalitzada per **fases**. Cada fase conté 1 o més serveis.
> Tots els serveis individuals tenen el mateix preu mensual: **10 €/mes**.
> La tarifa d'enginyeria/hora (configurable): **50 €/h** (env var `ENGINEER_HOURLY_RATE`).

---

## 0. Relació amb el model NexeLocal (Spec 22)

Els 10 serveis del catàleg (`CatalogService`) són els **blocs tècnics** que s'assignen als tenants. El **preu que veu i paga el client**, però, ve del model de fases de NexeLocal (Spec 22), no de `CatalogService.salePrice` ni `monthlyPrice`.

| Capa | Entitat | Propòsit |
|------|---------|---------|
| Tècnica | `CatalogService` | Quins serveis existeixen i com es configuren |
| Comercial | `SectorPricing` | Quant paga el client (per sector, mida i fase) |

**Mapeig fases → serveis del catàleg:**

| Fase | Serveis inclosos |
|------|----------------|
| F1 | `whatsapp-business` + `bot-ia-basic` |
| F2 | `automatitzacio-basica` (recordatoris + agenda) |
| F3 | `automatitzacio-avancada` (pressupostos PDF) |
| F4 | `google-analytics` + addon fidelització |
| F5 | (gestió equip — pendent d'implementació) |

El setup cobrat al client és `SectorPricing.setupPrice`, no la suma dels `salePrice` individuals.

---

## 1. Model de costos

### 1.1 Tipus de costos per servei

| Tipus cost | Descripció | Quan es cobra |
|-----------|-----------|---------------|
| **Setup** | Cost únic d'implementació (hores enginyeria × tarifa + despeses directes) | En acceptar el pressupost |
| **Mensual** | Manteniment, monitorització automàtica (Telegram alerts), part del VPS (2 €/client), i **fins a 1 canvi menor al mes** | A final de mes, pro rata el primer mes |

### 1.2 Tarifa horària

```
ENGINEER_HOURLY_RATE = 50 €/h (configurable)
```

### 1.3 Canvis menors inclosos al mensual

Els **10 €/mes** per servei inclouen fins a **1 canvi menor al mes** per servei, com ara:

- Landing: modificar textos, imatges, o reordenar blocs
- Domini: canviar DNS, renovar SSL, modificar registres
- WhatsApp: actualitzar plantilles de missatges
- Automatització: ajustar condicions o destinataris d'un workflow existent
- Bot IA: ajustar prompts o actualitzar base de coneixement

Canvis més grans (disseny nou, automatització nova, reestructuració) o més d'1 al mes es facturen com a hores d'enginyeria a part.

### 1.4 Costos variables (no inclosos al mensual)

Alguns serveis tenen costos d'ús variables que van a càrrec del client:

- **WhatsApp Business API**: Meta cobra per conversa (~0.04-0.15 € segons tipus)
- **Bot IA**: tokens d'API (Claude/ChatGPT) segons ús
- **Domini**: renovació anual (~12 €/any)

---

## 2. Serveis individuals

Cada servei es pot contractar solt (add-on) o dins d'una fase. **Tots a 10 €/mes**.

### 2.1 Landing

| Servei | Descripció | Hores eng | Cost setup (eng) | Despeses | Setup total | Mensual |
|--------|-----------|-----------|-----------------|---------|------------|---------|
| **Landing** | Landing multi-bloc (hero, serveis, FAQ, testimonis, CTA, contacte). Domini personalitzat, SEO bàsic (OG tags, canonical, sitemap), hostatge, SSL, formulari RGPD. Plantilla professional adaptada al negoci. Inclou 1 canvi menor/mes. | 2 h | 100 € | 0 € | 100 € | 10 € |

### 2.2 Domini

| Servei | Descripció | Hores eng | Cost setup (eng) | Despeses | Setup total | Mensual |
|--------|-----------|-----------|-----------------|---------|------------|---------|
| **Domini autogestionat** | El client compra el domini, nosaltres donem instruccions CNAME i verifiquem DNS. | 0.5 h | 25 € | 0 € | 25 € | 10 € |
| **Domini gestionat** | Compra del domini a Namecheap, configuració DNS completa, SSL Let's Encrypt. Inclou 1r any de domini. | 1 h | 50 € | 12 € (1r any) | 62 € | 10 € |

### 2.3 WhatsApp

| Servei | Descripció | Hores eng | Cost setup (eng) | Despeses | Setup total | Mensual |
|--------|-----------|-----------|-----------------|---------|------------|---------|
| **WhatsApp Business API** | Integració amb Meta Cloud API. Webhook, plantilles de missatges, recepció de respostes. Inclou 1 canvi de plantilla/mes. | 2 h | 100 € | 0 € | 100 € | 10 € |

### 2.4 Automatització (n8n)

| Servei | Descripció | Hores eng | Cost setup (eng) | Despeses | Setup total | Mensual |
|--------|-----------|-----------|-----------------|---------|------------|---------|
| **Automatització bàsica** | Workflow n8n simple: 1-2 nodes. Ex: formulari → WhatsApp, lead → Google Sheets. Inclou 1 ajust/mes. | 1 h | 50 € | 0 € | 50 € | 10 € |
| **Automatització avançada** | Workflow n8n amb 3+ nodes, condicions, API externes. Ex: webhook → IA → CRM → WhatsApp. Inclou 1 ajust/mes. | 3 h | 150 € | 0 € | 150 € | 10 € |

### 2.5 Intel·ligència Artificial

| Servei | Descripció | Hores eng | Cost setup (eng) | Despeses | Setup total | Mensual |
|--------|-----------|-----------|-----------------|---------|------------|---------|
| **Bot IA bàsic** | Chatbot amb prompts predefinits. Sense memòria ni base de coneixement. Inclou 1 ajust de prompt/mes. | 2 h | 100 € | 0 € | 100 € | 10 € |
| **Bot IA avançat (RAG)** | Chatbot amb Retrieval-Augmented Generation. Base de coneixement pròpia (docs, FAQ, preus). Memòria de conversa. Inclou 1 actualització de KB/mes. | 5 h | 250 € | 0 € | 250 € | 10 € |

### 2.6 Altres

| Servei | Descripció | Hores eng | Cost setup (eng) | Despeses | Setup total | Mensual |
|--------|-----------|-----------|-----------------|---------|------------|---------|
| **SMTP Corporatiu** | Correu transaccional (SendGrid/Resend). Verificació de domini, plantilles. | 1 h | 50 € | 0 € | 50 € | 10 € |
| **Google Analytics** | GA4 + Search Console + Tags Manager. Dashboard mensual. | 1 h | 50 € | 0 € | 50 € | 10 € |

---

## 3. Fases NexeLocal (mapeig comercial → serveis tècnics)

El preu que paga el client ve de `SectorPricing` (Spec 22), no de la suma de `CatalogService.salePrice`. Veure `specs/22-sector-pricing.md` per a la matriu completa de preus per sector i mida.

Les fases **no són acumulatives** — el client pot contractar qualsevol combinació. El mensual total = suma dels tiers en ordre (la 1a fase contractada = priceF1, la 2a = priceF2, etc.), independentment de quines fases siguin.

| Fase | Serveis del catàleg inclosos | Setup client | Mensual base (autònom) |
|------|------------------------------|-------------|------------------------|
| **F1** Comunicació 24/7 | `whatsapp-business` + `bot-ia-basic` | des de 150€ | **59€/mes** (priceF1) |
| **F2** Gestió de cites | `automatitzacio-basica` (agenda + recordatoris) | — | **+20€/mes** (priceF2) |
| **F3** Pressupostos | `automatitzacio-avancada` (PDFs + seguiment) | — | **+20€/mes** (priceF3) |
| **F4** Fidelització | `google-analytics` + addon postvenda | — | **+30€/mes** (priceF4) |
| **F5** Equip | gestió d'equip (pendent) | — | **+20€/mes** (priceF5) |

**Exemples de combinacions (autònom base):**
- F1 → 59€/mes
- F1 + F3 → 59 + 20 = **79€/mes** (2 fases)
- F1 + F4 → 59 + 20 = **79€/mes** (2 fases, mateix preu)
- F1 + F2 + F3 → 59 + 20 + 20 = **99€/mes**
- F1 + F3 + F5 → 59 + 20 + 20 = **99€/mes** (3 fases, mateix preu)

**Ampliació de fase:** 75€ setup únic per cada nova fase afegida.

---

## 4. Exemple de facturació

**Client: Pintor autònom — contracta F1 + F3 el 10 de maig**
- Setup: 150 € (facturat en acceptar, via Billing)
- Fases contractades: `["F1", "F3"]` — 2 fases → priceF1 + priceF2 = 59 + 20 = **79 €/mes**
- Primera factura mensual (31 de maig): 79 € × (16 dies / 31 dies) = **40,77 €** pro rata
- Factures següents: **79 €/mes** complets (bloquejat a `TenantService.monthlyPriceLocked`)

**Mateix client afegeix F5 el mes següent:**
- 3 fases → priceF1 + priceF2 + priceF3 = 59 + 20 + 20 = **99 €/mes**
- Setup d'ampliació: 75 € únic

---

## 5. Mapa servei ↔ mòdul plataforma

| Servei | Tipus Vault | Mòdul |
|--------|------------|-------|
| Landing | LANDING | 04 Engine + 05 Factory |
| Domini | OTHER | 04 Engine (domain mgmt) |
| WhatsApp | CREDENTIALS | 10 Automations |
| Automatització | AUTOMATION | 10 Automations (n8n) |
| Bot IA | OTHER | 10 Automations |
| SMTP | CREDENTIALS | 10 Automations |

---

## 6. Implementació dels preus — estat i decisions

### 6.1 Model de preus (decisió final)

| Concepte | On viu | Qui el pot canviar | Afecta clients antics? |
|---------|--------|-------------------|----------------------|
| `CatalogService.salePrice` | Catàleg (BD) | SUPER_ADMIN via `PATCH /vault/services/{id}/price` | ❌ No |
| `CatalogService.monthlyPrice` | Catàleg (BD) | SUPER_ADMIN via `PATCH /vault/services/{id}/price` | ❌ No |
| `TenantService.setupPriceLocked` | Assignació (BD) | Ningú (congèlat en crear) | — |
| `TenantService.monthlyPriceLocked` | Assignació (BD) | Ningú (congèlat en crear) | — |

**Regla fonamental:** En crear un `TenantService` (assignació de perfil o add-on), copiar immediatament `salePrice → setupPriceLocked` i `monthlyPrice → monthlyPriceLocked`. A partir d'aquí, facturar sempre des del `TenantService`, mai des del `CatalogService`.

### 6.2 Checklist d'implementació

- [x] `CatalogService.salePrice` — preu de setup al client
- [x] `CatalogService.cost` — cost intern
- [x] `CatalogService.monthlyPrice` — 10€/mes per defecte
- [x] `TenantService.setupPriceLocked`, `monthlyPriceLocked`, `activatedAt` — congelat en crear
- [x] `CatalogSeeder` v2.0 — 10 serveis del catàleg definitiu
- [x] `CatalogService.version` — versió del servei al catàleg
- [x] `TenantService.catalogVersionLocked`, `outdated`, `outdatedAt` — tracking de desfàs
- [ ] **Endpoint `PATCH /vault/services/{id}/price`**: actualitza catàleg sense tocar clients existents
- [ ] **`BillingCalculator`**: lògica de pro-rata mensual (dies actius / dies del mes)
- [ ] **`MonthlyBillingJob`** (`@Scheduled`): genera factures a final de mes via FinOps
- [ ] **`SepaMandate`** entity + endpoints: registrar IBAN + titular + data signatura
- [ ] **`MonthlyInvoice`** entity + endpoints: factures recurrents separades de les de setup
- [ ] **`SepaXmlGenerator`**: genera fitxer `pain.008` per al banc
- [ ] El **pressupost** (Billing) ha de mostrar `totalSetup` + `totalMensualEstimat` com a informació addicional

### 6.3 Flux de preus (resum)

```
ADMIN crea servei al catàleg:
  CatalogService { salePrice: 100€, monthlyPrice: 10€, cost: 20€, version: 1 }

ADMIN assigna perfil a tenant (ara o en 2 anys):
  TenantService { setupPriceLocked: 100€, monthlyPriceLocked: 10€, catalogVersionLocked: 1 }

ADMIN modifica preu al catàleg:
  CatalogService { salePrice: 120€, monthlyPrice: 12€ }  ← client antic no es veu afectat

Client antic facturació mensual (sempre):
  monthlyPriceLocked = 10€  ← congèlat des del dia de l'assignació

Client nou (assignació posterior al canvi):
  TenantService { setupPriceLocked: 120€, monthlyPriceLocked: 12€, catalogVersionLocked: 1 }
```

---

## 7. Versionat de serveis i desfàs

### 7.1 Principi fonamental

**Un client que ha acceptat unes condicions no es veu mai afectat per canvis al catàleg.** El seu `TenantService` és un snapshot immutable del moment d'acceptació. Cap modificació al `CatalogService` (ni de preu, ni de descripció, ni de funcionament) toca el servei del client.

### 7.2 Mecanisme de versió

| Camp | Entitat | Descripció |
|------|---------|-----------|
| `version` | `CatalogService` | S'incrementa cada vegada que SUPER_ADMIN fa canvis rellevants |
| `catalogVersionLocked` | `TenantService` | Copia de `version` en el moment d'assignació |
| `outdated` | `TenantService` | `true` quan `catalogVersion > catalogVersionLocked` |
| `outdatedAt` | `TenantService` | Timestamp del moment en què es va marcar desfassat |

### 7.3 Flux de versió

```
SUPER_ADMIN millora el servei Landing (nova funció, canvi de proveïdor, etc.):
  POST /vault/catalog-services/{id}/bump
  → CatalogService.version: 1 → 2
  → Tots els TenantService amb aquest servei: outdated = true

ADMIN revisa la situació i parla amb el client:
  Opció A: El client no vol canvis
    POST /vault/tenants/{tenantId}/services/{serviceId}/acknowledge
    → outdated = false (catalogVersionLocked continua sent 1)
    → El client segueix amb la v1 per sempre

  Opció B: El client vol el servei nou (potser amb cost addicional)
    → Es fa manualment: nova implementació, nova facturació si escau
    → Quan estigui llest, acknowledge per netejar el flag
```

### 7.4 Endpoints de versió

| Mètode | Endpoint | Rol | Descripció |
|--------|---------|-----|-----------|
| `POST` | `/vault/catalog-services/{id}/bump` | SUPER_ADMIN | Incrementa versió, marca desfasats |
| `POST` | `/vault/tenants/{tenantId}/services/{serviceId}/acknowledge` | ADMIN/SUPER_ADMIN | Neteja flag `outdated` |
| `GET` | `/vault/outdated` | ADMIN/SUPER_ADMIN | Llista tots els serveis de client desfasats |

### 7.5 Política de quan fer bump

No tots els canvis al catàleg requereixen bump. Fer-ne un implica notificar (via flag) tots els clients afectats i haver de gestionar la conversa. Utilitzar quan:

| Acció | Bump? |
|-------|-------|
| Canvi de preu al catàleg | ❌ (els clients existents no es veuen afectats de totes formes) |
| Canvi de descripció o nom del servei | ❌ (cosmètic) |
| Canvi de proveïdor que afecta al client | ✅ |
| Nova funcionalitat rellevant per al client | ✅ |
| Canvi de procediment que requereix acció del client | ✅ |
| Reestructuració del servei (ex: landing ara inclou multilingüe) | ✅ |
