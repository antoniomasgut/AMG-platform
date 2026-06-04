# Spec 28 — NexeLocal Service Configs

**Versió**: 1.1  
**Estat**: Completat ✅ (v1.1: camp `enabled` afegit a tots els serveis)  
**Mòdul**: 28  
**Depèn de**: Mòdul 22 (Sector Pricing), Mòdul 20 (Agents IA)

---

## 1. Visió general

Cada fase NexeLocal (F2–F5) té paràmetres de configuració específics per sector que determinen el comportament del bot i dels serveis. Aquests paràmetres es guarden per tenant i per servei, i el bot els llegeix per adaptar la seva resposta.

| Fase | Servei | Clau |
|------|--------|------|
| F2 | Agenda (cites / visites) | `AGENDA` |
| F3 | Pressupostos i preus | `PRESSUPOSTOS` |
| F4 | Seguiment de clients | `FIDELITZACIO` |
| F5 | Alertes & Equip intern | `EQUIP` |

---

## 2. Model de dades

### Taula `nexe_service_configs`

```sql
CREATE TABLE nexe_service_configs (
  tenant_id   UUID         NOT NULL,
  service_key VARCHAR(30)  NOT NULL,
  config_json TEXT,
  updated_at  TIMESTAMPTZ  DEFAULT NOW(),
  PRIMARY KEY (tenant_id, service_key)
);
```

- PK composta `(tenant_id, service_key)` — un únic registre per tenant i servei
- `config_json` emmagatzema la configuració com a JSON lliure
- Upsert: `PUT` sempre crea o actualitza

---

## 3. API

| Mètode | Ruta | Rol | Descripció |
|--------|------|-----|------------|
| `GET` | `/api/v1/nexe/tenants/{tenantId}/configs/{serviceKey}` | SUPER_ADMIN / ADMIN | Llegir config d'un servei |
| `PUT` | `/api/v1/nexe/tenants/{tenantId}/configs/{serviceKey}` | SUPER_ADMIN / ADMIN | Desar config (upsert) |

---

## 4. Configuració per servei

### 4.1 AGENDA (F2)

Mode determinat pel sector:

| Mode | Sectors |
|------|---------|
| `appointment` | FISIOTERAPEUTA, PSICOLEG, NUTRICIONISTA, PERRUQUERIA, ESTETICA, PERRUQUERIA_CANINA, VETERINARI, ACADEMIA |
| `inspection` | PINTOR, ELECTRICISTA, FONTANER, JARDINER, NETEJA |
| `vehicle` | TALLER_MECANIC |
| `meeting` | GESTORIA |

**Camps comuns**:
- `slotMinutes` — durada de cada cita/visita en minuts
- `bufferMinutes` — temps de marge entre cites
- `workingHours` — grid de 7 dies amb hora d'obertura/tancament
- `clientQuestions` — preguntes que el bot fa al client per recollir info prèvia
- `confirmationTemplate` — missatge de confirmació enviat al client
- `calendarIntegration` — `google_calendar` / `calendly` / `none`
- `serviceZone` — zona geogràfica (per a modes inspection)

**Exemple PINTOR (inspection)**:
```json
{
  "mode": "inspection",
  "slotMinutes": 60,
  "serviceZone": "Palma i rodalia",
  "clientQuestions": ["Adreça", "Tipus de treball", "m² aproximats", "Urgència"],
  "confirmationTemplate": "Hola {nom}, hem confirmat la visita d'inspecció per al {data} a les {hora}. Us enviarem el pressupost el mateix dia."
}
```

**Exemple FISIOTERAPEUTA (appointment)**:
```json
{
  "mode": "appointment",
  "slotMinutes": 60,
  "bufferMinutes": 15,
  "calendarIntegration": "google_calendar",
  "clientQuestions": ["Motiu de la consulta", "Ets pacient nou?", "Patologia prèvia"],
  "confirmationTemplate": "Cita confirmada per al {data} a les {hora}. Clínica {nom_negoci}."
}
```

---

### 4.2 PRESSUPOSTOS (F3)

Mode determinat pel sector:

| Mode | Sectors |
|------|---------|
| `formal` | PINTOR, ELECTRICISTA, FONTANER, JARDINER, NETEJA, TALLER_MECANIC, GESTORIA, ACADEMIA |
| `pricelist` | FISIOTERAPEUTA, PSICOLEG, NUTRICIONISTA, PERRUQUERIA, ESTETICA, PERRUQUERIA_CANINA, VETERINARI |

**Camps mode `formal`**:
- `validityDays` — dies de validesa del pressupost
- `headerText` — text de capçalera del PDF
- `footerText` — text de peu del PDF
- `catalogItems` — array de `{name, unit, unitPrice}` (per unitat, m², hora...)
- `includeVat` — boolean (IVA inclòs o no)
- `vatPct` — percentatge IVA (default 21)

**Camps mode `pricelist`**:
- `services` — array de `{name, durationMinutes, price, description}`
- `currency` — `EUR` per defecte
- `priceIncludesVat` — boolean

**Exemple PINTOR (formal)**:
```json
{
  "mode": "formal",
  "validityDays": 30,
  "headerText": "Pressupost sense compromís",
  "catalogItems": [
    {"name": "Pintura interior", "unit": "m²", "unitPrice": 8.50},
    {"name": "Pintura exterior", "unit": "m²", "unitPrice": 12.00},
    {"name": "Mà d'obra hora", "unit": "h", "unitPrice": 25.00}
  ],
  "includeVat": false,
  "vatPct": 21
}
```

**Exemple PERRUQUERIA (pricelist)**:
```json
{
  "mode": "pricelist",
  "services": [
    {"name": "Tall dona", "durationMinutes": 45, "price": 25},
    {"name": "Tint", "durationMinutes": 90, "price": 55},
    {"name": "Mechas", "durationMinutes": 120, "price": 75}
  ],
  "priceIncludesVat": true
}
```

---

### 4.3 FIDELITZACIO (F4)

Camps:
- `reviewsUrl` — URL de Google Reviews del negoci
- `followUpDays` — dies després del servei per enviar missatge de seguiment
- `followUpTemplate` — plantilla de missatge post-servei
- `reengagementMonths` — mesos d'inactivitat abans de contactar un client
- `reengagementTemplate` — plantilla de reenganchament

**Exemple**:
```json
{
  "reviewsUrl": "https://g.page/r/XXXXX/review",
  "followUpDays": 3,
  "followUpTemplate": "Hola {nom}! Com ha anat el servei? Si tens un moment, ens ajudaria molt que deixessis una ressenya: {reviews_url}",
  "reengagementMonths": 3,
  "reengagementTemplate": "Hola {nom}, fa temps que no et veiem! Tens alguna cosa pendent o vols demanar cita?"
}
```

---

### 4.4 EQUIP (F5)

Grup de Telegram compartit per a l'equip intern del tenant.

Camps:
- `telegramGroupId` — ID numèric del grup de Telegram (`-100XXXXXXXXXX`)
- `telegramGroupName` — nom del grup (visual, no funcional)
- `dailyReportEnabled` — boolean, informe diari al grup
- `dailyReportTime` — hora de l'informe (ex: `"08:00"`)
- `notifyNewLead` — boolean, notifica quan arriba un nou lead
- `notifyNewBooking` — boolean, notifica quan es fa una reserva

**Configuració del grup** (instruccions mostrades al portal):
1. Crear grup de Telegram per a l'equip
2. Afegir el bot (`@amgdl_bot`) al grup
3. Fer el bot administrador
4. Escriure `/start` al grup
5. Usar `@userinfobot` per obtenir l'ID del grup
6. Enganxar l'ID al camp `telegramGroupId`

**Exemple**:
```json
{
  "telegramGroupId": -1001234567890,
  "telegramGroupName": "Equip Pintures Miquel",
  "dailyReportEnabled": true,
  "dailyReportTime": "08:00",
  "notifyNewLead": true,
  "notifyNewBooking": true
}
```

---

## 5. Camp `enabled` — control d'activació per fase

Tots els serveis (AGENDA, PRESSUPOSTOS, FIDELITZACIO, EQUIP) tenen el camp `enabled: boolean` al JSON de configuració. **Per defecte és `false`** — un servei pot estar configurat però no actiu.

| Valor | Comportament |
|-------|-------------|
| `enabled: true` | El bloc s'injecta al system prompt del bot en tots els canals |
| `enabled: false` o absent | El bloc **no s'injecta** — el bot no gestiona aquesta funcionalitat |

Això permet:
- Configurar un servei (F3, F4...) sense activar-lo fins que el client no hagi contractat la fase
- Desactivar temporalment sense perdre la configuració
- Proves en sandbox abans d'activar en producció

**Exemple AGENDA inactiu** (configurat però no activat):
```json
{ "enabled": false, "mode": "inspection", "slotMinutes": 60 }
```

**Exemple AGENDA actiu**:
```json
{ "enabled": true, "mode": "inspection", "slotMinutes": 60, "calendar_type": "google", "google_calendar_id": "..." }
```

> Veure Spec 31 per al model complet de funcionalitats per fase de l'agent conversacional.

---

## 6. Integració amb el bot (PromptBuilder)

`PromptBuilder.buildNexeBlock()` llegeix les configs de tots 4 serveis i afegeix instruccions contextuals al system prompt del bot:

- **AGENDA**: instrueix el bot sobre com recollir dades per a cites/visites, quines preguntes fer, i com confirmar
- **PRESSUPOSTOS**: instrueix el bot sobre com presentar preus o com gestionar sol·licituds de pressupost formal
- **FIDELITZACIO**: permet al bot respondre sobre l'estat de les ressenyes i el programa de fidelització
- **EQUIP**: el bot sap que pot notificar al grup de Telegram intern quan cal escalada humana

---

## 7. Frontend

### Ruta
`/portal/admin/tenants/[id]/nexe/[service]`

On `[service]` pot ser: `agenda`, `pressupostos`, `fidelitzacio`, `equip`

### Accés des del detall de tenant
Secció "Assignació" → taula de fases NexeLocal → botó **Configurar** per a cada fase

### Formularis
Cada servei té el seu formulari adaptat per sector (detectat automàticament des del sector del tenant). Els camps pre-ompolerts amb valors per defecte del sector faciliten la configuració inicial.

---

## 8. Defaults per sector

El frontend omple automàticament els valors per defecte quan s'obre el formulari per primera vegada, basant-se en el sector del tenant. Implementat a `nexe-configs.ts`:

- `getAgendaDefaults(sector)` → retorna config per defecte per al mode corresponent
- `getPressupostosDefaults(sector)` → retorna config per defecte per al mode corresponent
- Els defaults inclouen preguntes típiques del sector, durades habituals i plantilles de text

---

## 9. Notes d'implementació

- La taula `nexe_service_configs` s'ha de crear manualment a producció (Hibernate `ddl-auto: validate` no la crea)
- SQL de creació: `CREATE TABLE nexe_service_configs (tenant_id UUID, service_key VARCHAR(30), config_json TEXT, updated_at TIMESTAMPTZ DEFAULT NOW(), PRIMARY KEY (tenant_id, service_key))`
- No hi ha validació d'esquema del JSON — és flexible per permetre evolució sense migracions
