# Spec 22 — Sector Pricing (NexeLocal Model)

## Context de negoci

NexeLocal és el model de preus basat en sector professional i mida d'empresa. Els clients contracten serveis digitals en 5 fases acumulatives (F1 → F5), cadascuna afegint noves capacitats. El preu mensual depèn del sector i la mida del negoci, no de serveis individuals.

### Fases de servei

| Fase | Nom | Contingut |
|------|-----|-----------|
| F1 | Comunicació 24/7 | WhatsApp/Telegram/Email automatitzat, web bàsica |
| F2 | Gestió de cites | Agenda digital, recordatoris automàtics, cancel·lacions |
| F3 | Pressupostos | Generació PDF, seguiment d'ofertes, acceptació digital |
| F4 | Fidelització | Seguiment postvenda, reactivació de clients, sol·licitud de resenyes |
| F5 | Equip | Coordinació d'empleats, partes diaris, comunicació interna |

Les fases són acumulatives: F3 inclou F1+F2+F3. El preu facturat és el bundle `monthly{phaseBundle}` de `SectorPricing`, no la suma de serveis individuals.

### Mides d'empresa

| Valor | Descripció |
|-------|-----------|
| AUTONOMO | 1 persona |
| PETIT | 2-3 persones |
| MITJA | 3-5+ persones |

---

## Model de dades

### Camps nous a `Tenant`

```java
BusinessSector sector;          // nullable, EnumType.STRING, length 30
BusinessSize   businessSize;    // nullable, column: business_size, length 20
ServicePhase   contractedPhase; // nullable, column: contracted_phase, length 5
```

### Entitat `SectorPricing`

Taula: `sector_pricing`
Constraint única: `(sector, business_size)`

| Camp | Tipus | Restricció |
|------|-------|-----------|
| id | UUID | PK, generada |
| sector | BusinessSector | NOT NULL |
| businessSize | BusinessSize | NOT NULL |
| setupPrice | BigDecimal(10,2) | NOT NULL |
| monthlyF1 | BigDecimal(10,2) | NOT NULL |
| monthlyF1f2 | BigDecimal(10,2) | NOT NULL (col: monthly_f1f2) |
| monthlyF1f2f3 | BigDecimal(10,2) | NOT NULL (col: monthly_f1f2f3) |
| monthlyComplete | BigDecimal(10,2) | NOT NULL (col: monthly_complete) |
| createdAt | Instant | @CreatedDate |
| updatedAt | Instant | @LastModifiedDate |

---

## Enums

### `BusinessSector`
```
PINTOR, ELECTRICISTA, FONTANER, JARDINER, NETEJA,
FISIOTERAPEUTA, PSICOLEG, NUTRICIONISTA,
PERRUQUERIA, ESTETICA,
GESTORIA, ACADEMIA,
TALLER_MECANIC, VETERINARI, PERRUQUERIA_CANINA
```

### `BusinessSize`
```
AUTONOMO, PETIT, MITJA
```

### `ServicePhase`
```
F1, F2, F3, F4, F5
```

---

## Matriu de preus (NexeLocal)

### Serveis a la llar

| Sector | Mida | Setup | F1 | F1+F2 | F1+F2+F3 | Complet |
|--------|------|------|----|-------|---------|--------|
| PINTOR | AUTONOMO | 150 | 59 | 79 | 99 | 129 |
| PINTOR | PETIT | 250 | 79 | 99 | 129 | 159 |
| ELECTRICISTA | AUTONOMO | 150 | 59 | 79 | 99 | 129 |
| ELECTRICISTA | PETIT | 250 | 79 | 99 | 129 | 159 |
| FONTANER | AUTONOMO | 150 | 59 | 79 | 99 | 129 |
| JARDINER | AUTONOMO | 150 | 59 | 79 | 99 | 129 |
| NETEJA | AUTONOMO | 150 | 59 | 79 | 99 | 129 |
| NETEJA | MITJA | 300 | 89 | 119 | 149 | 179 |

### Salut i benestar

| Sector | Mida | Setup | F1 | F1+F2 | F1+F2+F3 | Complet |
|--------|------|------|----|-------|---------|--------|
| FISIOTERAPEUTA | AUTONOMO | 175 | 69 | 89 | 109 | 139 |
| FISIOTERAPEUTA | PETIT | 275 | 99 | 129 | 169 | 199 |
| FISIOTERAPEUTA | MITJA | 375 | 129 | 169 | 229 | 279 |
| PSICOLEG | AUTONOMO | 175 | 69 | 89 | 109 | 139 |
| PSICOLEG | PETIT | 300 | 109 | 149 | 189 | 229 |
| NUTRICIONISTA | AUTONOMO | 175 | 59 | 79 | 99 | 129 |

### Estètica

| Sector | Mida | Setup | F1 | F1+F2 | F1+F2+F3 | Complet |
|--------|------|------|----|-------|---------|--------|
| PERRUQUERIA | AUTONOMO | 150 | 59 | 79 | 99 | 129 |
| PERRUQUERIA | PETIT | 300 | 99 | 139 | 179 | 219 |
| ESTETICA | AUTONOMO | 150 | 59 | 79 | 99 | 129 |
| ESTETICA | MITJA | 350 | 109 | 149 | 189 | 229 |

### Professionals

| Sector | Mida | Setup | F1 | F1+F2 | F1+F2+F3 | Complet |
|--------|------|------|----|-------|---------|--------|
| GESTORIA | AUTONOMO | 200 | 69 | 89 | 109 | 139 |
| GESTORIA | MITJA | 400 | 109 | 149 | 199 | 249 |
| ACADEMIA | AUTONOMO | 175 | 59 | 79 | 99 | 129 |
| ACADEMIA | MITJA | 300 | 99 | 139 | 169 | 209 |

### Automoció i mascotes

| Sector | Mida | Setup | F1 | F1+F2 | F1+F2+F3 | Complet |
|--------|------|------|----|-------|---------|--------|
| TALLER_MECANIC | PETIT | 150 | 59 | 79 | 99 | 129 |
| TALLER_MECANIC | MITJA | 275 | 89 | 119 | 149 | 179 |
| VETERINARI | AUTONOMO | 175 | 69 | 89 | 109 | 139 |
| VETERINARI | PETIT | 325 | 109 | 149 | 189 | 229 |
| PERRUQUERIA_CANINA | AUTONOMO | 150 | 49 | 69 | 89 | 119 |

---

## API

### `GET /api/v1/pricing`
- Auth: authenticated
- Retorna: llista de tots els `SectorPricingResponse` ordenats per sector i mida

### `GET /api/v1/pricing/lookup?sector=FISIOTERAPEUTA&size=AUTONOMO`
- Auth: authenticated
- Retorna: `SectorPricingResponse` o 404 si no trobat
- El DTO inclou `forPhase(String phase)` que retorna `PhaseLookup(setup, monthly)` per al bundle correcte

---

## Regla de facturació

El billing usa `SectorPricing.monthly{phaseBundle}` en funció de la fase contractada:

| contractedPhase | Camp a usar |
|----------------|------------|
| F1 | monthlyF1 |
| F2 | monthlyF1f2 |
| F3 | monthlyF1f2f3 |
| F4 o F5 | monthlyComplete |
| null | monthlyComplete |

---

## Checklist d'implementació

- [x] Enum `BusinessSector` (15 valors)
- [x] Enum `BusinessSize` (AUTONOMO, PETIT, MITJA)
- [x] Enum `ServicePhase` (F1-F5)
- [x] Entitat JPA `SectorPricing` amb constraint única (sector, business_size)
- [x] `SectorPricingRepository` amb `findBySectorAndBusinessSize` i `findAllByOrderBySectorAscBusinessSizeAsc`
- [x] `SectorPricingSeeder` @Order(2) @Profile("!test") — 27 entrades
- [x] DTO `SectorPricingResponse` amb nested record `PhaseLookup` i mètode `forPhase`
- [x] `PricingController` GET /pricing i GET /pricing/lookup
- [x] `Tenant` +sector, +businessSize, +contractedPhase (nullable)
- [x] `TenantResponse` +sector, +businessSize, +contractedPhase
- [x] `CreateTenantRequest` +sector, +businessSize, +contractedPhase
- [x] `UpdateTenantRequest` +sector, +businessSize, +contractedPhase
- [x] `TenantService` mapeja els nous camps a create, update i toResponse
- [ ] Migració Flyway (V22__add_sector_pricing.sql)
- [ ] Tests d'integració per a PricingController
- [ ] Frontend: selector de sector/mida a la creació/edició de tenant
