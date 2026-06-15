# Spec 47 — Catàleg de Fases per Sector

**Versió**: 1.0  
**Estat**: Aprovat  
**Font de veritat**: `SectorPhaseSeeder.java` → taula `sector_phases`  
**Depèn de**: Spec 22 (Sector Pricing), Spec 46 (Phase Integration Matrix)

---

## 1. Principi

Cada sector té entre 5 i 7 **fases sectorials** (`SectorPhase`) numerades. Aquestes fases concreten el valor real de cada funcionalitat per a un sector específic, amb nom, descripció, dependències i preu propi.

**`SectorPhase` és la font de veritat per a contractació i presentació comercial.**  
El `ServicePhase` genèric (F1-F5) s'usa per a lògica de scheduler i integrations matrix; quan hi ha conflicte, `SectorPhase` preval.

### Tipus de dependència (`PhaseDepType`)

| Tipus | Significat |
|-------|-----------|
| `BASE` | Punt d'entrada obligatori del sector. No té prerequisits |
| `OPTIONAL` | Es pot contractar de forma independent si el requisit ja està actiu |
| `REQUIRED` | Dependència tècnica forta: no funciona sense les fases indicades |

---

## 2. Grups de sectors

### 2.1 Oficis de Reforma (7 fases)

Sectors: **PINTOR · ELECTRICISTA · FONTANER · JARDINER · NETEJA · TALLER_MECANIC**

Estructura compartida. El diferenciador és el tipus de treball (exterior/interior, urgència, vehicle).

| Fase | Nom | Tipus | Req | Setup | /mes | Descripció |
|------|-----|-------|-----|-------|------|-----------|
| 1 | Generador de Pressupostos | BASE | — | 99€ | 59€ | Envio mesures per Telegram → bot genera pressupost → envia al client per email |
| 2 | Entrega Automàtica al Client | OPTIONAL | 1 | 49€ | 20€ | El pressupost s'envia automàticament per WhatsApp i email sense intervenció manual |
| 3 | Seguiment i Acceptació | OPTIONAL | 1 | 49€ | 18€ | Si el client no respon en X dies, el bot envia recordatori. Notifica quan accepta/rebutja |
| 4 | Agenda de Visita de Mesura | REQUIRED | 1 | 79€ | 18€ | El client sol·licita visita per WhatsApp → bot consulta agenda i confirma |
| 5 | Recordatoris Automàtics | OPTIONAL | 4 | 29€ | 12€ | 24h abans de la visita: recordatori amb opció de confirmar o cancel·lar |
| 6 | Interpretació de Fotos | OPTIONAL | 1 | 79€ | 15€ | L'operari envia fotos → bot analitza amb visió artificial i enriqueix el pressupost |
| 7 | Progrés i Postvenda | OPTIONAL | 4 | 49€ | 10€ | L'operari reporta avanços per Telegram → bot notifica el client. Sol·licita valoració al final |

**Fase que estalvia més temps: Fase 1+2+3**  
La generació manual de pressupostos i el seguiment posterior és el major malbaratament de temps als oficis. Amb fase 1+2+3 el 80% del flux de pressupostos és automàtic.

**Combinació recomanada per primer contracte**: Fase 1 + Fase 2 + Fase 3 (total setup: 197€, 97€/mes)

---

### 2.2 Salut i Benestar (6 fases)

Sectors: **FISIOTERAPEUTA · PSICOLEG · NUTRICIONISTA**

El dolor principal és l'agenda i el seguiment entre sessions.

| Fase | Nom | Tipus | Req | Setup | /mes | Descripció |
|------|-----|-------|-----|-------|------|-----------|
| 1 | Agenda de Cites | BASE | — | 99€ | 59€ | El pacient reserva per WhatsApp → bot mostra disponibilitat i confirma |
| 2 | Historial del Pacient | OPTIONAL | 1 | 69€ | 25€ | El professional consulta l'historial per Telegram. El pacient pot sol·licitar-lo per email |
| 3 | Registre de Sessió | REQUIRED | 1,2 | 79€ | 22€ | El professional dicta nota de veu per Telegram → bot estructura i guarda l'historial |
| 4 | Seguiment entre Sessions | OPTIONAL | 1 | 79€ | 18€ | El professional pauta exercicis → bot els envia per email/WhatsApp i recull evolució |
| 5 | Gestió de Bons i Pagaments | OPTIONAL | 1 | 69€ | 15€ | Control de sessions per bo, avís quan queden 2 sessions, renovació per WhatsApp, factures |
| 6 | Reactivació i Fidelització | OPTIONAL | 1 | 49€ | 12€ | Detecta pacients inactius i llança oferta personalitzada. Campanyes estacionals |

**Fase que estalvia més temps: Fase 1 + Fase 3**  
La gestió d'agenda elimina trucades i whatsapps manuals. El registre de sessió per nota de veu reemplaça entre 10-20 min d'escriptura per sessió.

**Combinació recomanada per primer contracte**: Fase 1 + Fase 2 + Fase 3 (total setup: 247€, 106€/mes)

---

### 2.3 Restaurant / Bar / Cafeteria (5 fases)

Sector: **RESTAURANTE**

| Fase | Nom | Tipus | Req | Setup | /mes | Descripció |
|------|-----|-------|-----|-------|------|-----------|
| 1 | Reserves de Taula | BASE | — | 99€ | 69€ | El client reserva per WhatsApp → bot confirma. Notificació per Telegram |
| 2 | Recordatoris de Reserva | OPTIONAL | 1 | 29€ | 20€ | 24-48h abans: recordatori amb opció de confirmar o cancel·lar. Allibera taula automàticament |
| 3 | Consulta de Menú i Carta | OPTIONAL | — | 39€ | 15€ | El client consulta menú, preus i al·lèrgens per WhatsApp a qualsevol hora |
| 4 | Comandes per Emportar | OPTIONAL | — | 89€ | 22€ | El client fa la comanda per WhatsApp → bot confirma i notifica cuina per Telegram |
| 5 | Fidelització i Comunicació | OPTIONAL | 1 | 49€ | 12€ | Menú del dia automàtic a clients subscrits. Campanyes d'ofertes i events |

**Fase que estalvia més temps: Fase 1 + Fase 2**  
Elimina el telèfon per a reserves i les no-shows. El recordatori automàtic redueix les cancel·lacions tardanes.

**Combinació recomanada per primer contracte**: Fase 1 + Fase 2 (total setup: 128€, 89€/mes)

---

### 2.4 Acadèmia / Centre de Formació (6 fases)

Sector: **ACADEMIA**

| Fase | Nom | Tipus | Req | Setup | /mes | Descripció |
|------|-----|-------|-----|-------|------|-----------|
| 1 | Informació i Captació | BASE | — | 79€ | 55€ | L'interessat consulta cursos, preus i horaris per WhatsApp → bot guia cap a la matrícula |
| 2 | Matrícula i Alta | REQUIRED | 1 | 89€ | 25€ | L'alumne es matricula per WhatsApp → bot recull dades i envia benvinguda per email |
| 3 | Gestió d'Assistència | OPTIONAL | 2 | 79€ | 20€ | El professor registra assistència per Telegram → bot notifica absències a famílies per WhatsApp |
| 4 | Seguiment del Progrés | OPTIONAL | 2 | 69€ | 18€ | El professor registra notes per Telegram → bot genera informe i l'envia a la família per email |
| 5 | Gestió de Pagaments i Recordatoris | OPTIONAL | 2 | 79€ | 18€ | Recordatori de pagament mensual per WhatsApp. Escala impagaments. Factures per email |
| 6 | Renovació i Fidelització | OPTIONAL | 2 | 49€ | 12€ | A prop del final de curs el bot contacta l'alumne i ofereix el nivell següent amb descompte |

**Fase que estalvia més temps: Fase 1 + Fase 2 + Fase 3**  
Captació sense trucades + matrícula automatitzada + control d'assistència per Telegram elimina la feina administrativa dels professors.

**Combinació recomanada per primer contracte**: Fase 1 + Fase 2 (total setup: 168€, 80€/mes)

---

### 2.5 Clínica Veterinària / Perruqueria Canina (6 fases)

Sectors: **VETERINARI · PERRUQUERIA_CANINA**

| Fase | Nom | Tipus | Req | Setup | /mes | Descripció |
|------|-----|-------|-----|-------|------|-----------|
| 1 | Agenda de Cites | BASE | — | 99€ | 59€ | El propietari agenda per WhatsApp indicant el tipus de servei → bot confirma i notifica per Telegram |
| 2 | Historial de la Mascota | OPTIONAL | 1 | 69€ | 24€ | El professional consulta l'historial per Telegram. El propietari pot sol·licitar-lo per email |
| 3 | Registre de Consulta | REQUIRED | 1,2 | 69€ | 20€ | El professional registra diagnòstic i tractament per Telegram → bot guarda i envia instruccions per email |
| 4 | Recordatoris de Vacunes i Revisions | OPTIONAL | 2 | 59€ | 18€ | El bot monitoritza el calendari sanitari i envia recordatori per WhatsApp quan s'apropa una vacuna |
| 5 | Seguiment de Tractaments | OPTIONAL | 3 | 69€ | 15€ | Quan es pauta un tractament, el bot envia recordatoris de medicació i recull reportes d'evolució |
| 6 | Fidelització i Postvenda | OPTIONAL | 1 | 39€ | 10€ | Felicita l'aniversari de la mascota. Recordatoris de revisions periòdiques. Campanyes estacionals |

**Fase que estalvia més temps: Fase 1 + Fase 2 + Fase 4**  
L'agenda automàtica + recordatoris de vacunes proactius eliminen la gestió telefònica i les visites perdudes.

**Combinació recomanada per primer contracte**: Fase 1 + Fase 2 (total setup: 168€, 83€/mes)

---

### 2.6 Perruqueria / Centre d'Estètica (5 fases)

Sectors: **PERRUQUERIA · ESTETICA**

| Fase | Nom | Tipus | Req | Setup | /mes | Descripció |
|------|-----|-------|-----|-------|------|-----------|
| 1 | Reserva de Cita | BASE | — | 79€ | 49€ | El client reserva per WhatsApp amb l'estilista favorit o el primer disponible → confirmació + Telegram |
| 2 | Recordatoris de Cita | OPTIONAL | 1 | 29€ | 18€ | 24-48h abans: recordatori amb opció de confirmar o cancel·lar. Allibera el buit automàticament |
| 3 | Historial del Client | OPTIONAL | 1 | 59€ | 16€ | L'estilista consulta per Telegram: serveis, color, preferències, productes, notes |
| 4 | Fidelització i Reactivació | OPTIONAL | 1 | 49€ | 14€ | Detecta clients inactius i llança oferta personalitzada. Campanyes estacionals. Programa de punts |
| 5 | Gestió de Productes | OPTIONAL | 3 | 49€ | 12€ | Després de cada sessió el bot recomana productes de manteniment. El client pot demanar-los per WhatsApp |

**Fase que estalvia més temps: Fase 1 + Fase 2**  
L'agenda automàtica i els recordatoris de cita eliminen els whatsapps manuals de confirmació i redueixen les no-shows fins a un 60%.

**Combinació recomanada per primer contracte**: Fase 1 + Fase 2 (total setup: 108€, 67€/mes)

---

### 2.7 Gestoria / Assessoria (6 fases)

Sector: **GESTORIA**

| Fase | Nom | Tipus | Req | Setup | /mes | Descripció |
|------|-----|-------|-----|-------|------|-----------|
| 1 | Captació i Consulta Inicial | BASE | — | 79€ | 55€ | El client consulta serveis, preus i documentació per WhatsApp → bot respon i agenda reunió |
| 2 | Alta i Recollida de Documentació | REQUIRED | 1 | 89€ | 25€ | El client fa l'alta per WhatsApp → bot recull dades i envia llista de documents per email |
| 3 | Gestió de Terminis i Recordatoris | OPTIONAL | 2 | 79€ | 20€ | Bot monitoritza terminis fiscals i envia recordatoris per WhatsApp. Notifica el professional per Telegram |
| 4 | Enviament de Documents i Informes | OPTIONAL | 2 | 69€ | 18€ | Bot envia documents processats, declaracions i informes per email. El professional afegeix notes per Telegram |
| 5 | Gestió de Pagaments | OPTIONAL | 2 | 69€ | 15€ | Recordatori de pagament mensual per WhatsApp. Factures per email. Escala impagaments |
| 6 | Renovació i Fidelització | OPTIONAL | 2 | 49€ | 12€ | A prop del final d'any el bot ofereix revisió anual amb descompte per continuïtat |

**Fase que estalvia més temps: Fase 3 (Terminis i Recordatoris)**  
Els terminis fiscals mai es poden oblidar. Automatitzar-ne els recordatoris elimina un 30% de les trucades d'urgència de clients.

**Combinació recomanada per primer contracte**: Fase 1 + Fase 2 + Fase 3 (total setup: 247€, 100€/mes)

---

### 2.8 Immobiliària (6 fases)

Sector: **INMOBILIARIA**

| Fase | Nom | Tipus | Req | Setup | /mes | Descripció |
|------|-----|-------|-----|-------|------|-----------|
| 1 | Captació de Propietats | BASE | — | 119€ | 79€ | El propietari registra la propietat per WhatsApp → bot recull dades i sol·licita fotos. Doc per email |
| 2 | Cerca i Filtratge | REQUIRED | 1 | 89€ | 28€ | El client descriu el que busca per WhatsApp → bot filtra el catàleg i retorna propietats que encaixen |
| 3 | Agenda de Visites | REQUIRED | 1,2 | 99€ | 25€ | El client sol·licita visita → bot coordina disponibilitat de l'agent i el propietari i confirma |
| 4 | Seguiment Post-visita | OPTIONAL | 3 | 69€ | 18€ | Després de la visita el bot recull feedback del client i qualifica el lead. Notifica l'agent per Telegram |
| 5 | Gestió d'Ofertes | OPTIONAL | 3 | 89€ | 18€ | El client fa l'oferta per WhatsApp → bot trasllada a l'agent i al propietari. Contraoferes al fil |
| 6 | Fidelització i Referències | OPTIONAL | 1 | 39€ | 12€ | Valoració automàtica en tancar operació. Programa de referits amb incentiu |

**Fase que estalvia més temps: Fase 2 + Fase 3**  
La qualificació automàtica de compradors (cerca/filtratge) elimina visites innecessàries. L'agenda coordinada elimina el ping-pong de WhatsApps entre agent, propietari i comprador.

**Combinació recomanada per primer contracte**: Fase 1 + Fase 2 + Fase 3 (total setup: 307€, 132€/mes)

---

### 2.9 Agència IA (pròpies) (6 fases)

Sector: **AGENCIA_IA** — Fases de l'agència AMG Digitalització per a la seva pròpia operativa

| Fase | Nom | Tipus | Req | Setup | /mes | Descripció |
|------|-----|-------|-----|-------|------|-----------|
| 1 | Captació i Presentació | BASE | — | 150€ | 89€ | Lead consulta serveis, tarifes i casos d'èxit per WhatsApp → bot respon i qualifica l'interès |
| 2 | Qualificació i Reunió | REQUIRED | 1 | 200€ | 49€ | Bot recull informació sobre el projecte (sector, objectiu, pressupost) i agenda la primera reunió |
| 3 | Pressupost Automàtic | OPTIONAL | 2 | 150€ | 39€ | L'equip defineix l'abast per Telegram → bot genera un pressupost preliminar i l'envia per email |
| 4 | Onboarding de Projecte | OPTIONAL | 2 | 100€ | 29€ | En confirmar el projecte el bot coordina el kick-off: qüestionari d'alta, credencials i assignació |
| 5 | Reporting i Seguiment | OPTIONAL | 4 | 100€ | 29€ | Actualitzacions d'estat per WhatsApp. KPIs mensuals per email. Alertes d'incidències |
| 6 | Expansió de Compte | OPTIONAL | 2 | 150€ | 49€ | Detecta senyals d'upsell i genera propostes noves automàticament |

---

## 3. Resum per sector — primera recomanació comercial

| Sector | Fases recomanades (primer contracte) | Dolor principal estalviat |
|--------|--------------------------------------|--------------------------|
| PINTOR / ELECTRICISTA / FONTANER / JARDINER / NETEJA | F1 + F2 + F3 | Generació i seguiment de pressupostos |
| TALLER_MECANIC | F1 + F2 + F3 | Pressupostos + agenda de deixada de vehicle |
| FISIOTERAPEUTA / PSICOLEG / NUTRICIONISTA | F1 + F2 + F3 | Agenda + historial + notes de sessió per veu |
| RESTAURANTE | F1 + F2 | Reserves i eliminació de no-shows |
| ACADEMIA | F1 + F2 | Captació i matrícula sense trucades |
| VETERINARI / PERRUQUERIA_CANINA | F1 + F2 | Agenda + historial de l'animal |
| PERRUQUERIA / ESTETICA | F1 + F2 | Agenda i recordatoris, eliminació de no-shows |
| GESTORIA | F1 + F2 + F3 | Captació, alta i recordatoris de terminis fiscals |
| INMOBILIARIA | F1 + F2 + F3 | Captació, cerca i agenda coordinada de visites |

---

## 4. Ús a l'agent de vendes (AGENCIA_IA)

Quan un prospecte pregunta "que oferiu?" o "en què ens podeu ajudar?", el bot de vendes de l'agència ha de:

1. **Preguntar el sector** si no l'ha detectat de la conversa: "Primer de tot, quin tipus de negoci tens?"
2. **Presentar les 3 primeres fases** del sector amb descripció breu i el benefici principal
3. **Destacar la fase que estalvia més temps** del sector (vegeu columna "Dolor principal")
4. **Proposar la demo de 20 minuts** com a CTA. No tancar preus per missatgeria.

L'agent AGENCIA_IA té accés al catàleg complet de fases injectat en el seu context pel `PromptBuilder`.

---

## 5. Connexió amb el model de contractació

La taula `sector_phases` és la referència de preus reals. La connexió amb els tenants es fa via:
- `ServiceProfile.sectorPhaseNumber` → número de fase contractada (1-7)
- `Tenant.contractedPhases` → fases genèriques actives ("F1,F3") per als schedulers i integration matrix

**Pendent**: Migrar la contractació per fases des del `ServiceProfile` genèric a una taula dedicada `tenant_contracted_sector_phases (tenant_id, phase_number)` per fer-ho explícit. Fins aleshores, l'admin gestiona manualment quines fases estan actives per cada tenant.
