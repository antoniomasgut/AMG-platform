# Mòdul 04: Engine — Landing Renderer

> **Versió:** 1.1
> **Data:** 2026-05-13
> **Dependències:** Mòdul 01 (Auth), Mòdul 02 (Vault)

---

## 1. Objectius

- Renderitzar i servir landings statiques per als clients als seus dominis personalitzats
- Gestionar el cicle de vida de les landings: esborrany → publicació → despublicació
- Suportar dos fluxos de domini: **autogestionat** (el client compra i configura el DNS) i **gestionat** (nosaltres comprem i gestionem el domini)
- Suportar blocs de contingut (hero, serveis, formularis, FAQ, etc.) amb estils configurables
- Integrar-se amb el Vault per verificar que el client té el servei LANDING actiu
- Renderitzar landings a alt rendiment amb cache i SSG/ISR

---

## 2. Abast

### 2.1 Funcionalitats incloses

- CRUD de landings per tenant
- Editor de landings per blocs (drag & drop no — lògica de dades sí)
- Publicació / despublicació de landings
- Històric de versions (esborrany + publicada)
- Dos fluxos de domini personalitzat:
  - **Autogestionat**: el client compra el domini, afegeix el CNAME, nosaltres verifiquem
  - **Gestionat**: nosaltres comprem el domini (Namecheap, etc.) amb dades del client, configurem el DNS, i el client paga la renovació
- Seguiment d'estat del domini (pending purchase, purchased, dns pending, verified, expired)
- Dades de registre del domini (registrant name, email, phone)
- SSL automàtic via Let's Encrypt / Traefik (independent del flux)
- Integració amb serveis LANDING del Vault (verificació d'accés)
- Formularis de contacte a les landings (recollida de leads)
- Cache de landings a Redis (HTML renderitzat)
- Mapa del lloc (sitemap.xml) automàtic per SEO

### 2.2 Funcionalitats excloses

- Editor visual drag & drop (es fa al Mòdul 05 Factory)
- Gestió d'assets / imatges (Mòdul 06 Assets)
- Automatitzacions post-formulari (Mòdul 10 Automations)
- Analítiques de visites (futur)
- Connexió amb APIs de registradors de dominis (es fa manualment des del panell d'admin)

### 2.3 Actors

| Actor | Descripció | Permisos |
|-------|-----------|----------|
| SUPER_ADMIN | Opera tota la plataforma | CRUD totes les landings, publicar/despublicar, gestionar dominis (managed i autogestionats) |
| ADMIN | Gestiona landings dels seus tenants | CRUD landings assignades, publicar/despublicar, gestionar dominis |
| CLIENT | Propietari de la landing | Editar esborranys, configurar domini (només autogestionat), veure estat. NO pot publicar |
| Visitants | Públic general | Veure la landing renderitzada (NO autenticat) |

---

## 3. Model de dades

### 3.1 Enums

#### DomainStatus

| Valor | Descripció |
|-------|-----------|
| NOT_CONFIGURED | No s'ha configurat cap domini |
| PENDING_PURCHASE | Domini gestionat: pendent de compra |
| PURCHASED | Domini gestionat: comprat (esperant DNS) |
| DNS_PENDING | Domini configurat, pendent de verificació DNS |
| VERIFIED | DNS verificat, SSL actiu |
| EXPIRED | Domini caducat |

### 3.2 Entitats (PostgreSQL)

#### Landing (Pàgina)

Defineix una landing page d'un tenant.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| tenantId | UUID | @Column(nullable=false) | FK a Tenant |
| serviceId | UUID | @Column(nullable=false) | FK a CatalogService (el servei LANDING associat) |
| title | String(200) | @Column(nullable=false) | Títol de la landing |
| slug | String(100) | @Column(nullable=false) | URL-friendly (ex: "restaurant-can-pedro") |
| metaDescription | String(300) | @Column | Meta description per SEO |
| ogImageUrl | String(500) | @Column | URL de la imatge OG |
| status | Enum(STRING) | @Enumerated @Builder.Default | `DRAFT` — estat propi de la landing |
| publishedVersionId | UUID | @Column(nullable=true) | FK a LandingVersion (versió publicada, null=no publicada) |
| customDomain | String(200) | @Column(unique, nullable=true) | Domini personalitzat (ex: "canpedro.com") |
| domainVerified | Boolean | @Column | Si el domini ha estat verificat |
| managedDomain | Boolean | @Column | Si nosaltres gestionem el domini (compra + DNS) |
| domainStatus | Enum(STRING) | @Enumerated @Column | Estat del domini (veure enum DomainStatus) |
| domainRegistrar | String(100) | @Column | Registrador on es va comprar (ex: "Namecheap") |
| domainRenewalDate | LocalDate | @Column | Propera data de renovació |
| domainRenewalPrice | BigDecimal | @Column(p=8,s=2) | Preu de renovació anual |
| domainOwnerName | String(150) | @Column | Nom del titular del domini |
| domainOwnerEmail | String(200) | @Column | Email del titular |
| domainOwnerPhone | String(30) | @Column | Telèfon del titular |
| isActive | Boolean | @Column(nullable=false) | Si la landing està activa |
| createdAt | Instant | @CreatedDate | |
| updatedAt | Instant | @LastModifiedDate | |

**Restricció única:** `(tenantId, slug)` — un tenant no pot tenir dues landings amb el mateix slug

**Sobre l'estat:** En crear una landing, es crea automàticament una LandingVersion DRAFT. El `status` de la Landing es deriva de `publishedVersionId`: si no és null → PUBLISHED, si és null → DRAFT. En publicar, s'actualitza `publishedVersionId` i `status=PUBLISHED`. En despublicar, `publishedVersionId=null` i `status=DRAFT`.

#### LandingVersion (Versió de la pàgina)

Cada versió conté el contingut complet de la landing en format JSON.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| landingId | UUID | @Column(nullable=false) | FK a Landing |
| versionNumber | Integer | @Column(nullable=false) | Número de versió (1, 2, 3...) |
| status | Enum(STRING) | @Column(nullable=false) | `DRAFT`, `PUBLISHED`, `ARCHIVED` |
| content | TEXT(JSON) | @Column(columnDefinition="TEXT", nullable=false) | JSON amb blocs de la landing |
| styles | TEXT(JSON) | @Column(columnDefinition="TEXT") | JSON amb estils globals (colors, fonts) |
| publishedAt | Instant | @Column | Quan es va publicar |
| createdAt | Instant | @CreatedDate | |

**Restricció única:** `(landingId, versionNumber)`

**Exemple de `content`:**
```json
{
  "blocks": [
    {
      "id": "blk_1",
      "type": "hero",
      "props": {
        "title": "Restaurant Can Pedro",
        "subtitle": "Cuina mallorquina des de 1985",
        "ctaText": "Reserva taula",
        "ctaUrl": "https://canpedro.com/reserva",
        "bgImageUrl": "https://assets.amg.cat/canpedro/hero.jpg"
      }
    },
    {
      "id": "blk_2",
      "type": "services",
      "props": {
        "title": "Els nostres serveis",
        "items": [
          { "icon": "food", "title": "Menú del dia", "description": "12€ amb pa i beguda" },
          { "icon": "cake", "title": "Esdeveniments", "description": "Casaments, comunions, celebracions" }
        ]
      }
    },
    {
      "id": "blk_3",
      "type": "contact-form",
      "props": {
        "title": "Contacta'ns",
        "email": "info@canpedro.com",
        "phone": "+34 971 123 456"
      }
    }
  ]
}
```

**Exemple de `styles`:**
```json
{
  "fontFamily": "'Inter', sans-serif",
  "primaryColor": "#1a365d",
  "secondaryColor": "#2b6cb0",
  "bgColor": "#ffffff",
  "textColor": "#1a202c",
  "borderRadius": "8px"
}
```

#### ContactLead (Lead de formulari)

Quan un visitant omple un formulari a la landing, es guarda aquí.

| Camp | Tipus | Mapeig JPA | Descripció |
|------|-------|-----------|------------|
| id | UUID | @Id @GeneratedValue | |
| landingId | UUID | @Column(nullable=false) | FK a Landing |
| name | String(150) | @Column | Nom del visitant |
| email | String(200) | @Column | Email |
| phone | String(30) | @Column | Telèfon |
| message | TEXT | @Column | Missatge |
| metadata | TEXT(JSON) | @Column(columnDefinition="TEXT") | Dades extra del formulari |
| createdAt | Instant | @CreatedDate | |

### 3.3 Blocs de landing (frontend)

Tipus de blocs suportats al render:

| Bloc | Props | Descripció |
|------|-------|-----------|
| `hero` | title, subtitle, ctaText, ctaUrl, bgImageUrl | Capçalera amb CTA |
| `text` | title, body, textAlign | Bloc de text simple |
| `services` | title, items[{icon, title, description}] | Graella de serveis |
| `gallery` | title, images[{url, alt, caption}] | Galeria d'imatges |
| `contact-form` | title, email, phone, address | Formulari de contacte |
| `faq` | title, items[{question, answer}] | Preguntes freqüents |
| `testimonials` | title, items[{name, text, avatarUrl}] | Testimonials de clients |
| `cta` | text, buttonText, buttonUrl | CTA simple |
| `footer` | companyName, email, phone, socialLinks | Peu de pàgina |
| `map` | address, lat, lng | Mapa incrustat (OpenStreetMap) |

---

## 4. API REST

Prefix base: `/api/v1/engine`

### 4.1 Gestió de landings

#### `POST /api/v1/engine/tenants/{tenantId}/landings` — Crear landing

**Rols:** SUPER_ADMIN, ADMIN

Request:
```json
{
  "title": "Restaurant Can Pedro",
  "slug": "restaurant-can-pedro",
  "metaDescription": "Restaurant de cuina mallorquina a Palma",
  "serviceId": "uuid-del-servei-landing-pro"
}
```

Response 201:
```json
{
  "id": "uuid",
  "title": "Restaurant Can Pedro",
  "slug": "restaurant-can-pedro",
  "status": "DRAFT",
  "versions": [],
  "publicUrl": null,
  "domainStatus": "NOT_CONFIGURED",
  "createdAt": "2026-05-13T10:00:00Z"
}
```

**Errors:**
| Codi | Situació |
|------|---------|
| 400 | Slug duplicat dins el tenant |
| 403 | Tenant no té el servei LANDING actiu |

#### `GET /api/v1/engine/tenants/{tenantId}/landings` — Llistar landings

**Rols:** SUPER_ADMIN, ADMIN, CLIENT (propi)

**Query:** `page`, `size`, `search`

#### `GET /api/v1/engine/tenants/{tenantId}/landings/{id}` — Veure landing

**Rols:** SUPER_ADMIN, ADMIN, CLIENT (propi)

#### `PUT /api/v1/engine/tenants/{tenantId}/landings/{id}` — Actualitzar landing (metadades)

**Rols:** SUPER_ADMIN, ADMIN

#### `DELETE /api/v1/engine/tenants/{tenantId}/landings/{id}` — Eliminar landing (soft delete)

**Rols:** SUPER_ADMIN, ADMIN

Response 204

### 4.2 Gestió de versions

#### `POST /api/v1/engine/landings/{landingId}/versions` — Crear versió (esborrany)

**Rols:** SUPER_ADMIN, ADMIN

Request:
```json
{
  "content": { "blocks": [...] },
  "styles": { "fontFamily": "...", "primaryColor": "..." }
}
```

Response 201: versió creada amb status `DRAFT`

#### `PUT /api/v1/engine/landings/{landingId}/versions/{versionId}` — Actualitzar versió esborrany

**Rols:** SUPER_ADMIN, ADMIN

Només es pot actualitzar si status = `DRAFT`.

#### `POST /api/v1/engine/landings/{landingId}/publish` — Publicar versió

**Rols:** SUPER_ADMIN, ADMIN

Agafa l'últim esborrany i el publica. La landing esdevé accessible.

Response 200:
```json
{
  "versionNumber": 3,
  "status": "PUBLISHED",
  "publicUrl": "https://canpedro.com",
  "publishedAt": "2026-05-13T10:00:00Z"
}
```

#### `POST /api/v1/engine/landings/{landingId}/unpublish` — Despublicar

**Rols:** SUPER_ADMIN, ADMIN

La landing deixa de ser accessible. El domini NO es desconfigura.

### 4.3 Dominis personalitzats

Dos fluxos:
- **Autogestionat** (`managed: false`): el client té el domini, nosaltres només verifiquem el CNAME
- **Gestionat** (`managed: true`): nosaltres comprem el domini. Calen dades del titular i data de renovació

#### `PUT /api/v1/engine/landings/{landingId}/domain` — Configurar domini

**Rols:** SUPER_ADMIN, ADMIN

El CLIENT no pot configurar dominis (de moment ho fem nosaltres).

Request (autogestionat):
```json
{
  "domain": "canpedro.com",
  "managed": false
}
```

Request (gestionat — només SUPER_ADMIN/ADMIN):
```json
{
  "domain": "canpedro.com",
  "managed": true,
  "registrar": "Namecheap",
  "renewalDate": "2027-05-13",
  "renewalPrice": 12.99,
  "ownerName": "Joan Servera",
  "ownerEmail": "joan@canpedro.com",
  "ownerPhone": "+34600123456"
}
```

Response 200:
```json
{
  "domain": "canpedro.com",
  "managed": false,
  "domainStatus": "DNS_PENDING",
  "dnsInstructions": "Afegeix un registre CNAME de canpedro.com a landings.amg.cat",
  "renewalDate": null,
  "renewalPrice": null
}
```

Per dominis gestionats, el `domainStatus` es posa a `PURCHASED` (si ja està comprat) o `PENDING_PURCHASE`.

**Errors:**
| Codi | Situació |
|------|---------|
| 400 | Domini ja configurat en una altra landing |
| 403 | CLIENT intenta crear un domini gestionat |

#### `GET /api/v1/engine/landings/{landingId}/domain/verify` — Verificar domini

**Rols:** SUPER_ADMIN, ADMIN

Comprova que el CNAME apunta al nostre servidor. Si OK, marca `domainVerified=true`, `domainStatus=VERIFIED` i configura SSL via Traefik.

Funciona igual per fluxos autogestionats i gestionats.

#### `POST /api/v1/engine/landings/{landingId}/domain/status` — Actualitzar estat del domini

**Rols:** SUPER_ADMIN, ADMIN

Permet canviar manualment l'estat del domini (per quan nosaltres comprem el domini o es renova).

Request:
```json
{
  "domainStatus": "PURCHASED",
  "registrar": "Namecheap",
  "renewalDate": "2027-05-13",
  "renewalPrice": 12.99
}
```

#### `DELETE /api/v1/engine/landings/{landingId}/domain` — Desconfigurar domini

**Rols:** SUPER_ADMIN, ADMIN

Torna `customDomain` a null i `domainStatus` a `NOT_CONFIGURED`.

### 4.4 Render públic (NO auth)

#### `GET /api/v1/engine/render/{slug}` — Renderitzar landing per slug

**Autenticació:** Cap (públic)

Busca la landing pel slug + domini de la request (`Host` header). Retorna HTML renderitzat amb cache.

**Cache:** Redis amb TTL de 5 minuts (invalidació en publicar nova versió)

**Response 200:** HTML complet amb CSS inline i OG tags

**Response 404:** Landing no trobada o no publicada

#### `GET /api/v1/engine/render/{slug}/sitemap.xml` — Sitemap

**Autenticació:** Cap (públic)

Genera sitemap.xml automàtic per a cada tenant.

### 4.5 Formularis

#### `POST /api/v1/engine/render/{slug}/contact` — Enviar formulari

**Autenticació:** Cap (públic)

**RGPD:** Cal consentiment explícit (`consent: true`)

Rate limit: 5 intents per IP / 15 minuts (protecció anti-spam)

Request:
```json
{
  "name": "Joan Servera",
  "email": "joan@example.com",
  "phone": "+34600123456",
  "message": "Vull demanar pressupost per un sopar de 20 persones",
  "consent": true
}
```

Response 201: `{ "message": "Missatge rebut correctament" }`

Crea un `ContactLead` amb metadades de consentiment.

### 4.6 Mapa complet d'endpoints

| Mètode | Ruta | Descripció | Auth | Rols |
|--------|------|-----------|------|------|
| POST | /engine/tenants/{tId}/landings | Crear landing | JWT | SUPER_ADMIN, ADMIN |
| GET | /engine/tenants/{tId}/landings | Llistar landings | JWT | Tots |
| GET | /engine/tenants/{tId}/landings/{id} | Veure landing | JWT | Tots |
| PUT | /engine/tenants/{tId}/landings/{id} | Actualitzar metadades | JWT | SUPER_ADMIN, ADMIN |
| DELETE | /engine/tenants/{tId}/landings/{id} | Eliminar landing | JWT | SUPER_ADMIN, ADMIN |
| POST | /engine/landings/{lId}/versions | Nova versió | JWT | SUPER_ADMIN, ADMIN |
| PUT | /engine/landings/{lId}/versions/{vId} | Actualitzar esborrany | JWT | SUPER_ADMIN, ADMIN |
| POST | /engine/landings/{lId}/publish | Publicar | JWT | SUPER_ADMIN, ADMIN |
| POST | /engine/landings/{lId}/unpublish | Despublicar | JWT | SUPER_ADMIN, ADMIN |
| PUT | /engine/landings/{lId}/domain | Configurar domini | JWT | SUPER_ADMIN, ADMIN |
| GET | /engine/landings/{lId}/domain/verify | Verificar domini | JWT | SUPER_ADMIN, ADMIN |
| POST | /engine/landings/{lId}/domain/status | Actualitzar estat domini | JWT | SUPER_ADMIN, ADMIN |
| DELETE | /engine/landings/{lId}/domain | Desconfigurar domini | JWT | SUPER_ADMIN, ADMIN |
| GET | /engine/render/{slug} | Renderitzar landing | Pública | — |
| GET | /engine/render/{slug}/sitemap.xml | Sitemap | Pública | — |
| POST | /engine/render/{slug}/contact | Enviar formulari | Pública | — |

---

## 5. Seguretat

### 5.1 Autenticació
- Endpoints de gestió: JWT (Mòdul 01 Auth)
- Endpoints de render: Públics (visitants no autenticats)
- Formularis: rate limiting per IP

### 5.2 Autorització (RBAC)
- SUPER_ADMIN: CRUD totes les landings de tots els tenants, gestió completa de dominis (managed i autogestionats)
- ADMIN: CRUD landings dels seus tenants assignats, gestió de dominis
- CLIENT: Veure estat i configurar domini autogestionat de les seves landings
- Visitants: Només veure landings publicades

### 5.3 Protecció de dades
- Validació de slugs (només lletres, números i guions)
- Rate limiting als endpoints públics
- Anti-spam als formularis (consentiment RGPD obligatori + reCAPTCHA opcional en v2)
- CORS restringit a dominis permesos

### 5.4 Domini personalitzat
- Verificació de propietat del domini via CNAME (ambdós fluxos)
- SSL automàtic via Let's Encrypt / Traefik
- Proxy invers per servir el domini correcte
- Les dades del titular del domini només són accessibles per SUPER_ADMIN i ADMIN

---

## 6. RGPD / LSSI

- **Dades personals:** Nom, email, telèfon dels visitants que omplen formularis (ContactLead) + dades del titular del domini
- **Base legal:** Interès legítim del client (el visitant contacta voluntàriament)
- **Conservació:** 24 mesos després de l'últim contacte, o fins que el client elimini la landing
- **Avís legal:** Cada landing ha d'incloure automàticament enllaç a l'avís legal i política de privacitat del tenant
- **Supressió:** En eliminar la landing, tots els ContactLead associats s'eliminen
- **GDPR compiellant:** Els formularis han d'incloure checkbox de consentiment explícit

---

## 7. Tests (QA)

### 7.1 Funcionals

| # | Cas | Resultat |
|---|-----|---------|
| 1 | Crear landing amb dades vàlides | 201 |
| 2 | Crear landing amb slug duplicat dins el tenant | 400 |
| 3 | Crear versió esborrany amb blocs | 201 |
| 4 | Publicar landing → accessible públicament | 200 + HTML renderitzat |
| 5 | Despublicar landing → 404 en públic | 404 |
| 6 | Configurar domini autogestionat | 200, estat DNS_PENDING |
| 7 | Configurar domini gestionat | 200, estat PENDING_PURCHASE o PURCHASED |
| 8 | Verificar domini (CNAME correcte) | 200, estat VERIFIED |
| 9 | Enviar formulari de contacte amb consentiment | 201, ContactLead creat |
| 10 | Enviar formulari sense consentiment | 400 |
| 11 | Landing amb slug inexistent → 404 | 404 |
| 12 | Llistar landings d'un tenant amb paginació | 200 + llista paginada |
| 13 | CLIENT veu les seves landings | 200, només les seves |
| 14 | CLIENT no pot crear domini gestionat | 403 |

### 7.2 Seguretat

| # | Cas | Resultat |
|---|-----|---------|
| 1 | Accés a gestió sense JWT | 401 |
| 2 | CLIENT modifica landing d'altre tenant | 403 |
| 3 | CLIENT publica landing (no hauria de poder) | 403 |
| 4 | Enviar formulari des de IP bloquejada (rate limit) | 429 |
| 5 | Landing publicada accessible sense autenticació | 200 |
| 6 | Landing no publicada NO accessible públicament | 404 |

### 7.3 Límits

| # | Cas | Resultat |
|---|-----|---------|
| 1 | Crear landing amb títol > 200 caràcters | 400 |
| 2 | Enviar formulari amb email mal format | 400 |
| 3 | Slug amb caràcters no permesos | 400 |

---

## 8. Dependències

| Mòdul | Dependència | Tipus |
|-------|-----------|-------|
| Mòdul 01 (Auth) | Autenticació JWT + RBAC | Forta |
| Mòdul 02 (Vault) | Verificació servei LANDING actiu per tenant | Forta |
| Mòdul 03 (Leads) | Creació de leads des de formularis | Dèbil |
| Mòdul 05 (Factory) | Editor de landings (consumirà les API d'Engine) | Forta |
| Mòdul 06 (Assets) | Imatges i fitxers de la landing | Dèbil |
| Mòdul 10 (Automations) | Automatitzacions post-formulari | Dèbil |

---

## 9. Obert / Pendents

- [ ] Definir els blocs concrets i les seves props exactes (tipus TypeScript)
- [ ] Decidir si el render és SSR (Next.js) o HTML estàtic generat + servit per Spring
- [ ] Sistema de templates o landings totalment lliures?
- [ ] Suportar landings en múltiples idiomes? (heretant del mòdul 13)
- [ ] Integració amb anàlitiques? (Google Analytics, Meta Pixel)
- [ ] Connexió amb APIs de registradors per automatitzar compra de dominis?
