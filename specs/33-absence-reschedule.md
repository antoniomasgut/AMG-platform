# Mòdul 33 — Absence & Reschedule Cascade

**Fases involucrades:** F2 (Agenda) + F4 (Fidelització) + F5 (Equip)  
**Trigger principal:** Comanda Telegram `/absencia [data]`

---

## Problema

Quan un professional (fisio, perruquer, etc.) no pot treballar un dia i un company no pot cobrir,
cal contactar manualment cada pacient per avisar i reprogramar. Amb F2+F4 actives,
el sistema ha de fer-ho automàticament.

---

## Flux principal

```
Professional envia "/absencia 2026-06-10" al grup Telegram
                ↓
Sistema busca totes les cites pendents del dia afectat
  (ScheduledAgentTask on agentSlug=appointment-reminder,
   status=PENDING, scheduledAt dins el dia indicat)
                ↓
Per cada cita afectada:
  1. Envia WhatsApp/Telegram al pacient:
     "Hola [nom], la teva cita del [data] a les [hora] s'ha hagut de cancel·lar.
      Et contactarem en breu per trobar un nou dia. Disculpa les molèsties."
  2. Marca la tasca de recordatori com CANCELLED
  3. Crea nova tasca: agentSlug=reschedule-pending (per a seguiment)
                ↓
Envia resum al grup Telegram (F5):
  "Absència [data]: X cites afectades. Missatges enviats. Reprogramació pendent."
                ↓
Registra AbsenceRecord per auditoriat
```

---

## Comandes Telegram suportades

| Comanda | Exemple | Acció |
|---------|---------|-------|
| `/absencia [data]` | `/absencia 2026-06-10` | Cascade per data concreta |
| `/absencia demà` | `/absencia demà` | Cascade per demà |
| `/absencia avui` | `/absencia avui` | Cascade per avui |

---

## Entitats noves

### AbsenceRecord
Taula: `absence_records`

| Camp | Tipus | Descripció |
|------|-------|-----------|
| id | UUID PK | |
| tenant_id | UUID | |
| absence_date | DATE | Data de l'absència |
| triggered_by | BIGINT | telegram_user_id del professional |
| affected_count | INT | Nombre de cites afectades |
| notified_count | INT | Nombre de pacients notificats amb èxit |
| created_at | TIMESTAMPTZ | |

### ScheduledTaskStatus (extensió)
Afegir: `CANCELLED` — tasca cancel·lada per absència

---

## Tasca de seguiment (reschedule-pending)

Quan una cita es cancel·la, es crea una nova `ScheduledAgentTask`:
```json
{
  "agentSlug": "reschedule-pending",
  "taskType": "RESCHEDULE_FOLLOWUP",
  "payload": {
    "identifier": "+34612345678",
    "channel": "WHATSAPP",
    "name": "Maria",
    "originalDate": "2026-06-10",
    "originalTime": "10:00"
  },
  "scheduledAt": "now + 24h"
}
```

Als 24h, `AgentTaskScheduler` envia missatge de seguiment:
*"Hola Maria, encara no hem trobat un nou dia per a la teva cita. Quan et va bé?"*

---

## Gating de fases

| Capacitat | Fase mínima |
|-----------|-------------|
| Comanda `/absencia` activa | F2 |
| Notificació automàtica als pacients | F2 + F4 |
| Resum al grup Telegram | F2 + F4 + F5 |
| Seguiment de reprogramació als 24h | F4 |

Si F4 no és contractada, el sistema envia avís però no programa el seguiment.

---

## Fitxers afectats

**Backend — nous:**
- `agents/domain/AbsenceRecord.java`
- `agents/domain/AbsenceRecordRepository.java`
- `agents/application/AbsenceRescheduleService.java`
- `agents/impl/RescheduleFollowUpAgent.java`

**Backend — modificats:**
- `agents/domain/ScheduledTaskStatus.java` — afegir CANCELLED
- `agents/domain/ScheduledAgentTaskRepository.java` — afegir query per tenant+date
- `agents/api/TelegramWebhookController.java` — handler `/absencia`

**Producció — SQL:**
```sql
CREATE TABLE absence_records (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id UUID NOT NULL,
  absence_date DATE NOT NULL,
  triggered_by BIGINT,
  affected_count INT NOT NULL DEFAULT 0,
  notified_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```
