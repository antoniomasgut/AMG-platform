# Mòdul 15: Demo — Landing interactiva amb agent IA per sector

> **Versió:** 2.1
> **Data:** 2026-07-02
> **Dependències:** Mòdul 04 (Engine), Mòdul 26 (RAG/SectorTemplate), Mòdul 30 (Chat Widget), Mòdul 01 (Auth)

---

## 1. Objectius

- Crear **demos personalitzades per sector** amb landing generada automàticament + agent de xat en viu
- L'admin pot crear, llistar i desactivar sessions de demo
- Cada sessió té una **landing pre-construïda** amb el creador de landings (Engine) i els blocs de contingut del sector
- El **xat de la landing** usa el prompt del `SectorTemplate` (tipus `agent-prompt`) del sector corresponent
- Sessions de **24 hores** de durada màxima; desactivables manualment
- Si l'usuari escriu paraules malsonants, la conversa es tanca amb avís

---

## 2. Abast

### 2.1 Funcionalitats incloses

- Landing de demo generada per l'Engine amb blocs: `hero`, `services`, `chat-cta`, `contact-form`
- Chat widget integrat a la landing (via `LandingChatContext` + `/api/v1/chat/sessions`)
- Tenant de demo per sector (`demo_perruqueria`, `demo_restaurante`, ...) — `isFree=true`, creat automàticament
- 17 sectors predefinits amb contingut específic (titular, subtítol, 3 serveis)
- Prompt de l'agent des de `SectorTemplate.type=agent-prompt` (o genèric si no existeix)
- Moderació: paraules malsonants → `terminated=true` (el widget ja ho gestiona)
- Admin pot desactivar sessions (`isActive=false`)
- TTL: 24h des de la creació

### 2.2 Funcionalitats excloses (V1 demos)

- El V1 de demos simples (flows n8n mockejats) es manté com a `/api/v1/demo` (DemoOrchestrator)
- No hi ha SMTP ni WhatsApp real a la demo
- No s'envia cap dada de la demo al CRM de producció

---

## 3. Model de dades

### 3.1 Entitat: `DemoSession` (taula `demo_sessions`)

| Camp | Tipus | Descripció |
|------|-------|-----------|
| id | UUID | PK |
| token | UUID | Token públic únic |
| prospect_email | VARCHAR(255) | Email del prospect (default: demo@amgdl.com) |
| company_name | VARCHAR(150) | Nom de l'empresa que veurà la demo |
| agent_context | TEXT | Context addicional per a l'agent |
| sector | VARCHAR(50) | Sector de negoci (PERRUQUERIA, RESTAURANTE, ...) |
| landing_slug | VARCHAR(100) | Slug de la landing generada (`demo-{token}`) |
| is_active | BOOLEAN | True fins que l'admin desactiva |
| expires_at | TIMESTAMPTZ | Creació + 24h |
| blocked_at | TIMESTAMPTZ | Si ha estat bloquejada per moderació |
| block_reason | VARCHAR(255) | Motiu del bloqueig |
| created_at | TIMESTAMPTZ | Auditing |

### 3.2 Landing de demo (`landings` + `landing_versions`)

- **slug**: `demo-{token}` (únic per sessió)
- **tenant_id**: ID del tenant `demo_{sector}`
- **service_id**: NULL (demo, sense servei de pagament)
- **status**: PUBLISHED (publicada immediatament)
- **LandingChatContext**: creat automàticament amb system prompt del `SectorTemplate`

### 3.3 Tenant de demo (`tenants`)

- **slug**: `demo_{sector_lowercase}` (e.g., `demo_perruqueria`)
- **name**: `Demo {SectorLabel}` (e.g., `Demo Perruqueria`)
- **isFree**: true — no genera facturació
- **Creat automàticament** la primera vegada que es crea una demo del sector

---

## 4. Endpoints API

### 4.1 Admin (requereix autenticació)

| Mètode | Ruta | Descripció |
|--------|------|-----------|
| POST | /api/v1/admin/demo/sessions | Crear nova sessió de demo |
| GET | /api/v1/admin/demo/sessions | Llistar totes les sessions |
| PATCH | /api/v1/admin/demo/sessions/{token} | Actualitzar companyName / agentContext |
| DELETE | /api/v1/admin/demo/sessions/{token} | Desactivar sessió (`isActive=false`) |

#### POST /api/v1/admin/demo/sessions
```json
{
  "sector": "PERRUQUERIA",
  "companyName": "Perruqueria Mireia",
  "agentContext": "context addicional opcional",
  "prospectEmail": "optional@email.com"
}
```

Resposta:
```json
{
  "token": "uuid",
  "landingUrl": "https://api.amgdl.com/api/v1/engine/render/demo-{token}",
  "sector": "PERRUQUERIA",
  "companyName": "Perruqueria Mireia",
  "demoEmail": "demo@amgdl.com",
  "expiresAt": "ISO-8601",
  "active": true
}
```

### 4.2 Públics (sense autenticació)

| Mètode | Ruta | Descripció |
|--------|------|-----------|
| GET | /api/v1/demo/sessions/{token} | Obtenir info de la sessió + landing URL |
| POST | /api/v1/demo/sessions/{token}/reply | Enviar missatge (canal email legacy) |
| GET | /api/v1/engine/render/demo-{token} | **Renderitza la landing de demo** |
| POST | /api/v1/chat/sessions | Iniciar sessió de xat (landingSlug=demo-{token}) |
| POST | /api/v1/chat/sessions/{id}/messages | Enviar missatge al xat |

### 4.3 Compatibilitat cap enrere

Les rutes `/api/v1/demo/inbox/**` i `/api/v1/admin/demo/inbox` segueixen funcionant (aliases).

---

## 5. Sectors predefinits

| Sector | Titular |
|--------|---------|
| PERRUQUERIA | El teu estil, la nostra passió |
| ESTETICA | Bellesa que s'expressa |
| FISIOTERAPEUTA | Recupera el teu moviment |
| PSICOLEG | El benestar comença aquí |
| NUTRICIONISTA | Menja bé, viu millor |
| TALLER_MECANIC | El teu cotxe en bones mans |
| VETERINARI | La salut de la teva mascota, primer |
| RESTAURANTE | Sabors que et faran tornar |
| ELECTRICISTA | Solucions elèctriques professionals |
| FONTANER | Urgències i instal·lacions 24h |
| JARDINER | Espais verds que inspiren |
| NETEJA | Espais impecables, clients feliços |
| GESTORIA | La teva empresa en ordre |
| ACADEMIA | Aprèn amb els millors |
| PERRUQUERIA_CANINA | El teu millor amic mereix el millor |
| INMOBILIARIA | La teva llar ideal t'espera |
| AGENCIA_IA | Automatitza el teu negoci amb IA |
| PINTOR | Pintura professional, acabats impecables |

---

## 6. Blocs de la landing de demo

Cada landing de demo conté 4 blocs en ordre:

1. **hero** — Titular + subtítol + CTA "Parla amb el nostre agent" (acció: `chat`)
2. **services** — 3 serveis específics del sector
3. **chat-cta** — Crida a l'acció per obrir el xat
4. **contact-form** — Formulari de contacte estàndard

Les styles configuren `chatEnabled: true` i `primaryColor` específic del sector.

---

## 7. Prompt de l'agent

1. Cerca `SectorTemplate.sector={SECTOR}` i `type='agent-prompt'`
2. Si existeix: substitueix les variables `{NOM_NEGOCI}`, `{SERVEIS}`, `{HORARI}`, etc.
3. Si no existeix: prompt genèric ("Ets l'assistent virtual de X. Respon en català...")

---

## 8. Moderació

Gestionada per `ChatSessionService.containsProfanity()`:
- Si l'usuari escriu una paraula malsonant → `terminated=true` en la resposta del xat
- El widget mostra "La conversa s'ha tancat per incompliment de les normes d'ús."
- La sessió Redis es elimina

---

## 9. Migracions

| Migració | Contingut |
|----------|-----------|
| V2 | Crea taula `demo_sessions` (baseline) |
| V34 | Afegeix columna `sector` |
| V35 | Afegeix `is_active`, `landing_slug`; fa `service_id` nullable a `landings` |

---

## 10. Fitxers principals

| Fitxer | Propòsit |
|--------|---------|
| `agents/api/DemoController.java` | Endpoints admin + públics de demo sessions |
| `agents/application/DemoInboxService.java` | Lògica de negoci: crear, validar, desactivar sessions |
| `demo/application/DemoLandingService.java` | Crear tenant de demo + landing Engine + LandingChatContext |
| `agents/domain/DemoSession.java` | Entitat JPA |
| `agents/domain/DemoSessionRepository.java` | Repositori JPA |
| `demo/api/DemoController.java` | V1 demos (flows n8n mockejats, es manté) |
