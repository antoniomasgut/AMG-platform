---
name: sessio-progres
description: Registre de progrés per sessió del workspace portalAMGv3-claude
metadata:
  node_type: memory
  type: reference
  originSessionId: 36b6fec1-512f-4a98-b5a6-95597f0469d0
---

# Registre de progrés del workspace AMG

Aquest document recull el progrés de cada sessió. Cada entrada resumeix el que s'ha fet i deixa clar per on seguir.

---

## Sessió 2026-05-13 "Mòduls 05 Factory + 06 Assets"

### Què s'ha fet
1. **Spec 06-assets.md (v1.0)**: Creada — gestió d'imatges i fitxers (Asset entity, 5 endpoints, filesystem storage, thumbnails)
2. **Spec 05-factory.md (v1.1)**: Actualitzada — API paths corregits a `/api/v1/engine/...`, rutes frontend sense `[locale]`, permisos CLIENT afegits, referència a 06-assets
3. **Spec serveis-cataleg.md (v2.0)**: Catàleg complet de serveis amb costos (setup + 10€/mes/servei), 3 fases, tarifa enginyeria 50€/h
4. **EngineController**: Versions obertes a CLIENT amb verificació de tenant
5. **Mòdul 06 Assets (backend)**: Implementació completa
   - `Asset` entity + `AssetRepository`
   - `AssetOrchestrator` amb emmagatzematge filesystem per tenant, generació de miniatures (ImageIO), validació MIME + magic bytes
   - `AssetController` amb 5 endpoints (upload, list, serveFile, serveThumbnail, delete)
   - Config `StorageConfig`, SecurityConfig actualitzat
   - 9 tests d'integració — tots verds
6. **Mòdul 05 Factory (frontend)**: Implementació completa
   - `services/factory.ts` + `services/assets.ts` — API calls Engine + Assets
   - `store/editor.ts` — Zustand store per l'estat de l'editor
   - Components: `BlockCatalog`, `BlockProperties`, `PageStylesPanel`, `FactoryCanvas`, `BlockRenderer`, `PreviewToolbar`, `FactoryLayout`, `ImagePicker`, `TemplateSelector`
   - Rutes: `/portal/landings`, `/portal/landings/new`, `/portal/landings/[id]/edit`, `/portal/landings/[id]/preview`
   - Sidebar del portal actualitzat amb enllaç a Landings
   - Dependències: `@tanstack/react-query` + `zustand` instal·lades
   - `Providers.tsx` amb QueryClientProvider
   - Build exitós

### Mòduls completats
- **Mòdul 01** — Auth — ✅
- **Mòdul 02** — Vault (AES-256) — ✅
- **Mòdul 03** — Leads CRM — ✅
- **Mòdul 04** — Engine (Landing Renderer) — ✅
- **Mòdul 05** — Factory (Landing Editor) — ✅
- **Mòdul 06** — Assets (Storage) — ✅
- **Mòdul 07** — Billing — ✅
- **Mòdul 13** — i18n + SEO + RGPD — ✅
- **Mòdul 14** — Admin Frontend — ✅

### Per a la propera sessió
- Mòdul 08 (FinOps — Holded, domain renewals, server expenses)
- Mòdul 09 (Payments — Stripe)
- Mòdul 10 (Automations — n8n)
- Mòdul 11 (Ops & Health — monitoring)
- Mòdul 12 (Prospecting)
- Mòdul de Comunicacions (WhatsApp/Telegram/Email adapters)
- Connectar login real amb backend (substituir mock data del portal)
- Crear `CatalogSeedInitializer.java` per sembrar el catàleg de serveis
