# Spec 37 — Tenant Document Builder

**Versió:** 1.0
**Data:** 2026-06-05
**Estat:** Esborrany
**Depèn de:** Spec 01 (Auth), Spec 02 (Vault), Spec 07 (Billing), Spec 25 (Omnichannel Inbox)

---

## 1. Visió general

El Tenant Document Builder (TDB) és un sistema multi-tenant per crear, personalitzar i generar documents comercials sense programar. Permet a qualsevol empresa dissenyar visualment plantilles de pressupostos, factures, albarans, contractes i altres documents, definir les dades necessàries i generar-los via IA o manualment.

L'objectiu és que el chatbot de l'agent IA (WhatsApp, Chat Widget) pugui generar documents automàticament durant una conversa responent a peticions com "Envia'm un pressupost per a 10 hores de neteja" o "Fes una factura per al client Joan Pérez".

---

## 2. Arquitectura

El sistema es compon de 5 mòduls independents:

| Mòdul | Responsabilitat |
|-------|----------------|
| **Document Templates** | Gestió de plantilles reutilitzables per tipus de document |
| **Layout Builder** | Editor visual drag & drop dels blocs de cada plantilla |
| **Data Sources** | Fonts de dades que consumeixen els blocs (empresa, client, document, variables, càlculs) |
| **Calculation Engine** | Variables configurables, paràmetres interns, fórmules i regles condicionals |
| **AI Assistant Layer** | Modificació de plantilles via llenguatge natural |

---

## 3. Document Templates

### 3.1 Entitats

#### DocumentTemplate

| Camp | Tipus | Descripció |
|------|-------|-----------|
| `id` | UUID (PK) | Identificador únic |
| `tenantId` | UUID (FK → Tenant) | Tenant propietari |
| `name` | VARCHAR(100) | Nom descriptiu de la plantilla |
| `documentType` | ENUM | Tipus de document (veure 3.2) |
| `version` | INTEGER | Versió actual (incrementa automàticament) |
| `active` | BOOLEAN | Si la plantilla està activa |
| `layout` | JSONB | Configuració del layout (blocs i posicions) |
| `dataBindings` | JSONB | Mapatge de dades per bloc |
| `styles` | JSONB | Estils globals de la plantilla |
| `createdAt` | TIMESTAMPTZ | Data de creació |
| `updatedAt` | TIMESTAMPTZ | Data de modificació |

#### DocumentTemplateVersion

| Camp | Tipus | Descripció |
|------|-------|-----------|
| `id` | UUID (PK) | Identificador únic |
| `templateId` | UUID (FK → DocumentTemplate) | Plantilla pare |
| `version` | INTEGER | Número de versió |
| `layout` | JSONB | Layout en aquesta versió |
| `dataBindings` | JSONB | Data bindings en aquesta versió |
| `styles` | JSONB | Estils en aquesta versió |
| `createdAt` | TIMESTAMPTZ | Data de creació |
| `notes` | TEXT | Nota opcional sobre el canvi |

#### GeneratedDocument

| Camp | Tipus | Descripció |
|------|-------|-----------|
| `id` | UUID (PK) | Identificador únic |
| `tenantId` | UUID (FK → Tenant) | Tenant propietari |
| `templateId` | UUID (FK → DocumentTemplate) | Plantilla usada |
| `templateVersion` | INTEGER | Versió de la plantilla usada |
| `number` | VARCHAR(50) | Número de document (autogenerat segons tenant) |
| `status` | ENUM | `DRAFT`, `FINALIZED`, `SENT`, `PAID`, `CANCELLED` |
| `customerId` | UUID (FK → Lead) | Client associat (opcional) |
| `customerData` | JSONB | Dades del client en el moment de generar |
| `variables` | JSONB | Valors de les variables en el moment de generar |
| `articles` | JSONB | Línies d'article/servei |
| `calculated` | JSONB | Resultats calculats (subtotal, tax, total) |
| `htmlContent` | TEXT | HTML generat |
| `pdfUrl` | VARCHAR(255) | URL del PDF a MinIO (si s'ha generat) |
| `generatedAt` | TIMESTAMPTZ | Data de generació |
| `createdAt` | TIMESTAMPTZ | Data de creació |

### 3.2 Document Types

| Type | Descripció |
|------|-----------|
| `quote` | Pressupost / Proposta econòmica |
| `invoice` | Factura |
| `delivery_note` | Albara / Justificant de lliurament |
| `contract` | Contracte de serveis |
| `report` | Informe / Diagnòstic |
| `proposal` | Proposta comercial |
| `custom` | Document personalitzat |

---

## 4. Layout Builder

### 4.1 Sistema de grid

- 12 columnes
- Altura variable per bloc (mínim 1 unitat, cada unitat ~50px)
- Cada bloc pot: moure's, redimensionar-se (width x height), ocultar-se, duplicar-se

### 4.2 Model de bloc (emmagatzemat a `layout` JSONB de la plantilla)

```json
{
  "id": "block-uuid",
  "type": "logo",
  "x": 0,
  "y": 0,
  "w": 4,
  "h": 2,
  "visible": true,
  "config": {
    "style": {
      "fontSize": 14,
      "fontWeight": "normal",
      "alignment": "left",
      "backgroundColor": "",
      "border": ""
    }
  },
  "dataBinding": {
    "source": "company",
    "field": "logo"
  }
}
```

### 4.3 Biblioteca de blocs

| Categoria | Bloc | ID |
|-----------|------|-----|
| Identitat | Logo | `logo` |
| Identitat | Info empresa | `company_info` |
| Identitat | Info client | `customer_info` |
| Document | Títol | `document_title` |
| Document | Número | `document_number` |
| Document | Data | `document_date` |
| Document | Validesa | `document_validity` |
| Comercial | Taula productes | `products_table` |
| Comercial | Taula serveis | `services_table` |
| Comercial | Subtotal | `subtotal` |
| Comercial | Descompte | `discount` |
| Comercial | Impost | `tax` |
| Comercial | Total | `total` |
| Contingut | Text | `text` |
| Contingut | Text enriquit | `rich_text` |
| Contingut | Imatge | `image` |
| Contingut | Separador | `separator` |
| Contingut | Salt de pàgina | `page_break` |
| Legal | Termes | `terms` |
| Legal | Condicions | `conditions` |
| Legal | Signatura | `signature` |
| Integracions | Codi QR | `qr_code` |
| Integracions | Codi barres | `barcode` |
| Integracions | Enllaç pagament | `payment_link` |

---

## 5. Data Sources

### 5.1 Fonts disponibles

Cada bloc pot bindejar-se a una font de dades. Les fonts disponibles són:

| Font | Descripció |
|------|-----------|
| `company` | Dades de l'empresa del tenant |
| `customer` | Dades del client (Lead) |
| `document` | Dades del document (número, data, validesa) |
| `variables` | Variables definides per l'usuari |
| `calculated` | Resultats dels càlculs |

### 5.2 Camps per font

**company:**
```json
{ "name": "Nom empresa", "taxId": "CIF/NIF", "phone": "Telèfon", "email": "Email", "address": "Adreça", "logo": "URL logo" }
```

**customer:**
```json
{ "name": "Nom client", "taxId": "CIF/NIF", "address": "Adreça", "phone": "Telèfon", "email": "Email" }
```

**document:**
```json
{ "number": "Número document", "date": "Data", "validUntil": "Data validesa" }
```

### 5.3 Placeholders dins de text

Els blocs de text suporten placeholders que es resolen en temps de generació:

```
Client: {{customer.name}}
Total: {{calculated.total}}
Validesa: {{document.validUntil}}
Referència: {{document.number}}
```

---

## 6. Calculation Engine

### 6.1 Variables configurables

L'usuari pot definir variables que demanarà el sistema en generar un document.

```json
{
  "name": "hours",
  "label": "Hores treballades",
  "type": "number",
  "required": true,
  "default": 0
}
```

Tipus disponibles: `text`, `number`, `decimal`, `boolean`, `date`, `select`

### 6.2 Paràmetres interns

Valors fixos usats en fórmules, configurables per tenant:

```json
{
  "hourPrice": 45,
  "meterPrice": 12.5,
  "discountThreshold": 20,
  "discountPercent": 10,
  "taxRate": 21
}
```

### 6.3 Fórmules

Suport per a expressions aritmètiques simples:

```
hours * hourPrice
meters * meterPrice
quantity * unitPrice
```

Els operadors suportats són: `+`, `-`, `*`, `/`, `(` ,`)`, parèntesis per agrupar.

### 6.4 Regles condicionals

```
IF hours > discountThreshold THEN discount = discountPercent
IF total > 1000 THEN discount = 15
```

---

## 7. AI Assistant Layer

### 7.1 Operacions via llenguatge natural

L'usuari pot modificar plantilles mitjançant llenguatge natural. La IA no modifica codi directament, sinó que retorna operacions estructurades.

**Exemples d'usuaris:**
- "Posa el logo a l'esquerra"
- "Afegeix una secció de signatura al final"
- "Mostra el telèfon del client"
- "Canvia el color del títol a blau fosc"
- "Afegeix la data de validesa al costat del número de document"

### 7.2 Format de sortida de la IA

```json
{
  "operations": [
    {
      "action": "move",
      "blockId": "logo",
      "x": 0,
      "y": 0
    },
    {
      "action": "update_style",
      "blockId": "document_title",
      "style": {
        "fontSize": 18,
        "fontWeight": "bold",
        "alignment": "center"
      }
    }
  ]
}
```

Accions suportades: `add_block`, `remove_block`, `move`, `resize`, `update_style`, `update_data_binding`, `set_visibility`, `duplicate_block`, `add_text_placeholder`

---

## 8. API REST

### 8.1 Endpoints

| Mètode | Path | Descripció |
|--------|------|-----------|
| `GET` | `/api/v1/documents/templates` | Llista plantilles del tenant |
| `POST` | `/api/v1/documents/templates` | Crea plantilla |
| `GET` | `/api/v1/documents/templates/{id}` | Obté plantilla |
| `PUT` | `/api/v1/documents/templates/{id}` | Actualitza plantilla (crea versió nova) |
| `DELETE` | `/api/v1/documents/templates/{id}` | Elimina plantilla (soft delete) |
| `POST` | `/api/v1/documents/templates/{id}/duplicate` | Duplica plantilla |
| `GET` | `/api/v1/documents/templates/{id}/versions` | Llista versions |
| `GET` | `/api/v1/documents/templates/{id}/versions/{version}` | Obté versió específica |
| `POST` | `/api/v1/documents/templates/{id}/restore/{version}` | Restaura versió |
| `GET` | `/api/v1/documents/templates/{id}/preview` | Previsualitza plantilla |
| `POST` | `/api/v1/documents/generate` | Genera document (HTML) |
| `POST` | `/api/v1/documents/generate/pdf` | Genera document (PDF) |
| `GET` | `/api/v1/documents/list` | Llista documents generats |
| `GET` | `/api/v1/documents/{id}` | Obté document generat |
| `GET` | `/api/v1/documents/{id}/pdf` | Descarrega PDF |
| `POST` | `/api/v1/documents/{id}/send` | Envia document per email |
| `POST` | `/api/v1/documents/ai/apply` | Aplica operacions IA a una plantilla |

### 8.2 Seguretat

Tots els endpoints requereixen autenticació JWT.
Els rols `ADMIN` i `SUPER_ADMIN` tenen accés complet.
El rol `CLIENT` pot veure només els documents del seu tenant.

---

## 9. Generació de documents

### 9.1 Procés

1. L'usuari selecciona una plantilla
2. El sistema demana les variables requerides (definides al Calculation Engine)
3. L'usuari introdueix articles/serveis (descripció, quantitat, preu unitari)
4. El sistema resol placeholders, aplica fórmules i regles condicionals
5. Genera HTML a partir del layout i les dades
6. Opcionalment, genera PDF via Puppeteer/Playwright al backend

### 9.2 Entrada

```json
{
  "templateId": "uuid",
  "customerId": "uuid-opcional",
  "customerData": {
    "name": "Joan Pérez",
    "taxId": "12345678A",
    "address": "C/ Major 1, Palma"
  },
  "variables": {
    "hours": 10
  },
  "articles": [
    { "description": "Neteja oficines", "quantity": 10, "unitPrice": 45 }
  ]
}
```

### 9.3 Sortides

- **HTML**: renderitzat en temps real per previsualització
- **PDF**: generat sota demanda, emmagatzemat a MinIO (Spec 06)
- **Email**: enviat via Brevo (sistema d'email transaccional existent)

---

## 10. Integració amb Agent IA

### 10.1 Generació automàtica des del chatbot

L'agent conversacional (Spec 20, Spec 25) pot generar documents automàticament durant una conversa. Quan el client diu "Envia'm un pressupost", l'agent:

1. Identifica el tipus de document necessari
2. Pregunta les dades necessàries (quantitat d'articles, hores, etc.)
3. Genera el document usant la plantilla per defecte del tenant
4. Envia l'enllaç al document/WP al client

### 10.2 Tags de conversa

Es defineixen aquests tags per a la integració amb el sistema de converses:

| Tag | Descripció |
|-----|-----------|
| `[GENERA_DOC:{tipus}:{variables}]` | Genera un document i envia al client |
| `[DOC_CREAT:{docId}:{url}]` | Resposta: document creat |

---

## 11. Multi-tenant

- Cada tenant té les seves pròpies plantilles, variables, fórmules, estils i documents
- No es comparteix informació entre tenants
- Cada document té un prefix de numeració configurable per tenant
- El logo i dades d'empresa s'obtenen del perfil del tenant

---

## 12. Casos d'ús

### 12.1 Pressupost ràpid des de WhatsApp

1. Client envia: "Hola, quant costaria netejar 100m²?"
2. Agent respon les dades i diu: "Puc generar-te un pressupost, vols que te l'enviï?"
3. Client: "Sí, si us plau"
4. Agent genera pressupost i enllaç al PDF

### 12.2 Factura des del portal

1. Admin del tenant accedeix al portal
2. Selecciona plantilla "Factura Estàndard"
3. Introdueix client, conceptes i quantitats
4. Previsualitza el resultat
5. Genera i descarrega PDF

### 12.3 Personalització de plantilles via IA

1. Admin: "Vull que el logo quedi centrat a dalt"
2. Sistema envia operació IA → la IA retorna `{ action: "move", blockId: "logo", x: 4, y: 0, w: 4 }`
3. Layout s'actualitza automàticament
4. Admin veu el canvi en temps real a la previsualització

---

## 13. Frontend

### 13.1 Rutes del portal

| Ruta | Descripció |
|------|-----------|
| `/portal/admin/documents` | Llista de plantilles de documents |
| `/portal/admin/documents/new` | Crea nova plantilla |
| `/portal/admin/documents/{id}/edit` | Editor visual de la plantilla |
| `/portal/admin/documents/{id}/preview` | Previsualització a pantalla completa |
| `/portal/admin/documents/generate/{id}` | Generar document |
| `/portal/admin/documents/list` | Llista de documents generats |
| `/portal/admin/documents/view/{id}` | Visualitzar document generat |

### 13.2 Pantalla d'editor

Dividida en dues meitats:
- **Esquerra:** selector de blocs + config del bloc seleccionat
- **Dreta:** renderització en temps real del document

El panell esquerre mostra:
- Paleta de blocs (drag per afegir al canvas)
- Configuració del bloc seleccionat (estils, data binding)
- Variables i paràmetres

El panell dret mostra el document renderitzat en HTML que s'actualitza instantàniament amb cada canvi.

---

## 14. QA / Test Cases

### 14.1 Templates
| # | Cas | Esperat |
|---|-----|---------|
| TC-01 | Crear plantilla quote | 201 + template amb layout buit |
| TC-02 | Crear plantilla sense nom | 400 |
| TC-03 | Obtenir plantilla per ID | 200 + dades correctes |
| TC-04 | Obtenir plantilla inexistent | 404 |
| TC-05 | Actualitzar plantilla | 200 + versió incrementada |
| TC-06 | Eliminar plantilla | 204 + active=false |
| TC-07 | Duplicar plantilla | 201 + layout heretat |
| TC-08 | Llistar versions | 200 + array amb històric |
| TC-09 | Restaurar versió anterior | 200 + layout restaurat |

### 14.2 Layout Builder
| # | Cas | Esperat |
|---|-----|---------|
| TC-10 | Afegir bloc al layout | 200 + bloc visible al layout |
| TC-11 | Moure bloc | 200 + coordenades actualitzades |
| TC-12 | Redimensionar bloc | 200 + width/height actualitzats |
| TC-13 | Ocultar bloc | 200 + visible=false |
| TC-14 | Bloc fora de grid | 400 |

### 14.3 Generation
| # | Cas | Esperat |
|---|-----|---------|
| TC-15 | Generar document (HTML) | 200 + HTML amb placeholders resolts |
| TC-16 | Generar document sense dades obligatòries | 400 |
| TC-17 | Generar PDF | 200 + URL de PDF |
| TC-18 | Placeholder desconegut | Apareix tal qual (sense resoldre) |
| TC-19 | Fórmula: `10 * 45` | 450 |
| TC-20 | Regla condicional: `IF hours > 20 THEN discount = 10` | 10% descompte si >20h |

### 14.4 AI Assistant
| # | Cas | Esperat |
|---|-----|---------|
| TC-21 | "Posa el logo a l'esquerra" | Operació move retornada |
| TC-22 | "Afegeix signatura" | Operació add_block retornada |
| TC-23 | Entrada buida | 400 |

### 14.5 Multi-tenancy
| # | Cas | Esperat |
|---|-----|---------|
| TC-24 | Tenant A veu només plantilles de A | OK |
| TC-25 | Tenant B no veu plantilles de A | OK (no error, array buit) |

---

## 15. SQL de creació de taules

```sql
CREATE TABLE document_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(100) NOT NULL,
    document_type VARCHAR(20) NOT NULL CHECK (document_type IN ('quote','invoice','delivery_note','contract','report','proposal','custom')),
    version INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT true,
    layout JSONB NOT NULL DEFAULT '[]',
    data_bindings JSONB NOT NULL DEFAULT '{}',
    styles JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE document_template_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES document_templates(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    layout JSONB NOT NULL,
    data_bindings JSONB NOT NULL DEFAULT '{}',
    styles JSONB NOT NULL DEFAULT '{}',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(template_id, version)
);

CREATE TABLE generated_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    template_id UUID NOT NULL REFERENCES document_templates(id),
    template_version INTEGER NOT NULL,
    number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','FINALIZED','SENT','PAID','CANCELLED')),
    customer_id UUID REFERENCES leads(id),
    customer_data JSONB NOT NULL DEFAULT '{}',
    variables JSONB NOT NULL DEFAULT '{}',
    articles JSONB NOT NULL DEFAULT '[]',
    calculated JSONB NOT NULL DEFAULT '{}',
    html_content TEXT,
    pdf_url VARCHAR(255),
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE document_number_sequences (
    tenant_id UUID NOT NULL PRIMARY KEY REFERENCES tenants(id),
    prefix VARCHAR(10) NOT NULL DEFAULT 'DOC',
    next_number INTEGER NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_document_templates_tenant ON document_templates(tenant_id);
CREATE INDEX idx_document_template_versions_template ON document_template_versions(template_id);
CREATE INDEX idx_generated_documents_tenant ON generated_documents(tenant_id);
CREATE INDEX idx_generated_documents_template ON generated_documents(template_id);
CREATE INDEX idx_generated_documents_status ON generated_documents(status);
```
