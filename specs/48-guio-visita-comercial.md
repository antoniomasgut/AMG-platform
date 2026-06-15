# Spec 48 — Guió de Visita Comercial NexeLocal

**Versió:** 1.0 · **Estat:** Aprovat  
**Autor:** AMG Digitalització  
**Relacionat amb:** Spec 22 (Sector Pricing), Spec 45 (Post-Budget Booking), Spec 47 (Sector Phases Catalog), Mòdul 48 (Setup Intake — V62)

---

## 1. Objectiu

Documentar el flux complet de la visita comercial (presencial o videotrucada), des que contactem un lead fins que el client té l'agent actiu i les integracions configurades. Defineix:

- Quina informació hem de recollir del client
- Quines preguntes fer i en quin ordre
- Com presentar les fases i els preus
- El flux de pressupost → acceptació → fitxa de configuració → posada en marxa

---

## 2. Informació disponible abans de la visita

El comercial ha de revisar el CRM (`/portal/leads`) abans de la visita per saber:

| Camp | On és |
|------|-------|
| Nom i sector del negoci | Lead CRM |
| Telèfon de WhatsApp | Lead CRM |
| Origen del lead (widget, Meta Ads, referència) | Lead CRM |
| Notes de conversa prèvia | Lead CRM / historial WhatsApp |

---

## 3. Guió de conversa — fase per fase

### 3.1 Obertura (5 min)

Objectiu: trencar el gel, confirmar context, establir agenda.

```
"Hola [nom], gràcies per rebre'ns. Em confirmes que tens [negoci]?
Avui voldria entendre una mica el vostre dia a dia i veure si el que
fem pot ajudar-vos. Trigarem uns 30 minuts. ¿Et va bé?"
```

### 3.2 Diagnosi de la situació actual (10 min)

Recollir sense pressa, amb preguntes obertes. El comercial omple la fitxa de lead durant la conversa.

#### a) Negoci i clients

- Quants clients atens al mes aproximadament?
- Quins són els teus canals actuals de contacte? (trucada, WhatsApp personal, formulari web...)
- Quina és la consulta més repetida que reps?
- Quant de temps dediques a respondre missatges i gestionar cites/pressupostos per setmana?

#### b) Infraestructura digital actual

| Pregunta | Possible resposta | Implicació |
|----------|------------------|------------|
| Tens web pròpia? | Sí / No / En construcció | Si sí, demanar URL |
| Tens domini propi? | Sí / No | Si no, podem registrar-n'hi un |
| Fas servir WhatsApp Business (app groga)? | Sí / No / Personal | Si no, cal activar-lo |
| Tens l'API de WhatsApp (no l'app)? | Sí / No / No sé | Ens en cuidem nosaltres |
| Tens Telegram? | Sí / No | Canal intern de notificacions per al negoci |
| Tens Google Calendar? | Sí / No | Per sincronitzar agenda si contracten F2 |

#### c) Punts de dolor concrets

- Quan ets tancat o ocupat, qui respon els WhatsApps?
- Has perdut clients per no respondre a temps?
- Quant temps portes a fer pressupostos manualment?
- Els clients et demanen sovint el mateix? (horaris, preus, disponibilitat)

### 3.3 Presentació de la solució (10 min)

Usar el catàleg de fases del sector del client (Spec 47). Presentar de més simple a més complet:

1. **Explicar el nucli (sempre F1 / SP1):** "El primer que posem és l'agent d'IA que atén el WhatsApp 24h. No cal que facis res — respons automàticament les preguntes freqüents i capta les dades del client."

2. **Presentar les 3 primeres fases del seu sector:**
   - Explicar QUÈ fa cada fase, no la tecnologia
   - Destacar quina estalvia més temps (veure recomanació a Spec 47, §2 de cada sector)
   - Donar un exemple concret: "Per exemple, un pintor com tu — el bot rep el WhatsApp, demana els m² i la zona, i et genera el pressupost sol. Tu no tocs res fins que el client ja l'ha rebut."

3. **Mostrar el panel de gestió** (demo en directe si és possible):
   - Converses centralitzades
   - Pressupostos enviats i estats
   - Cites del dia

4. **No donar preu fins que tinguem el context complet.**

### 3.4 Preguntes específiques per web i presència digital

Abans de parlar de preus, aclarir la situació web perquè forma part del pressupost:

```
"Ara parlant de la part web — hi ha tres opcions, et les explico ràpid:"
```

| Opció | Setup | Mensual | Per a qui |
|-------|-------|---------|-----------|
| Sense landing (sols WhatsApp + agent) | — | — | Ja té web o no la vol |
| Micro-landing | 30 € | 9 €/mes | Vol presència web mínima |
| Landing Pro | 80 € | 15 €/mes | Vol landing + widget xat + botó WhatsApp |

Preguntes a fer:
- "Vols que el widget de xat de l'agent aparegui a la teva web?"
- "Vols un botó de WhatsApp visible a la web?"
- "Prefereixes usar el teu domini actual, registrar-ne un de nou, o deixar-ho per més endavant?"

**Recomanació estàndard:** Landing Pro si no té web (ja inclou widget + WhatsApp). Si ja té web, integrar widget i botó WhatsApp a la seva web existent (servei tècnic addicional).

### 3.5 Tancament i acord de pressupost (5 min)

Un cop recollida tota la informació:

```
"Bé, doncs amb tot el que m'has explicat, t'envio el pressupost
personalitzat avui mateix. El tens en 24 hores. Revises-lo i
parlem per WhatsApp si tens qualsevol dubte."
```

**No donar preus exactes oralment** — sempre remetre al pressupost formal. Es pot donar una forquilla orientativa:

```
"Estem parlant d'un setup inicial entre X i Y euros, i uns Z euros al mes.
Però el pressupost final depèn de les fases que triem."
```

---

## 4. Fitxa de recollida d'informació (checklist comercial)

El comercial ha de tenir tots aquests camps al finalitzar la visita:

### Identificació del negoci
- [ ] Nom comercial
- [ ] Sector (BusinessSector)
- [ ] Nom del responsable
- [ ] Telèfon WhatsApp Business
- [ ] Email de contacte
- [ ] Adreça / zona d'actuació

### Infraestructura actual
- [ ] Té web pròpia? URL:
- [ ] Té domini propi? Quin:
- [ ] WhatsApp Business actiu? (app / API / cap)
- [ ] Google Calendar? Email:
- [ ] Telegram personal/negoci?

### Landing i web
- [ ] Vol landing? (cap / micro / pro)
- [ ] Vol widget de xat a la web?
- [ ] Vol botó de WhatsApp?
- [ ] Situació del domini: (el seu / nou / cap)
- [ ] Nom de domini preferit (si nou):

### Fases seleccionades
- [ ] SP1 (F1 Agent IA) — sempre inclosa
- [ ] SP2 (F2 Agenda/Reserves) — si aplica al sector
- [ ] SP3 (F3 Pressupostos) — si aplica al sector
- [ ] SP4+ — fases addicionals del seu sector

### Informació per a la configuració de l'agent
- [ ] Horaris d'atenció
- [ ] Serveis principals (per als prompts)
- [ ] Preus orientatius (si els vol al bot)
- [ ] To de comunicació (formal / proper / tècnic)
- [ ] Casos especials o urgències del sector

---

## 5. Forquilles de preu orientatives per presentar oralment

> Sempre remetre al pressupost formal. Aquests valors són per a la conversa.

### Paquet mínim (SP1 únicament — agent IA bàsic)

| Mida negoci | Setup | Mensual |
|-------------|-------|---------|
| Autònom | 290 € | 59 €/mes |
| Petit | 390 € | 79 €/mes |
| Mitjà | 590 € | 99 €/mes |

_"Per poder atendre els teus clients 24h a WhatsApp sense fer res tu."_

### Paquet recomanat per sector (SP1 + SP2 o SP3)

Depèn del sector — consultar `specs/22-sector-pricing.md` i `specs/47-sector-phases-catalog.md`. Exemple per a un **pintor**:

| SP1 + SP3 (Agent + Pressupostos) | Autònom: ~560€ setup + ~99€/mes |
|---|---|

### Paquet complet (totes les fases del sector)

Preus del sector pricing complet. Setup total = suma de `setupFn` per cada fase. Mensual = suma progressiva.

### Serveis addicionals a mencionar

| Servei | Setup |
|--------|-------|
| Landing Pro | 80 € + 15 €/mes |
| Micro-landing | 30 € + 9 €/mes |
| WhatsApp Business API | 50 € |
| Domini gestionat | 60 € (inclou 1r any) + 15 €/any |

---

## 6. Flux post-visita fins a l'activació

```
VISITA COMERCIAL
     │
     ▼
CRM: lead → prospecte, notes de visita
     │
     ▼
PRESSUPOST GENERAT (portal admin)
  - Fases seleccionades per sector
  - Landing / domini inclosos o no
  - Mida del negoci → preus correctes
     │
     ▼
ENVIAT AL CLIENT (email + WhatsApp)
  - Link pàgina d'acceptació (/accept-budget/{token})
     │
     ▼ (client accepta)
ACCEPTACIÓ CONFIRMADA
     │
     ├── Auto: email de confirmació al client
     ├── Auto: Telegram intern → "Nou client: [nom]"
     │
     ▼
FITXA DE CONFIGURACIÓ (Setup Intake)
  - El comercial genera la fitxa des del portal (o s'ha omplit durant la visita)
  - S'envia link al client per WhatsApp/email (si falta informació)
  - Seccions: AGENT · WEB · PRESSUPOSTOS · AGENDA · FIDELITZACIO (segons fases)
     │
     ▼ (fitxa completa)
POSADA EN MARXA
  - Activació WhatsApp Business API (si no la té)
  - Configuració agent (prompt, horaris, serveis)
  - Landing / domini (si contractats)
  - Integracions: Google Calendar, Google Reviews URL
  - Test end-to-end amb el client
     │
     ▼
ENTREGA AL CLIENT
  - Accés al panel (/portal)
  - Tutorial bàsic (15 min)
  - Canal Telegram intern activat
```

---

## 7. Fitxa de configuració (Setup Intake) — detall tècnic

La fitxa és la pàgina pública `/setup-intake/{token}` generada des del portal admin.

### Quan omplir-la

| Moment | Recomanació |
|--------|-------------|
| Durant la visita | Ideal — el comercial omple mentre parla |
| Just després de la visita | Comercial omple amb les notes recollides |
| Post-acceptació (enviada al client) | Si el client vol revisar-la ell |
| Trucada de seguiment | Omplir junts per telèfon |

### Seccions de la fitxa per sector

| Secció | Sempre | OFICIS | SALUT | CITES | RESTAURANTE | ACADEMIA | GESTORIA | INMOBILIARIA |
|--------|--------|--------|-------|-------|-------------|----------|----------|--------------|
| AGENT (informació bàsica de l'agent) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| WEB (landing, domini, widgets) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| PRESSUPOSTOS (catàleg, validesa, condicions) | | SP1+SP3 | SP5 | — | — | SP2 | SP2 | — |
| AGENDA (durada, calendar, notes) | | SP4 | SP2 | SP2 | SP2 | — | SP4 | SP3 |
| FIDELITZACIO (Google Reviews, seguiment) | | SP7 | SP6 | SP4+ | SP5 | SP6 | SP6 | SP6 |

### Flux d'aplicació de la fitxa

Un cop la fitxa és `COMPLETE`:
1. Comercial revisa les dades al portal
2. Aplica manualment als serveis (`nexe_service_configs`) o via wizard de la fase
3. Activa l'agent i fa test de conversa
4. Entrega accés al client

---

## 8. Missatges de WhatsApp suggerits (post-visita)

### Enviament del pressupost
```
Hola [nom]! 👋

Tal com hem comentat, t'envio el pressupost per a [negoci].

Revisa'l aquí: [link acceptació]

Si tens qualsevol dubte, m'escrius per aquí.
Una salutació!
```

### Recordatori (si no ha obert en 48h)
```
Hola [nom], et volia recordar que tens el pressupost pendent de revisió.
Aquí tens el link: [link]

Si vols quedar per comentar-lo, m'ho dius! 😊
```

### Post-acceptació — enviament fitxa
```
Perfecte [nom], m'alegro! 🎉

Per posar-nos en marxa, necessito uns detalls de configuració.
Pots omplir-los aquí (triga uns 5 min): [link fitxa]

Un cop ho tingui, comencem! 🚀
```

---

## 9. Preguntes freqüents del prospect

| Pregunta | Resposta recomanada |
|----------|---------------------|
| "Quant de temps triga la posada en marxa?" | "Normalment entre 48 i 72h un cop tenim la informació completa." |
| "He de fer alguna cosa jo?" | "Poca cosa — omplir la fitxa de configuració i confirmar que el WhatsApp Business és el correcte. Nosaltres fem la resta." |
| "I si vull canviar alguna cosa?" | "Ho pots gestionar tu des del panel, o ens ho dius i ho canviem nosaltres." |
| "Puc cancel·lar quan vulgui?" | "Sí, és mensual. No hi ha permanència." |
| "Funciona en català?" | "Sí, l'agent pot respondre en català, castellà, anglès o alemany — tu tries." |
| "I si el bot no sap respondre alguna cosa?" | "T'avisa per Telegram i pots agafar tu la conversa quan vulguis." |
| "Tens referències de clients del meu sector?" | "Sí, puc passar-te el contacte d'un client que va donar permís per a referències." |

---

## 10. Criteris de qualificació del lead (MQL → SQL)

Un lead passa a SQL (Sales Qualified) quan compleix ≥ 3:

- [ ] Té WhatsApp Business o vol activar-lo
- [ ] Gestiona >10 clients/mes
- [ ] Dedica >3h/setmana a gestió manual de consultes
- [ ] No té sistema d'automatització actual
- [ ] Ha mostrat interès per la demo
- [ ] Té pressupost aprovat (o és autònom/petit negoci que decideix sol)

---

*Spec creat: 2026-06-15*
