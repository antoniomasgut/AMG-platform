# Mòdul 15: Demo — Showroom públic d'automatitzacions

> **Versió:** 1.0
> **Data:** 2026-05-13
> **Dependències:** Cap (mòdul autònom, sense persistència)

---

## 1. Objectius

- Oferir **demos gratuïtes i sense registre** per mostrar com funcionen les automatitzacions de la plataforma
- Facilitar la conversió de prospects mostrant casos d'ús reals pas a pas
- **Cost zero** — les demos són dades maquetades, no executen res en producció

---

## 2. Abast

### 2.1 Funcionalitats incloses

- Llistat públic de demos disponibles
- Cada demo mostra un flux d'automatització amb passos encadenats (ordre, icona, acció, detall, durada)
- Endpoints públics sense autenticació
- Dades estàtiques en memòria (mock complet, no BBDD, no crides externes)

### 2.2 Funcionalitats excloses

- Sense persistència (no JPA, no PostgreSQL)
- Sense execució real de workflows n8n
- Sense integració amb Stripe, WhatsApp, Google Places reals
- Sense autenticació ni RBAC
- Sense cost d'infraestructura associat

---

## 3. Model de dades

Cap entitat JPA. Totes les dades són estàtiques en memòria (`Map<String, DemoFlowResponse>`).

### 3.1 DTOs

#### DemoFlowSummary
| Camp | Tipus | Descripció |
|------|-------|-----------|
| id | String | Identificador únic (ex: "new-lead") |
| title | String | Títol humà (ex: "Captació automàtica de leads") |
| description | String | Descripció breu del flux |

#### DemoStepResponse
| Camp | Tipus | Descripció |
|------|-------|-----------|
| order | int | Número de pas (1-5) |
| icon | String | Emoji representatiu |
| action | String | Acció del pas |
| detail | String | Explicació detallada del que passa |
| duration | String | Quant de temps triga aquest pas |

#### DemoFlowResponse
| Camp | Tipus | Descripció |
|------|-------|-----------|
| id | String | Identificador únic |
| title | String | Títol humà |
| description | String | Descripció del flux |
| steps | List<DemoStepResponse> | Llista de passos (5 per demo) |

#### DemoListResponse
| Camp | Tipus | Descripció |
|------|-------|-----------|
| demos | List<DemoFlowSummary> | Llista de totes les demos disponibles |

---

## 4. Endpoints API

| Mètode | Ruta | Auth | Descripció |
|--------|------|------|-----------|
| GET | /api/v1/demo | Públic | Llistar totes les demos disponibles |
| GET | /api/v1/demo/{id} | Públic | Veure una demo concreta (ex: new-lead) |

---

## 5. Demos incloses

### 5.1 Captació automàtica de leads (`new-lead`)

1. 📝 Client omple formulari de contacte a la web → instantani
2. ⚡ Formulari envia dades a n8n via webhook → < 1s
3. 💾 n8n crea un lead al CRM → < 1s
4. 📱 Alerta al Telegram de gestió → < 2s
5. 📋 Tasca de seguiment automàtica → < 1s

### 5.2 Pressupost i email automatitzat (`budget-email`)

1. 💰 Client sol·licita pressupost per una landing → instantani
2. 📄 n8n genera PDF professional amb detall → < 3s
3. 📧 S'envia el PDF per email al client → < 2s
4. 🔗 S'adjunta enllaç segur per pagar → < 1s
5. ✅ Alerta quan es rep el pagament → automàtic

### 5.3 WhatsApp Business automatitzat (`whatsapp-auto`)

1. 💬 Client escriu "Quant val una landing?" al WhatsApp → instantani
2. 🤖 n8n detecta paraula clau i respon amb llista de preus → < 1s
3. 🏷️ Contacte s'etiqueta com a "interessat-landing" al CRM → < 1s
4. 📊 Consulta registrada al dashboard → < 1s
5. 👤 Si cal, es deriva a un humà → automàtic

### 5.4 Gestió de ressenyes Google (`reviews-auto`)

1. ⭐ Client deixa ressenya de 3⭐ al negoci → instantani
2. 🔔 Alerta al Telegram amb la ressenya → < 5 min
3. 📝 Tasca per respondre la ressenya en 2h → automàtic
4. 📈 Ressenya registrada al dashboard de reputació → < 1s
5. 📊 Resum setmanal de totes les ressenyes → setmanal

---

## 6. Configuració

Cap (no hi ha secrets, API keys ni BBDD).

---

## 7. Seguretat

- Endpoints públics (sense JWT, sense @PreAuthorize)
- Només GET (read-only)
- Dades estàtiques: no s'emmagatzema res de l'usuari
- CORS obert (per mostrar des de qualsevol frontend)

---

## 8. Tests

5 tests mínims:

| # | Test | Esperat |
|---|------|---------|
| 1 | Llistar demos | 200, 4 demos |
| 2 | Veure "new-lead" | 200, 5 passos |
| 3 | Veure "budget-email" | 200, 5 passos |
| 4 | ID inexistent | 404 |
| 5 | Sense autenticació | 200 OK (accés públic) |

---

## 9. Resum de fitxers

| Fitxer | Propòsit |
|--------|---------|
| DemoController.java | 2 endpoints públics |
| DemoService.java | Interface del servei |
| DemoOrchestrator.java | 4 demos hardcodejades en memòria |
| DemoListResponse.java | DTO llista |
| DemoFlowResponse.java | DTO flux complet |
| DemoStepResponse.java | DTO pas individual |
| DemoControllerTest.java | 5 tests d'integració |
