# Spec 24 — Fluxos de Configuració i Activació d'Agents

**Versió:** 1.0  
**Data:** 2026-05-23  
**Estat:** Esborrany  
**Depèn de:** Spec 20 (Agents), Spec 14 (Admin Frontend), Serveis Catàleg

---

## 1. Principi fonamental

> **Un agent es pot configurar quan està ATURAT. En activar-lo, entra en funcionament i la configuració queda bloquejada. El client rep instruccions d'accés.**

Això implica un cicle de vida clar per a cada servei/agent:

```
PENDING → CONFIGURANT → ACTIU → (opcional) ATURAT → CONFIGURANT → ACTIU
```

- **PENDING**: servei assignat al tenant però no configurat
- **CONFIGURANT** (=`isEnabled: false` + status `CONFIGURED`): admin configurant, bot offline
- **ACTIU** (=`isEnabled: true` + status `VERIFIED`): bot online, configuració en mode lectura
- **ATURAT** (=`isEnabled: false` + status `VERIFIED`): bot offline temporalment, config editable

---

## 2. Catàleg d'agents i el seu flux

### 2.1 Bot IA Bàsic (`bot-ia-basic`)

**Descripció:** Chatbot que respon als clients finals via Telegram o WhatsApp. Respostes basades en el prompt del sistema configurat per sector.

**Canals suportats:** Telegram · WhatsApp Twilio · WhatsApp Meta

#### Flux de configuració (admin):

```
1. PENDING
   ↓ Admin obre la card "Agent IA & Canals"
   
2. CONFIGURANT (isEnabled: false)
   ├─ Seleccionar model d'IA (deepseek-chat / claude-*)
   ├─ Escriure o editar prompt del sistema
   ├─ Seleccionar mode de resposta (AUTO / HYBRID / MANUAL)
   ├─ Configurar canal(s):
   │   ├─ Telegram: el webhook ja és actiu per URL ({tenantId})
   │   │            el client haurà d'iniciar conversa per vincular chatId
   │   ├─ WhatsApp Twilio: entrar número E.164 del compte Twilio
   │   └─ WhatsApp Meta: entrar Phone Number ID del compte Meta
   └─ Guardar configuració
   
3. ACTIVAR (admin prem "Activar servei")
   ├─ Sistema: isEnabled = true
   ├─ Sistema: status = VERIFIED
   ├─ Sistema: genera missatge d'instruccions per al client
   └─ Bot ja respon a missatges entrants
   
4. ACTIU (config en mode lectura)
   ├─ Admin pot canviar mode (AUTO/HYBRID/MANUAL) sense aturar
   ├─ Admin NO pot canviar model ni prompt sense aturar primer
   └─ Admin pot veure converses i estadístiques
```

#### Instruccions per al client (enviades en activar):
- **Telegram**: "El vostre bot de Telegram ja és actiu. Compartiu aquest enllaç amb els vostres clients: t.me/AMGDL_Test_Bot"
- **WhatsApp**: "El vostre bot de WhatsApp ja és actiu al número {número}. Els clients us poden escriure directament."

---

### 2.2 Bot IA Avançat RAG (`bot-ia-advanced`)

**Descripció:** Com el bàsic però amb base de coneixement pròpia (documents, FAQ, preus) i memòria de conversa.

**Canals suportats:** Telegram · WhatsApp · Email

#### Flux de configuració (admin):

```
1. PENDING
   ↓
2. CONFIGURANT
   ├─ Tot el flux del Bot IA Bàsic
   ├─ + Pujar documents de coneixement (PDF, TXT, URL)
   │   → indexació vectorial (ChromaDB / pgvector)
   └─ + Definir FAQ en format Q&A

3. ACTIVAR → bot respon amb context de la KB

4. ACTIU
   ├─ Admin pot afegir nous documents sense aturar (addició no-destructiva)
   └─ Admin ha d'aturar per: canviar model, canviar prompt base, re-indexar tot
```

#### Instruccions per al client:
- Igual que Bot IA Bàsic + "Si voleu actualitzar la base de coneixement, contacteu amb el vostre gestor."

---

### 2.3 WhatsApp Business API (`whatsapp-business`)

**Descripció:** Integració del canal WhatsApp sense bot (només recepció/enviament manual o via n8n).

**Nota:** Diferent del "Bot IA + canal WhatsApp". Aquí el canal existeix però no hi ha IA associada.

#### Flux de configuració:

```
1. PENDING
   ↓
2. CONFIGURANT
   ├─ Triar integració: Twilio o Meta directe
   ├─ Si Twilio:
   │   ├─ Entrar número de Twilio (E.164)
   │   ├─ Configurar webhook Twilio → URL del sistema
   │   └─ Test: enviar missatge de prova
   └─ Si Meta:
       ├─ Entrar Phone Number ID
       ├─ Entrar Access Token (permanent)
       └─ Test: verificar webhook GET

3. VERIFICAR (admin prem "Verificar connexió")
   ├─ Sistema crida el canal amb missatge de prova
   ├─ Si OK → status = VERIFIED (però isEnabled = false encara)
   └─ Si KO → estat torna a CONFIGURANT, mostra error

4. ACTIVAR
   ├─ isEnabled = true
   └─ Canal actiu per rebre missatges

5. ACTIU
   └─ Admin NO pot canviar número ni tokens sense aturar
```

#### Instruccions per al client:
- "El vostre WhatsApp Business ja és actiu. Els clients us poden escriure al {número}."
- "Podeu accedir als missatges des del portal: {URL}"

---

### 2.4 Automatització Bàsica (`automatitzacio-basica`)

**Descripció:** Workflow amb 1-2 nodes (ex: formulari → WhatsApp, recordatori d'agenda).

**Nota tècnica:** Implementat amb el motor d'agents del Mòdul 20, NO n8n.

#### Flux de configuració:

```
1. PENDING
   ↓
2. CONFIGURANT
   ├─ Admin selecciona tipus d'automatització:
   │   ├─ RECORDATORI_24H: envia recordatori via WhatsApp/Telegram 24h abans de cita
   │   ├─ FORMULARI_LEADS: captura leads de formulari web → notificació
   │   └─ BENVINGUDA: missatge de benvinguda quan un client s'afegeix
   ├─ Configurar paràmetres específics (hora d'enviament, plantilla de missatge, etc.)
   └─ Test: executar workflow manualment

3. ACTIVAR → workflow actiu (s'executa automàticament)

4. ACTIU
   └─ Admin pot ajustar missatge sense aturar (canvi no-destructiu)
      Admin ha d'aturar per: canviar tipus d'automatització, canviar canal
```

#### Instruccions per al client:
- "La vostra automatització ja és activa. Rebreu notificacions automàtiques a {canal}."

---

### 2.5 Automatització Avançada (`automatitzacio-avancada`)

**Descripció:** Workflow complex (3+ nodes, condicions, API externes — ex: pressupostos PDF).

#### Flux de configuració:

```
1. PENDING
   ↓
2. CONFIGURANT (requereix intervenció manual del tècnic)
   ├─ Admin configura via portal: paràmetres bàsics
   ├─ Tècnic configura el workflow internament
   ├─ Admin verifica el resultat amb el client (estat AWAITING_CLIENT)
   └─ Client confirma → estat CONFIGURED

3. ACTIVAR (admin + client han confirmat)

4. ACTIU
   └─ Canvis = nova fase de configuració (aturar → configurar → activar)
```

---

### 2.6 Landing (`landing`)

**Descripció:** Pàgina web del client.

#### Flux de configuració:

```
1. PENDING
   ↓
2. CONFIGURANT
   ├─ Admin crea/edita landing al Factory (editor)
   ├─ Preview disponible en tot moment
   └─ Landing NO publicada (no accessible via domini del client)

3. ACTIVAR (publicar)
   ├─ Landing accessible via domini o subdomini
   └─ Status = VERIFIED

4. ACTIU
   ├─ Admin pot editar contingut sense aturar (canvis es publiquen immediatament)
   └─ Aturar = despublicar (mostrar pàgina de manteniment)
```

#### Instruccions per al client:
- "La vostra web ja és accessible a: {URL}"

---

### 2.7 SMTP Corporatiu (`smtp-corporatiu`)

**Descripció:** Correu transaccional (SendGrid/Resend).

#### Flux de configuració:

```
1. PENDING
   ↓
2. CONFIGURANT
   ├─ Entrar credencials SMTP (host, port, user, password)
   ├─ Entrar domini del correu (hola@empresaclient.com)
   └─ Test: enviar correu de prova

3. VERIFICAR → si OK, status = VERIFIED

4. ACTIVAR

5. ACTIU → canviar credencials requereix aturar
```

#### Instruccions per al client:
- "El vostre correu corporatiu {email} ja és actiu."

---

### 2.8 Google Analytics (`google-analytics`)

**Descripció:** GA4 + Search Console + GTM.

#### Flux de configuració:

```
1. PENDING
   ↓
2. CONFIGURANT
   ├─ Entrar Measurement ID (G-XXXXXXXXXX)
   ├─ Opcionalment: GTM Container ID
   └─ Test: verificar que el tag és accessible

3. ACTIVAR → tracking actiu

4. ACTIU → canviar ID requereix aturar
```

---

## 3. Canvis necessaris a l'aplicació

### 3.1 Estat "CONFIGURANT" vs "ATURAT"

**Problema actual:** `isEnabled` controla si el bot respon, però no hi ha distinció clara entre "mai configurat" i "configurat però aturat". La lògica de "bloqueja edició quan actiu" no existeix.

**Solució:**

```
TenantService:
  - isEnabled: Boolean  ← ja implementat (toggle on/off)
  
AgentConfigCard (frontend):
  - Quan isEnabled = true: camps en read-only, botó "Aturar per editar"
  - Quan isEnabled = false: camps editables, botó "Activar"
  - Excepció: mode AUTO/HYBRID/MANUAL és editable sempre (no afecta la config tècnica)
```

### 3.2 Botó d'activació amb validació

**Problema actual:** No hi ha validació de "la configuració és completa?" abans d'activar.

**Solució:** Lògica de validació per servei:

| Servei | Validació per activar |
|--------|----------------------|
| Bot IA | Prompt no buit + mínim 1 canal configurat |
| WhatsApp | Número configurat + verificació OK |
| Automatització | Tipus seleccionat + paràmetres complets |
| Landing | Landing creada i té contingut |
| SMTP | Credencials configurades + test OK |
| Analytics | Measurement ID configurat |

### 3.3 Modal d'activació amb instruccions

**Problema actual:** L'activació és silenciosa (toggle ràpid sense feedback).

**Solució:** Quan l'admin activa un servei, mostrar un modal que:
1. Confirma l'activació ("Activar ara?")
2. Mostra les instruccions per al client ("Compartiu això amb el client:")
3. Botó "Copiar instruccions" per enviar-les via WhatsApp/Email
4. Botó "Confirmar i activar"

### 3.4 Camps en read-only quan actiu

**Implementació frontend:**

```tsx
// AgentConfigCard
const isLocked = channels?.isActive === true;

<textarea
  readOnly={isLocked}
  className={isLocked ? 'opacity-60 cursor-not-allowed' : ''}
  ...
/>

{isLocked && (
  <div className="text-xs text-ink-3 flex items-center gap-1">
    <I.Lock size={10} /> Atura el bot per editar la configuració
  </div>
)}
```

### 3.5 Instruccions per canal (backend)

Nou endpoint: `GET /api/v1/agents/conversational/{tenantId}/activation-instructions`

Retorna les instruccions específiques per al client basades en la configuració actual:

```json
{
  "telegram": {
    "active": true,
    "instructions": "Els vostres clients poden parlar amb el bot a t.me/AMGDL_Test_Bot",
    "qrCode": "https://api.qrserver.com/v1/create-qr-code/?data=t.me/AMGDL_Test_Bot"
  },
  "whatsapp": {
    "active": false,
    "instructions": null
  }
}
```

### 3.6 Notificació automàtica al tenant en activar

Quan l'admin activa un servei, el sistema envia un missatge Telegram al tenant (al chatId del TenantChatLink):

```
✅ [NOM SERVEI] activat

El vostre [servei] ja és en línia. 

[instruccions específiques del canal]

Per desactivar-lo temporalment, contacteu amb el vostre gestor.
```

---

## 4. Priorització d'implementació

| Prioritat | Canvi | Complexitat |
|-----------|-------|------------|
| **Alta** | Camps read-only quan `isActive=true` | Baixa (frontend) |
| **Alta** | Validació abans d'activar (prompt no buit, canal configurat) | Baixa |
| **Alta** | Modal d'activació amb instruccions | Mitjana |
| **Mitjana** | Endpoint `activation-instructions` | Baixa (backend) |
| **Mitjana** | Notificació Telegram al tenant en activar | Baixa (backend) |
| **Baixa** | Distinció PENDING vs ATURAT a la UI | Baixa |
| **Baixa** | Validació de verificació WhatsApp (test connexió) | Alta |

---

## 5. Referència d'estats per servei

```
isEnabled=false + status=PENDING    → "Pendent de configuració"
isEnabled=false + status=CONFIGURED → "Configurat — aturat"
isEnabled=false + status=VERIFIED   → "Aturat (prèviament actiu)"
isEnabled=true  + status=VERIFIED   → "✅ Actiu"
isEnabled=true  + status=CONFIGURED → (estat transitori, mai hauria de passar)
```

---

## 6. Mockup del flux d'activació (UI)

```
┌─────────────────────────────────────────────────────┐
│  Agent IA & Canals                            [ATURAT]│
├─────────────────────────────────────────────────────┤
│  Model: DeepSeek V3                                  │
│  Mode:  ● AUTO  ○ HÍBRID  ○ MANUAL                  │
│                                                      │
│  Telegram: ● Configurat (chat 5520713163)            │
│  WhatsApp: ○ No configurat                           │
│                                                      │
│  Prompt:  [Ets un assistent de pintura...]     ✏️    │
│                                                      │
│         [  ▶ ACTIVAR BOT  ]                         │
└─────────────────────────────────────────────────────┘

── Quan s'activa ──

┌─────────────────────────────────────────────────────┐
│  ✅ Bot activat                                      │
│                                                      │
│  Compartiu això amb el vostre client:               │
│  ┌─────────────────────────────────────────────┐   │
│  │ El vostre assistent virtual ja és actiu!    │   │
│  │ Escriviu-nos per Telegram:                  │   │
│  │ 👉 t.me/AMGDL_Test_Bot                      │   │
│  └─────────────────────────────────────────────┘   │
│  [📋 Copiar missatge]  [✓ Tancar]                   │
└─────────────────────────────────────────────────────┘

── Quan està actiu ──

┌─────────────────────────────────────────────────────┐
│  Agent IA & Canals                            [ACTIU]│
├─────────────────────────────────────────────────────┤
│  Model: DeepSeek V3                          🔒     │
│  Mode:  ● AUTO  ○ HÍBRID  ○ MANUAL  ← editable     │
│                                                      │
│  Telegram: ✅ Actiu (chat 5520713163)                │
│                                                      │
│  Prompt:  [Ets un assistent de pintura...]   🔒     │
│           🔒 Atura el bot per editar                │
│                                                      │
│         [  ⏸ ATURAR BOT  ]                         │
└─────────────────────────────────────────────────────┘
```
