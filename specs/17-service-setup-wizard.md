# Mòdul 17: Service Setup Wizard — Assistent gràfic per a la implementació de serveis

> **Versió:** 1.0
> **Data:** 2026-05-15
> **Autor:** [per determinar]
> **Dependències:** Mòdul 01 (Auth), Mòdul 02 (Vault), Mòdul 13 (i18n)

---

## 1. Objectius

- Proporcionar un assistent visual (wizard) que guï l'ADMIN pas a pas durant la implementació de cada servei.
- Mostrar, abans de començar, què es configurarà, quines dades cal preparar i quins són els passos.
- Reduir errors d'implementació guiant l'operador amb instruccions clares i validacions.
- Unificar l'experiència de configuració de tots els tipus de servei sota un patró comú.

---

## 2. Abast

### 2.1 Funcionalitats incloses

- **Pantalla de benvinguda del wizard** per a cada servei: explica què és el servei, què necessitarà l'operador (credencials, API keys, etc.) i quants passos té.
- **Navegació pas a pas** amb botons "Anterior" / "Següent" i indicador de progrés.
- **Passos dinàmics** segons el tipus de servei: cada servei defineix els seus propis passos (ex: WHATSAPP → 1. Credencials API, 2. Webhook, 3. Provar connexió).
- **Integració amb el Vault**: cada pas del wizard es correspon amb accions sobre el Vault (crear credential fields, verificar, avançar fases).
- **Botó "Configurar més tard"** que permet ajornar la configuració i tornar al tenant.
- **Pantalla de confirmació final** amb resum del que s'ha configurat i opció de verificar.
- **Definició de wizards per tipus de servei** en fitxers de configuració (no requereix BD).

### 2.2 Funcionalitats excloses

- Wizards personalitzats per client (tots els clients amb el mateix servei veuen el mateix wizard)
- Wizards per a serveis addon (els addons es configuren dins del wizard del servei pare)
- Editor visual de wizards al panell d'admin

### 2.3 Actors

| Actor | Descripció | Comportament |
|-------|-----------|--------------|
| ADMIN | Personal operatiu que implementa serveis | Veu el wizard en obrir la configuració d'un servei en estat PENDING o CONFIGURING. El wizard li guia fins a completar la configuració. |
| SUPER_ADMIN | Propietari de la plataforma | Com ADMIN. |

CLIENT no veu mai el wizard. El wizard és exclusiu de l'operador que implementa.

---

## 3. Flux principal

Quan un ADMIN accedeix a la configuració d'un servei (des del tenant detail → servei en PENDING/CONFIGURING):

```
1. Pantalla de benvinguda
   ┌─────────────────────────────────────────────┐
   │  Configuració: WhatsApp Business            │
   │                                             │
   │  Què farem?                                 │
   │  Configurarem l'API de WhatsApp Business    │
   │  per al teu client. Necessitaràs:           │
   │   • API Key de WhatsApp Business            │
   │   • Número de telèfon verificat             │
   │   • Webhook URL per rebre missatges         │
   │                                             │
   │  Passos (3):                                │
   │  ░░░░░░░░░░░░░░░░░░░░░░  0 / 3             │
   │                                             │
   │  1. Credencials API      ← pendent          │
   │  2. Configuració webhook  ← pendent         │
   │  3. Verificació           ← pendent         │
   │                                             │
   │  [Configurar més tard]  [Començar →]        │
   └─────────────────────────────────────────────┘

2. Pas 1: Credencials API
   ┌─────────────────────────────────────────────┐
   │  Pas 1 de 3: Credencials API                │
   │                                             │
   │  Introdueix les credencials de l'API de     │
   │  WhatsApp Business:                         │
   │                                             │
   │  API Key:        [_____________________]    │
   │  Phone Number:   [_____________________]    │
   │  Account ID:     [_____________________]    │
   │                                             │
   │  [Anterior]                    [Següent →]  │
   └─────────────────────────────────────────────┘

3. Pas 2: Configuració webhook
   ┌─────────────────────────────────────────────┐
   │  Pas 2 de 3: Configuració webhook           │
   │                                             │
   │  Configura el webhook per rebre missatges:  │
   │                                             │
   │  Webhook URL: [https://...______________]   │
   │                                             │
   │  Copia aquesta URL al teu panell de         │
   │  Meta Developers:                           │
   │  ┌─────────────────────────────────────┐    │
   │  │ https://api.amg.cat/webhook/xxx     │    │
   │  └─────────────────────────────────────┘    │
   │  [📋 Copiar]                                │
   │                                             │
   │  [Anterior]                    [Següent →]  │
   └─────────────────────────────────────────────┘

4. Pas 3: Verificació
   ┌─────────────────────────────────────────────┐
   │  Pas 3 de 3: Verificació                    │
   │                                             │
   │  Prova la connexió amb WhatsApp:            │
   │                                             │
   │  [🔄 Provar connexió]                       │
   │                                             │
   │  Estat: ● Connexió correcta                 │
   │                                             │
   │  [Anterior]           [Finalitzar →]        │
   └─────────────────────────────────────────────┘

5. Pantalla final
   ┌─────────────────────────────────────────────┐
   │  ✅ Configuració completada!                │
   │                                             │
   │  WhatsApp Business configurat correctament: │
   │   ✓ Credencials API guardades               │
   │   ✓ Webhook configurat                      │
   │   ✓ Connexió verificada                     │
   │                                             │
   │  [Tornar al tenant]                         │
   └─────────────────────────────────────────────┘
```

---

## 4. Definició de wizards

Cada tipus de servei defineix el seu wizard en un fitxer de configuració:

```
frontend/src/config/service-wizards/
├── index.ts                    ← Mapa serviceType → wizard config
├── WHATSAPP.ts                 ← Wizard per a WHATSAPP
├── SMTP.ts                     ← Wizard per a SMTP
├── LANDING.ts                  ← Wizard per a LANDING (crear landing des de plantilla)
├── BOT_IA.ts                   ← Wizard per a Bot IA bàsic
├── BOT_IA_RAG.ts               ← Wizard per a Bot IA avançat
├── AUTOMATION.ts               ← Wizard per a automatitzacions n8n
└── DOMAIN.ts                   ← Wizard per a dominis
```

### 4.1 Estructura d'un wizard

```typescript
interface ServiceWizardConfig {
  serviceType: string;
  titleKey: string;                     // Clau i18n: "Configuració: WhatsApp Business"
  descriptionKey: string;               // Clau i18n: explica què es farà
  prerequisitesKey: string;             // Clau i18n: què necessita l'operador (API keys, etc.)
  steps: WizardStep[];
}

interface WizardStep {
  id: string;
  titleKey: string;
  descriptionKey: string;
  type: 'credentials' | 'info' | 'form' | 'verify' | 'copy' | 'link';
  /**
   * Camps del Vault associats a aquest pas.
   * Quan l'usuari omple el formulari, es creen/actualitzen credential fields al Vault.
   */
  fields?: WizardField[];
  /**
   * Acció a executar en completar el pas (opcional).
   * Ex: verify → crida l'endpoint de verificació
   *     advance → avança la fase al Vault
   */
  action?: {
    type: 'verify' | 'advance' | 'approve' | 'none';
    endpoint?: string;                  // endpoint del Vault a cridar
  };
}

interface WizardField {
  id: string;
  labelKey: string;
  type: 'text' | 'password' | 'url' | 'number' | 'select';
  required: boolean;
  placeholderKey?: string;
  hintKey?: string;                     // Ajuda contextual per al camp
  validation?: {
    pattern?: string;                   // Regex per validar
    minLength?: number;
    maxLength?: number;
    messageKey: string;                 // Missatge d'error si no valida
  };
}
```

### 4.2 Exemple: WHATSAPP

```typescript
// frontend/src/config/service-wizards/WHATSAPP.ts
const whatsappWizard: ServiceWizardConfig = {
  serviceType: 'WHATSAPP',
  titleKey: 'wizard.whatsapp.title',
  descriptionKey: 'wizard.whatsapp.description',
  prerequisitesKey: 'wizard.whatsapp.prerequisites',
  steps: [
    {
      id: 'api-credentials',
      titleKey: 'wizard.whatsapp.step1.title',
      descriptionKey: 'wizard.whatsapp.step1.description',
      type: 'credentials',
      fields: [
        {
          id: 'api_key',
          labelKey: 'wizard.whatsapp.field.api_key',
          type: 'password',
          required: true,
        },
        {
          id: 'phone_number',
          labelKey: 'wizard.whatsapp.field.phone',
          type: 'text',
          required: true,
          validation: {
            pattern: '^\\+?[0-9]{7,15}$',
            messageKey: 'wizard.validation.phone',
          },
        },
        {
          id: 'account_id',
          labelKey: 'wizard.whatsapp.field.account_id',
          type: 'text',
          required: true,
        },
      ],
    },
    {
      id: 'webhook',
      titleKey: 'wizard.whatsapp.step2.title',
      descriptionKey: 'wizard.whatsapp.step2.description',
      type: 'copy',
      fields: [
        {
          id: 'webhook_url',
          labelKey: 'wizard.whatsapp.field.webhook_url',
          type: 'url',
          required: false,       // Generat automàticament, només es mostra per copiar
        },
      ],
    },
    {
      id: 'verify',
      titleKey: 'wizard.whatsapp.step3.title',
      descriptionKey: 'wizard.whatsapp.step3.description',
      type: 'verify',
      action: { type: 'verify' },
    },
  ],
};
```

### 4.3 Exemple: SMTP

```typescript
const smtpWizard: ServiceWizardConfig = {
  serviceType: 'SMTP',
  titleKey: 'wizard.smtp.title',
  descriptionKey: 'wizard.smtp.description',
  prerequisitesKey: 'wizard.smtp.prerequisites',
  steps: [
    {
      id: 'server-config',
      titleKey: 'wizard.smtp.step1.title',
      descriptionKey: 'wizard.smtp.step1.description',
      type: 'credentials',
      fields: [
        { id: 'smtp_host', labelKey: 'wizard.smtp.field.host', type: 'text', required: true },
        { id: 'smtp_port', labelKey: 'wizard.smtp.field.port', type: 'number', required: true },
        { id: 'smtp_user', labelKey: 'wizard.smtp.field.user', type: 'text', required: true },
        { id: 'smtp_pass', labelKey: 'wizard.smtp.field.password', type: 'password', required: true },
        {
          id: 'smtp_security',
          labelKey: 'wizard.smtp.field.security',
          type: 'select',
          required: true,
          options: [
            { value: 'TLS', labelKey: 'wizard.smtp.security.tls' },
            { value: 'SSL', labelKey: 'wizard.smtp.security.ssl' },
            { value: 'NONE', labelKey: 'wizard.smtp.security.none' },
          ],
        },
      ],
    },
    {
      id: 'verify',
      titleKey: 'wizard.smtp.step2.title',
      descriptionKey: 'wizard.smtp.step2.description',
      type: 'verify',
      action: { type: 'verify' },
    },
  ],
};
```

### 4.4 Exemple: LANDING

La LANDING no necessita credencials externes, sinó crear el contingut:

```typescript
const landingWizard: ServiceWizardConfig = {
  serviceType: 'LANDING',
  titleKey: 'wizard.landing.title',
  descriptionKey: 'wizard.landing.description',
  prerequisitesKey: 'wizard.landing.prerequisites',
  steps: [
    {
      id: 'select-template',
      titleKey: 'wizard.landing.step1.title',
      descriptionKey: 'wizard.landing.step1.description',
      type: 'form',
      // Selector de plantilla (del mòdul de templates)
    },
    {
      id: 'fill-content',
      titleKey: 'wizard.landing.step2.title',
      descriptionKey: 'wizard.landing.step2.description',
      type: 'form',
      // Omplir contingut per secció segons la plantilla seleccionada
    },
    {
      id: 'publish',
      titleKey: 'wizard.landing.step3.title',
      descriptionKey: 'wizard.landing.step3.description',
      type: 'verify',
      action: { type: 'advance' },
    },
  ],
};
```

---

## 5. Components del frontend

### 5.1 `ServiceWizard` (`src/components/wizard/ServiceWizard.tsx`)

Component principal del wizard.

**Props:**
```typescript
interface ServiceWizardProps {
  tenantId: string;
  serviceId: string;
  serviceType: string;
  serviceName: string;
  onComplete: () => void;            // Es crida en finalitzar
  onCancel: () => void;              // Es crida en "Configurar més tard"
}
```

**Estats interns:**
| Estat | Descripció |
|-------|-----------|
| LOADING | Carregant configuració del wizard i dades del servei |
| WELCOME | Pantalla de benvinguda amb resum de passos |
| STEP_ACTIVE | Pas actiu del wizard (1 de N) |
| STEP_LOADING | Executant acció del pas (ex: verificant) |
| STEP_ERROR | Error en executar l'acció del pas |
| COMPLETE | Tots els passos completats, pantalla de resum |
| CANCELLED | L'usuari ha ajornat la configuració |

**Layout general:**
```
┌─────────────────────────────────────────────┐
│  [indicador de progrés]  Pas X de N         │
│                                             │
│  ┌─────────────────────────────────────┐    │
│  │                                     │    │
│  │   Contingut del pas actual          │    │
│  │   (varia segons el tipus de pas)    │    │
│  │                                     │    │
│  └─────────────────────────────────────┘    │
│                                             │
│  [Anterior]                    [Següent →]  │
│                                             │
│  [Configurar més tard]                      │
└─────────────────────────────────────────────┘
```

### 5.2 `WizardStepRenderer` (`src/components/wizard/WizardStepRenderer.tsx`)

Renderitza el contingut d'un pas segons el seu `type`:

| type | Render |
|------|--------|
| `credentials` | Formulari amb camps de text/password + validació |
| `info` | Text informatiu amb icona |
| `form` | Formulari genèric amb camps definits al wizard |
| `verify` | Botó "Provar connexió" + indicador d'estat |
| `copy` | Camp de text amb botó "Copiar" |
| `link` | Enllaç extern + instruccions |

Cada tipus de pas pot tenir el seu propi estat:

| type | Estats |
|------|--------|
| `credentials` | empty, filling, valid, invalid |
| `verify` | idle, loading, success, error |
| `copy` | idle, copied |

### 5.3 `WizardProgress` (`src/components/wizard/WizardProgress.tsx`)

Indicador de progrés visual.

**Props:**
```typescript
interface WizardProgressProps {
  steps: WizardStepSummary[];     // { id, title, status: 'pending' | 'active' | 'done' | 'error' }
  currentStep: number;
}
```

**Layout:**
```
Pas 1: Credencials  ●━━━━━━○━━━━━━○
Pas 2: Webhook              ●━━━━━○
Pas 3: Verificació                 ●
```

Visual: cercle per a cada pas. El cercle actiu es mostra amb l'accent taronja. Els completats, en verd amb check. Els pendents, en gris.

### 5.4 `WizardWelcome` (`src/components/wizard/WizardWelcome.tsx`)

Pantalla de benvinguda abans de començar.

**Props:**
```typescript
interface WizardWelcomeProps {
  serviceType: string;
  serviceName: string;
  config: ServiceWizardConfig;
  onStart: () => void;
  onCancel: () => void;
}
```

Mostra:
- Nom del servei i icona
- Explicació de què es configurarà
- Llista de requisits previs (qué necessita l'operador)
- Llista de passos amb estats (tots pendents)
- Botó "Començar" i "Configurar més tard"

### 5.5 `WizardComplete` (`src/components/wizard/WizardComplete.tsx`)

Pantalla de confirmació final.

**Props:**
```typescript
interface WizardCompleteProps {
  serviceName: string;
  summary: WizardStepSummary[];    // Resum de cada pas completat
  onFinish: () => void;
}
```

Mostra:
- Icona de completat (check verd)
- "Configuració completada!"
- Resum dels passos realitzats (cada un amb el seu check)
- Botó "Tornar al tenant"

---

## 6. Integració amb Vault

### 6.1 Punt d'entrada

El wizard s'obre des del **tenant detail** quan l'ADMIN fa clic a un servei en estat `PENDING` o `CONFIGURING`:

```typescript
// Tenant detail → servei del Vault
<button onClick={() => openWizard(service)}>
  Configurar {service.name}
</button>
```

També es pot obrir des del **dashboard** si hi ha serveis pendents de configurar (GuidesSection del tenant).

### 6.2 Accions del Vault des del wizard

Cada pas pot executar accions sobre el Vault:

| Acció | Endpoint Vault | Quan |
|-------|---------------|------|
| Guardar credencials | `PUT /tenants/{tId}/services/{sId}/fields/{fId}` | En completar un pas `credentials` |
| Verificar connexió | `POST /tenants/{tId}/services/{sId}/verify` | En un pas `verify` |
| Avançar fase | `POST /tenants/{tId}/phases/{pId}/advance` | Quan el pas implica completar una fase |

### 6.3 Estats del servei durant el wizard

```
PENDING → CONFIGURING (en obrir el wizard)
CONFIGURING → READY_FOR_DELIVERY (en completar l'últim pas)
```

El wizard pot gestionar avançar la fase automàticament si tots els passos s'han completat.

---

## 7. Rutes del frontend

```
/portal/admin/tenants/[id]                 → Tenant detail
  └── Clic a "Configurar {servei}"          → Obre el wizard com a modal o pàgina

/portal/admin/tenants/[id]/services/[serviceId]/setup
  → Pàgina dedicada al wizard de configuració del servei
```

El wizard es pot obrir com a **modal** (superposat al tenant detail) o com a **pàgina independent** (ruta pròpia). La implementació ha de suportar ambdós casos, però la navegació principal serà la pàgina dedicada.

---

## 8. Claus i18n

```json
{
  "wizard": {
    "start": "Començar",
    "next": "Següent",
    "previous": "Anterior",
    "finish": "Finalitzar",
    "cancel": "Configurar més tard",
    "step": "Pas {current} de {total}",
    "verify": "Provar connexió",
    "verify_success": "Connexió correcta",
    "verify_error": "Verificació fallida",
    "verify_loading": "Verificant...",
    "copy": "Copiar",
    "copied": "Copiat!",
    "complete_title": "Configuració completada!",
    "complete_cta": "Tornar al tenant",
    "whatsapp": {
      "title": "Configuració: WhatsApp Business",
      "description": "Configurarem l'API de WhatsApp Business per al teu client.",
      "prerequisites": "Necessitaràs: clau API de WhatsApp Business, número de telèfon verificat i Account ID de Meta.",
      "step1": { "title": "Credencials API", "description": "Introdueix les credencials de l'API de WhatsApp Business." },
      "step2": { "title": "Configuració webhook", "description": "Configura el webhook per rebre missatges." },
      "step3": { "title": "Verificació", "description": "Prova la connexió per assegurar que tot funciona." },
      "field": {
        "api_key": "API Key",
        "phone": "Número de telèfon",
        "account_id": "Account ID",
        "webhook_url": "Webhook URL"
      }
    },
    "smtp": {
      "title": "Configuració: SMTP Corporatiu",
      "description": "Configurarem el servidor SMTP per al correu corporatiu del teu client.",
      "prerequisites": "Necessitaràs: servidor SMTP, port, usuari i contrasenya.",
      "step1": { "title": "Configuració del servidor", "description": "Introdueix les dades del servidor SMTP." },
      "step2": { "title": "Verificació", "description": "Prova l'envió per assegurar que el servidor funciona." },
      "field": {
        "host": "Servidor SMTP",
        "port": "Port",
        "user": "Usuari",
        "password": "Contrasenya",
        "security": "Seguretat"
      }
    },
    "landing": {
      "title": "Configuració: Landing Pro",
      "description": "Crearem la landing page del teu client a partir d'una plantilla.",
      "prerequisites": "Necessitaràs: contingut del client (textos, imatges, xarxes socials).",
      "step1": { "title": "Selecciona plantilla", "description": "Tria la plantilla que millor s'adapti al negoci del client." },
      "step2": { "title": "Omple el contingut", "description": "Personalitza cada secció amb la informació del client." },
      "step3": { "title": "Publica", "description": "Revisa i publica la landing." }
    },
    "validation": {
      "phone": "El número de telèfon no és vàlid",
      "required": "Aquest camp és obligatori",
      "url": "La URL no és vàlida",
      "email": "El correu electrònic no és vàlid"
    }
  }
}
```

---

## 9. Seguretat

- Les credencials s'emmagatzemen al Vault amb xifrat AES-256 (ja implementat al mòdul 02)
- El wizard no guarda dades sensibles al frontend
- Només ADMIN/SUPER_ADMIN poden accedir al wizard
- Les accions del wizard (verify, advance) estan protegides per `@PreAuthorize` al backend

---

## 10. Tests QA

### 10.1 Tests unitaris

| ID | Descripció | Assert |
|----|-----------|--------|
| WZ-01 | Wizard carrega configuració correcta per WHATSAPP | 3 passos, títol correcte |
| WZ-02 | Wizard carrega configuració correcta per SMTP | 2 passos, títol correcte |
| WZ-03 | Pantalla de benvinguda mostra resum de passos | Llista de passos visible, progrés 0/N |
| WZ-04 | Navegació entre passos funciona | Clic Següent → pas 2, clic Anterior → pas 1 |
| WZ-05 | Validació de camps: camp obligatori buit | Error "Aquest camp és obligatori" |
| WZ-06 | Validació de camps: patró incorrecte | Error de validació específic |
| WZ-07 | Botó "Configurar més tard" crida onCancel | onCancel cridat 1 cop |
| WZ-08 | Pantalla de completat mostra resum | Tots els passos marcats amb check |
| WZ-09 | Pas verify mostra loading, success i error | Estats canvien correctament |
| WZ-10 | Pas copy: clic copia al portapapers | navigator.clipboard.writeText cridat |

### 10.2 Tests d'integració (Playwright)

| ID | Descripció | Steps | Assert |
|----|-----------|-------|--------|
| WZ-E1 | Wizard WHATSAPP complet | Tenant detail → Configurar WHATSAPP → omplir credencials → configurar webhook → verificar | Servei passa a READY_FOR_DELIVERY |
| WZ-E2 | Wizard SMTP complet | Tenant detail → Configurar SMTP → omplir servidor → verificar | Servei passa a READY_FOR_DELIVERY |
| WZ-E3 | Cancel·lar wizard no canvia estat | Obrir wizard → clic "Configurar més tard" | Servei segueix en PENDING/CONFIGURING |
| WZ-E4 | Navegació: Anterior no perd dades | Omplir camp al pas 1 → Següent → Anterior | Camp conserva el valor |

---

## 11. Dependències

| Mòdul | Motiu | Tipus |
|-------|-------|-------|
| Mòdul 01 — Auth | tenantId, rol per autorització | Forta |
| Mòdul 02 — Vault | Credentials fields, verify, advance phase | Forta |
| Mòdul 04 — Engine | Crear landing des del wizard (si LANDING) | Forta |
| Mòdul 13 — i18n | Traduccions als 4 idiomes | Forta |

---

## 12. Criteris d'acceptació

- [ ] En obrir la configuració d'un servei, es mostra el wizard amb benvinguda + llista de passos
- [ ] Cada tipus de servei té el seu propi wizard amb passos específics
- [ ] La navegació entre passos és funcional (Anterior/Següent)
- [ ] Les dades introduïdes es guarden al Vault (credential fields)
- [ ] La verificació de connexió funciona i mostra resultat correcte
- [ ] En completar tots els passos, el servei es marca com a READY_FOR_DELIVERY
- [ ] "Configurar més tard" tanca el wizard sense canviar l'estat del servei
- [ ] Els camps requerits tenen validació amb missatge d'error
- [ ] El botó Copiar copia el valor al portapapers
- [ ] Texts disponibles en els 4 idiomes: ca, es, en, de
