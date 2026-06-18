# Spec 50 — Sales & Implementation Pipeline

**Versió:** 1.0
**Data:** 2026-06-18
**Estat:** Aprovat
**Depèn de:** M03 (Leads CRM), M07 (Billing), M12 (Prospecting), M15 (Demo), M17 (Service Setup Wizard), M32 (Tenant Notifications), M43 (Communication Templates), M45 (Post-Budget Booking)

---

## 1. Problema

El flux actual de prospecció → implementació → activació té tres gaps crítics:

1. **M12 genera prospects però no crea leads a M03 automàticament** — pas manual, risc de pèrdua.
2. **Quan el client completa el Setup Wizard (M17), el SUPER_ADMIN no rep cap notificació** — implementació pot quedar aturada dies.
3. **En activar el tenant, no s'envia cap comunicació automàtica al client** — el client no sap que el servei ja funciona.

A més, no hi ha visibilitat del pipeline complet ni SLAs definits per a cada etapa.

---

## 2. Visió general

Un pipeline d'etapes explícites que cobreix tot el cicle de vida d'un client, des del primer contacte fins a l'activació i l'onboarding. Cada etapa té:

- Una **transició automàtica** o manual clara
- Una **acció de notificació** (Telegram intern i/o missatge al client)
- Un **SLA opcional** que dispara una alerta si no es transita en el temps definit

---

## 3. Etapes del pipeline

```
PROSPECT → CONTACTED → DEMO_SENT → PROPOSAL_SENT
→ PROPOSAL_ACCEPTED → SETUP_COMPLETED
→ IMPLEMENTING → ACTIVE → CHURNED
```

| Etapa | Descripció | Qui la transita | SLA alerta |
|-------|-----------|----------------|-----------|
| `PROSPECT` | Trobat per M12 o afegit manualment | M12 automàtic / SUPER_ADMIN | — |
| `CONTACTED` | S'ha enviat un primer contacte | SUPER_ADMIN (manual) | 24h sense acció → alerta Telegram |
| `DEMO_SENT` | Demo M15 enviada al prospect | SUPER_ADMIN (manual) | 48h sense resposta → follow-up automàtic al client |
| `PROPOSAL_SENT` | Pressupost M07 enviat | Automàtic en crear pressupost | 48h sense resposta → follow-up automàtic al client |
| `PROPOSAL_ACCEPTED` | Client accepta pressupost i paga | Automàtic via M07/M45 | — |
| `SETUP_COMPLETED` | Client completa el Setup Wizard M17 | Automàtic via M17 | 24h sense acció SUPER_ADMIN → alerta urgent Telegram |
| `IMPLEMENTING` | SUPER_ADMIN ha iniciat la implementació tècnica | SUPER_ADMIN (manual, 1 clic) | 72h sense activar → alerta Telegram |
| `ACTIVE` | Tenant activat, agent en funcionament | Automàtic en activar tenant | — |
| `CHURNED` | Client ha cancel·lat o inactiu | SUPER_ADMIN (manual) | — |

---

## 4. Connexions amb mòduls existents

### 4.1 M12 → M03 (Prospecció → Lead)

Quan M12 troba un prospect, crear automàticament un `Lead` a M03:

```java
Lead lead = Lead.builder()
    .tenantId(systemTenantId)  // tenant del SUPER_ADMIN
    .name(prospect.getBusinessName())
    .phone(prospect.getPhone())
    .source("PROSPECTING")
    .stage(LeadStage.NEW)
    .pipelineStage(PipelineStage.PROSPECT)
    .notes("Origen: " + prospect.getSector() + " · " + prospect.getCity())
    .build();
```

### 4.2 M07 → Pipeline (Pressupost creat)

En crear un pressupost per a un lead, transitar automàticament a `PROPOSAL_SENT`.

### 4.3 M07/M45 → Pipeline (Pressupost acceptat)

En acceptar un pressupost, transitar a `PROPOSAL_ACCEPTED` i enviar token de Setup Wizard.

### 4.4 M17 → Pipeline (Setup completat)

En completar el Setup Wizard, transitar a `SETUP_COMPLETED` i notificar SUPER_ADMIN per Telegram:

```
🔔 Setup completat — [NOM CLIENT]
Sector: [SECTOR] · Fases: [F1, F2...]
Acció requerida: iniciar implementació
→ /portal/admin/tenants/[id]/wizard
```

### 4.5 Activació del tenant → Pipeline (ACTIVE)

En activar un tenant, transitar a `ACTIVE` i disparar la seqüència d'onboarding (vegeu secció 6).

---

## 5. Model de dades

### 5.1 Extensió de `leads` (ALTER TABLE)

```sql
ALTER TABLE leads
  ADD COLUMN pipeline_stage VARCHAR(30) NOT NULL DEFAULT 'PROSPECT',
  ADD COLUMN pipeline_updated_at TIMESTAMPTZ,
  ADD COLUMN sla_deadline TIMESTAMPTZ,
  ADD COLUMN sla_alerted BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_leads_pipeline_stage ON leads(pipeline_stage);
CREATE INDEX idx_leads_sla_deadline ON leads(sla_deadline) WHERE sla_deadline IS NOT NULL;
```

### 5.2 Nova taula `pipeline_events`

Traçabilitat de cada transició d'etapa:

```sql
CREATE TABLE pipeline_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL REFERENCES leads(id),
    from_stage VARCHAR(30),
    to_stage VARCHAR(30) NOT NULL,
    triggered_by VARCHAR(50) NOT NULL,  -- 'SYSTEM', 'SUPER_ADMIN', 'CLIENT'
    actor_id UUID,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_pipeline_events_lead ON pipeline_events(lead_id);
```

---

## 6. Seqüència d'onboarding post-activació

En transitar a `ACTIVE`, disparar automàticament 3 missatges via M43:

| Dia | Canal | Acció M43 | Contingut |
|-----|-------|-----------|-----------|
| 0 | WhatsApp + Email | `ONBOARDING_DAY0` | "El teu agent ja està actiu. Aquí tens com provar-lo: [link portal]" |
| 3 | WhatsApp | `ONBOARDING_DAY3` | "Ja portes 3 dies actiu. Consells per treure'n més profit." |
| 7 | WhatsApp + Email | `ONBOARDING_DAY7` | "Primera setmana! Resum d'activitat del teu agent." |

La programació dels missatges dels dies 3 i 7 es fa amb un `ScheduledTask` intern (similar al seguiment de pressupostos de M45).

---

## 7. Pàgina d'estat per al client

Nova URL pública (sense autenticació, protegida per token): `/status/[token]`

El client rep aquest link en el missatge `ONBOARDING_DAY0`.

### 7.1 Contingut

```
AMG Digitalitzacions · Estat del teu projecte

✅ Configuració rebuda           [data]
✅ Pagament confirmat            [data]
✅ Setup del negoci completat    [data]
🔄 Implementació tècnica         En curs (estimat: 48h)
⏳ Proves i validació            Pendent
⏳ Agent actiu                   Pendent

Tens dubtes? WhatsApp: +34 614 492 062
```

Un cop activat:

```
✅ Configuració rebuda
✅ Pagament confirmat
✅ Setup del negoci completat
✅ Implementació tècnica
✅ Agent actiu des de [data]

El teu agent ja respon als clients. Accedeix al portal:
→ [link portal client]
```

### 7.2 Token

El `status_token` es genera en acceptar el pressupost (UUID únic, guardat al `Lead` o al `Tenant`). No expira.

---

## 8. Dashboard pipeline (SUPER_ADMIN)

Nova vista al panell d'administració: `/portal/admin/pipeline`

### 8.1 Vista kanban

Columnes: una per etapa del pipeline. Cada targeta mostra:
- Nom del client
- Sector + fases contractades
- Temps en aquesta etapa
- Indicador SLA (verd / groc / vermell)

### 8.2 Vista llista

Taula ordenable per etapa, temps en etapa, SLA.

### 8.3 Filtres

- Per etapa
- Per sector
- Clients amb SLA expirat

---

## 9. SLA — Monitor de fons

Un `@Scheduled` job cada hora comprova:

```java
List<Lead> atRisk = leadRepo.findBySlaDeadlineBeforeAndSlaAlertedFalse(Instant.now());
for (Lead lead : atRisk) {
    telegramService.sendSuperAdminAlert(
        "⚠️ SLA expirat — " + lead.getName() +
        " porta " + hoursInStage(lead) + "h a etapa " + lead.getPipelineStage()
    );
    lead.setSlaAlerted(true);
    leadRepo.save(lead);
}
```

---

## 10. Follow-ups automàtics de pressupost

Quan `pipeline_stage = PROPOSAL_SENT` i han passat 48h sense transitar:

1. Enviar plantilla `BUDGET_FOLLOWUP_D2` via M43 (WhatsApp)
2. Als 5 dies: plantilla `BUDGET_FOLLOWUP_D5` (WhatsApp + Email)
3. Als 10 dies: alerta interna Telegram al SUPER_ADMIN ("Pressupost sense resposta — considera trucada")

---

## 11. API REST

### 11.1 Pipeline

| Mètode | Path | Descripció |
|--------|------|-----------|
| `GET` | `/api/v1/pipeline` | Llista leads amb etapa de pipeline (SUPER_ADMIN) |
| `PUT` | `/api/v1/pipeline/{leadId}/stage` | Transitar etapa manualment |
| `GET` | `/api/v1/pipeline/{leadId}/events` | Historial de transicions |
| `GET` | `/api/v1/pipeline/stats` | Estadístiques per etapa |

### 11.2 Estat públic

| Mètode | Path | Descripció |
|--------|------|-----------|
| `GET` | `/api/v1/status/{token}` | Retorna estat del projecte (públic, per token) |

---

## 12. Frontend

### 12.1 Fitxers a crear/modificar

| Fitxer | Acció |
|--------|-------|
| `frontend/src/app/[locale]/portal/admin/pipeline/page.tsx` | Nou — vista kanban + llista |
| `frontend/src/app/[locale]/status/[token]/page.tsx` | Nou — pàgina d'estat pública |
| `frontend/src/services/pipeline.ts` | Nou — service calls |
| `frontend/src/messages/*.json` | Modificar — claus `pipeline.*` i `status.*` |

### 12.2 Modificacions existents

| Fitxer | Canvi |
|--------|-------|
| `LeadService.java` | Afegir gestió de `pipeline_stage` i `sla_deadline` |
| `BudgetService.java` | Transitar pipeline en crear/acceptar pressupost |
| `SetupWizardService.java` | Transitar pipeline + notificar en completar wizard |
| `TenantActivationService.java` | Transitar pipeline + disparar seqüència onboarding |

---

## 13. Prioritat d'implementació

```
FASE A — Gaps crítics (aquesta setmana):
  1. M12 → M03 auto-create lead amb pipeline_stage=PROSPECT
  2. M17 → notificació Telegram SUPER_ADMIN en completar setup
  3. Activació → missatge ONBOARDING_DAY0 al client

FASE B — Visibilitat:
  4. Taula pipeline_events + ALTER leads
  5. Pàgina /status/[token] per al client
  6. SLA monitor @Scheduled

FASE C — Pipeline complet:
  7. Dashboard kanban /portal/admin/pipeline
  8. Follow-ups automàtics de pressupost
  9. Seqüència onboarding dia 3 i dia 7
```

---

## 14. Verificacions QA

```
[ ] Crear prospect via M12 → lead apareix a M03 amb stage=PROSPECT
[ ] Completar Setup Wizard → Telegram arriba al SUPER_ADMIN en < 1 min
[ ] Activar tenant → client rep WhatsApp ONBOARDING_DAY0 en < 2 min
[ ] /status/[token] mostra etapa correcta sense autenticació
[ ] Acceptar pressupost i no respondre 48h → follow-up WhatsApp enviat
[ ] SLA expirat → alerta Telegram SUPER_ADMIN
[ ] Historial de transicions visible a /pipeline/[leadId]/events
```
