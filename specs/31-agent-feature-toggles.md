# Spec 31 — Agent Conversacional: Funcionalitats per Fase

**Versió:** 1.0  
**Data:** 2026-06-03  
**Estat:** Actiu ✅  
**Depèn de:** Spec 20 (Agents), Spec 28 (NexeLocal Service Configs), Spec 30 (Landing Chat Widget)

---

## 1. Visió general

L'agent conversacional AMG (WhatsApp, Email, Chat widget) adapta el seu comportament en funció de les fases NexeLocal que el tenant té contractades i activades. Cada funcionalitat s'activa o desactiva de forma independent mitjançant el camp `"enabled": boolean` de la configuració corresponent.

**Principi:** si una funcionalitat no té `enabled: true`, el bot **no sap** que existeix — no la mencionarà ni intentarà executar-la.

---

## 2. Matriu de funcionalitats per fase

| Funcionalitat | Fase | Config key | Canal Xat | Canal WA | Canal Email | Implementat |
|---------------|------|-----------|-----------|----------|-------------|-------------|
| Resposta bàsica (FAQ, info, preus genèrics) | F1 | — (system prompt) | ✅ | ✅ | ✅ | ✅ |
| Booking cites / visites + Google Calendar | F2 | `AGENDA` | ✅ | ✅ | ✅ | ✅ |
| Informació de preus i pressupostos | F3 | `PRESSUPOSTOS` | ✅ | ✅ | ✅ | ✅ |
| Recollida de ressenyes Google | F4 | `FIDELITZACIO` | ✅ | ✅ | ✅ | ✅ |
| Notificació equip intern (grup Telegram) | F5 | `EQUIP` | ✅ | ✅ | ✅ | ✅ |
| RAG / Base de coneixement | F2–F5 | `RAG` (futur) | ✅ | ✅ | ✅ | ⏳ |
| Missatge fora d'horari | Cross | `HORARI` (futur) | ✅ | ✅ | ✅ | ⏳ |

---

## 3. Fases i el seu impacte en el bot

### F1 — Web (sempre activa)

El bot respon a preguntes generals sobre el negoci basant-se en el system prompt configurat al portal. No requereix cap config `nexe_service_configs`.

Capacitats:
- Respondre FAQ
- Informar sobre serveis (sense preus exactes si F3 no activa)
- Recollir el contacte del client (nom + telèfon via pre-chat form)
- Crear Lead automàticament

### F2 — Agenda (`AGENDA.enabled: true`)

El bot pot gestionar cites o visites d'inspecció:
- Pregunta les dades necessàries al client (definides a `clientQuestions`)
- Confirma data, hora i lloc
- Emet el tag `[CONFIRMA_CITA:{...}]` que el sistema intercepta i crea l'event al Google Calendar
- Envia plantilla de confirmació al client

**Sense F2 activa:** el bot respon "per concertar visita, contacta'ns per telèfon" o deriva al WhatsApp del negoci.

### F3 — Pressupostos (`PRESSUPOSTOS.enabled: true`)

Mode `pricelist`:
- El bot comparteix el llistat de serveis i preus directament
- Pot calcular preus aproximats per superfície, durada, etc.

Mode `formal`:
- El bot **no dona preus tancats** — recull les dades del client i informa que "en Joan us enviarà el pressupost en 24h"
- Crea una petició de pressupost (nota al lead o notificació al tenant)

**Sense F3 activa:** el bot respon "per a preus i pressupostos, contacta'ns directament".

### F4 — Fidelització (`FIDELITZACIO.enabled: true`)

- Quan el bot detecta un client satisfet (expressió positiva al final d'una conversa), suggereix deixar una ressenya a Google amb l'URL configurat
- El sistema pot enviar missatges de seguiment post-servei (fora de l'scope del bot conversacional — gestionat per agents programats del Mòdul 20)

**Sense F4 activa:** el bot no menciona ressenyes ni programes de fidelització.

### F5 — Equip (`EQUIP.enabled: true`)

- Quan una conversa requereix intervenció humana o una informació que el bot no pot resoldre, el bot ho comunica i notifica el grup de Telegram de l'equip intern
- El bot pot mencionar membres de l'equip per nom si estan configurats

**Sense F5 activa:** el bot deriva cap al WhatsApp o telèfon del negoci.

---

## 4. Funcionalitats cross-cutting (no lligades a fase específica)

### 4.1 RAG / Base de coneixement (⏳ Spec 26)

Quan el Mòdul 26 (RAG) estigui activat per al tenant:
- `"RAG_ENABLED": true` a `nexe_service_configs` (o via `TenantAIConfig`)
- El bot consulta la KB vectorial del tenant per respondre preguntes específiques
- Actiu en tots els canals

### 4.2 Missatge fora d'horari (⏳ futur)

Config `HORARI`:
```json
{
  "enabled": true,
  "timezone": "Europe/Madrid",
  "schedule": [
    { "day": "monday",    "open": "09:00", "close": "18:00" },
    { "day": "tuesday",   "open": "09:00", "close": "18:00" },
    { "day": "wednesday", "open": "09:00", "close": "18:00" },
    { "day": "thursday",  "open": "09:00", "close": "18:00" },
    { "day": "friday",    "open": "09:00", "close": "17:00" },
    { "day": "saturday",  "open": "10:00", "close": "14:00" },
    { "day": "sunday",    "open": null,    "close": null     }
  ],
  "outsideHoursMessage": "Ara estem tancats. Estem disponibles de {HORARI}. Et respondrem quan obrim!"
}
```

Quan actiu, el bot respon immediatament amb el missatge fora d'horari sense cridar la IA (estalvi de tokens i millor experiència).

### 4.3 Idioma de resposta (⏳ futur)

Camp `responseLanguage` al system prompt del tenant:
- `auto` (per defecte): detecta l'idioma del client i respon en el mateix
- `ca`, `es`, `en`, `de`: força un idioma concret

---

## 5. Com el bot assembla el context (PromptBuilder)

L'ordre d'assemblatge del system prompt final és:

```
[1] System prompt base del tenant (TenantChatLink.agentSystemPrompt)
      ↓
[2] Bloc AGENDA    (si AGENDA.enabled = true)
      ↓
[3] Bloc PRESSUPOSTOS (si PRESSUPOSTOS.enabled = true)
      ↓
[4] Bloc FIDELITZACIO (si FIDELITZACIO.enabled = true)
      ↓
[5] Bloc EQUIP     (si EQUIP.enabled = true)
      ↓
[6] Bloc RAG       (si RAG actiu — futur)
      ↓
[7] Bloc HORARI    (si fora d'horari — futur)
```

Cada bloc afegeix entre 2 i 8 línies d'instruccions. El prompt total rarament supera els 800 tokens.

---

## 6. Canals i diferències de comportament

| Aspecte | Chat Widget | WhatsApp | Email |
|---------|------------|----------|-------|
| Pre-chat form (nom + telèfon) | ✅ Obligatori | N/A (número ja conegut) | N/A (adreça ja coneguda) |
| Creació automàtica de Lead | ✅ | ✅ (via `ConversationalAgentService`) | ✅ |
| Booking cites (F2) | ✅ | ✅ | ✅ |
| Pressupostos (F3) | ✅ | ✅ | ✅ |
| Límit de missatges per sessió | 20 (Redis TTL 2h) | Il·limitat | Il·limitat |
| Profanitat → tancament sessió | ✅ | ✅ | ✅ |
| Fora d'horari (futur) | ✅ | ✅ | ✅ |

---

## 7. Gestió del flag `enabled` al portal

Al formulari de cada servei (`/portal/admin/tenants/[id]/nexe/[service]`), el primer camp és sempre un toggle **"Actiu / Inactiu"** que mapeja a `enabled`.

Quan es desa amb `enabled: false`, la configuració es preserva però el bot no l'usa. L'admin pot tornar a activar-la en qualsevol moment sense reconfigurar.

**Control d'accés:**
- SUPER_ADMIN: pot activar/desactivar qualsevol servei de qualsevol tenant
- ADMIN (tenant): pot desactivar temporalment, però **no** pot activar un servei que no ha contractat (controlat per les fases verificades del tenant)

---

## 8. Notes d'implementació

- `PromptBuilder.buildAgendaBlock()` ja és `public` — accessible des de `ChatSessionService`
- `ChatSessionService` comprova `enabled` de l'AGENDA abans d'injectar el bloc i procesar `[CONFIRMA_CITA:{...}]`
- `ConversationalAgentService` (WA/Email) delega a `PromptBuilder.buildContextAddendum()` — quan tots els blocs comproven `enabled`, el comportament serà consistent entre canals
- Els blocs PRESSUPOSTOS, FIDELITZACIO i EQUIP han d'afegir la comprovació `enabled` al `PromptBuilder` (pendent — seguint el patró de l'AGENDA)
