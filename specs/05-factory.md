# Mòdul 05: Factory — Landing Editor

> **Versió:** 1.1
> **Data:** 2026-05-13
> **Dependències:** Mòdul 01 (Auth), Mòdul 04 (Engine), Mòdul 06 (Assets)
>
> **Canvis v1.1:**
> - Corregides rutes API a `/api/v1/engine/...` (vs `/engine/...`)
> - Rutes frontend corregides: ara usen `/[locale]/portal/...` (Mòdul 13 i18n)
> - Afegits permisos CLIENT per a endpoints de versions (edició d'esborranys)
> - Referència a `specs/06-assets.md`

---

## 1. Objectius

- Proporcionar un editor visual de landings al portal del client
- Permetre seleccionar, ordenar i configurar blocs de contingut
- Gestionar estils globals (colors, fonts, espaiat)
- Previsualitzar la landing en temps real (escriptori + mòbil)
- Publicar landings des de l'editor amb un clic
- Gestionar imatges i assets de la landing

---

## 2. Abast

### 2.1 Funcionalitats incloses

- Editor visual de landings per blocs al portal
- Catàleg de blocs predefinits (hero, serveis, formulari, FAQ, etc.)
- Configuració de propietats de cada bloc (títol, text, imatges, enllaços)
- Ordenació de blocs (arrossegar per reordenar)
- Estils globals (color primari, secundari, fons, tipografia)
- Previsualització en temps real (escriptori + tauleta + mòbil)
- Publicació / despublicació des de l'editor
- Gestió d'imatges (pujar, seleccionar, retallar)
- Plantilles de landing per començar ràpid
- Historial de versions i retorn a versions anteriors

### 2.2 Funcionalitats excloses

- Render de la landing pública (Mòdul 04 Engine)
- Renderització SSR / cache / CDN (Mòdul 04 Engine)
- Gestió de dominis personalitzats (Mòdul 04 Engine)
- Leads i formularis (Mòdul 03 Leads + Mòdul 04 Engine)
- Automatitzacions post-publicació (Mòdul 10 Automations)

### 2.3 Actors

| Actor | Descripció | Permisos |
|-------|-----------|----------|
| SUPER_ADMIN | Opera totes les landings | Editar qualsevol landing de qualsevol tenant |
| ADMIN | Gestiona landings dels seus clients | Editar landings assignades |
| CLIENT | Propietari de la landing | Editar les seves pròpies landings (no publicar) |

---

## 3. Model de dades

La Factory **no introdueix entitats noves a la BD**. Totes les dades es gestionen a través del Mòdul 04 Engine:

- **Landing** → metadades (títol, slug, domini)
- **LandingVersion** → content (blocs) + styles (estils)
- **Assets** → imatges i fitxers (Mòdul 06 Assets)

La Factory és una capa de presentació que consumeix les APIs de Engine i Assets.

### 3.1 Estat de l'editor (frontend — Zustand)

L'estat de l'editor es gestiona al frontend amb Zustand. No es persisteix a la BD fins que l'usuari guarda.

```typescript
interface EditorState {
  landingId: string;
  versionId: string;
  content: PageContent;
  styles: PageStyles;
  selectedBlockId: string | null;
  isDirty: boolean;
  previewMode: 'desktop' | 'tablet' | 'mobile';

  // Actions
  setBlocks: (blocks: Block[]) => void;
  addBlock: (type: BlockType, index?: number) => void;
  removeBlock: (blockId: string) => void;
  moveBlock: (blockId: string, newIndex: number) => void;
  updateBlockProps: (blockId: string, props: Partial<BlockProps>) => void;
  setStyles: (styles: Partial<PageStyles>) => void;
  selectBlock: (blockId: string | null) => void;
  saveDraft: () => Promise<void>;
  publish: () => Promise<void>;
}
```

### 3.2 Tipus de blocs (TypeScript)

```typescript
type BlockType =
  | 'hero' | 'text' | 'services' | 'gallery' | 'contact-form'
  | 'faq' | 'testimonials' | 'cta' | 'footer' | 'map'
  | 'opening-hours' | 'pricing' | 'team' | 'video' | 'reviews'
  | 'chat-cta' | 'whatsapp-cta';

/**
 * Disponibilitat per tipus de landing:
 * MICRO: hero, text, services, gallery, faq, cta, testimonials, map, footer, contact-form
 * PRO:   tots els anteriors + opening-hours, pricing, team, video, reviews, whatsapp-cta, chat-cta
 *
 * whatsapp-cta (PRO):
 *   - Sense F1: wa.me/{whatsappPersonal} → WhatsApp personal del negoci, resposta manual
 *   - Amb F1:   wa.me/{whatsappBusinessApi} → WhatsApp Business API (Mòdul 27), agent IA + Inbox (Mòdul 25)
 *
 * chat-cta (PRO + F1): chat widget IA incrustat a la landing (Spec 30)
 */
interface Block {
  id: string;
  type: BlockType;
  props: Record<string, unknown>;
}

interface PageContent {
  blocks: Block[];
}

interface PageStyles {
  fontFamily: string;
  primaryColor: string;
  secondaryColor: string;
  bgColor: string;
  textColor: string;
  borderRadius: string;
}
```

### 3.3 Plantilles de landing

Plantilles predefinides per crear landings ràpidament. Es defineixen com a JSON al frontend.

| Plantilla | Blocs inclosos | Ús típic |
|-----------|---------------|-----------|
| `restaurant` | hero, text, services, gallery, testimonials, contact-form, footer | Restaurants i bars |
| `profesional` | hero, services, testimonials, cta, contact-form | Professionals (advocats, metges) |
| `comercio` | hero, services, gallery, contact-form, map, footer | Botigues i comerços locals |
| `evento` | hero, text, gallery, contact-form | Esdeveniments i celebracions |
| `basica` | hero, text, contact-form | Landing mínima |

---

## 4. API REST

La Factory **no té API pròpia**. Consumeix:

| API | Mòdul | Ús |
|-----|-------|-----|
| `POST /api/v1/engine/landings/{lId}/versions` | 04 Engine | Guardar esborrany |
| `PUT /api/v1/engine/landings/{lId}/versions/{vId}` | 04 Engine | Actualitzar esborrany |
| `POST /api/v1/engine/landings/{lId}/publish` | 04 Engine | Publicar |
| `POST /api/v1/engine/landings/{lId}/unpublish` | 04 Engine | Despublicar |
| `GET /api/v1/engine/tenants/{tId}/landings/{id}` | 04 Engine | Carregar landing |
| `POST /api/v1/assets/upload` | 06 Assets | Pujar imatge |
| `GET /api/v1/assets/tenant/{tId}` | 06 Assets | Llistar imatges |

---

## 5. Components del frontend

Estructura de components a `src/components/factory/`:

### 5.1 Layout de l'editor

```
FactoryLayout
├── FactorySidebar (esquerra)
│   ├── BlockCatalog       ← catàleg de blocs per arrossegar
│   ├── BlockProperties    ← propietats del bloc seleccionat
│   └── PageStylesPanel    ← estils globals
├── FactoryCanvas (centre)
│   ├── PreviewToolbar     ← escriptori / tauleta / mòbil
│   ├── BlockRenderer      ← render del bloc (preview)
│   └── BlockDropZone      ← zona per soltar blocs
└── FactoryTopbar (superior)
    ├── LandingInfo        ← nom i estat
    ├── SaveButton
    ├── PublishButton
    └── PreviewButton      ← obrir en nova pestanya
```

### 5.2 Components individuals

| Component | Descripció |
|-----------|-----------|
| `FactoryLayout` | Layout principal de l'editor (3 columnes) |
| `FactorySidebar` | Barra lateral esquerra amb pestanyes |
| `BlockCatalog` | Llista de blocs disponibles per afegir |
| `BlockProperties` | Formulari de propietats del bloc seleccionat |
| `PageStylesPanel` | Configuració d'estils globals (color picker, font selector) |
| `FactoryCanvas` | Àrea central de previsualització |
| `PreviewToolbar` | Canvi entre escriptori / tauleta / mòbil |
| `BlockRenderer` | Render d'un bloc individual en mode editor |
| `BlockDropZone` | Zona buida per arrossegar blocs nous |
| `BlockWrapper` | Wrapper amb controls (moure, editar, eliminar) |
| `ImagePicker` | Selector d'imatges (pujar noves o seleccionar existents) |
| `ColorPicker` | Selector de color amb previsualització |
| `FontSelector` | Selector de fonts Google |
| `SaveStatusIndicator` | Indicador de "guardant..." o "canvis pendents" |
| `TemplateSelector` | Selecció de plantilla inicial |
| `VersionHistory` | Historial de versions amb opció de restaurar |

### 5.3 Rutes del frontend

Rutes dins del grup `/[locale]/portal/` (seguint l'estructura del Mòdul 13 i18n):

```
/[locale]/portal/landings                     → Llistat de landings del tenant
/[locale]/portal/landings/new                 → Selector de plantilla
/[locale]/portal/landings/[id]/edit           → Editor complert
/[locale]/portal/landings/[id]/preview        → Previsualització (pestanya nova)
```

---

## 6. Flux d'usuari

### 6.1 Crear landing nova

1. Usuari va a `/[locale]/portal/landings/new`
2. Selecciona una plantilla (o "des de zero")
3. Es crea la landing via Engine API amb versió esborrany
4. Es redirigeix a `/portal/landings/[id]/edit`

### 6.2 Editar landing

1. Carregar landing + última versió esborrany (o crear-ne una de nova si no n'hi ha)
2. Omplir estat del Zustand amb el contingut
3. L'usuari arrossega blocs, canvia propietats, ajusta estils
4. `isDirty = true` després de cada canvi
5. En clicar "Guardar", es fa PUT a Engine API
6. En clicar "Publicar", es fa POST a Engine publish

### 6.3 Publicar

1. Guarda l'esborrany automàticament
2. Crida `POST /api/v1/engine/landings/{id}/publish`
3. Mostra confirmació amb URL pública
4. Opció de "Veure landing" (obre enllaç públic)

---

## 7. Seguretat

### 7.1 Autenticació
- Totes les crides a Engine API van amb JWT
- L'editor només es mostra a usuaris autenticats al portal

### 7.2 Autorització
- CLIENT no pot publicar (només ADMIN / SUPER_ADMIN)
- CLIENT pot editar esborranys (crear i actualitzar versions)
- CLIENT només veu les seves landings
- ADMIN veu landings dels seus tenants

> **Nota d'implementació:** Els endpoints `POST /api/v1/engine/landings/{landingId}/versions` i `PUT /api/v1/engine/landings/{landingId}/versions/{versionId}` al EngineController requereixen `@PreAuthorize` que permeti CLIENT. Actualment estan restringits a `hasAnyRole('SUPER_ADMIN', 'ADMIN')`. Cal canviar-los per permetre CLIENT (verificant que la landing és del seu tenant).

### 7.3 Validació
- Validació de blocs abans de guardar (camps obligatoris, formats)
- Mida màxima del JSON content: 500KB
- Mida màxima d'imatge: 5MB

---

## 8. Tests (QA)

### 8.1 Funcionals

| # | Cas | Resultat |
|---|-----|---------|
| 1 | Carregar editor amb landing existent | Blocs renderitzats correctament |
| 2 | Afegir bloc hero amb títol i imatge | Bloc visible al canvas |
| 3 | Reordenar blocs | Ordre actualitzat al canvas |
| 4 | Canviar color primari | Preview s'actualitza en temps real |
| 5 | Guardar esborrany | 200, versió desada |
| 6 | CLIENT guarda i ADMIN publica | Landing publicada correctament |
| 7 | Canviar a previsualització mòbil | Layout responsiu correcte |
| 8 | Seleccionar plantilla | Blocs de la plantilla carregats |
| 9 | Esborrar bloc | Bloc desapareix del canvas |
| 10 | Obrir editor de landing publicada | Carrega última versió |

### 8.2 Seguretat

| # | Cas | Resultat |
|---|-----|---------|
| 1 | CLIENT intenta publicar | Botó publicar deshabilitat |
| 2 | Usuari sense sessió a ruta /edit | Redirigeix a login |
| 3 | CLIENT veu landing d'altre tenant | 403 o no apareix al llistat |

### 8.3 UX

| # | Cas | Resultat |
|---|-----|---------|
| 1 | Sortir de l'editor amb canvis sense guardar | Confirmació "Tens canvis sense guardar" |
| 2 | Publicar sense guardar abans | Guarda automàticament abans de publicar |
| 3 | Pujar imatge des de l'editor | Imatge disponible al selector |

---

## 9. Dependències

| Mòdul | Dependència | Tipus |
|-------|-----------|-------|
| Mòdul 01 (Auth) | Autenticació al portal | Forta |
| Mòdul 04 (Engine) | CRUD versions, publicar/despublicar | Forta |
| Mòdul 06 Assets | `specs/06-assets.md` | Pujar i seleccionar imatges | Forta |
| Mòdul 13 (i18n) | Traduccions de l'editor als 4 idiomes | Forta |

---

## 10. Obert / Pendents

- [ ] Definir les props exactes de cada bloc (interfície TypeScript completa)
- [ ] Decidir si el BlockRenderer fa servir Tailwind classes dinàmiques o CSS-in-JS
- [ ] Integració amb IA per generar contingut de blocs? (ex: "escriu un hero per a un restaurant")
- [ ] Autoguardat cada 30 segons si hi ha canvis?
- [ ] Mode col·laboratiu? (edició simultània)
