# Spec 26 — Base de Coneixement RAG (Knowledge Base per a l'Agent IA)

**Versió:** 1.0
**Data:** 2026-05-23
**Estat:** Esborrany
**Depèn de:** Spec 20 (Agents), Spec 25 (Omnichannel Inbox), Spec 17 (Service Setup Wizard)

---

## 1. Visió general

Quan un client activa el Bot IA Avançat (`bot-ia-rag`), el bot necessita **dues fonts d'informació**:

1. **Com s'ha de comportar la IA** — la seva personalitat, to, idioma, regles i restriccions.
2. **Què sap del negoci** — la informació real de l'empresa que la IA usarà per respondre: serveis, preus, horaris, FAQ, política de cancel·lació, etc.

Sense aquesta configuració, el bot respon de forma genèrica i inventa informació. Amb la Knowledge Base configurada, el bot respon **com un empleat de l'empresa** que coneix perfectament el negoci.

---

## 2. Dos components

### 2.1 Perfil de Comportament (`AgentBehaviorProfile`)

Controla *com* respon el bot: el seu caràcter, to, idioma i límits.

### 2.2 Base de Coneixement (`KnowledgeBase`)

Controla *el que sap* el bot: la informació específica del negoci aportada per l'administrador.

Els dos components es guarden per tenant al Vault (encriptats) i els llegeix el `PromptBuilder` de l'Spec 20 en cada conversa.

---

## 3. Informació que necessitem del client

### 3.1 Dades bàsiques del negoci

Aquesta és la informació fonamental que el bot usarà per presentar-se i respondre consultes generals.

| Camp | Descripció | Exemple |
|------|-----------|---------|
| `business_name` | Nom del negoci tal com ha d'aparèixer | "Clínica Dental Pons" |
| `business_type` | Tipus de negoci (ajuda a contextualitzar) | "Clínica dental a Palma" |
| `address` | Adreça completa, incloent barri si és rellevant | "Av. Joan Miró 45, Palma (davant Mercadona)" |
| `phone` | Telèfon principal per derivar consultes urgents | "+34 971 234 567" |
| `email` | Email de contacte del negoci | "info@clinicadental.com" |
| `website` | URL web si en té | "www.clinicadentalpons.com" |
| `location_hints` | Indicacions per arribar-hi (opcional) | "Planta baixa, timbre amb nom Pons" |

### 3.2 Horaris d'atenció

El bot ha de saber quan es pot atendre al client. La IA gestionarà situacions com "ara estem tancats, però pots demanar cita per demà".

| Camp | Descripció | Exemple |
|------|-----------|---------|
| `schedule` | Horari de dilluns a divendres | "9h - 14h · 16h - 20h" |
| `schedule_weekend` | Horari cap de setmana (buit si tancat) | "Dissabte 9h - 13h. Diumenge tancat." |
| `holidays` | Festius locals o tancaments especials (opcional) | "Tancat el 17 de gener (Sant Antoni)" |
| `response_time_note` | Nota sobre temps de resposta (opcional) | "Les sol·licituds de cita rebudes fora d'horari es gestionen el matí següent" |

### 3.3 Serveis i preus

**Aquesta és la secció més important.** El bot ha de poder respondre "quant costa una neteja de boca?" o "feis ortodòncia?".

Es capturen com una llista d'ítems estructurats. Cada ítem té:

| Camp | Descripció | Exemple |
|------|-----------|---------|
| `service_name` | Nom del servei | "Neteja dental + revisió" |
| `service_description` | Descripció breu (1-2 frases) | "Eliminació de placa i sarro, poliment dental i revisió de l'estat de la boca. Inclou radiografia de rutina." |
| `price` | Preu o rang de preus | "Desde 45 €" |
| `duration` | Durada aproximada (si aplica) | "30-45 minuts" |
| `booking_required` | Cal cita prèvia? | Sí / No / Recomanable |
| `notes` | Condicions especials (opcional) | "Preu especial per a menors de 12 anys: 35 €" |

Exemples de serveis per sector:

**Clínica dental:** Neteja, Empasts, Ortodòncia (brackets/alineadors), Implants, Blanqueig, Extraccions, Endodòncia (matar nervi), Radiografies panoràmiques.

**Bar / Restaurant:** Menú del dia, Carta de plats, Carta de vins, Servei de terrassa, Menús especials per a grups, Serveis per a esdeveniments.

**Taller mecànic:** Canvi d'oli, ITV, Reparació de frens, Canvi de rodes, Diagnòstic d'avaries, Servei de grua.

**Perruqueria / Estètica:** Tall, Color, Manicura, Pedicura, Tractaments facials, Depilació.

**Fisioterapeuta:** Massatge terapèutic, Teràpia manual, Rehabilitació, Sessions domiciliàries.

### 3.4 Sistema de cites

Si el negoci gestiona cites, el bot ha de saber com funciona el procés.

| Camp | Descripció | Exemple |
|------|-----------|---------|
| `booking_method` | Com es demana cita | "Per WhatsApp o trucada. No acceptem cites per email." |
| `booking_lead_time` | Amb quanta antelació cal demanar | "Mínim 24h d'antelació" |
| `cancellation_policy` | Política de cancel·lació | "Cancel·lació gratuïta fins 4 hores abans. Passades les 4h, cobrem el 50% de la sessió." |
| `waiting_list` | Existeix llista d'espera? | "Tenim llista d'espera per a implants. Temps d'espera habitual: 2-3 setmanes." |
| `booking_confirmation` | Com es confirma la cita | "Confirmem la cita per WhatsApp en 1 hora (horari d'obertura)." |

### 3.5 FAQ — Preguntes freqüents

Les preguntes que els clients fan amb més freqüència. Amb 5-10 respostes ben redactades, el bot resoldrà el 80% de les consultes.

Cada entrada té:

| Camp | Descripció | Exemple |
|------|-----------|---------|
| `question` | La pregunta tal com la fa el client | "Passeu per mútua?" |
| `answer` | La resposta completa i útil | "Sí, treballem amb ASISA, SANITAS i AXA. Per a pacients de mútua, portau la targeta el dia de la visita. Si la vostra mútua no hi és a la llista, us podem atendre per via privada." |

Preguntes habituals per sector:

**Clínica dental:** "Passeu per mútua?", "Feis finançament?", "Quant dura el tractament d'ortodòncia?", "Teniu aparcament?", "Podeu atendre en urgent?"

**Restaurant:** "Teniu menú del dia?", "Feis reserva?", "Teniu opcions vegetarianes?", "Podeu fer àpat per a grup?", "Teniu estacionament?"

**Taller:** "Feis pressupost gratuït?", "Quant trigueu en un canvi d'oli?", "Heu d'agafar cita per a ITV?", "Doneu cotxe de substitució?"

### 3.6 Restriccions i escalada

Defineix quan el bot ha de deixar de respondre i avisar a un humà.

| Camp | Descripció | Exemple |
|------|-----------|---------|
| `topics_off_limits` | Temes que el bot NO ha de respondre | "No donar informació sobre casos de pacients concrets. No comentar preus competència." |
| `escalation_triggers` | Situacions que requereixen atenció humana | "Queixes greus, urgències mèdiques, consultes de pressupost per a tractaments de +500€." |
| `escalation_contact` | A qui derivar (telèfon, email, persona) | "Truqueu directament al 971 234 567 o demaneu parlar amb la Dra. Pons." |
| `disclaimer` | Nota legal o aclaratoria (opcional) | "La informació que facilita aquest assistent és orientativa. Per a diagnòstic i tractament, consulteu amb el vostre dentista." |

### 3.7 Personalitat del bot

Defineix el caràcter i to de la IA.

| Camp | Opcions | Descripció |
|------|---------|-----------|
| `bot_name` | Text lliure | Nom amb el que es presenta la IA. Ex: "Marta", "L'Assistent de Clínica Pons", "Bot de la Perruqueria Gemma" |
| `tone` | `professional`, `proper`, `formal`, `desinvolt` | El registre del llenguatge |
| `language` | `catala`, `castella`, `bilingual_adapt` | Català sempre, Castellà sempre, o adaptar-se a l'idioma del client |
| `response_length` | `breu`, `detallat`, `adaptatiu` | Respostes curtes i directes vs. explicatives |
| `emoji_use` | `cap`, `moderat`, `expressiu` | Si usa emojis o no |
| `personality_notes` | Text lliure (opcional) | "Som un negoci familiar, respon amb calidesa. Utilitza 'vosaltres' no 'ustedes'." |

### 3.8 Informació addicional (bloc de text lliure)

Informació que no encaixa en cap categoria anterior: història del negoci, valors, certificats, premis, etc.

Exemples:
- "Tenim 25 anys d'experiència a Mallorca. Especialistes en ortopantomografies digitals."
- "Tots els nostres vehicles tenen certificat Eco. Lliurament a domicili dins Palma (+10€)."
- "El 90% dels nostres clients arriben per recomanació. Premis: Millor Pastisseria Inca 2024."

---

## 4. Documents i arxius

A banda dels camps estructurats, el client pot pujar **documents** que la IA usarà com a referència:

| Tipus | Format | Ús |
|-------|--------|-----|
| Carta de serveis / menú | PDF | Informació detallada de productes i preus |
| Llista de preus | PDF / imatge | Tarifes oficials |
| Política de privacitat resumida | Text | Quan els clients demanen sobre dades personals |
| Fitxa tècnica de productes | PDF | Per a negocis amb productes específics |
| Mapa / Instruccions d'accés | Imatge | Per enviar quan donen instruccions d'arribar |

**Limitació de la versió inicial:** màxim 5 documents, 10 MB per document. Formats suportats: PDF, TXT.

---

## 5. Memòria del client (Customer Memory)

### 5.1 Principi fonamental

> **L'agent ha de poder respondre com si hagués parlat sempre amb aquell client.**

Quan un client torna a escriure, el bot ha de saber:
- Quines consultes ha fet anteriorment ("la setmana passada em vas dir que el preu de l'empast era 80 €")
- Cites o comandes passades ("vaig demanar hora per al dijous, hi ha algun canvi?")
- Preferències o acords prèvis ("recordo que estàs en llista d'espera per als implants")
- Queixes o incidències anteriors (tractament especial)

Sense aquesta memòria, el bot sembla que "no recorda res" i és molt frustrant per al client.

### 5.2 Estratègia de memòria en dues capes

```
Conversa entrant (nou missatge del client)
        ↓
ConversationService.buildContext(tenantId, customerIdentifier, channel)
        │
        ├── CAPA 1: Conversa recent (< 30 missatges)
        │   └── SELECT * FROM conversations WHERE tenant_id = ? AND customer_identifier = ?
        │       ORDER BY created_at DESC LIMIT 30
        │       → inclou directament al context com a historial complet
        │
        └── CAPA 2: Resum de converses anteriors (> 30 missatges)
            └── Contact.conversationSummary (text generat automàticament)
                → inclou com a bloc "HISTORIAL DEL CLIENT"
```

### 5.3 Generació automàtica del resum

Quan el nombre total de missatges d'un client supera **30**, el sistema:

1. Pren els missatges antics (> 30) que encara no estan resumits
2. Envia a Claude Haiku un prompt de compressió:
   ```
   Fes un resum molt compacte d'aquesta conversa passada entre un assistent
   virtual i un client. Destaca: compromisos adquirits, serveis esmentats,
   preferències del client, incidències, cites acordades. Màxim 200 paraules.
   
   [missatges anteriors]
   ```
3. Desa el resum a `Contact.conversationSummary` (s'acumula, no se substitueix)
4. Marca els missatges antics com a `summarized = true`

El resum s'actualitza automàticament cada 30 missatges nous. El procés és asíncron (background job), no bloqueja la conversa en curs.

### 5.4 Context complet que rep la IA

```
SYSTEM PROMPT:
  [comportament del bot]
  [informació del negoci]
  [serveis i preus]
  [FAQ]
  [restriccions]

HISTORIAL DEL CLIENT (si existeix):
  Resum de converses anteriors amb aquest client:
  "{Contact.conversationSummary}"

CONVERSA ACTUAL:
  [últims 30 missatges en ordre cronològic]

USER:
  [nou missatge del client]
```

### 5.5 Privacitat i RGPD

- El `conversationSummary` és part de les dades personals del client (Reglament RGPD art. 17)
- S'esborrarà quan el tenant elimini el contacte
- El tenant pot esborrar l'historial d'un client des de la pestanya "Coneixement" → "Contactes"
- Tots els missatges es guarden a PostgreSQL (no a Redis, que és només caché temporal)
- Temps de retenció configurable per tenant: 6 mesos / 1 any / indefinit (default: 1 any)

### 5.6 Canvi a Spec 20 — `ConversationService.loadHistory`

El mètode actual carrega "últims 20 missatges de Redis (TTL 48h)".

**Comportament corregit:**

```
loadHistory(tenantId, customerIdentifier, channel):
  1. Busca Contact per (tenantId, customerIdentifier, channel)
  2. Si no existeix → retorna historial buit + crea Contact
  3. Si existeix:
     a. Carrega els últims 30 missatges de PostgreSQL (conversations, no Redis)
     b. Carrega Contact.conversationSummary (pot ser null)
     c. Retorna { recentMessages: [...], summary: "..." }

Redis ara s'usa NOMÉS per caché de lectura ràpida (TTL 10 min).
PostgreSQL és la font de veritat per a l'historial.
```

---

## 6. Estratègia RAG tècnica

### 6.1 Enfocament: Context Injection (v1)

Per a la majoria de negocis locals, el volum de coneixement és petit (< 50 KB de text). En comptes d'un vector store dedicat (complex, costós), usem **injecció directa al context**:

```
system_prompt = comportament_base
              + dades_negoci_estructurades  (JSON compacte del Vault)
              + text_documents_rellevants   (chunks seleccionats per keyword)
              + resum_historial_client      (Contact.conversationSummary)
              + conversa_recent             (últims 30 missatges de PostgreSQL)
```

Avantages: zero infraestructura extra, tot a PostgreSQL, fàcil de mantenir.
Limitació: knowledge bases molt grans (> 100 KB) degraden la qualitat de resposta.

### 6.2 Migració a pgvector (v2, futur)

Quan un client supera el llindar de 50 KB de coneixement:
- Activar `pgvector` a la instància PostgreSQL existent
- Vectoritzar els chunks amb l'API d'embeddings de text-embedding-3-small (OpenAI) o equivalent
- Substituir la selecció per keyword per similarity search

La interfície del `PromptBuilder` no canvia — la migració és transparent.

---

## 7. Entitats de domini

### Canvi a `Contact` (Spec 25 — extensió)

S'afegeix el camp `conversationSummary` a l'entitat `Contact` existent:

| Camp | Tipus | Notes |
|------|-------|-------|
| `conversationSummary` | TEXT | Resum generat automàticament de converses passades. Null si < 30 missatges totals. |
| `totalMessageCount` | Integer | Comptador de missatges totals (per disparar la generació del resum) |
| `summaryUpdatedAt` | Instant | Data de l'últim resum generat |

### KnowledgeBase

| Camp | Tipus | Notes |
|------|-------|-------|
| `id` | UUID | PK |
| `tenantId` | UUID | 1:1 amb tenant |
| `version` | Integer | per gestionar actualitzacions |
| `isActive` | Boolean | default true |
| `createdAt` / `updatedAt` | Instant | |

### KnowledgeEntry

Cada bloc d'informació estructurada.

| Camp | Tipus | Notes |
|------|-------|-------|
| `id` | UUID | PK |
| `knowledgeBaseId` | UUID | FK |
| `category` | Enum | `BUSINESS_INFO`, `SCHEDULE`, `SERVICE`, `FAQ`, `RESTRICTION`, `BEHAVIOR`, `EXTRA` |
| `key` | String(100) | identificador intern (ex: `service.neteja_dental`) |
| `content` | TEXT | contingut en text pla (JSON si estructurat) |
| `sortOrder` | Integer | ordre de prioritat al context |
| `isActive` | Boolean | permet desactivar entrades sense esborrar |
| `updatedAt` | Instant | per saber si cal regenerar el context |

### KnowledgeDocument

Arxius pujats pel client.

| Camp | Tipus | Notes |
|------|-------|-------|
| `id` | UUID | PK |
| `knowledgeBaseId` | UUID | FK |
| `filename` | String(255) | nom original del fitxer |
| `storagePath` | String(500) | path a MinIO |
| `fileSize` | Long | bytes |
| `contentType` | String(50) | `application/pdf`, `text/plain` |
| `extractedText` | TEXT | text extret del PDF (processat en background) |
| `isProcessed` | Boolean | si s'ha extret el text |
| `uploadedAt` | Instant | |

---

## 7. API REST

Prefix: `/api/v1/knowledge`

| Mètode | Ruta | Accés | Descripció |
|--------|------|-------|-----------|
| GET | `/{tenantId}` | Autenticat | Obtenir la KB completa del tenant |
| PUT | `/{tenantId}/entries` | Admin | Actualitzar totes les entrades d'una categoria |
| PUT | `/{tenantId}/behavior` | Admin | Actualitzar el perfil de comportament |
| POST | `/{tenantId}/documents` | Admin | Pujar un document (multipart) |
| DELETE | `/{tenantId}/documents/{docId}` | Admin | Eliminar un document |
| GET | `/{tenantId}/preview` | Admin | Previsualitzar el system prompt que rebria la IA |
| POST | `/{tenantId}/test` | Admin | Enviar un missatge de prova i veure la resposta |

---

## 8. Integració amb PromptBuilder (Spec 20)

El `PromptBuilder` existent s'amplia per llegir de `KnowledgeBase`:

```
PromptBuilder.build(tenantId):
  1. Carrega KnowledgeBase del tenant (o usa fallback genèric si no existeix)
  2. Construeix el system prompt:
     ├─ Bloc COMPORTAMENT  ← KnowledgeEntry[BEHAVIOR]
     ├─ Bloc NEGOCI        ← KnowledgeEntry[BUSINESS_INFO] + KnowledgeEntry[SCHEDULE]
     ├─ Bloc SERVEIS       ← KnowledgeEntry[SERVICE] (ordenats per sortOrder)
     ├─ Bloc FAQ           ← KnowledgeEntry[FAQ] (top 10 per espai)
     ├─ Bloc RESTRICCIONS  ← KnowledgeEntry[RESTRICTION]
     └─ Bloc DOCUMENTS     ← KnowledgeDocument.extractedText (si caben al context)
  3. Retorna system prompt
```

**Màxim de tokens del system prompt:** 8.000 tokens (≈ 32 KB). Si se supera, s'ometen documents i FAQs menys prioritaris.

---

## 9. Frontend — Wizard de configuració (Spec 17)

El wizard de `bot-ia-rag` s'amplia amb **4 passos**:

### Pas 1 — Personalitat del bot

- Nom del bot (input text)
- To (selector: Professional / Proper / Formal / Desinvolt)
- Idioma (selector: Català / Castellà / Adaptatiu)
- Ús d'emojis (selector)
- Notes de personalitat (textarea, opcional)

### Pas 2 — Informació del negoci

Formulari estructurat en seccions:

**Dades bàsiques:** nom, tipus, adreça, telèfon, email, web, indicacions

**Horaris:** dilluns-divendres, cap de setmana, festius, nota de temps de resposta

**Cites:** mètode, antelació, política cancel·lació, confirmació

### Pas 3 — Serveis i FAQ

- **Serveis:** llista dinàmica, cada ítem amb nom, descripció, preu, durada (afegir / editar / eliminar)
- **FAQ:** llista de parelles pregunta-resposta (afegir / editar / eliminar)

### Pas 4 — Restriccions i documents

- Temes prohibits (textarea)
- Disparadors d'escalada (textarea)
- Contacte d'escalada (input)
- Disclaimer (textarea, opcional)
- Upload de documents (PDF/TXT, màx. 5 fitxers × 10 MB)

### Previsualització en temps real

A la banda dreta del wizard (desktop) o sota cada pas (mòbil): **previsualització del system prompt** que rebrà la IA. El client veu exactament el que el bot "sabrà".

### Test de resposta

Al final del wizard: camp de text on escriure un missatge de prova. El sistema envia el missatge a la IA amb el system prompt configurat i mostra la resposta. Permet validar la configuració abans d'activar.

---

## 10. Frontend — Gestió de la KB (post-activació)

Pàgina `/portal/agents` → nova pestanya **"Coneixement"**:

- Resum del que conté la KB (n serveis, n FAQ, n documents, darrera actualització)
- Edició ràpida de qualsevol secció (expandible inline)
- Indicador de si el bot és actiu i un botó "Actualitzar bot" (regenera el context en cache)
- Historial de canvis (data + qui ho va modificar)

### Secció "Contactes i memòria"

Subtaula per gestionar la memòria del bot per client:

| Columna | Descripció |
|---------|-----------|
| Client | Identifier del client (nom editable si s'ha assignat) |
| Canal | WhatsApp / Telegram / Email |
| Total missatges | Comptador de tota la conversa |
| Darrer contacte | Data de l'últim missatge |
| Resum | Previsualització del resum generat (expandible) |
| Accions | Veure historial complet · Esborrar memòria · Esborrar tot l'historial |

**Esborrar memòria** esborra el `conversationSummary` però manté els missatges.
**Esborrar tot l'historial** esborra tots els missatges i el resum (RGPD dret d'oblit).

---

## 11. Casos QA

### Base de coneixement

| # | Cas | Resultat esperat |
|---|-----|-----------------|
| KB-01 | Bot sense KB configurada | Usa prompt genèric; avisa "Bot configurat en mode bàsic" |
| KB-02 | Client pregunta preu d'un servei | Bot respon amb preu de la KB |
| KB-03 | Client pregunta fora d'horari | Bot indica horaris i quan s'atendrà |
| KB-04 | Client pregunta tema prohibit | Bot indica que no pot ajudar i deriva al contacte d'escalada |
| KB-05 | Client pregunta en castellà (mode adaptatiu) | Bot respon en castellà |
| KB-06 | Client pregunta sobre document pujat (PDF) | Bot incorpora info del PDF al context |
| KB-07 | Admin actualitza preu d'un servei | La propera conversa ja té el preu actualitzat |
| KB-08 | KB supera 8.000 tokens | Documents omesos; FAQs menys prioritàries omeses; avís a l'admin |
| KB-09 | Upload PDF corrupte | Error clar a la UI; no es desa |
| KB-10 | Test de resposta pre-activació | Mostra resposta real de la IA amb el system prompt configurat |

### Memòria del client

| # | Cas | Resultat esperat |
|---|-----|-----------------|
| MEM-01 | Client nou, 1r missatge | No hi ha historial; bot respon sense context previ |
| MEM-02 | Client amb 15 missatges previs torna al cap de 2 dies | Bot carrega els 15 missatges de PostgreSQL i té context complet |
| MEM-03 | Client amb 50 missatges previs | Bot carrega: resum comprimit dels primers 20 + últims 30 missatges |
| MEM-04 | Client diu "la setmana passada em vas dir que el preu era X" | Bot consulta el resum/historial i confirma o corregeix la informació |
| MEM-05 | Client té converses per WhatsApp i per Telegram | Dos contactes separats (per canal); si es fa merge manual, comparten resum |
| MEM-06 | Admin esborra la memòria d'un client | Proper missatge del client: bot comença sense context previ |
| MEM-07 | Conversa supera 30 missatges nous des de l'últim resum | Background job genera resum; `summaryUpdatedAt` s'actualitza |
| MEM-08 | Error a l'API en generar resum | Resum no s'actualitza; bot continua funcionant amb l'últim resum vàlid |
| MEM-09 | Client demana "esborra el meu historial" | Bot informa que cal contactar directament; admin pot fer-ho des del portal |
| MEM-10 | Resum + missatges recents + KB superen 8.000 tokens | Es trunca el resum (no els missatges recents); avís a l'admin |

---

## 12. Estat i planificació

| Ítem | Estat |
|------|-------|
| Spec | ✅ Esborrany v1.1 |
| Entitats de domini (`KnowledgeBase`, `KnowledgeEntry`, `KnowledgeDocument`) | ⏳ Pendent |
| Extensió `Contact` amb `conversationSummary` (Spec 25) | ⏳ Pendent |
| Migració BD | ⏳ Pendent |
| API REST `/api/v1/knowledge` | ⏳ Pendent |
| Fix `ConversationService.loadHistory` (PostgreSQL + resum) | ⏳ Pendent |
| Background job de generació de resums | ⏳ Pendent |
| Integració PromptBuilder (Spec 20) | ⏳ Pendent |
| Wizard ampliació 4 passos | ⏳ Pendent |
| Extracció text de PDFs (background job) | ⏳ Pendent |
| Pestanya "Coneixement" a `/portal/agents` | ⏳ Pendent |
| Preview + Test de resposta | ⏳ Pendent |
| Migració a pgvector (v2) | ⏳ Futur |

---

## 13. Notes per al setup inicial d'un client nou

Quan AMG configura un client nou, el flux és:

1. **Reunió de recollida d'informació** (15-30 min) — repassar totes les seccions de la secció 3 d'aquest spec amb el client
2. **Emplenar el wizard** — l'admin d'AMG omple el wizard al portal
3. **Fer el test de resposta** — enviar 5-10 preguntes habituals del sector
4. **Ajustar** — corregir respostes incorrectes o millorar els textos
5. **Activar el bot** — el client comença a rebre respostes de la IA

**Temps estimat de setup:** 1-2 hores per a un negoci típic.
**Temps de manteniment mensual:** 15-30 min (actualitzar preus, horaris, serveis nous).
