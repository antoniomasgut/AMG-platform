# Spec 46 — Phase Integration Matrix

**Versió**: 1.0  
**Estat**: Aprovat  
**Mòdul**: 46  
**Depèn de**: Spec 22 (Sector Pricing), Spec 28 (NexeLocal Service Configs), Spec 44 (Secure Document Delivery), Spec 45 (Post-Budget Booking)

---

## 1. Principi fonamental

**F1 és sempre la base.** Un tenant pot contractar F1 sola i el sistema és completament funcional: l'agent IA respon per WhatsApp/email/widget, captura leads i gesticna FAQs.

Les fases F2–F5 s'afegeixen com a **capes independents** sobre F1. No hi ha prerequisit implícit entre elles — F3 no requereix F2, F4 no requereix F3. El que sí existeix és una **lògica d'integració**: quan dues fases coincideixen en un tenant, s'activen fluxos automàtics entre elles.

---

## 2. Matriu d'aplicabilitat per sector

Indica si una fase té sentit per a un sector i amb quin mode:

| Sector | F1 | F2 | F3 | F4 | F5 |
|--------|----|----|----|----|-----|
| FISIOTERAPEUTA | ✅ base | ✅ `appointment` | ⚡ `pricelist` | ✅ | ⚡ |
| PSICOLEG | ✅ | ✅ `appointment` | ⚡ `pricelist` | ✅ | ⚡ |
| NUTRICIONISTA | ✅ | ✅ `appointment` | ⚡ `pricelist` | ✅ | ⚡ |
| PERRUQUERIA | ✅ | ✅ `appointment` | ⚡ `pricelist` | ✅ | ⚡ |
| ESTETICA | ✅ | ✅ `appointment` | ⚡ `pricelist` | ✅ | ⚡ |
| VETERINARI | ✅ | ✅ `appointment` | ⚡ `pricelist` | ✅ | ⚡ |
| PERRUQUERIA_CANINA | ✅ | ✅ `appointment` | ⚡ `pricelist` | ✅ | ⚡ |
| ACADEMIA | ✅ | ⚡ `appointment` | ⚡ `formal` | ✅ | ⚡ |
| PINTOR | ✅ | ⚡ `inspection` | ✅ `formal` | ⚡ | ⚡ |
| ELECTRICISTA | ✅ | ⚡ `inspection` | ✅ `formal` | ⚡ | ⚡ |
| FONTANER | ✅ | ⚡ `inspection` | ✅ `formal` | ⚡ | ✅ |
| JARDINER | ✅ | ⚡ `inspection` | ✅ `formal` | ⚡ | ⚡ |
| NETEJA | ✅ | ⚡ `inspection` | ✅ `formal` | ⚡ | ⚡ |
| TALLER_MECANIC | ✅ | ✅ `vehicle` | ✅ `formal` | ⚡ | ⚡ |
| GESTORIA | ✅ | ⚡ `meeting` | ✅ `formal` | ✅ | ✅ |

**Llegenda:**
- ✅ **Prioritària** — recomanada com a primera opció per al sector; apareix pre-seleccionada al wizard
- ⚡ **Disponible** — té sentit però no és el dolor principal; apareix com a opció secundària
- Absència — no aplicable o sense cas d'ús clar per al sector

El camp `contractedPhases` al `Tenant` (p. ex. `"F1,F3"`) és la font de veritat. El wizard suggereix, però el tenant pot contractar qualsevol combinació.

---

## 3. Punts d'integració entre fases

Quan un tenant té dues fases actives, el sistema activa fluxos automàtics. Cada punt d'integració és **opcional** i configurable per tenant.

### 3.1 F3 → F2: Reserva post-acceptació de pressupost

**Trigger:** Client accepta un pressupost (Mòdul 44 `DocumentViewService.accept()`).

| Fases actives | Comportament |
|---------------|-------------|
| Només F3 | Telegram al tenant: "✅ {nom} ha acceptat el pressupost. Contacta'l per concretar la data." |
| F3 + F2 | Telegram al tenant + enviament automàtic d'enllaç de reserva al client |
| F3 + F2 + `autoBookingOnAccept=false` | Igual que "Només F3" |

**Mode de reserva** determinat per la config AGENDA del tenant:

| Mode F2 | Sectors típics | Label per al client |
|---------|---------------|-------------------|
| `appointment` | Fisioterapeuta, Perruqueria... | "Reserva la teva cita" |
| `vehicle` | Taller mecànic | "Programa l'entrega del vehicle" |
| `inspection` | Pintor, Electricista... | "Concreta el dia d'inici" |
| `meeting` | Gestoria | "Concerta la reunió" |

Veure Spec 45 per als detalls d'implementació.

---

### 3.2 F2 → F4: Seguiment post-cita

**Trigger:** Cita completada (Google Calendar event en el passat + confirmada).

| Fases actives | Comportament |
|---------------|-------------|
| Només F2 | Cap acció automàtica |
| F2 + F4 | Passades `followUpDays` hores/dies → WhatsApp al client demanant ressenya Google (`reviewsUrl`) |

Config a `nexe_service_configs.FIDELITZACIO`: `followUpDays`, `followUpTemplate`, `reviewsUrl`.

Implementat parcialment (Mòdul 33 és el cas invers: absència → resposicionament de cites). El job de follow-up post-cita **pendent** — és un cas d'ús de `AppointmentReminderScheduler` ampliat.

---

### 3.3 F3 → F4: Seguiment post-servei

**Trigger:** Document acceptat (pressupost acceptat = client ha dit que sí al treball).

| Fases actives | Comportament |
|---------------|-------------|
| Només F3 | Cap acció automàtica post-acceptació (llevar la notificació al tenant) |
| F3 + F4 | Passats `followUpDays` dies → WhatsApp al client: "Com ha anat el servei?" + demanda de ressenya |

Diferència amb 3.2: el trigger no és la cita sinó l'acceptació del pressupost. Per a sectors sense F2 (pintor, electricista), és l'únic mecanisme de seguiment postvenda automàtic.

---

### 3.4 F1 → F4: Reactivació de clients inactius

**Trigger:** Client no ha tingut cap interacció en `reengagementMonths` mesos.

| Fases actives | Comportament |
|---------------|-------------|
| Només F1 | Cap acció automàtica |
| F1 + F4 | Job mensual → WhatsApp al client: "Fa temps que no et veiem..." (`reengagementTemplate`) |

Implementat a `LeadFollowUpScheduler` (Mòdul 33 base).

---

### 3.5 F5 → F2: Cascada d'absència (Mòdul 33)

**Trigger:** Comanda Telegram `/absencia [data]` des del grup d'equip (F5).

| Fases actives | Comportament |
|---------------|-------------|
| Només F5 | Avís al grup, però sense cites que reprogramar |
| F5 + F2 | Cascada automàtica: avisar clients afectats + marcar cites com CANCELLED |

Implementat al Mòdul 33.

---

## 4. Lògica de detecció en el backend

`hasPhase(tenant, phase)` és el punt central de decisió:

```java
// A Tenant o un helper de TenantService
public boolean hasPhase(String phase) {
    if (contractedPhases == null || contractedPhases.isBlank()) return false;
    return Arrays.asList(contractedPhases.split(",")).contains(phase);
}
```

Cada punt d'integració consulta `hasPhase()` per decidir si activa el flux opcional:

```java
// Exemple a DocumentViewService.accept():
if (token.hasPhase("F3") && token.hasPhase("F2") && prefs.isAutoBookingOnAccept()) {
    sendBookingInvitation(token, bookingToken);
}
```

`contractedPhases` al tenant és la única font de veritat — no mirar flags ni taules addicionals.

---

## 5. Wizard de configuració (impacte en Mòdul 17)

El wizard de setup post-contractació ha de respectar la combinació de fases real del tenant. Mostrar només els passos rellevants:

| Fase | Pas del wizard |
|------|---------------|
| F1 (sempre) | Configurar agent IA, KB, canal WhatsApp |
| F2 (si activa) | Configurar `MeetingSettings` + Google Calendar |
| F3 (si activa) | Configurar plantilles de pressupost, catàleg de preus |
| F4 (si activa) | `reviewsUrl`, `followUpDays`, plantilles de fidelització |
| F5 (si activa) | `telegramGroupId` de l'equip |

Un tenant amb F1+F3 no veu el pas de calendari. Un tenant amb F1+F2 no veu el pas de plantilles de pressupost.

---

## 6. Recomanació per sector al wizard (Mòdul 16/17)

Quan s'activa el wizard per primera vegada, pre-seleccionar les fases prioritàries (✅ de la matriu) però permetre desmarcar-les. No bloquejar cap combinació.

Missatge orientatiu per sector (de `sector-dolors.md`):

| Sector | Combinació recomanada | Motiu |
|--------|----------------------|-------|
| FISIOTERAPEUTA | F1 + F2 (+ F4) | Agenda és el dolor principal |
| ELECTRICISTA | F1 + F3 | Pressupostos és el dolor principal; booking no és natural |
| PINTOR | F1 + F3 | Igual |
| FONTANER | F1 + F5 | Urgències → coordinació interna |
| TALLER_MECANIC | F1 + F2 + F3 | Reserva de vehicle + pressupost post-diagnòstic |
| GESTORIA | F1 + F3 + F5 | Documents + coordinació interna |
| ACADEMIA | F1 + F4 | Retenció d'alumnes > agenda |

---

## 7. Correccions a specs anteriors

### Spec 45 — Post-Budget Booking

**Secció 2 (Flux principal)** — condicional incorrecte:

~~"si tenant té F2 activa + autoBookingOnAccept=true"~~

**Correcte:**
```
[si tenant té F3 activa]   ← sempre notifica Telegram
[si tenant té F3 + F2]     ← envia booking link al client
[si autoBookingOnAccept=false] ← no envia booking (Telegram igualment)
```

**Secció 10 (Perquè no és una nova fase)** — frase incorrecta:

~~"La condició necessària és tenir F2 + F3 ambdues actives"~~

**Correcte:** La condició per rebre la notificació Telegram és tenir F3. La condició per a l'enviament automàtic del booking link és tenir F3 + F2.

---

## 8. Fases no implementades (estat actual)

| Integració | Estat |
|-----------|-------|
| F3 → F2 (booking post-pressupost) | ⏳ Spec 45 aprovat, pendent implementar |
| F2 → F4 (follow-up post-cita) | ⏳ Pendent |
| F3 → F4 (follow-up post-servei) | ⏳ Pendent |
| F1 → F4 (reactivació inactius) | ✅ Parcialment implementat (`LeadFollowUpScheduler`) |
| F5 → F2 (cascada absència) | ✅ Implementat (Mòdul 33) |

---

## 9. Correspondència F1-F5 ↔ SectorPhase per sector

La **capa F1-F5** descriu capacitats lògiques (agent IA, agenda, pressupostos, fidelització, equip) i governa els triggers d'integració i els schedulers. La **capa SectorPhase** (fases 1-7 per sector, veure Spec 47) descriu workflows concrets per a cada sector.

Les dues capes coexisteixen:
- `Tenant.contractedPhases` (`"F1,F3"`) → font de veritat per a `hasPhase()` i els triggers d'integració
- `sector_phases` (BD) → font de veritat per a preus, dependències i presentació comercial

### 9.1 Mapping conceptual per grup de sectors

**Oficis de reforma** (PINTOR · ELECTRICISTA · FONTANER · JARDINER · NETEJA · TALLER_MECANIC)

| F-Phase | Capacitat | SectorPhases equivalents |
|---------|-----------|--------------------------|
| F1 | Agent IA 24h | implícit en SP1 (la BASE inclou l'agent) |
| F2 | Agenda | SP4 (Agenda Visita de Mesura) + SP5 (Recordatoris) |
| F3 | Pressupostos | SP1 (Generador) + SP2 (Entrega) + SP3 (Seguiment i Acceptació) |
| F4 | Fidelització | SP7 (Progrés i Postvenda) |
| F5 | Equip | part de SP7 (reportes per Telegram) |

---

**Salut** (FISIOTERAPEUTA · PSICOLEG · NUTRICIONISTA)

| F-Phase | Capacitat | SectorPhases equivalents |
|---------|-----------|--------------------------|
| F1 | Agent IA 24h | implícit en SP1 |
| F2 | Agenda | SP1 (Agenda Cites — és la BASE del sector) |
| F3 | Pressupostos / factures | SP5 (Gestió de Bons i Pagaments) |
| F4 | Fidelització / seguiment | SP2 (Historial) + SP3 (Registre) + SP4 (Seguiment) + SP6 (Reactivació) |
| F5 | Equip | no mapejat directament |

---

**Restaurant**

| F-Phase | Capacitat | SectorPhases equivalents |
|---------|-----------|--------------------------|
| F1 | Agent IA 24h | implícit en SP1 |
| F2 | Agenda (reserves) | SP1 (Reserves de Taula, BASE) + SP2 (Recordatoris) |
| F3 | Pressupostos / comandes | SP4 (Comandes per Emportar) |
| F4 | Fidelització | SP5 (Fidelització i Comunicació) |
| F5 | Equip | no mapejat |

---

**Acadèmia**

| F-Phase | Capacitat | SectorPhases equivalents |
|---------|-----------|--------------------------|
| F1 | Agent IA 24h | SP1 (Informació i Captació, BASE) |
| F2 | Agenda | no mapejat directament (no hi ha booking de slot) |
| F3 | Pressupostos / documents | SP2 (Matrícula) + SP5 (Pagaments) |
| F4 | Fidelització / seguiment | SP3 (Assistència) + SP4 (Progrés) + SP6 (Renovació) |
| F5 | Equip | SP3 (professors per Telegram) |

---

**Veterinari / Perruqueria canina**

| F-Phase | Capacitat | SectorPhases equivalents |
|---------|-----------|--------------------------|
| F1 | Agent IA 24h | implícit en SP1 |
| F2 | Agenda | SP1 (Agenda Cites, BASE) + SP4 (Recordatoris Vacunes) |
| F3 | Pressupostos | no mapejat directament |
| F4 | Fidelització / seguiment | SP2 (Historial) + SP3 (Registre) + SP5 (Tractaments) + SP6 (Postvenda) |
| F5 | Equip | no mapejat |

---

**Perruqueria / Estètica**

| F-Phase | Capacitat | SectorPhases equivalents |
|---------|-----------|--------------------------|
| F1 | Agent IA 24h | implícit en SP1 |
| F2 | Agenda | SP1 (Reserva Cita, BASE) + SP2 (Recordatoris) |
| F3 | Pressupostos | SP5 (Gestió de Productes) |
| F4 | Fidelització | SP3 (Historial) + SP4 (Fidelització i Reactivació) |
| F5 | Equip | no mapejat |

---

**Gestoria**

| F-Phase | Capacitat | SectorPhases equivalents |
|---------|-----------|--------------------------|
| F1 | Agent IA 24h | SP1 (Captació i Consulta, BASE) |
| F2 | Agenda (reunions) | part de SP1 (agenda reunió dins captació) |
| F3 | Pressupostos / documents | SP2 (Alta/Documentació) + SP4 (Enviament Documents) + SP5 (Pagaments) |
| F4 | Fidelització / terminis | SP3 (Terminis i Recordatoris) + SP6 (Renovació) |
| F5 | Equip | SP3 (notificació professional per Telegram) |

---

**Immobiliària**

| F-Phase | Capacitat | SectorPhases equivalents |
|---------|-----------|--------------------------|
| F1 | Agent IA 24h | SP1 (Captació Propietats, BASE) + SP2 (Cerca i Filtratge) |
| F2 | Agenda | SP3 (Agenda Visites, REQUIRED:1,2) |
| F3 | Pressupostos / ofertes | SP5 (Gestió d'Ofertes) |
| F4 | Fidelització | SP4 (Seguiment Post-visita) + SP6 (Fidelització i referències) |
| F5 | Equip | no mapejat |

---

### 9.2 Regla pràctica per a `hasPhase()`

Fins que existeixi una taula `tenant_contracted_sector_phases` (pendent, veure Spec 47 §5), la detecció de fases es fa via `Tenant.contractedPhases`:

```java
// Exemple: saber si el tenant té capacitat d'agenda activa
tenant.hasPhase("F2")  // → true si contractedPhases conté "F2"
```

El mapping de la secció 9.1 és la guia per saber **quina F-phase correspon a cada SectorPhase** en el wizard de setup i en la presentació comercial. El trigger d'integració (p. ex. F3 → F2) s'avalua sempre a nivell F-phase, independentment del sector.

### 9.3 Cas especial: BASE diferent per sector

En sectors d'oficis (PINTOR, ELECTRICISTA...), la SectorPhase BASE (SP1) engloba **F1 + F3** alhora (l'agent i el generador de pressupostos van sempre junts). En sectors de salut, perruqueria i veterinari, la BASE engloba **F1 + F2** (l'agent i l'agenda van junts).

Implicació: **un tenant d'oficis no pot tenir F1 sense F3** en la pràctica; i **un tenant de salut no pot tenir F1 sense F2**. El wizard de setup ha de respectar això i no oferir F1 aïllada en aquests sectors.
