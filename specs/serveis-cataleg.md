# Catàleg de Serveis AMG Digitalització

> **Versió:** 2.0
> **Data:** 2026-05-13
> **Propòsit:** Catàleg complet de serveis individuals que oferim, amb costos interns i preus de venda.
> Cada client rep una oferta personalitzada per **fases**. Cada fase conté 1 o més serveis.
> Tots els serveis individuals tenen el mateix preu mensual: **10 €/mes**.
> La tarifa d'enginyeria/hora (configurable): **50 €/h** (env var `ENGINEER_HOURLY_RATE`).

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

## 3. Fases d'implementació

Cada fase agrupa serveis amb un descompte al setup respecte a contractar-los solts. Les fases es configuren independentment.

### Fase 1 — Presència Digital

| Serveis inclosos | Setup per separat | Setup fase | Mensual fase |
|-----------------|------------------|-----------|-------------|
| Landing (2h) + Domini gestionat (1h + 12 €) | 100 + 62 = 162 € | 150 € (3h eng + 12 €) | 20 €/mes (2 serveis × 10 €) |

### Fase 2 — Comunicació i Automatització

| Serveis inclosos | Setup per separat | Setup fase | Mensual fase |
|-----------------|------------------|-----------|-------------|
| WhatsApp (2h) + 2 Automatitzacions bàsiques (2h) | 100 + 100 = 200 € | 180 € (4h eng) | 30 €/mes (3 serveis × 10 €) |

### Fase 3 — Intel·ligència Avançada

| Serveis inclosos | Setup per separat | Setup fase | Mensual fase |
|-----------------|------------------|-----------|-------------|
| Bot IA RAG (5h) + Automatització avançada (3h) | 250 + 150 = 400 € | 350 € (6h eng) | 20 €/mes (2 serveis × 10 €) |

---

## 4. Exemple de facturació

**Client: Restaurant Can Pedro**
- Contracta: Fase 1 (Presència Digital) el 10 de maig
- Setup: 150 € (facturat en acceptar)
- Implementació: 5 dies (del 10 al 15 de maig)
- Primera factura mensual (31 de maig):
  - 2 serveis × 10 € × (16 dies actius / 31 dies) = **10,32 €** pro rata
- Factures següents: **20 €/mes** complets

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

## 6. Notes per implementació

- [ ] Afegir `monthlyPrice` a l'entitat `CatalogService`
- [ ] El `cost` de `CatalogService` = setupHours × hourlyRate + directCosts
- [ ] `CatalogService.salePrice` = preu setup per al client
- [ ] `CatalogService.monthlyPrice` = 10 € per defecte
- [ ] El **setup** es cobra al crear el pressupost (mòdul 07 Billing)
- [ ] El **mensual** es factura a final de mes amb pro rata el primer mes
- [ ] La tarifa horària (`ENGINEER_HOURLY_RATE`) es pot modificar via variable d'entorn
- [ ] Les fases es creen com a `ServiceProfile` personalitzat per a cada client
