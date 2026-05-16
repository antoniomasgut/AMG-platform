# Mòdul 17: Service Guides — Guia d'ús de serveis per al client

> **Versió:** 1.0
> **Data:** 2026-05-15
> **Autor:** [per determinar]
> **Dependències:** Mòdul 01 (Auth), Mòdul 02 (Vault), Mòdul 13 (i18n)

---

## 1. Objectius

- Proporcionar una guia pràctica per a cada servei implantat, explicant al client com utilitzar-lo.
- Mostrar la guia automàticament quan un servei passa a estat `READY_FOR_DELIVERY` al Vault.
- Centralitzar totes les guies en una secció accessible des del dashboard del client.
- Incloure credencials, instruccions d'accés, captures i resolució de problemes bàsics.
- Reduir el nombre de consultes de suport ("Com accedeixo al meu SMTP?", "On està la meva landing?").

---

## 2. Abast

### 2.1 Funcionalitats incloses

- **ServiceGuide** — component que renderitza la guia d'un servei específic amb:
  - Títol i descripció del servei
  - Estat del servei (READY_FOR_DELIVERY, CONFIGURING, etc.)
  - Seccions dinàmiques segons el tipus de servei (accés, credencials, configuració, FAQ)
  - Credencials en clar per a ADMIN/SUPER_ADMIN, emmascarades per a CLIENT (ja gestionat pel Vault)
- **ServiceGuidesList** — llistat de totes les guies dels serveis implantats al tenant
- **GuidesSection** — secció al dashboard del client que mostra les guies dels serveis marcats com a READY_FOR_DELIVERY
- **Notificació** al dashboard quan hi ha un servei recentment entregat (ready for delivery)
- Contingut de guia per a cada tipus de servei, definit en un fitxer de configuració (no requereix BD)
- Traduccions als 4 idiomes (ca, es, en, de)

### 2.2 Funcionalitats excloses

- Editor de guies al panell d'admin (el contingut de cada guia es defineix al codi, no és dinàmic)
- Guies personalitzades per client (tots els clients amb el mateix servei veuen la mateixa guia)
- Vídeos tutorials integrats (només text + enllaços)
- Sistema de feedback ("T'ha estat útil aquesta guia?")
- Múltiples versions d'una guia per tipus de servei

### 2.3 Actors

| Actor | Descripció | Comportament |
|-------|-----------|--------------|
| CLIENT | Usuari final del tenant | Veu les guies dels seus serveis `READY_FOR_DELIVERY`. Les credencials es mostren emmascarades. |
| ADMIN | Personal operatiu | Veu totes les guies del tenant amb credencials en clar. Pot accedir a la configuració del servei des de la guia. |
| SUPER_ADMIN | Propietari de la plataforma | Com ADMIN. |

---

## 3. Font de dades

Les guies NO tenen entitat pròpia a la BD. El contingut de cada guia es defineix en un fitxer de configuració al frontend:

```
frontend/src/config/service-guides/
├── index.ts              ← Mapa: serviceType → guide config
├── LANDING.ts            ← Guia per a serveis de tipus LANDING
├── WHATSAPP.ts           ← Guia per a serveis de tipus WHATSAPP
├── SMTP.ts               ← Guia per a serveis de tipus SMTP
├── BOT_IA.ts             ← Guia per a bot IA bàsic
├── BOT_IA_RAG.ts         ← Guia per a bot IA avançat (RAG)
├── AUTOMATION.ts         ← Guia per automatitzacions n8n
└── DOMAIN.ts             ← Guia per a dominis
```

Cada fitxer exporta un objecte amb la configuració de la guia:

```typescript
interface ServiceGuideConfig {
  type: string;                     // 'LANDING' | 'WHATSAPP' | 'SMTP' | ...
  titleKey: string;                 // clau i18n per al títol
  descriptionKey: string;           // clau i18n per a la descripció curta
  sections: GuideSection[];         // seccions de la guia
  faq?: GuideFAQ[];                 // preguntes freqüents específiques del servei
}

interface GuideSection {
  id: string;
  titleKey: string;
  type: 'text' | 'credentials' | 'link' | 'steps' | 'warning' | 'info';
  content: string | string[];       // text o llista de passos (claus i18n)
}

interface GuideFAQ {
  questionKey: string;
  answerKey: string;
}
```

**Per què al codi i no a la BD?**
- El contingut de les guies canvia poc i no requereix personalització per client.
- Les credencials dinàmiques (contrasenyes, URLs) es carreguen del Vault, no estan a la guia.
- Evita crear una entitat nova i un CRUD d'admin per a contingut que és essencialment estàtic.

---

## 4. Rutes del frontend

```
/portal/serveis                    → ServiceGuidesList — llistat de tots els serveis amb guia
/portal/serveis/[serviceType]      → ServiceGuide — guia d'un tipus de servei específic
/portal/serveis/[serviceType]/[serviceId] → ServiceGuide — guia d'una instància específica amb credencials
```

El dashboard inclou una secció:

```
/portal                            → portal/page.tsx (dashboard)
                                     └── si hi ha serveis READY_FOR_DELIVERY:
                                       └── <ServiceDeliveryAlert /> + <GuidesSection />
```

---

## 5. Components del frontend

### 5.1 `ServiceGuide` (`src/components/guides/ServiceGuide.tsx`)

Component principal que renderitza la guia completa d'un servei.

**Props:**
```typescript
interface ServiceGuideProps {
  serviceType: string;                    // 'LANDING', 'WHATSAPP', etc.
  serviceId?: string;                     // opcional — si es passa, carrega credencials reals
  tenantId: string;
  userRole: 'SUPER_ADMIN' | 'ADMIN' | 'CLIENT';
}
```

**Lògica interna:**
1. Carrega la config de la guia des de `service-guides/index.ts` segons `serviceType`
2. Si `serviceId` està present, carrega les dades del servei via `getTenantSetup()` o un endpoint específic
3. Renderitza les seccions de la guia
4. Per a seccions de tipus `credentials`, carrega els valors del Vault (emmascarats si CLIENT)

**Seccions dinàmiques per tipus de servei:**

| Tipus | Seccions |
|-------|----------|
| LANDING | Descripció, Accés (URL de la landing), Com editar, Com publicar, FAQ |
| WHATSAPP | Descripció, Credencials (API Key, Webhook URL), Configuració, Provar connexió, FAQ |
| SMTP | Descripció, Credencials (Servidor, Port, Usuari, Contrasenya), Configuració clients, FAQ |
| BOT_IA | Descripció, Accés (URL del bot), Com configurar respostes, Com entrenar, FAQ |
| BOT_IA_RAG | Descripció, Accés, Com afegir documents, Com provar, FAQ |
| AUTOMATION | Descripció, Workflows actius, Com gestionar, Enllaç a n8n, FAQ |
| DOMAIN | Descripció, DNS (nameservers), Configuració, Temps de propagació, FAQ |

**Estats:**
| Estat | Què es mostra |
|-------|--------------|
| Loading | Skeleton de la guia |
| Guide not found | Missatge: "Guia no disponible per a aquest servei" |
| Service not found | Missatge: "Servei no trobat" + enllaç al llistat |
| Ready | Guia completa amb seccions |
| No credentials | Secció de credencials amb missatge "Credencials no configurades" |

### 5.2 `ServiceGuidesList` (`src/components/guides/ServiceGuidesList.tsx`)

Llistat de tots els serveis del tenant que tenen guia disponible.

**Props:**
```typescript
interface ServiceGuidesListProps {
  tenantId: string;
  userRole: string;
}
```

**Layout:**
```
Serveis disponibles
┌──────────────────────────────────┐
│ 🌐 Landing Pro                  │
│ Publicada · https://...         │
│ [Veure guia →]                  │
├──────────────────────────────────┤
│ 💬 WhatsApp Business            │
│ Configurat · Actiu              │
│ [Veure guia →]                  │
├──────────────────────────────────┤
│ 📧 SMTP Corporatiu              │
│ Configurat · Actiu              │
│ [Veure guia →]                  │
└──────────────────────────────────┘
```

### 5.3 `GuidesSection` (`src/components/guides/GuidesSection.tsx`)

Secció que s'integra al dashboard per mostrar els serveis recentment entregats.

```
Serveis actius
┌─────────────────────────────────────┐
│ 🆕 WhatsApp Business — Llest!       │
│   Tens el servei actiu des de fa 2 dies│
│   [Veure guia d'ús →]               │
├─────────────────────────────────────┤
│ 📧 SMTP Corporatiu — Llest!         │
│   Tens el servei actiu des de fa 5 dies│
│   [Veure guia d'ús →]               │
└─────────────────────────────────────┘
```

Es mostra quan:
- El tenant té serveis amb estat `READY_FOR_DELIVERY`
- Es limita als **3 més recents**, amb enllaç "Veure tots els serveis"

### 5.4 `ServiceDeliveryAlert` (`src/components/guides/ServiceDeliveryAlert.tsx`)

Notificació al dashboard quan hi ha serveis recentment entregats.

```
┌────────────────────────────────────────────┐
│ 🎉 Tens nous serveis actius!               │
│                                            │
│ ✅ WhatsApp Business — Llest des de hui    │
│ ✅ SMTP Corporatiu — Llest des de dimarts  │
│                                            │
│ [Veure guies]                              │
└────────────────────────────────────────────┘
```

Es mostra **una sola vegada** per servei. Es guarda a `localStorage` la data de `lastDeliverySeen` per saber quins serveis són "nous" (entregats després de l'última visita).

---

## 6. Integració amb Vault

Les dades dels serveis (estat, credencials, URL) es carreguen des de `getTenantSetup()` (Mòdul 02). No es creen endpoints nous.

```typescript
const { data: setup } = useQuery({
  queryKey: ['tenant-setup', tenantId],
  queryFn: () => getTenantSetup(tenantId),
});

// Serveis amb guia disponible = serveis en fases aprovades + serveis directes del perfil
const servicesWithGuides = setup?.profiles.flatMap(profile =>
  profile.phases.flatMap(phase =>
    phase.services
      .filter(s => s.status === 'READY_FOR_DELIVERY')
      .map(s => ({
        serviceId: s.service.id,
        serviceType: s.service.type,
        serviceName: s.service.name,
        status: s.status,
        guide: getGuideConfig(s.service.type),  // del fitxer de configuració
      }))
  )
).filter(s => s.guide !== undefined) ?? [];
```

---

## 7. Contingut de guies (per tipus de servei)

### 7.1 LANDING

| Secció | Tipus | Contingut |
|--------|-------|-----------|
| Accés | link | URL de la landing (des del Vault) |
| Com editar | steps | 1. Ves al portal 2. Obre l'editor 3. Modifica 4. Desa |
| Com publicar | steps | 1. Edita el contingut 2. Previsualitza 3. Publica |
| Dominis | text | Explicació de com funciona el domini personalitzat |
| FAQ | faq | "Com canvio el color?", "Com afegeixo una secció?", etc. |

### 7.2 WHATSAPP

| Secció | Tipus | Contingut |
|--------|-------|-----------|
| Accés | text | El servei WhatsApp Business API està integrat al vostre portal |
| Com funciona | text | Explicació del flux: formulari web → notificació WhatsApp |
| Credencials API | credentials | API Key, Webhook URL (només ADMIN/SUPER_ADMIN) |
| Provar | link | Enllaç per provar l'envió des del panell admin |
| FAQ | faq | "Per què no rebo missatges?", "Com canvio el número?", etc. |

### 7.3 SMTP

| Secció | Tipus | Contingut |
|--------|-------|-----------|
| Credencials | credentials | Servidor, Port, Usuari, Contrasenya, Seguretat (TLS/SSL) |
| Configuració Outlook | steps | 1. Obre Configuració 2. Afegeix compte 3. Introdueix dades |
| Configuració Gmail | steps | 1. Configuració 2. Veure totes 3. Afegeix compte 4. SMTP |
| Configuració Thunderbird | steps | 1. Fitxer 2. Nou 3. Correu 4. Configuració manual |
| Provar envió | link | Enllaç al panell per enviar un correu de prova |
| FAQ | faq | "Per què no puc enviar correus?", "Quin port usar?", etc. |

### 7.4 BOT_IA / BOT_IA_RAG

| Secció | Tipus | Contingut |
|--------|-------|-----------|
| Accés | link | URL del bot (ex: `https://chat.amg.cat/nom-bot`) |
| Com funciona | text | Explicació de com interactuar amb el bot |
| Gestionar respostes | link | Enllaç al panell de gestió de coneixement |
| FAQ | faq | "Com afegeixo més informació?", "Per què no respon bé?", etc. |

### 7.5 AUTOMATION

| Secció | Tipus | Contingut |
|--------|-------|-----------|
| Accés | link | Enllaç a n8n (ex: `https://n8n.amg.cat`) |
| Workflows | text | Llistat de workflows actius i la seva funció |
| Com gestionar | steps | 1. Accedeix a n8n 2. Inicia sessió 3. Gestiona workflows |
| FAQ | faq | "El workflow ha fallat, què faig?", "Puc afegir noves automatitzacions?", etc. |

### 7.6 DOMAIN

| Secció | Tipus | Contingut |
|--------|-------|-----------|
| DNS | credentials | Nameservers a configurar al registrador de domini |
| Configuració | steps | 1. Accedeix al teu registrador 2. Canvia nameservers 3. Espera propagació |
| Temps propagació | info | La propagació pot trigar entre 24h i 72h |
| Verificació | link | Enllaç per verificar l'estat del domini |
| FAQ | faq | "Quant triga?", "El meu web no es veu", etc. |

---

## 8. Rutes i navegació

```
Dashboard
├── [GuidesSection] ← serveis READY_FOR_DELIVERY
├── [ServiceDeliveryAlert] ← notificació nous serveis
└── "Veure tots els serveis" → /portal/serveis

/portal/serveis
├── Llistat de tots els serveis del tenant amb guia
├── Cada targeta → /portal/serveis/[serviceType]/[serviceId]
└── Botó "Veure guia" per a cada servei

/portal/serveis/[serviceType]/[serviceId]
├── Guia completa del servei
├── Credencials (en clar o emmascarades segons rol)
└── Enllaç a configuració (ADMIN/SUPER_ADMIN)
```

---

## 9. Claves i18n

```json
{
  "guides": {
    "title": "Serveis",
    "subtitle": "Guies d'ús dels teus serveis actius",
    "no_services": "Encara no tens serveis actius",
    "no_guides": "Guia no disponible per a aquest servei",
    "service_not_found": "Servei no trobat",
    "view_guide": "Veure guia",
    "view_all": "Veure tots els serveis",
    "new_services_title": "Tens nous serveis actius!",
    "new_services_cta": "Veure guies",
    "section_credentials": "Credencials",
    "section_access": "Accés",
    "section_setup": "Configuració",
    "section_troubleshoot": "Resolució de problemes",
    "section_howto": "Com utilitzar-ho",
    "no_credentials": "Credencials no configurades",
    "status": "Estat",
    "status_ready": "Actiu",
    "status_configuring": "En configuració",
    "status_pending": "Pendent",
    "last_delivery": "Llest des de {date}"
  }
}
```

---

## 10. Seguretat

- Les credencials es carreguen del Vault, que ja aplica emmascarament segons el rol
- No es guarden credencials al localStorage ni al codi de les guies
- Les guies només es mostren per a serveis del tenant de l'usuari autenticat
- `localStorage` només guarda dates de "vist" (`deliverySeen_<serviceId>`), no dades sensibles

---

## 11. Tests QA

### 11.1 Tests unitaris

| ID | Descripció | Assert |
|----|-----------|--------|
| SG-01 | Renderitza guia completa per a LANDING | Totes les seccions de LANDING visibles |
| SG-02 | Renderitza guia completa per a WHATSAPP | Totes les seccions de WHATSAPP visibles |
| SG-03 | Credencials emmascarades per CLIENT | Valors mostrats com "••••••••" |
| SG-04 | Credencials en clar per ADMIN | Valors mostrats sense emmascarar |
| SG-05 | Guia no disponible mostra missatge | "Guia no disponible per a aquest servei" |
| SG-06 | ServiceGuidesList mostra només serveis amb guia | Cada element té guia configurada |
| SG-07 | GuidesSection al dashboard mostra 3 més recents | Màxim 3 targetes |
| SG-08 | ServiceDeliveryAlert no es mostra si no hi ha nous serveis | Component no visible |
| SG-09 | ServiceDeliveryAlert es mostra amb serveis nous | Alerta visible amb llista de serveis |
| SG-10 | GuidesSection buida si cap servei READY_FOR_DELIVERY | "Encara no tens serveis actius" |

### 11.2 Tests d'integració (Playwright)

| ID | Descripció | Steps | Assert |
|----|-----------|-------|--------|
| SG-E1 | CLIENT veu guia d'una landing entregada | Login CLIENT, anar a /portal/serveis/LANDING/{id} | Guia visible, credencials emmascarades |
| SG-E2 | ADMIN veu guia amb credencials en clar | Login ADMIN, anar a /portal/serveis/LANDING/{id} | Guia visible, credencials en clar |
| SG-E3 | Llistat de serveis mostra només actius | Tenant amb LANDING + WHATSAPP actius | 2 targetes al llistat |
| SG-E4 | Navegació des del dashboard a guia | Dashboard → clic "Veure guies" | Arriba a /portal/serveis |
| SG-E5 | Servei no disponible mostra 404 | Navegar a /portal/serveis/INEXISTENT/123 | Missatge "Guia no disponible" |

---

## 12. Dependències

| Mòdul | Motiu | Tipus |
|-------|-------|-------|
| Mòdul 01 — Auth | tenantId, rol per emmascarament | Forta |
| Mòdul 02 — Vault | `getTenantSetup()` per dades de serveis i credencials | Forta |
| Mòdul 13 — i18n | Traduccions als 4 idiomes | Forta |

---

## 13. Criteris d'acceptació

- [ ] Cada tipus de servei té una guia d'ús amb seccions rellevants
- [ ] Les guies es mostren automàticament quan un servei arriba a `READY_FOR_DELIVERY`
- [ ] Les credencials es mostren emmascarades per a CLIENT i en clar per a ADMIN/SUPER_ADMIN
- [ ] El dashboard mostra una secció amb els serveis actius (màxim 3)
- [ ] El dashboard mostra una alerta de "nous serveis" si n'hi ha d'acabats d'entregar
- [ ] La navegació és: Dashboard → Llistat → Guia detall
- [ ] El contingut de les guies està traduït als 4 idiomes (ca, es, en, de)
- [ ] El llistat de serveis només mostra serveis amb guia disponible
