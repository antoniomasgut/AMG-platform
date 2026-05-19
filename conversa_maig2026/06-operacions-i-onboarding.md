# 06 — Operacions i Onboarding de Clients

## Rols de l'equip

| Persona | Rol | Responsabilitats |
|---|---|---|
| **Antonio** | Tècnic | Desenvolupament, infraestructura, monitorització, incidències |
| **Guillem** | Comercial | Captació, demos, pressupostos, tancament, onboarding comercial |
| **Rebecca** | Administrativa | Contractes, facturació, suport bàsic, coordinació |

---

## Flux d'alta d'un client nou

```
1. CAPTACIÓ (Guillem)
   Visita o trucada → Demo amb un cas real del seu sector
   Presentació de fases i tarifes
   Tancament i signatura de contracte
        ↓
2. CONTRACTE (Rebecca)
   Envia contracte per signar (Docusign)
   Cobra setup per transferència
   Obre fitxa del client al CRM
        ↓
3. FORMULARI D'ONBOARDING (Rebecca/Guillem)
   Envia formulari al client (Google Forms o web)
   Client omple: nom negoci, serveis, preus, horari, to
   Temps estimat per al client: 10-15 minuts
        ↓
4. CONFIGURACIÓ (Antonio)
   Crea el tenant a la BD amb les dades del formulari
   Configura els canals (WhatsApp, Telegram, Email)
   Prova l'agent amb casos reals del sector
   Temps estimat: 30-60 minuts
        ↓
5. REVISIÓ (Guillem o Rebecca)
   Envia 5 missatges de prova al nou agent
   Comprova que les respostes són correctes
   Temps estimat: 15 minuts
        ↓
6. ENTREGA AL CLIENT (Guillem)
   Trucada de 15 minuts per explicar com funciona
   Entrega número WhatsApp i/o bot Telegram
   Explica el panel de control (mode híbrid)
   Temps estimat: 15-20 minuts
        ↓
7. SEGUIMENT (Rebecca)
   Missatge a la setmana 1: "Com va tot?"
   Missatge al mes 1: oferta de fase addicional
   Factura mensual automàtica
```

**Temps total d'onboarding per client: ~2h repartides entre l'equip**

---

## Formulari d'onboarding (Google Forms)

```
SECCIÓ 1 — El teu negoci
  □ Nom del negoci *
  □ Sector (desplegable) *
  □ Mida (autònom / equip petit / gabinet) *
  □ Adreça (opcional, per a zones de cobertura)

SECCIÓ 2 — Contacte i canals
  □ Telèfon del dueño (per notificacions urgents) *
  □ Email del dueño *
  □ Canal preferit: WhatsApp / Telegram / Tots dos *

SECCIÓ 3 — Configuració de l'agent
  □ Horari d'atenció *
  □ Zona de cobertura (si escau)
  □ Serveis que ofereixes (un per línia) *
  □ Preus dels serveis principals *
  □ To desitjat de l'agent:
      ○ Molt formal
      ○ Professional i proper (recomanat)
      ○ Informal i desenfadat
  □ Hi ha alguna cosa que l'agent NO hauria de dir o fer?

SECCIÓ 4 — Equip (si F5)
  □ Nom i telèfon de cada empleat

SECCIÓ 5 — Mode de l'agent
  □ Mode preferit:
      ○ Automàtic (agent envia tot sol)
      ○ Híbrid (agent suggereix, tu aprobes)
      ○ Manual (tu respons, agent en silenci)
```

---

## Checklist de configuració (Antonio)

```
□ Crear tenant a PostgreSQL amb ID únic
□ Configurar system prompt amb dades del formulari
□ Assignar número WhatsApp (Twilio)
□ Crear bot Telegram (si escau)
□ Configurar email del negoci (Resend)
□ Registrar webhooks als canals
□ Provar les eines: cita, pressupost, disponibilitat
□ Verificar que el mode híbrid funciona
□ Confirmar que les notificacions arriben al dueño
□ Documentar configuració especial a la fitxa del client
```

---

## Manteniment mensual per client

**Temps estimat: 45-60 minuts al mes per client**

| Tasca | Responsable | Temps |
|---|---|---|
| Revisió de logs i errors | Antonio | 15 min |
| Resposta a incidències | Antonio | Variable |
| Canvis de configuració sol·licitats | Antonio | 15-30 min |
| Facturació i seguiment de pagament | Rebecca | 5 min |
| Missatge de seguiment al client | Rebecca | 5 min |

### Canvis inclosos en el mensual (≤2h/mes)

```
✅ Actualitzar preus o serveis
✅ Canviar horaris
✅ Afegir instruccions especials
✅ Ajustar to de l'agent
✅ Canviar mode (auto/híbrid/manual)
✅ Afegir o treure empleats (F5)
```

### Canvis que suposen cost addicional

```
⚠️ Implementar una fase nova → preu de setup de la fase
⚠️ Integracions personalitzades → pressupost a part
⚠️ Formació presencial → €50/hora
```

---

## Gestió d'incidències

### Nivells d'alerta

```
🟢 NORMAL   → Agent respon, tot funciona
🟡 AVÍS     → Resposta lenta (>5s), errors puntuals
🔴 CRÍTICA  → Agent no respon, servei caigut
```

### Protocol d'incidència

```
1. Sistema detecta error (monitor cada 5 minuts)
2. Alerta automàtica per WhatsApp a Antonio
3. Antonio revisa i corregeix
4. Si no es resol en 30 min → alerta a Guillem
5. Guillem contacta el client si és necessari
6. Rebecca documenta l'incident al CRM

SLA: resolució en menys de 2 hores en horari laboral
```

### Causes habituals d'incidències

```
70% → Problemes amb Meta/WhatsApp (fora del nostre control)
15% → Client que vol canviar alguna cosa
10% → Actualitzacions de l'aplicació
 5% → Servidor o BD
```

---

## Escalar fases existents

Quan un client vol ampliar a una fase nova:

```
1. Guillem presenta la nova fase (trucada o visita)
2. Rebecca envia addenda al contracte
3. Cobra el setup de la nova fase
4. Antonio implementa en 24-48h
5. Guillem confirma amb el client
```

---

## CRM simplificat (Notion o Airtable)

Camps per a cada client:

```
Nom del negoci
Sector / Mida
Nom i telèfon del dueño
Data d'alta
Fases contractades
Preu mensual
Mode de l'agent
Estat (actiu / suspès / baixa)
Pròxima renovació
Notes especials
Historial d'incidències
Historial de canvis
```

---

## Comunicació amb els clients

### Missatges automàtics de seguiment (Fase 4)

Configurats a l'agent del propi NexeLocal (autopromocional):

```
Setmana 1 post-alta:
  "Hola [nom], ja fa una setmana que el teu assistent
   virtual de NexeLocal està en marxa. Com va tot?
   Tens algun dubte o vols ajustar alguna cosa?"

Mes 1:
  "Hola [nom], el teu agent ha gestionat X missatges
   aquest mes. Recorda que pots ampliar amb la fase
   de [fase_recomanada] per a [benefici concret].
   Vols que t'ho expliquem?"

Mes 3 (si no han ampliat):
  "Hola [nom], molts dels nostres clients del sector
   [sector] han ampliat amb [fase] i han vist [benefici].
   T'interessa una trucada de 15 minuts per veure-ho?"
```

### Recordatoris de renovació (Rebecca)

```
30 dies abans de venciment del compromís mínim:
  "Hola [nom], el teu contracte de NexeLocal es renova
   automàticament el [data]. Si vols fer algun canvi
   o ampliar el servei, digues-nos!"

Si volen donar de baixa:
  Protocol de retenció: oferta personalitzada,
  canvi de pla, pausa de servei vs baixa total
```

---

## Eines de l'equip

| Eina | Ús | Cost |
|---|---|---|
| Google Workspace | Email corporatiu, Drive, Meet | ~€18/mes (3 usuaris) |
| Notion o Airtable | CRM, documentació interna | Gratis |
| Holded | Facturació i comptabilitat | ~€30/mes |
| Docusign | Signatura digital de contractes | ~€15/mes |
| GitHub | Codi font i CI/CD | Gratis |
| Hetzner Cloud | Infraestructura | ~€35/mes |

**Total eines:** ~€98/mes fixos

---

## Primeres 12 setmanes — pla d'acció

```
Setmanes 1-4: Construcció
  Antonio: App bàsica funcionant (F1+F2)
  Guillem: Identifica 10 possibles clients pilots
  Rebecca: Prepara contractes i formularis

Setmanes 5-8: Pilots
  Antonio: Alta dels primers 5-10 clients pilots
  Guillem: Acompanya els clients en les primeres setmanes
  Rebecca: Facturació i seguiment

Setmanes 9-12: Escala
  Antonio: Implementa F3 i F4, millores basades en feedback
  Guillem: Accelera captació amb casos d'èxit reals
  Rebecca: Sistematitza processos d'alta i facturació
```
